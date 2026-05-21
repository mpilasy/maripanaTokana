<script lang="ts">
	import type { HourlyForecast } from '$lib/domain/weatherData';

	interface Props {
		forecasts: HourlyForecast[];
		metricPrimary: boolean;
		itemWidth: number;
		itemSpacing: number;
		height?: number;
		scrollLeft?: number;
		containerWidth?: number;
	}

	let { forecasts, metricPrimary, itemWidth, itemSpacing, height = 40, scrollLeft = 0, containerWidth = 0 }: Props = $props();

	let temps = $derived(forecasts.map(f => metricPrimary ? f.temperature.celsius : f.temperature.fahrenheit));
	let minTemp = $derived(Math.min(...temps));
	let maxTemp = $derived(Math.max(...temps));
	let tempRange = $derived(maxTemp - minTemp === 0 ? 1 : maxTemp - minTemp);

	let paddedMin = $derived(minTemp - (tempRange * 0.15));
	let paddedMax = $derived(maxTemp + (tempRange * 0.15));
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

	let points = $derived(temps.map((temp, i) => ({ x: getX(i), y: getY(temp) })));

	let horizontalTicks = $derived(() => {
		const ticks = [];
		const start = Math.ceil(paddedMin);
		const end = Math.floor(paddedMax);
		for (let i = start; i <= end; i++) {
			if (Math.abs(i - minTemp) < 0.2 || Math.abs(i - maxTemp) < 0.2) continue;
			ticks.push(getY(i));
		}
		return ticks;
	});

	let { midnightIndices, noonIndices } = $derived.by(() => {
		const midnight: number[] = [];
		const noon: number[] = [];
		for (let i = 0; i < forecasts.length; i++) {
			const h = new Date(forecasts[i].time).getHours();
			if (h === 0) midnight.push(i);
			else if (h === 12) noon.push(i);
		}
		return { midnightIndices: midnight, noonIndices: noon };
	});

	let pathData = $derived(() => {
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

	let fillData = $derived(() => {
		if (points.length < 2) return '';
		let d = `M ${points[0].x} ${height}`;
		d += ` L ${points[0].x} ${points[0].y}`;
		for (let i = 1; i < points.length; i++) {
			const prev = points[i - 1];
			const curr = points[i];
			const cp1x = prev.x + (curr.x - prev.x) / 2;
			d += ` C ${cp1x} ${prev.y} ${cp1x} ${curr.y} ${curr.x} ${curr.y}`;
		}
		d += ` L ${points[points.length - 1].x} ${height} Z`;
		return d;
	});

	let showViewport = $derived(containerWidth > 0 && totalWidth > containerWidth);
	let vpLeft = $derived(showViewport ? Math.max(0, scrollLeft / totalWidth * svgWidth) : 0);
	let vpRight = $derived(showViewport ? Math.min(svgWidth, (scrollLeft + containerWidth) / totalWidth * svgWidth) : svgWidth);
</script>

<svg width={svgWidth} {height} viewBox="0 0 {svgWidth} {height}" class="temp-chart">
	<defs>
		<linearGradient id="fillGradient" x1="0" y1="0" x2="0" y2="1">
			<stop offset="0%" stop-color="#FFFFFF" stop-opacity="0.15" />
			<stop offset="100%" stop-color="#FFFFFF" stop-opacity="0" />
		</linearGradient>
	</defs>

	<!-- Horizontal Ticks -->
	{#each horizontalTicks() as y}
		<line x1="0" y1={y} x2={svgWidth} y2={y} stroke="white" stroke-width="0.5" stroke-opacity="0.1" />
	{/each}

	<!-- Standout Min/Max Lines -->
	<line x1="0" y1={getY(minTemp)} x2={svgWidth} y2={getY(minTemp)} stroke="white" stroke-width="0.8" stroke-opacity="0.3" stroke-dasharray="2 1" />
	<line x1="0" y1={getY(maxTemp)} x2={svgWidth} y2={getY(maxTemp)} stroke="white" stroke-width="0.8" stroke-opacity="0.3" stroke-dasharray="2 1" />

	<!-- Midnight Vertical Lines + Labels -->
	{#each midnightIndices as idx}
		{@const x = getX(idx)}
		<line x1={x} y1="0" x2={x} y2={height} stroke="white" stroke-width="1" stroke-opacity="0.4" />
		<text x={x} y="9" text-anchor="middle" font-size="8" fill="white" fill-opacity="0.7">00:00</text>
	{/each}

	<!-- Noon Vertical Lines + Labels -->
	{#each noonIndices as idx}
		{@const x = getX(idx)}
		<line x1={x} y1="0" x2={x} y2={height} stroke="white" stroke-width="1" stroke-opacity="0.2" stroke-dasharray="4 4" />
		<text x={x} y="9" text-anchor="middle" font-size="8" fill="white" fill-opacity="0.45">12:00</text>
	{/each}

	<path d={fillData()} fill="url(#fillGradient)" />
	<path d={pathData()} fill="none" stroke="white" stroke-width="2" stroke-opacity="1.0" />

	{#each points as point}
		<circle cx={point.x} cy={point.y} r="3" fill="white" />
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
	.temp-chart {
		display: block;
		pointer-events: none;
	}
</style>
