<script lang="ts">
	import type { HourlyForecast as HourlyForecastType } from '$lib/domain/weatherData';
	import { wmoEmoji } from '$lib/api/wmoWeatherCode';
	import DualUnitText from './DualUnitText.svelte';

	const CARDINAL = ['N','NNE','NE','ENE','E','ESE','SE','SSE','S','SSW','SW','WSW','W','WNW','NW','NNW'];
	const DISPLAY_MODES = 4; // 0=temp, 1=wind, 2=precip, 3=pressure

	interface Props {
		forecasts: HourlyForecastType[];
		metricPrimary: boolean;
		dailySunrise: number[];
		dailySunset: number[];
		loc: (s: string) => string;
		onToggleUnits: () => void;
	}

	let { forecasts, metricPrimary, dailySunrise, dailySunset, loc, onToggleUnits }: Props = $props();

	let displayMode = $state(0);

	function cycleMode() {
		displayMode = (displayMode + 1) % DISPLAY_MODES;
	}

	function formatHour(millis: number): string {
		const d = new Date(millis);
		return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
	}

	function isNightForHour(time: number): boolean {
		let dayIdx = 0;
		for (let i = dailySunrise.length - 1; i >= 0; i--) {
			if (dailySunrise[i] <= time) { dayIdx = i; break; }
		}
		const sr = dailySunrise[dayIdx] ?? 0;
		const ss = dailySunset[dayIdx] ?? 0;
		return time < sr || time > ss;
	}

	function cardinalDir(deg: number): string {
		return CARDINAL[((deg % 360 + 360) % 360 * 16 / 360) % 16];
	}
</script>

<div class="hourly-row">
	{#each forecasts as item}
		{@const [tempP, tempS] = item.temperature.displayDual(metricPrimary)}
		{@const [windP, windS] = item.windSpeed.displayDual(metricPrimary)}
		{@const [precipP, precipS] = item.precipitation.displayDual(metricPrimary)}
		{@const [pressP, pressS] = item.pressure.displayDual(metricPrimary)}
		<div class="hourly-card">
			<span class="hour">{loc(formatHour(item.time))}</span>
			<button class="emoji-btn" type="button" onclick={cycleMode}>{wmoEmoji(item.weatherCode, isNightForHour(item.time))}</button>
			{#if displayMode === 0}
				<DualUnitText primary={loc(tempP)} secondary={loc(tempS)} onClick={onToggleUnits} />
			{:else if displayMode === 1}
				<DualUnitText primary={loc(`${windP} ${cardinalDir(item.windDeg)}`)} secondary={loc(`${windS} ${cardinalDir(item.windDeg)}`)} onClick={onToggleUnits} />
			{:else if displayMode === 2}
				<DualUnitText primary={loc(precipP)} secondary={loc(precipS)} onClick={onToggleUnits} />
			{:else}
				<DualUnitText primary={loc(pressP)} secondary={loc(pressS)} onClick={onToggleUnits} />
			{/if}
			<span class="precip-prob">
				{item.precipProbability > 0 ? loc(`${item.precipProbability}%`) : ''}
			</span>
		</div>
	{/each}
</div>

<style>
	.hourly-row {
		display: flex;
		flex-wrap: nowrap;
		gap: 12px;
		overflow-x: auto;
		scroll-snap-type: x mandatory;
		padding: 8px 0;
		max-width: 100%;
		-webkit-overflow-scrolling: touch;
	}

	.hourly-row::-webkit-scrollbar {
		display: none;
	}

	.hourly-card {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 4px;
		padding: 12px;
		background: rgba(42, 31, 165, 0.6);
		border-radius: 16px;
		scroll-snap-align: start;
		flex-shrink: 0;
		min-width: 80px;
	}

	.hour {
		font-size: 12px;
		color: rgba(255,255,255,0.7);
		font-feature-settings: var(--font-features);
	}

	.emoji-btn {
		font-size: 20px;
		cursor: pointer;
		padding: 4px 0;
		background: none;
		border: none;
		color: inherit;
		user-select: none;
		-webkit-tap-highlight-color: transparent;
	}

	.precip-prob {
		font-size: 11px;
		color: #64B5F6;
		min-height: 14px;
		font-feature-settings: var(--font-features);
	}
</style>
