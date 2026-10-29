<!-- SPDX-FileCopyrightText: Orinasa Njarasoa -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
<script lang="ts">
	import { monotonePath } from '#lib/utils/monotonePath.js';
	import TierLegend from './TierLegend.svelte';

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
		color?: string;
		/** One color per point, e.g. by tier; overrides `color` for the dots and bars. */
		pointColors?: string[];
		highLabel?: string;
		lowLabel?: string;
		legend?: { color: string; label: string }[];
		/** Shown when every value is zero, so an empty chart reads as none rather than missing data. */
		emptyLabel?: string;
		/** A second line on its own scale (another unit), e.g. the dew point under the humidity; line style only. */
		second?: { values: (number | null)[]; color: string; highLabel?: string; lowLabel?: string } | null;
		/** The band that marks the part of the row in view; off in a share image. */
		showBand?: boolean;
	}

	let {
		values, itemWidth, itemSpacing, scrollLeft = 0, containerWidth = 0, showBand = true, height = 48,
		style = 'line', fromZero = false, color = 'white', pointColors, highLabel, lowLabel, legend, emptyLabel, second = null,
	}: Props = $props();

	// Room above and below the line for the labels
	const PAD = 12;

	let valid = $derived(values.filter((v): v is number => v != null && Number.isFinite(v)));
	let max = $derived(valid.length ? Math.max(...valid) : 0);
	let zeroBased = $derived(style === 'bars' || fromZero);
	let min = $derived(zeroBased ? 0 : valid.length ? Math.min(...valid) : 0);
	let lowest = $derived(valid.length ? Math.min(...valid) : 0);

	let totalWidth = $derived(values.length * itemWidth + (values.length - 1) * itemSpacing);
	let svgWidth = $derived(containerWidth > 0 ? containerWidth : totalWidth);
	let xScale = $derived(containerWidth > 0 ? containerWidth / totalWidth : 1);

	function getX(i: number) {
		return (i * (itemWidth + itemSpacing) + itemWidth / 2) * xScale;
	}

	function getY(v: number, lo = min, hi = max) {
		const bottom = zeroBased ? height - 1.5 : height - PAD;
		return hi > lo ? bottom - ((v - lo) / (hi - lo)) * (bottom - PAD) : (bottom + PAD) / 2;
	}

	let points = $derived(values.flatMap((v, i) => (v != null && Number.isFinite(v) ? [{ x: getX(i), y: getY(v), i, v }] : [])));
	let highPoint = $derived(points.find((p) => p.v === max));
	let lowPoint = $derived(points.find((p) => p.v === lowest));
	let barWidth = $derived(Math.max(2, itemWidth * xScale * 0.6));

	// The second line, on its own scale
	let second2 = $derived.by(() => {
		if (!second) return null;
		const vs = second.values.filter((v): v is number => v != null && Number.isFinite(v));
		if (!vs.length) return null;
		const hi = Math.max(...vs);
		const lo = Math.min(...vs);
		const pts = second.values.flatMap((v, i) => (v != null && Number.isFinite(v) ? [{ x: getX(i), y: getY(v, lo, hi), i, v }] : []));
		const high = pts.find((p) => p.v === hi);
		const low = hi > lo ? pts.find((p) => p.v === lo) : undefined;
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

	let showViewport = $derived(showBand && containerWidth > 0 && totalWidth > containerWidth);
	let vpLeft = $derived(showViewport ? Math.max(0, scrollLeft / totalWidth * svgWidth) : 0);
	let vpRight = $derived(showViewport ? Math.min(svgWidth, (scrollLeft + containerWidth) / totalWidth * svgWidth) : svgWidth);
</script>

{#if legend}
	<TierLegend entries={legend} />
{/if}
<svg width={svgWidth} {height} viewBox="0 0 {svgWidth} {height}" class="overview-chart" aria-hidden="true">
	{#if style === 'bars'}
		<line x1="0" y1={height - 0.5} x2={svgWidth} y2={height - 0.5} stroke="white" stroke-width="1" stroke-opacity="0.2" />
		{#each points as p}
			{#if p.v > 0}
				<rect x={p.x - barWidth / 2} y={p.y} width={barWidth} height={height - p.y} rx="1.5" fill={pointColors?.[p.i] ?? color} />
			{/if}
		{/each}
		{#if valid.length && max <= 0 && emptyLabel}
			<text x={svgWidth / 2} y={height / 2 + 3} text-anchor="middle" font-size="10" font-weight="bold" fill={color}>{emptyLabel}</text>
		{/if}
		{#if highPoint && max > 0 && highLabel}
			<text x={labelX(highPoint.x)} y={Math.max(9, highPoint.y - 3)} text-anchor={anchor(highPoint.x)} font-size="9" font-weight="bold" fill={color}>{highLabel}</text>
		{/if}
	{:else}
		{#if second && second2}
			<!-- The second line first, so the main line stays on top; its labels come after the main line -->
			<path d={monotonePath(second2.pts)} fill="none" stroke={second.color} stroke-width="2" />
			{#each second2.pts as p}
				<circle cx={p.x} cy={p.y} r="2.5" fill={second.color} />
			{/each}
		{/if}
		<path d={monotonePath(points)} fill="none" stroke={color} stroke-opacity={pointColors ? 0.5 : 1} stroke-width="2" />
		{#each points as p}
			<circle cx={p.x} cy={p.y} r="3" fill={pointColors?.[p.i] ?? color} />
		{/each}
		{#if second && second2}
			{#if second2.high && second.highLabel}
				<text x={second2.high.x} y={second2.high.y} text-anchor={second2.high.anchor} font-size="9" font-weight="bold" fill={second.color}>{second.highLabel}</text>
			{/if}
			{#if second2.low && second.lowLabel}
				<text x={second2.low.x} y={second2.low.y} text-anchor={second2.low.anchor} font-size="9" font-weight="bold" fill={second.color}>{second.lowLabel}</text>
			{/if}
		{/if}
		{#if highPoint && highLabel}
			<text x={labelX(highPoint.x)} y={highPoint.y - 5} text-anchor={anchor(highPoint.x)} font-size="9" font-weight="bold" fill={pointColors?.[highPoint.i] ?? color}>{highLabel}</text>
		{/if}
		{#if zeroBased}
			<line x1="0" y1={height - 0.5} x2={svgWidth} y2={height - 0.5} stroke="white" stroke-width="1" stroke-opacity="0.2" />
		{/if}
		{#if lowPoint && lowLabel && max > lowest}
			<!-- Below the point, or above it when the point sits on the zero line -->
			<text x={labelX(lowPoint.x)} y={zeroBased ? lowPoint.y - 5 : lowPoint.y + 11} text-anchor={anchor(lowPoint.x)} font-size="9" font-weight="bold" fill={pointColors?.[lowPoint.i] ?? color}>{lowLabel}</text>
		{/if}
	{/if}

		{#if showViewport}
		<!-- The part of the row scrolled into view, as a light band -->
		<rect x={vpLeft} y="0" width={vpRight - vpLeft} {height} rx="6" fill="white" fill-opacity="0.06" />
	{/if}
</svg>

<style>
	.overview-chart {
		display: block;
		pointer-events: none;
		overflow: visible;
	}
</style>
