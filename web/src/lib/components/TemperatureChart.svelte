<!-- SPDX-FileCopyrightText: Orinasa Njarasoa -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
<script lang="ts">
	import { runsOf, splitDual } from '#lib/utils/dualLabel.js';
	import { monotonePath } from '#lib/utils/monotonePath.js';
	import { SUPPORTED_LOCALES, localizeDigits } from '#lib/i18n/index.js';
	import { localeIndex } from '#lib/stores/preferences.js';
	import { _ } from 'svelte-i18n';
	import type { HourlyForecast } from '#lib/domain/weatherData.js';
	import { ChartStyle } from '#lib/utils/chartStyle.js';
	import { chartDomain, chartMidnights, chartNightRuns } from '#lib/utils/chartView.js';
	import { chartKeyLeft, chartKeySize, type ChartBox } from './OverviewChart.svelte';

	/**
	 * The 24 hours' temperature under the hourly cards, with the dew point as a thin line on the same scale,
	 * the night hours darker, and a line at each local midnight.
	 */
	interface Props {
		forecasts: HourlyForecast[];
		metricPrimary: boolean;
		itemWidth: number;
		itemSpacing: number;
		height?: number;
		scrollLeft?: number;
		containerWidth?: number;
		/** The band that marks the part of the row in view; off in a share image. */
		showBand?: boolean;
		/** The place's UTC offset, for the midnight and noon lines. */
		utcOffsetSeconds?: number;
		/** The night flag of each hour, for a dark band. */
		night?: boolean[];
		/** The hour under the finger: a line, and a dot on each line. */
		selected?: number | null;
	}

	let { forecasts, metricPrimary, itemWidth, itemSpacing, height = 40, scrollLeft = 0, containerWidth = 0, showBand = true, utcOffsetSeconds = 0, night = [], selected = null }: Props = $props();

	let temps = $derived(forecasts.map(f => metricPrimary ? f.temperature.celsius : f.temperature.fahrenheit));
	let dews = $derived(forecasts.map(f => (f.dewPoint && Number.isFinite(f.dewPoint.celsius) ? (metricPrimary ? f.dewPoint.celsius : f.dewPoint.fahrenheit) : null)));
	const loc = (s: string) => localizeDigits(s, SUPPORTED_LOCALES[$localeIndex]);
	let ariaLabel = $derived.by(() => {
		const valid = forecasts.map(f => f.temperature).filter(t => Number.isFinite(t.celsius));
		if (valid.length === 0) return undefined;
		const hi = valid.reduce((a, b) => (b.celsius > a.celsius ? b : a));
		const lo = valid.reduce((a, b) => (b.celsius < a.celsius ? b : a));
		const show = (t: typeof hi) => metricPrimary ? t.displayCelsius() : t.displayFahrenheit();
		return $_('chart_temperature_summary', { values: { high: loc(show(hi)), low: loc(show(lo)) } });
	});
	// One scale for both lines, with round ends as in the chart view. When the dew point is far below the
	// temperature, the scale grows to keep it, and the temperature line uses less of the height.
	let domain = $derived(chartDomain([...temps, ...dews], false, metricPrimary ? 5 : 9) ?? { min: 0, max: 1, ticks: [] });
	let freezing = $derived(metricPrimary ? 0 : 32);

	let totalWidth = $derived(forecasts.length * itemWidth + (forecasts.length - 1) * itemSpacing);
	let svgWidth = $derived(containerWidth > 0 ? containerWidth : totalWidth);
	let xScale = $derived(containerWidth > 0 ? containerWidth / totalWidth : 1);

	function getX(index: number) {
		return (index * (itemWidth + itemSpacing) + itemWidth / 2) * xScale;
	}

	// Room above the line for the high's label and below it for the low's
	const LABEL_ROOM = 13;

	function getY(temp: number) {
		return LABEL_ROOM + (height - 2 * LABEL_ROOM) * (1 - (temp - domain.min) / (domain.max - domain.min));
	}

	let points = $derived(temps.map((temp, i) => ({ x: getX(i), y: getY(temp) })));
	// The dew point; a missing value leaves a gap
	let dewPoints = $derived(dews.flatMap((v, i) => (v != null ? [{ x: getX(i), y: getY(v), i }] : [])));
	let dewPath = $derived(runsOf(dewPoints).filter((r) => r.length > 1).map((r) => monotonePath(r)).join(' '));

	let times = $derived(forecasts.map((f) => f.time));
	let midnightIndices = $derived(chartMidnights(times, utcOffsetSeconds));
	// Noon at the place, as the midnights
	let noonIndices = $derived(times.flatMap((t, i) => (new Date(t + utcOffsetSeconds * 1000).getUTCHours() === 12 ? [i] : [])));

	// Night hours, over the whole place of each hour in the row
	let nightRects = $derived.by(() => {
		const half = ((itemWidth + itemSpacing) * xScale) / 2;
		return chartNightRuns(night).map((r) => {
			const left = Math.max(0, getX(r.start) - half);
			return { left, width: Math.min(svgWidth, getX(r.end - 1) + half) - left };
		});
	});

	function generateMonotonePath(pts: { x: number; y: number }[]): string {
		const n = pts.length;
		if (n < 2) return '';
		if (n === 2) return `M ${pts[0].x.toFixed(1)} ${pts[0].y.toFixed(1)} L ${pts[1].x.toFixed(1)} ${pts[1].y.toFixed(1)}`;

		const dx = new Float64Array(n - 1);
		const dy = new Float64Array(n - 1);
		const ms = new Float64Array(n - 1);

		for (let i = 0; i < n - 1; i++) {
			dx[i] = Math.max(0.0001, pts[i + 1].x - pts[i].x);
			dy[i] = pts[i + 1].y - pts[i].y;
			ms[i] = dy[i] / dx[i];
		}

		const ds = new Float64Array(n);
		ds[0] = ms[0];
		ds[n - 1] = ms[n - 2];

		for (let i = 1; i < n - 1; i++) {
			if (ms[i - 1] * ms[i] <= 0) {
				ds[i] = 0;
			} else {
				ds[i] = (ms[i - 1] + ms[i]) / 2;
			}
		}

		for (let i = 0; i < n - 1; i++) {
			if (ms[i] === 0) {
				ds[i] = 0;
				ds[i + 1] = 0;
			} else {
				const alpha = ds[i] / ms[i];
				const beta = ds[i + 1] / ms[i];
				const dist = alpha * alpha + beta * beta;
				if (dist > 9) {
					const tau = 3 / Math.sqrt(dist);
					ds[i] = tau * alpha * ms[i];
					ds[i + 1] = tau * beta * ms[i];
				}
			}
		}

		let d = `M ${pts[0].x.toFixed(1)} ${pts[0].y.toFixed(1)}`;
		for (let i = 0; i < n - 1; i++) {
			const h = dx[i];
			const p1 = pts[i];
			const p2 = pts[i + 1];
			const cp1X = (p1.x + h / 3).toFixed(1);
			const cp1Y = (p1.y + (ds[i] * h) / 3).toFixed(1);
			const cp2X = (p2.x - h / 3).toFixed(1);
			const cp2Y = (p2.y - (ds[i + 1] * h) / 3).toFixed(1);
			d += ` C ${cp1X} ${cp1Y}, ${cp2X} ${cp2Y}, ${p2.x.toFixed(1)} ${p2.y.toFixed(1)}`;
		}
		return d;
	}

	let linePathStr = $derived(generateMonotonePath(points));
	let fillPathStr = $derived.by(() => {
		if (points.length < 2 || !linePathStr) return '';
		return `${linePathStr} L ${points[points.length - 1].x.toFixed(1)} ${height} L ${points[0].x.toFixed(1)} ${height} Z`;
	});

	let maxPointInfo = $derived.by(() => {
		if (points.length === 0 || forecasts.length === 0) return null;
		const minY = Math.min(...points.map(p => p.y));
		const maxIndices = points.map((p, i) => p.y === minY ? i : -1).filter(i => i !== -1);
		const midIdx = maxIndices[Math.floor(maxIndices.length / 2)];
		const pt = points[midIdx];
		const label = forecasts[midIdx].temperature.displayDual(metricPrimary).join(' · ');
		const above = Math.max(9, pt.y - 4);
		// Under the line where the label above the peak would cover a 00:00 or 12:00 label
		const covers = above - 9 < 9 && [...midnightIndices, ...noonIndices].some((idx) => {
			const w = label.length * 9 * 0.6;
			const a = anchor(pt.x);
			const left = a === 'start' ? labelX(pt.x) : a === 'end' ? labelX(pt.x) - w : pt.x - w / 2;
			const t = getX(idx);
			return left < t + 12 && left + w > t - 12;
		});
		return { x: pt.x, y: covers ? pt.y + 13 : above, label };
	});

	let minPointInfo = $derived.by(() => {
		if (points.length === 0 || forecasts.length === 0) return null;
		const maxY = Math.max(...points.map(p => p.y));
		const minIndices = points.map((p, i) => p.y === maxY ? i : -1).filter(i => i !== -1);
		const midIdx = minIndices[Math.floor(minIndices.length / 2)];
		const pt = points[midIdx];
		const label = forecasts[midIdx].temperature.displayDual(metricPrimary).join(' · ');
		return { x: pt.x, y: Math.min(height - 1, pt.y + 11), label };
	});

	// Labels near an edge grow inwards so they stay inside the chart
	function anchor(x: number): 'start' | 'middle' | 'end' {
		return x < svgWidth / 4 ? 'start' : x > (svgWidth * 3) / 4 ? 'end' : 'middle';
	}

	function labelX(x: number) {
		const a = anchor(x);
		return a === 'start' ? Math.max(0, x - 4) : a === 'end' ? Math.min(svgWidth, x + 4) : x;
	}

	// The dew point key in the row of the time labels, where nothing is in the way
	let dewKey = $derived.by(() => {
		if (dewPoints.length === 0) return null;
		const text = `● ${$_('detail_dewpoint')}`;
		const boxes: ChartBox[] = [...midnightIndices, ...noonIndices].map((i) => ({ left: getX(i) - 12, top: 1, right: getX(i) + 12, bottom: 11 }));
		if (maxPointInfo) {
			// The width is an estimate (about 5.5 px a letter at 9 px bold), as for the labels of the overview charts
			const width = maxPointInfo.label.length * 5.5;
			const a = anchor(maxPointInfo.x);
			const x = labelX(maxPointInfo.x);
			const left = a === 'start' ? x : a === 'end' ? x - width : x - width / 2;
			boxes.push({ left, top: maxPointInfo.y - 9, right: left + width, bottom: maxPointInfo.y + 2 });
		}
		const size = chartKeySize(text);
		const left = chartKeyLeft(size.width, size.height, svgWidth, boxes);
		return left == null ? null : { text, left };
	});

	let showViewport = $derived(showBand && containerWidth > 0 && totalWidth > containerWidth);
	let vpLeft = $derived(showViewport ? Math.max(0, scrollLeft / totalWidth * svgWidth) : 0);
	let vpRight = $derived(showViewport ? Math.min(svgWidth, (scrollLeft + containerWidth) / totalWidth * svgWidth) : svgWidth);
</script>

<svg width={svgWidth} {height} viewBox="0 0 {svgWidth} {height}" class="temp-chart" role={ariaLabel ? 'img' : undefined} aria-label={ariaLabel}>
	<defs>
		<linearGradient id="fillGradient" x1="0" y1="0" x2="0" y2="1">
			<stop offset="0%" stop-color="#FFFFFF" stop-opacity="0.15" />
			<stop offset="100%" stop-color="#FFFFFF" stop-opacity="0" />
		</linearGradient>
	</defs>

	<!-- The night hours darker, and a line at each local midnight, with its label -->
	{#each nightRects as r (r.left)}
		<rect x={r.left} y="0" width={r.width} {height} fill={ChartStyle.night} />
	{/each}
	{#each midnightIndices as idx}
		{@const x = getX(idx)}
		<line x1={x} y1="0" x2={x} y2={height} stroke={ChartStyle.midnight} stroke-width={ChartStyle.midnightWidth} />
		<text x={x} y="9" text-anchor="middle" font-size="8" fill="white" fill-opacity="0.7">00:00</text>
	{/each}

	<!-- Noon Vertical Lines + Labels -->
	{#each noonIndices as idx}
		{@const x = getX(idx)}
		<line x1={x} y1="0" x2={x} y2={height} stroke="white" stroke-width="1" stroke-opacity="0.1" stroke-dasharray="4 4" />
		<text x={x} y="9" text-anchor="middle" font-size="8" fill="white" fill-opacity="0.45">12:00</text>
	{/each}

	<!-- A faint line at freezing (0 °C, 32 °F) when it is inside the chart -->
	{#if freezing > domain.min && freezing < domain.max}
		<line x1="0" x2={svgWidth} y1={getY(freezing)} y2={getY(freezing)} stroke={ChartStyle.freezing} stroke-width={ChartStyle.freezingWidth} stroke-dasharray={ChartStyle.freezingDash} />
	{/if}
	{#if fillPathStr}
		<path d={fillPathStr} fill="url(#fillGradient)" />
	{/if}
	<!-- The dew point under the temperature line -->
	{#if dewPath}
		<path d={dewPath} fill="none" stroke={ChartStyle.dewPoint} stroke-width={ChartStyle.thinLineWidth} />
	{/if}
	<!-- The temperature line, with no dot at each hour, as in the chart view -->
	{#if linePathStr}
		<path d={linePathStr} fill="none" stroke={ChartStyle.temperature} stroke-width={ChartStyle.lineWidth} />
	{/if}

	{#if maxPointInfo}
		<text x={labelX(maxPointInfo.x)} y={maxPointInfo.y} text-anchor={anchor(maxPointInfo.x)} direction="ltr" font-size="9" font-weight="bold" fill="#FF8A65">{splitDual(maxPointInfo.label)[0]}{#if splitDual(maxPointInfo.label)[1]}<tspan font-size="0.8em" font-weight="500" fill-opacity="0.7">{" " + splitDual(maxPointInfo.label)[1]}</tspan>{/if}</text>
	{/if}
	{#if minPointInfo}
		<text x={labelX(minPointInfo.x)} y={minPointInfo.y} text-anchor={anchor(minPointInfo.x)} direction="ltr" font-size="9" font-weight="bold" fill="#64B5F6">{splitDual(minPointInfo.label)[0]}{#if splitDual(minPointInfo.label)[1]}<tspan font-size="0.8em" font-weight="500" fill-opacity="0.7">{" " + splitDual(minPointInfo.label)[1]}</tspan>{/if}</text>
	{/if}

	{#if dewKey}
		<text x={dewKey.left} y="8" direction="ltr" font-size="8" fill={ChartStyle.dewPoint}>{dewKey.text}</text>
	{/if}

		{#if showViewport}
		<!-- The part of the row scrolled into view, as a light band -->
		<rect x={vpLeft} y="0" width={vpRight - vpLeft} {height} rx="6" fill="white" fill-opacity="0.06" />
	{/if}

	<!-- The selected hour: a line from top to bottom, and a dot on each line -->
	{#if selected != null && points[selected]}
		{@const x = getX(selected)}
		<line x1={x} x2={x} y1="0" y2={height} stroke={ChartStyle.selection} stroke-width={ChartStyle.selectionWidth} />
		{#each [dews[selected] != null ? getY(dews[selected]!) : null, Number.isFinite(points[selected].y) ? points[selected].y : null] as y, k (k)}
			{#if y != null}
				<circle cx={x} cy={y} r={ChartStyle.selectionDotRadius} fill={ChartStyle.selectionDot} />
			{/if}
		{/each}
	{/if}
</svg>

<style>
	.temp-chart {
		display: block;
		pointer-events: none;
	}
</style>
