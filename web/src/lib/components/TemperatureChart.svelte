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
		const tempObj = forecasts[midIdx].temperature;
		const label = metricPrimary ? tempObj.displayCelsius() : tempObj.displayFahrenheit();
		return { x: pt.x, y: Math.min(height - 4, pt.y + 12), label };
	});

	let minPointInfo = $derived.by(() => {
		if (points.length === 0 || forecasts.length === 0) return null;
		const maxY = Math.max(...points.map(p => p.y));
		const minIndices = points.map((p, i) => p.y === maxY ? i : -1).filter(i => i !== -1);
		const midIdx = minIndices[Math.floor(minIndices.length / 2)];
		const pt = points[midIdx];
		const tempObj = forecasts[midIdx].temperature;
		const label = metricPrimary ? tempObj.displayCelsius() : tempObj.displayFahrenheit();
		return { x: pt.x, y: Math.max(10, pt.y - 2), label };
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

	{#if fillPathStr}
		<path d={fillPathStr} fill="url(#fillGradient)" />
	{/if}
	{#if linePathStr}
		<path d={linePathStr} fill="none" stroke="white" stroke-width="2" stroke-opacity="1.0" />
	{/if}

	{#each points as point}
		<circle cx={point.x} cy={point.y} r="3" fill="white" />
	{/each}

	{#if maxPointInfo}
		<text x={maxPointInfo.x} y={maxPointInfo.y} text-anchor="middle" font-size="9" font-weight="bold" fill="#FF7043">{maxPointInfo.label}</text>
	{/if}
	{#if minPointInfo}
		<text x={minPointInfo.x} y={minPointInfo.y} text-anchor="middle" font-size="9" font-weight="bold" fill="#64B5F6">{minPointInfo.label}</text>
	{/if}

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
