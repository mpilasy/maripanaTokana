<script lang="ts">
	import type { DailyForecast as DailyForecastType } from '$lib/domain/weatherData';

	interface Props {
		forecasts: DailyForecastType[];
		metricPrimary: boolean;
		itemWidth: number;
		itemSpacing: number;
		height?: number;
		scrollLeft?: number;
		containerWidth?: number;
	}

	let { forecasts, metricPrimary, itemWidth, itemSpacing, height = 48, scrollLeft = 0, containerWidth = 0 }: Props = $props();

	let maxTemps = $derived(forecasts.map(f => metricPrimary ? f.tempMax.celsius : f.tempMax.fahrenheit));
	let minTemps = $derived(forecasts.map(f => metricPrimary ? f.tempMin.celsius : f.tempMin.fahrenheit));

	let allTemps = $derived([...maxTemps, ...minTemps]);
	let globalMin = $derived(Math.min(...allTemps));
	let globalMax = $derived(Math.max(...allTemps));
	let tempRange = $derived(globalMax - globalMin === 0 ? 1 : globalMax - globalMin);

	let paddedMin = $derived(globalMin - (tempRange * 0.2));
	let paddedMax = $derived(globalMax + (tempRange * 0.2));
	let paddedRange = $derived(paddedMax - paddedMin);

	let totalWidth = $derived(forecasts.length * itemWidth + (forecasts.length - 1) * itemSpacing);
	let svgWidth = $derived(containerWidth > 0 ? containerWidth : totalWidth);
	let xScale = $derived(containerWidth > 0 ? containerWidth / totalWidth : 1);

	function getX(index: number) {
		return (index * (itemWidth + itemSpacing) + itemWidth / 2) * xScale;
	}

	function getY(temp: number) {
		return height - ((temp - paddedMin) / paddedRange * height);
	}

	let maxPoints = $derived(maxTemps.map((temp, i) => ({ x: getX(i), y: getY(temp) })));
	let minPoints = $derived(minTemps.map((temp, i) => ({ x: getX(i), y: getY(temp) })));

	let horizontalTicks = $derived(() => {
		const ticks = [];
		const start = Math.ceil(paddedMin);
		const end = Math.floor(paddedMax);
		for (let i = start; i <= end; i++) {
			ticks.push({ y: getY(i), temp: i });
		}
		return ticks;
	});

	let mondayIndices = $derived(forecasts.map((f, i) => {
		const date = new Date(f.date);
		return date.getDay() === 1 ? i : -1;
	}).filter(idx => idx !== -1));

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

	let maxPath = $derived(generateMonotonePath(maxPoints));
	let minPath = $derived(generateMonotonePath(minPoints));

	let showViewport = $derived(containerWidth > 0 && totalWidth > containerWidth);
	let vpLeft = $derived(showViewport ? Math.max(0, scrollLeft / totalWidth * svgWidth) : 0);
	let vpRight = $derived(showViewport ? Math.min(svgWidth, (scrollLeft + containerWidth) / totalWidth * svgWidth) : svgWidth);
</script>

<svg width={svgWidth} {height} viewBox="0 0 {svgWidth} {height}" class="daily-chart">
	<!-- Horizontal Ticks -->
	{#each horizontalTicks() as tick}
		{@const isMajor = tick.temp % 5 === 0}
		<line
			x1="0" y1={tick.y} x2={svgWidth} y2={tick.y}
			stroke="white"
			stroke-width={isMajor ? "1" : "0.5"}
			stroke-opacity={isMajor ? "0.25" : "0.1"}
		/>
	{/each}

	<!-- Monday Vertical Lines -->
	{#each mondayIndices as idx}
		{@const x = getX(idx)}
		<line x1={x} y1="0" x2={x} y2={height} stroke="white" stroke-width="1" stroke-opacity="0.2" stroke-dasharray="2 2" />
	{/each}

	<!-- Area between high and low -->
	{#if maxPoints.length >= 2}
		{@const minPointsReversed = [...minPoints].reverse()}
		{@const areaPath = maxPath + ` L ${minPointsReversed.map(p => `${p.x} ${p.y}`).join(' ')} Z`}
		<path d={areaPath} fill="white" fill-opacity="0.1" />
	{/if}

	<path d={maxPath} fill="none" stroke="#FF7043" stroke-width="2.5" stroke-linecap="round" />
	<path d={minPath} fill="none" stroke="#64B5F6" stroke-width="2.5" stroke-linecap="round" />

	{#each maxPoints as point}
		<circle cx={point.x} cy={point.y} r="3" fill="#FF7043" />
	{/each}
	{#each minPoints as point}
		<circle cx={point.x} cy={point.y} r="3" fill="#64B5F6" />
	{/each}

	<!-- Viewport overlay: dim the portions outside the visible window -->
	{#if showViewport}
		{#if vpLeft > 0}
			<rect x="0" y="0" width={vpLeft} {height} fill="black" fill-opacity="0.35" />
		{/if}
		{#if vpRight < svgWidth}
			<rect x={vpRight} y="0" width={svgWidth - vpRight} {height} fill="black" fill-opacity="0.35" />
		{/if}
		<line x1={vpLeft} y1="0" x2={vpLeft} y2={height} stroke="white" stroke-width="1.5" stroke-opacity="0.7" />
		<line x1={vpRight} y1="0" x2={vpRight} y2={height} stroke="white" stroke-width="1.5" stroke-opacity="0.7" />
	{/if}
</svg>

<style>
	.daily-chart {
		display: block;
		pointer-events: none;
	}
</style>
