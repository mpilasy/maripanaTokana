<!-- SPDX-FileCopyrightText: Orinasa Njarasoa -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
<script lang="ts" module>
	/** The box of a label in a chart, in px. */
	export interface ChartBox {
		left: number;
		top: number;
		right: number;
		bottom: number;
	}

	/** The width and height of a chart key ("● name", 8px): the width is an estimate of about 4.5 px a letter. */
	export const chartKeySize = (text: string) => ({ width: text.length * 4.5, height: 10 });

	/**
	 * Where a small "● name" key (`width` px wide, at the top of a chart `chartWidth` px wide) goes: its left edge,
	 * at the end, or at the start when a box of `avoid` is in the way. Null when there is no room at either end.
	 */
	export function chartKeyLeft(width: number, height: number, chartWidth: number, avoid: ChartBox[]): number | null {
		const pad = 4;
		if (width + 2 * pad > chartWidth) return null;
		const overlaps = (left: number) => avoid.some((b) => b.left < left + width && b.right > left && b.top < height && b.bottom > 0);
		return [chartWidth - width - pad, pad].find((left) => !overlaps(left)) ?? null;
	}
</script>

<script lang="ts">
	import { runsOf, splitDual } from '#lib/utils/dualLabel.js';
	import { monotonePath } from '#lib/utils/monotonePath.js';
	import TierLegend from './TierLegend.svelte';
	import { ChartStyle } from '#lib/utils/chartStyle.js';
	import { chartDomain, chartNightRuns, type ChartDomain } from '#lib/utils/chartView.js';

	/**
	 * The chart under a row of forecast cards: every item at once, squeezed to the card's width,
	 * with the part scrolled into view highlighted. Callers pass values in the primary unit and
	 * ready-made labels (with both units) for the highest and lowest points.
	 */
	interface Props {
		values: (number | null)[];
		itemWidth: number;
		itemSpacing: number;
		scrollLeft?: number;
		containerWidth?: number;
		height?: number;
		style?: 'line' | 'bars';
		/** Scale from zero, with zero on the bottom edge: for amounts where 0 means none (wind, UV, AQI). */
		fromZero?: boolean;
		/** For temperatures: a faint line at freezing (0 °C, 32 °F) when it is inside the chart. */
		freezingAt?: number | null;
		color?: string;
		/** One color per point, e.g. by tier; overrides `color` for the dots and bars. */
		pointColors?: string[];
		highLabel?: string;
		lowLabel?: string;
		legend?: { color: string; label: string }[];
		/** Shown when every value is zero, so an empty chart reads as none rather than missing data. */
		emptyLabel?: string;
		/**
		 * A second line, e.g. the dew point under the humidity, on the scale `scale`: 'own' (from its values, another
		 * unit; the default), 'shared' (the scale of the main values, as the gusts with the wind) or 'percent' (0 to
		 * 100 %, as the chance of rain over the precipitation). `width` (default the main line's) and `dashed` set
		 * its look; `key` is its name, for a small key at the top of the chart when there is room. The labels of its
		 * highest and lowest points show only with the line style and the 'own' scale.
		 */
		second?: {
			values: (number | null)[]; color: string; highLabel?: string; lowLabel?: string;
			scale?: 'own' | 'shared' | 'percent'; width?: number; dashed?: boolean; key?: string;
		} | null;
		/** The smallest height of the scale, in the unit of the values. */
		minSpan?: number;
		/** Hourly charts: the night flag of each item, for a dark band, and the items at local midnight, for a line. */
		night?: boolean[];
		midnights?: number[];
		/** The item under the finger: a line, and a dot on each line. */
		selected?: number | null;
		/** The band that marks the part of the row in view; off in a share image. */
		showBand?: boolean;
	}

	let {
		values, itemWidth, itemSpacing, scrollLeft = 0, containerWidth = 0, showBand = true, height = 48,
		style = 'line', fromZero = false, color = 'white', pointColors, highLabel, lowLabel, legend, emptyLabel, second = null, freezingAt = null,
		minSpan = 0, night = [], midnights = [], selected = null,
	}: Props = $props();

	/** The fixed 0-100 % scale of a second line. */
	const PERCENT_SCALE: ChartDomain = { min: 0, max: 100, ticks: [0, 50, 100] };

	// Room above and below the line for the labels
	const PAD = 12;

	let valid = $derived(values.filter((v): v is number => v != null && Number.isFinite(v)));
	let max = $derived(valid.length ? Math.max(...valid) : 0);
	let zeroBased = $derived(style === 'bars' || fromZero);
	// Round ends (steps of 1, 2 or 5 times a power of 10), as in the chart view
	let secondScale = $derived(second?.scale ?? 'own');
	let domain = $derived(chartDomain([...values, ...(second && secondScale === 'shared' ? second.values : [])], zeroBased, minSpan));
	let secondDomain = $derived(
		!second ? null : secondScale === 'shared' ? domain : secondScale === 'percent' ? PERCENT_SCALE : chartDomain(second.values, false, 0),
	);
	let lowest = $derived(valid.length ? Math.min(...valid) : 0);

	let totalWidth = $derived(values.length * itemWidth + (values.length - 1) * itemSpacing);
	let svgWidth = $derived(containerWidth > 0 ? containerWidth : totalWidth);
	let xScale = $derived(containerWidth > 0 ? containerWidth / totalWidth : 1);

	function getX(i: number) {
		return (i * (itemWidth + itemSpacing) + itemWidth / 2) * xScale;
	}

	function getY(v: number, d: ChartDomain | null = domain) {
		const bottom = zeroBased ? height - 1.5 : height - PAD;
		return d && d.max > d.min ? bottom - ((v - d.min) / (d.max - d.min)) * (bottom - PAD) : bottom;
	}

	let points = $derived(values.flatMap((v, i) => (v != null && Number.isFinite(v) ? [{ x: getX(i), y: getY(v), i, v }] : [])));
	let highPoint = $derived(points.find((p) => p.v === max));
	let lowPoint = $derived(points.find((p) => p.v === lowest));
	let barWidth = $derived(Math.max(2, itemWidth * xScale * 0.6));

	// The second line; its labels only on its own scale, with the line style
	let second2 = $derived.by(() => {
		if (!second || !secondDomain) return null;
		const vs = second.values.filter((v): v is number => v != null && Number.isFinite(v));
		if (!vs.length) return null;
		const hi = Math.max(...vs);
		const lo = Math.min(...vs);
		const pts = second.values.flatMap((v, i) => (v != null && Number.isFinite(v) ? [{ x: getX(i), y: getY(v, secondDomain), i, v }] : []));
		const labelled = style === 'line' && secondScale === 'own';
		const high = labelled && second.highLabel ? pts.find((p) => p.v === hi) : undefined;
		const low = labelled && second.lowLabel && hi > lo ? pts.find((p) => p.v === lo) : undefined;
		return {
			pts,
			high: high && placeBeside(second.highLabel ?? '', high.x, high.y - 5, highLabel && highPoint ? { text: highLabel, x: highPoint.x, y: highPoint.y - 5 } : null),
			low: low && placeBeside(second.lowLabel ?? '', low.x, low.y + 11, lowLabel && lowPoint && max > lowest ? { text: lowLabel, x: lowPoint.x, y: zeroBased ? lowPoint.y - 5 : lowPoint.y + 11 } : null),
		};
	});

	// The left and right edges of a label, from an estimate of its width (9px bold)
	function span(text: string, x: number): [number, number] {
		const width = text.length * 5.5;
		const a = anchor(x);
		const left = a === 'start' ? labelX(x) : a === 'end' ? labelX(x) - width : x - width / 2;
		return [left, left + width];
	}

	// A label that would overlap the main line's label goes next to it, on its row
	function placeBeside(text: string, x: number, y: number, other: { text: string; x: number; y: number } | null) {
		const [left, right] = span(text, x);
		if (!other) return { x: labelX(x), y, anchor: anchor(x) };
		const [oLeft, oRight] = span(other.text, other.x);
		const gap = 6;
		if (right + gap <= oLeft || left >= oRight + gap) return { x: labelX(x), y, anchor: anchor(x) };
		const width = right - left;
		return oRight + gap + width <= svgWidth
			? { x: oRight + gap, y: other.y, anchor: 'start' as const }
			: { x: Math.max(width, oLeft - gap), y: other.y, anchor: 'end' as const };
	}

	// Labels near an edge grow inwards so they stay inside the chart
	function anchor(x: number): 'start' | 'middle' | 'end' {
		return x < svgWidth / 4 ? 'start' : x > (svgWidth * 3) / 4 ? 'end' : 'middle';
	}

	function labelX(x: number) {
		const a = anchor(x);
		return a === 'start' ? Math.max(0, x - 4) : a === 'end' ? Math.min(svgWidth, x + 4) : x;
	}

	// Night hours, over the whole place of each hour in the row
	let nightRects = $derived.by(() => {
		const half = ((itemWidth + itemSpacing) * xScale) / 2;
		return chartNightRuns(night).map((r) => {
			const left = Math.max(0, getX(r.start) - half);
			return { left, width: Math.min(svgWidth, getX(r.end - 1) + half) - left };
		});
	});

	// The box of a 9px bold label whose text starts, ends or is centred (`a`) at `x`, on the baseline `y`
	function labelBox(text: string, x: number, a: 'start' | 'middle' | 'end', y: number, letter = 5.5): ChartBox {
		const width = text.length * letter;
		const left = a === 'start' ? x : a === 'end' ? x - width : x - width / 2;
		return { left, top: y - 9, right: left + width, bottom: y + 2 };
	}

	// The key of the second line at the top of the chart, where no label is in the way
	let key = $derived.by(() => {
		if (!second?.key || !second2) return null;
		const text = `● ${second.key}`;
		const boxes: ChartBox[] = [];
		if (style === 'bars') {
			if (valid.length && max <= 0 && emptyLabel) boxes.push(labelBox(emptyLabel, svgWidth / 2, 'middle', height / 2 + 3, 6));
			if (highPoint && max > 0 && highLabel) boxes.push(labelBox(highLabel, labelX(highPoint.x), anchor(highPoint.x), Math.max(9, highPoint.y - 3)));
		} else {
			if (highPoint && highLabel) boxes.push(labelBox(highLabel, labelX(highPoint.x), anchor(highPoint.x), highPoint.y - 5));
			if (lowPoint && lowLabel && max > lowest) boxes.push(labelBox(lowLabel, labelX(lowPoint.x), anchor(lowPoint.x), zeroBased ? lowPoint.y - 5 : lowPoint.y + 11));
			if (second2.high && second.highLabel) boxes.push(labelBox(second.highLabel, second2.high.x, second2.high.anchor, second2.high.y));
			if (second2.low && second.lowLabel) boxes.push(labelBox(second.lowLabel, second2.low.x, second2.low.anchor, second2.low.y));
		}
		const size = chartKeySize(text);
		const left = chartKeyLeft(size.width, size.height, svgWidth, boxes);
		return left == null ? null : { text, left };
	});

	// The dots of the selected item: on the main line (the bars are not a line) and on the second line
	let selectedDots = $derived.by(() => {
		if (selected == null) return [];
		const main = style === 'line' ? points.find((p) => p.i === selected) : undefined;
		const other = second2?.pts.find((p) => p.i === selected);
		return [other?.y, main?.y].filter((y): y is number => y != null);
	});

	let showViewport = $derived(showBand && containerWidth > 0 && totalWidth > containerWidth);
	let vpLeft = $derived(showViewport ? Math.max(0, scrollLeft / totalWidth * svgWidth) : 0);
	let vpRight = $derived(showViewport ? Math.min(svgWidth, (scrollLeft + containerWidth) / totalWidth * svgWidth) : svgWidth);
</script>

{#if legend}
	<TierLegend entries={legend} />
{/if}
<svg width={svgWidth} {height} viewBox="0 0 {svgWidth} {height}" class="overview-chart" aria-hidden="true">
	<!-- A missing value leaves a gap in the second line, as in the main line -->
	{#snippet secondLine()}
		{#if second && second2}
			<path d={runsOf(second2.pts).filter((r) => r.length > 1).map((r) => monotonePath(r)).join(' ')} fill="none" stroke={second.color}
				stroke-width={second.width ?? ChartStyle.lineWidth} stroke-dasharray={second.dashed ? ChartStyle.dewPointDash : undefined} />
		{/if}
	{/snippet}
	<!-- Hourly charts: the night hours darker, and a line at each local midnight -->
	{#each nightRects as r (r.left)}
		<rect x={r.left} y="0" width={r.width} {height} fill={ChartStyle.night} />
	{/each}
	{#each midnights as i (i)}
		<line x1={getX(i)} x2={getX(i)} y1="0" y2={height} stroke={ChartStyle.midnight} stroke-width={ChartStyle.midnightWidth} />
	{/each}
	{#if style === 'bars'}
		<line x1="0" y1={height - 0.5} x2={svgWidth} y2={height - 0.5} stroke="white" stroke-width="1" stroke-opacity="0.2" />
		{#each points as p}
			{#if p.v > 0}
				<rect x={p.x - barWidth / 2} y={p.y} width={barWidth} height={height - p.y} rx="1.5" fill={pointColors?.[p.i] ?? color} />
			{/if}
		{/each}
		<!-- The second line over the bars -->
		{@render secondLine()}
		{#if valid.length && max <= 0 && emptyLabel}
			<text x={svgWidth / 2} y={height / 2 + 3} text-anchor="middle" font-size="10" font-weight="bold" fill={color}>{emptyLabel}</text>
		{/if}
		{#if highPoint && max > 0 && highLabel}
			<text x={labelX(highPoint.x)} y={Math.max(9, highPoint.y - 3)} text-anchor={anchor(highPoint.x)} direction="ltr" font-size="9" font-weight="bold" fill={color}>{splitDual(highLabel)[0]}{#if splitDual(highLabel)[1]}<tspan font-size="0.8em" font-weight="500" fill-opacity="0.7">{" " + splitDual(highLabel)[1]}</tspan>{/if}</text>
		{/if}
	{:else}
		<!-- The second line first, so the main line stays on top; its labels come after the main line -->
		{@render secondLine()}
		{#if freezingAt != null && domain && freezingAt > domain.min && freezingAt < domain.max}
			<line x1="0" x2={svgWidth} y1={getY(freezingAt)} y2={getY(freezingAt)} stroke={ChartStyle.freezing} stroke-width={ChartStyle.freezingWidth} stroke-dasharray={ChartStyle.freezingDash} />
		{/if}
		<!-- A missing value leaves a gap: each run of points with no missing value is its own curve -->
		<path d={runsOf(points).filter((r) => r.length > 1).map((r) => monotonePath(r)).join(' ')} fill="none" stroke={color} stroke-opacity={pointColors ? 0.5 : 1} stroke-width={ChartStyle.lineWidth} />
		<!-- Dots only for the colours of the tiers: a line of one colour has none, as in the chart view -->
		{#if pointColors}
			{#each points as p}
				<circle cx={p.x} cy={p.y} r="3" fill={pointColors[p.i] ?? color} />
			{/each}
		{/if}
		{#if second && second2}
			{#if second2.high && second.highLabel}
				<text x={second2.high.x} y={second2.high.y} text-anchor={second2.high.anchor} direction="ltr" font-size="9" font-weight="bold" fill={second.color}>{splitDual(second.highLabel)[0]}{#if splitDual(second.highLabel)[1]}<tspan font-size="0.8em" font-weight="500" fill-opacity="0.7">{" " + splitDual(second.highLabel)[1]}</tspan>{/if}</text>
			{/if}
			{#if second2.low && second.lowLabel}
				<text x={second2.low.x} y={second2.low.y} text-anchor={second2.low.anchor} direction="ltr" font-size="9" font-weight="bold" fill={second.color}>{splitDual(second.lowLabel)[0]}{#if splitDual(second.lowLabel)[1]}<tspan font-size="0.8em" font-weight="500" fill-opacity="0.7">{" " + splitDual(second.lowLabel)[1]}</tspan>{/if}</text>
			{/if}
		{/if}
		{#if highPoint && highLabel}
			<text x={labelX(highPoint.x)} y={highPoint.y - 5} text-anchor={anchor(highPoint.x)} direction="ltr" font-size="9" font-weight="bold" fill={pointColors?.[highPoint.i] ?? color}>{splitDual(highLabel)[0]}{#if splitDual(highLabel)[1]}<tspan font-size="0.8em" font-weight="500" fill-opacity="0.7">{" " + splitDual(highLabel)[1]}</tspan>{/if}</text>
		{/if}
		{#if zeroBased}
			<line x1="0" y1={height - 0.5} x2={svgWidth} y2={height - 0.5} stroke="white" stroke-width="1" stroke-opacity="0.2" />
		{/if}
		{#if lowPoint && lowLabel && max > lowest}
			<!-- Below the point, or above it when the point sits on the zero line -->
			<text x={labelX(lowPoint.x)} y={zeroBased ? lowPoint.y - 5 : lowPoint.y + 11} text-anchor={anchor(lowPoint.x)} direction="ltr" font-size="9" font-weight="bold" fill={pointColors?.[lowPoint.i] ?? color}>{splitDual(lowLabel)[0]}{#if splitDual(lowLabel)[1]}<tspan font-size="0.8em" font-weight="500" fill-opacity="0.7">{" " + splitDual(lowLabel)[1]}</tspan>{/if}</text>
		{/if}
	{/if}

	{#if key && second}
		<text x={key.left} y="8" direction="ltr" font-size="8" fill={second.color}>{key.text}</text>
	{/if}

		{#if showViewport}
		<!-- The part of the row scrolled into view, as a light band -->
		<rect x={vpLeft} y="0" width={vpRight - vpLeft} {height} rx="6" fill="white" fill-opacity="0.06" />
	{/if}

	<!-- The selected item: a line from top to bottom, and a dot on each line -->
	{#if selected != null && selected >= 0 && selected < values.length}
		<line x1={getX(selected)} x2={getX(selected)} y1="0" y2={height} stroke={ChartStyle.selection} stroke-width={ChartStyle.selectionWidth} />
		{#each selectedDots as y, k (k)}
			<circle cx={getX(selected)} cy={y} r={ChartStyle.selectionDotRadius} fill={ChartStyle.selectionDot} />
		{/each}
	{/if}
</svg>

<style>
	.overview-chart {
		display: block;
		pointer-events: none;
		overflow: visible;
	}
</style>
