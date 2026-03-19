<script lang="ts">
	import { _ } from 'svelte-i18n';
	import type { HourlyForecast as HourlyForecastType } from '$lib/domain/weatherData';
	import { getCardinalDirection } from '$lib/domain/windSpeed';
	import { wmoEmoji } from '$lib/api/wmoWeatherCode';
	import { formatHourInDeviceTime, isRemoteTimezone } from '$lib/utils/date';
	import DualUnitText from './DualUnitText.svelte';

	interface Props {
		forecasts: HourlyForecastType[];
		metricPrimary: boolean;
		dailySunrise: number[];
		dailySunset: number[];
		loc: (s: string) => string;
		onToggleUnits: () => void;
		utcOffsetSeconds: number;
	}

	let { forecasts, metricPrimary, dailySunrise, dailySunset, loc, onToggleUnits, utcOffsetSeconds }: Props = $props();

	let displayMode = $state('Temperature'); // Temperature, Wind, Precipitation, Pressure

	let isRemote = $derived(isRemoteTimezone(utcOffsetSeconds));

	function toggleMode() {
		if (displayMode === 'Temperature') displayMode = 'Wind';
		else if (displayMode === 'Wind') displayMode = 'Precipitation';
		else if (displayMode === 'Precipitation') displayMode = 'Pressure';
		else displayMode = 'Temperature';
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
</script>

<div class="hourly-row">
	{#each forecasts as item}
		{@const [tempP, tempS] = item.temperature.displayDual(metricPrimary)}
		<div class="hourly-card">
			<span class="hour">{loc(formatHour(item.time))}</span>
			{#if isRemote}
				<span class="hour-device">{loc(formatHourInDeviceTime(item.time, utcOffsetSeconds))} &nbsp;📱</span>
			{/if}
			<button class="emoji-btn" onclick={toggleMode}>
				{wmoEmoji(item.weatherCode, isNightForHour(item.time))}
			</button>
			{#if displayMode === 'Temperature'}
				{@const [tempP, tempS] = item.temperature.displayDual(metricPrimary)}
				<DualUnitText
					primary={loc(tempP)}
					secondary={loc(tempS)}
					primarySize="14px"
					align="center"
					onClick={onToggleUnits}
				/>
			{:else if displayMode === 'Wind'}
				{@const [windP, windS] = item.windSpeed.displayDual(metricPrimary)}
				{@const dir = getCardinalDirection(item.windDeg, $_('cardinal_directions'))}
				<DualUnitText
					primary={loc(`${windP} ${dir}`)}
					secondary={loc(windS)}
					primarySize="14px"
					align="center"
					onClick={onToggleUnits}
				/>
			{:else if displayMode === 'Precipitation'}
				{@const [rainP, rainS] = item.precipitation.displayDual(metricPrimary)}
				<DualUnitText
					primary={loc(rainP)}
					secondary={loc(rainS)}
					primarySize="14px"
					align="center"
					onClick={onToggleUnits}
				/>
			{:else if displayMode === 'Pressure'}
				{@const [pressP, pressS] = item.pressure.displayDual(metricPrimary)}
				<DualUnitText
					primary={loc(pressP)}
					secondary={loc(pressS)}
					primarySize="14px"
					align="center"
					onClick={onToggleUnits}
				/>
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

	.hour-device {
		font-size: 9px;
		color: rgba(255,255,255,0.35);
		font-feature-settings: var(--font-features);
		margin-top: -3px;
		text-align: center;
	}

	.emoji-btn {
		font-size: 20px;
		background: none;
		border: none;
		color: inherit;
		cursor: pointer;
		padding: 0;
		font-family: inherit;
	}

	.precip-prob {
		font-size: 11px;
		color: #64B5F6;
		min-height: 14px;
		font-feature-settings: var(--font-features);
	}
</style>
