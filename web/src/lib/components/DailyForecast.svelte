<script lang="ts">
	import { _ } from 'svelte-i18n';
	import type { DailyForecast as DailyForecastType } from '$lib/domain/weatherData';
	import { getCardinalDirection } from '$lib/domain/windSpeed';
	import { wmoEmoji, wmoDescriptionKey } from '$lib/api/wmoWeatherCode';
	import { formatDayName, formatDayMonth } from '$lib/utils/date';
	import DualUnitText from './DualUnitText.svelte';
	import DailyTemperatureChart from './DailyTemperatureChart.svelte';

	interface Props {
		forecasts: DailyForecastType[];
		metricPrimary: boolean;
		localeTag: string;
		loc: (s: string) => string;
		onToggleUnits: () => void;
	}

	let { forecasts, metricPrimary, localeTag, loc, onToggleUnits }: Props = $props();

	let displayMode = $state('Temperature'); // Temperature, Wind, Precipitation

	function toggleMode() {
		if (displayMode === 'Temperature') displayMode = 'Wind';
		else if (displayMode === 'Wind') displayMode = 'Precipitation';
		else displayMode = 'Temperature';
	}

</script>

<div class="daily-list">
	{#if forecasts.length > 0}
		<DailyTemperatureChart {forecasts} {metricPrimary} />
	{/if}
	{#each forecasts as item}
		<div class="daily-row">
			<div class="day-info">
				<span class="day-name">{formatDayName(item.date, localeTag)}</span>
				<span class="day-date">{loc(formatDayMonth(item.date, localeTag))}</span>
			</div>
			<button class="daily-weather-btn" onclick={toggleMode}>
				{wmoEmoji(item.weatherCode)} {$_(wmoDescriptionKey(item.weatherCode))}
			</button>
			<span class="daily-precip">
				{item.precipProbability > 0 ? loc(`${item.precipProbability}%`) : ''}
			</span>
			{#if displayMode === 'Temperature'}
				{@const [maxP, maxS] = item.tempMax.displayDual(metricPrimary)}
				{@const [minP, minS] = item.tempMin.displayDual(metricPrimary)}
				<DualUnitText
					primary={loc(`\u2191${maxP} \u2193${minP}`)}
					secondary={loc(`\u2191${maxS} \u2193${minS}`)}
					primarySize="13px"
					onClick={onToggleUnits}
				/>
			{:else if displayMode === 'Wind'}
				{@const [windP, windS] = item.windSpeed.displayDual(metricPrimary)}
				{@const dir = getCardinalDirection(item.windDeg, $_('cardinal_directions'))}
				<DualUnitText
					primary={loc(`${windP} ${dir}`)}
					secondary={loc(windS)}
					primarySize="13px"
					onClick={onToggleUnits}
				/>
			{:else if displayMode === 'Precipitation'}
				{@const [rainP, rainS] = item.precipitation.displayDual(metricPrimary)}
				<DualUnitText
					primary={loc(rainP)}
					secondary={loc(rainS)}
					primarySize="13px"
					onClick={onToggleUnits}
				/>
			{/if}
		</div>
	{/each}
</div>

<style>
	.daily-list {
		display: flex;
		flex-direction: column;
		gap: 8px;
	}

	.daily-row {
		display: flex;
		align-items: center;
		gap: 8px;
		padding: 12px 16px;
		background: rgba(42, 31, 165, 0.3);
		border-radius: 12px;
	}

	.day-info {
		display: flex;
		flex-direction: column;
		width: 100px;
		flex-shrink: 0;
	}

	.day-name {
		font-size: 14px;
		font-weight: 500;
		color: white;
	}

	.day-date {
		font-size: 10px;
		color: rgba(255,255,255,0.4);
	}

	.daily-weather-btn {
		font-size: 12px;
		color: rgba(255,255,255,0.7);
		flex: 1;
		min-width: 0;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
		background: none;
		border: none;
		cursor: pointer;
		text-align: left;
		padding: 0;
		font-family: inherit;
	}

	.daily-precip {
		font-size: 11px;
		color: #64B5F6;
		min-width: 30px;
		text-align: end;
		font-feature-settings: var(--font-features);
	}
</style>
