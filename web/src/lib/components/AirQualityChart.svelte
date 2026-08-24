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

	let peakAqiInfo = $derived.by(() => {
		if (values.length === 0 || points.length === 0) return null;
		const maxVal = Math.max(...values);
		const maxIndices = values.map((v, i) => v === maxVal ? i : -1).filter(i => i !== -1);
		const midIdx = maxIndices[Math.floor(maxIndices.length / 2)];
		const pt = points[midIdx];
		const color = dotColors[midIdx];
		return { x: pt.x, y: Math.max(10, pt.y - 4), aqi: Math.round(maxVal).toString(), color };
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

			{#if fillPathStr}
				<path d={fillPathStr} fill="url(#aqiFillGradient)" />
			{/if}
			{#if linePathStr}
				<path d={linePathStr} fill="none" stroke="white" stroke-width="2" />
			{/if}

			{#each points as point, i}
				<circle cx={point.x} cy={point.y} r="4" fill={dotColors[i]} />
			{/each}

			{#if peakAqiInfo}
				<text x={peakAqiInfo.x} y={peakAqiInfo.y} text-anchor="middle" font-size="9" font-weight="bold" fill={peakAqiInfo.color}>AQI {peakAqiInfo.aqi}</text>
			{/if}
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
