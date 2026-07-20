<script lang="ts">
	import type { HourlyAirQuality } from '$lib/domain/weatherData';
	import type { AqiStandard, AqiTier } from '$lib/domain/airQuality';
	import { AirQualityIndex, AQI_TIER_COLORS } from '$lib/domain/airQuality';
	import { json } from 'svelte-i18n';
	import { onMount } from 'svelte';
	import TierLegend from './TierLegend.svelte';

	interface Props {
		forecasts: HourlyAirQuality[];
		primaryStandard: AqiStandard;
		height?: number;
	}

	let { forecasts, primaryStandard, height = 140 }: Props = $props();

	const AQI_TIER_ORDER: AqiTier[] = ['good', 'moderate', 'unhealthy', 'very_unhealthy', 'hazardous'];
	let legendEntries = $derived(($json('aqi_tier_labels') as string[]).map((label, i) => ({
		color: AQI_TIER_COLORS[AQI_TIER_ORDER[i]],
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

	let values = $derived(forecasts.map(f => primaryStandard === 'EUROPEAN' ? f.europeanValue : f.usValue));
	let minValue = $derived(values.length ? Math.min(...values) : 0);
	let maxValue = $derived(values.length ? Math.max(...values) : 0);
	let valueRange = $derived(maxValue - minValue === 0 ? 1 : maxValue - minValue);

	let paddedMin = $derived(minValue - (valueRange * 0.15));
	let paddedMax = $derived(maxValue + (valueRange * 0.15));
	let paddedRange = $derived(paddedMax - paddedMin);

	function getX(index: number, total: number) {
		if (total <= 1) return containerWidth / 2;
		return (index / (total - 1)) * containerWidth;
	}

	function getY(value: number) {
		return height - ((value - paddedMin) / paddedRange * height);
	}

	let points = $derived(values.map((value, i) => ({ x: getX(i, forecasts.length), y: getY(value) })));
	let dotColors = $derived(forecasts.map(f => AQI_TIER_COLORS[AirQualityIndex.tierFor(
		primaryStandard === 'EUROPEAN' ? f.europeanValue : f.usValue, primaryStandard
	)]));

	let midnightIndices = $derived(forecasts.map((f, i) => new Date(f.time).getHours() === 0 ? i : -1).filter(i => i !== -1));
	let noonIndices = $derived(forecasts.map((f, i) => new Date(f.time).getHours() === 12 ? i : -1).filter(i => i !== -1));

	let linePath = $derived(() => {
		if (points.length < 2) return '';
		let d = `M ${points[0].x} ${points[0].y}`;
		for (let i = 1; i < points.length; i++) {
			const prev = points[i - 1];
			const curr = points[i];
			const cp1x = prev.x + (curr.x - prev.x) / 2;
			d += ` C ${cp1x} ${prev.y} ${cp1x} ${curr.y} ${curr.x} ${curr.y}`;
		}
		return d;
	});

	let fillPath = $derived(() => {
		if (points.length < 2) return '';
		let d = `M ${points[0].x} ${height} L ${points[0].x} ${points[0].y}`;
		for (let i = 1; i < points.length; i++) {
			const prev = points[i - 1];
			const curr = points[i];
			const cp1x = prev.x + (curr.x - prev.x) / 2;
			d += ` C ${cp1x} ${prev.y} ${cp1x} ${curr.y} ${curr.x} ${curr.y}`;
		}
		d += ` L ${points[points.length - 1].x} ${height} Z`;
		return d;
	});
</script>

<div class="chart-row" bind:this={container}>
	<TierLegend entries={legendEntries} />
	<div class="chart-wrapper" style="height: {height}px;">
		<svg width="100%" height="100%" viewBox="0 0 {containerWidth} {height}">
			<defs>
				<linearGradient id="aqiFillGradient" x1="0" y1="0" x2="0" y2="1">
					<stop offset="0%" stop-color="#FFFFFF" stop-opacity="0.12" />
					<stop offset="100%" stop-color="#FFFFFF" stop-opacity="0" />
				</linearGradient>
			</defs>

			<!-- Midnight Vertical Lines + Labels -->
			{#each midnightIndices as idx}
				{@const x = getX(idx, forecasts.length)}
				<line x1={x} y1="0" x2={x} y2={height} stroke="white" stroke-width="1" stroke-opacity="0.4" />
				<text x={x} y="9" text-anchor="middle" font-size="8" fill="white" fill-opacity="0.7">00:00</text>
			{/each}

			<!-- Noon Vertical Lines + Labels -->
			{#each noonIndices as idx}
				{@const x = getX(idx, forecasts.length)}
				<line x1={x} y1="0" x2={x} y2={height} stroke="white" stroke-width="1" stroke-opacity="0.2" stroke-dasharray="4 4" />
				<text x={x} y="9" text-anchor="middle" font-size="8" fill="white" fill-opacity="0.45">12:00</text>
			{/each}

			{#if fillPath()}
				<path d={fillPath()} fill="url(#aqiFillGradient)" />
			{/if}
			<path d={linePath()} fill="none" stroke="white" stroke-width="2" />

			{#each points as point, i}
				<circle cx={point.x} cy={point.y} r="4" fill={dotColors[i]} />
			{/each}
		</svg>
	</div>
</div>

<style>
	.chart-row {
		width: 100%;
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
