<script lang="ts">
	import type { DailyForecast as DailyForecastType } from '$lib/domain/weatherData';

	interface Props {
		forecasts: DailyForecastType[];
		metricPrimary: boolean;
		height?: number;
	}

	let { forecasts, metricPrimary, height = 48 }: Props = $props();

	let maxTemps = $derived(forecasts.map(f => metricPrimary ? f.tempMax.celsius : f.tempMax.fahrenheit));
	let minTemps = $derived(forecasts.map(f => metricPrimary ? f.tempMin.celsius : f.tempMin.fahrenheit));

	let allTemps = $derived([...maxTemps, ...minTemps]);
	let globalMin = $derived(Math.min(...allTemps));
	let globalMax = $derived(Math.max(...allTemps));
	let tempRange = $derived(globalMax - globalMin === 0 ? 1 : globalMax - globalMin);

	let paddedMin = $derived(globalMin - (tempRange * 0.2));
	let paddedMax = $derived(globalMax + (tempRange * 0.2));
	let paddedRange = $derived(paddedMax - paddedMin);

	function getX(index: number, total: number) {
		if (total <= 1) return 50;
		return (index / (total - 1)) * 100;
	}

	function getY(temp: number) {
		return 100 - ((temp - paddedMin) / paddedRange * 100);
	}

	let maxPoints = $derived(maxTemps.map((temp, i) => ({ x: getX(i, forecasts.length), y: getY(temp) })));
	let minPoints = $derived(minTemps.map((temp, i) => ({ x: getX(i, forecasts.length), y: getY(temp) })));

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

	let maxPath = $derived(generatePath(maxPoints));
	let minPath = $derived(generatePath(minPoints));
</script>

<div class="daily-chart-row">
	<div class="chart-wrapper" style="height: {height}px;">
		<svg width="100%" height="100%" viewBox="0 0 100 100" preserveAspectRatio="none">
			<!-- Horizontal Ticks -->
			{#each horizontalTicks() as tick}
				{@const isMajor = tick.temp % 5 === 0}
				<line 
					x1="0" y1={tick.y} x2="100" y2={tick.y} 
					stroke="white" 
					stroke-width={isMajor ? "0.8" : "0.5"} 
					stroke-opacity={isMajor ? "0.25" : "0.1"} 
					vector-effect="non-scaling-stroke" 
				/>
			{/each}

			<!-- Monday Vertical Lines -->
			{#each mondayIndices as idx}
				{@const x = getX(idx, forecasts.length)}
				<line x1={x} y1="0" x2={x} y2="100" stroke="white" stroke-width="1" stroke-opacity="0.2" stroke-dasharray="2 2" vector-effect="non-scaling-stroke" />
			{/each}

			<!-- Area between high and low -->
			{#if maxPoints.length >= 2}
				{@const minPointsReversed = [...minPoints].reverse()}
				{@const areaPath = maxPath + ` L ${minPointsReversed.map(p => `${p.x} ${p.y}`).join(' ')} Z`}
				<path d={areaPath} fill="white" fill-opacity="0.1" />
			{/if}
			
			<path d={maxPath} fill="none" stroke="#FF7043" stroke-width="2.5" stroke-linecap="round" vector-effect="non-scaling-stroke" />
			<path d={minPath} fill="none" stroke="#64B5F6" stroke-width="2.5" stroke-linecap="round" vector-effect="non-scaling-stroke" />
			
			{#each maxPoints as point}
				<circle cx={point.x} cy={point.y} r="1.5" fill="#FF7043" />
			{/each}
			{#each minPoints as point}
				<circle cx={point.x} cy={point.y} r="1.5" fill="#64B5F6" />
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
