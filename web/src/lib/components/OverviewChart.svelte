<script lang="ts">
	import { monotonePath } from '$lib/utils/monotonePath';
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
	}

	let {
		values, itemWidth, itemSpacing, scrollLeft = 0, containerWidth = 0, height = 48,
		style = 'line', fromZero = false, color = 'white', pointColors, highLabel, lowLabel, legend,
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

	function getY(v: number) {
		const bottom = zeroBased ? height - 1.5 : height - PAD;
		return max > min ? bottom - ((v - min) / (max - min)) * (bottom - PAD) : (bottom + PAD) / 2;
	}

	let points = $derived(values.flatMap((v, i) => (v != null && Number.isFinite(v) ? [{ x: getX(i), y: getY(v), i, v }] : [])));
	let highPoint = $derived(points.find((p) => p.v === max));
	let lowPoint = $derived(points.find((p) => p.v === lowest));
	let barWidth = $derived(Math.max(2, itemWidth * xScale * 0.6));

	// Labels near an edge grow inwards so they stay inside the chart
	function anchor(x: number): 'start' | 'middle' | 'end' {
		return x < svgWidth / 4 ? 'start' : x > (svgWidth * 3) / 4 ? 'end' : 'middle';
	}

	function labelX(x: number) {
		const a = anchor(x);
		return a === 'start' ? Math.max(0, x - 4) : a === 'end' ? Math.min(svgWidth, x + 4) : x;
	}

	let showViewport = $derived(containerWidth > 0 && totalWidth > containerWidth);
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
		{#if highPoint && max > 0 && highLabel}
			<text x={labelX(highPoint.x)} y={Math.max(9, highPoint.y - 3)} text-anchor={anchor(highPoint.x)} font-size="9" font-weight="bold" fill={color}>{highLabel}</text>
		{/if}
	{:else}
		<path d={monotonePath(points)} fill="none" stroke={color} stroke-opacity={pointColors ? 0.5 : 1} stroke-width="2" />
		{#each points as p}
			<circle cx={p.x} cy={p.y} r="3" fill={pointColors?.[p.i] ?? color} />
		{/each}
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
