<script lang="ts">
	import type { DailyForecast as DailyForecastType } from '$lib/domain/weatherData';
	import type { UvTier } from '$lib/domain/uv';
	import { uvColorFor, UV_TIER_COLORS } from '$lib/domain/uv';
	import { json } from 'svelte-i18n';
	import { onMount } from 'svelte';
	import TierLegend from './TierLegend.svelte';

	interface Props {
		forecasts: DailyForecastType[];
		height?: number;
	}

	let { forecasts, height = 48 }: Props = $props();

	const UV_TIER_ORDER: UvTier[] = ['low', 'moderate', 'high', 'veryHigh', 'extreme'];
	let legendEntries = $derived(($json('uv_labels') as string[]).map((label, i) => ({
		color: UV_TIER_COLORS[UV_TIER_ORDER[i]],
		label,
	})));

	let containerWidth = $state(100);
	let container: HTMLDivElement | undefined = $state();

	onMount(() => {
		if (!container) return;
		const observer = new ResizeObserver((entries) => {
			for (const entry of entries) {
				containerWidth = entry.contentRect.width;
			}
		});
		observer.observe(container);
		return () => observer.disconnect();
	});

	let uvValues = $derived(forecasts.map(f => f.uvIndexMax));
	let uvMax = $derived(uvValues.length ? Math.max(...uvValues) : 0);
	let paddedMax = $derived(uvMax > 0 ? uvMax * 1.2 : 1);

	function getX(index: number, total: number) {
		if (total <= 1) return containerWidth / 2;
		return (index / (total - 1)) * containerWidth;
	}

	function getY(uv: number) {
		return height - (uv / paddedMax * height);
	}

	let uvPoints = $derived(uvValues.map((uv, i) => ({ x: getX(i, forecasts.length), y: getY(uv), color: uvColorFor(uv) })));

	let horizontalTicks = $derived(() => {
		const ticks = [];
		const end = Math.floor(paddedMax);
		for (let i = 0; i <= end; i++) {
			ticks.push({ y: getY(i), value: i });
		}
		return ticks;
	});

	let mondayIndices = $derived(forecasts.map((f, i) => {
		const date = new Date(f.date);
		return date.getDay() === 1 ? i : -1;
	}).filter(idx => idx !== -1));

	function generatePath(points: { x: number; y: number }[]) {
		if (points.length < 2) return '';
		let d = `M ${points[0].x} ${points[0].y}`;
		for (let i = 1; i < points.length; i++) {
			const prev = points[i - 1];
			const curr = points[i];
			const cp1x = prev.x + (curr.x - prev.x) / 2;
			d += ` C ${cp1x} ${prev.y} ${cp1x} ${curr.y} ${curr.x} ${curr.y}`;
		}
		return d;
	}

	let uvPath = $derived(generatePath(uvPoints));
</script>

<div class="daily-chart-row" bind:this={container}>
	<TierLegend entries={legendEntries} />
	<div class="chart-wrapper" style="height: {height}px;">
		<svg width="100%" height="100%" viewBox="0 0 {containerWidth} {height}">
			<!-- Horizontal Ticks -->
			{#each horizontalTicks() as tick}
				{@const isMajor = tick.value % 5 === 0}
				<line
					x1="0" y1={tick.y} x2={containerWidth} y2={tick.y}
					stroke="white"
					stroke-width={isMajor ? "1" : "0.5"}
					stroke-opacity={isMajor ? "0.25" : "0.1"}
				/>
			{/each}

			<!-- Monday Vertical Lines -->
			{#each mondayIndices as idx}
				{@const x = getX(idx, forecasts.length)}
				<line x1={x} y1="0" x2={x} y2={height} stroke="white" stroke-width="1" stroke-opacity="0.2" stroke-dasharray="2 2" />
			{/each}

			<path d={uvPath} fill="none" stroke="white" stroke-opacity="0.5" stroke-width="2" stroke-linecap="round" />

			{#each uvPoints as point}
				<circle cx={point.x} cy={point.y} r="3" fill={point.color} />
			{/each}
		</svg>
	</div>
</div>

<style>
	.daily-chart-row {
		background: rgba(42, 31, 165, 0.3);
		border-radius: 12px;
		padding: 16px;
		margin-bottom: 8px;
	}

	.chart-wrapper {
		width: 100%;
		position: relative;
	}

	svg {
		display: block;
		overflow: visible;
	}
</style>
