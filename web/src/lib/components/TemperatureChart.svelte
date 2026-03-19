<script lang="ts">
	import type { HourlyForecast } from '$lib/domain/weatherData';

	interface Props {
		forecasts: HourlyForecast[];
		metricPrimary: boolean;
		itemWidth: number;
		itemSpacing: number;
		height?: number;
	}

	let { forecasts, metricPrimary, itemWidth, itemSpacing, height = 40 }: Props = $props();

	let temps = $derived(forecasts.map(f => metricPrimary ? f.temperature.celsius : f.temperature.fahrenheit));
	let minTemp = $derived(Math.min(...temps));
	let maxTemp = $derived(Math.max(...temps));
	let tempRange = $derived(maxTemp - minTemp === 0 ? 1 : maxTemp - minTemp);

	let paddedMin = $derived(minTemp - (tempRange * 0.15));
	let paddedMax = $derived(maxTemp + (tempRange * 0.15));
	let paddedRange = $derived(paddedMax - paddedMin);

	let totalWidth = $derived(forecasts.length * itemWidth + (forecasts.length - 1) * itemSpacing);

	function getX(index: number) {
		return index * (itemWidth + itemSpacing) + itemWidth / 2;
	}

	function getY(temp: number) {
		return height - ((temp - paddedMin) / paddedRange * height);
	}

	let points = $derived(temps.map((temp, i) => ({ x: getX(i), y: getY(temp) })));

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
</script>

<svg width={totalWidth} {height} viewBox="0 0 {totalWidth} {height}" class="temp-chart">
	<defs>
		<linearGradient id="fillGradient" x1="0" y1="0" x2="0" y2="1">
			<stop offset="0%" stop-color="#FFFFFF" stop-opacity="0.15" />
			<stop offset="100%" stop-color="#FFFFFF" stop-opacity="0" />
		</linearGradient>
	</defs>
	<path d={fillData()} fill="url(#fillGradient)" />
	<path d={pathData()} fill="none" stroke="white" stroke-width="2" stroke-opacity="1.0" />
</svg>

<style>
	.temp-chart {
		display: block;
		pointer-events: none;
	}
</style>
