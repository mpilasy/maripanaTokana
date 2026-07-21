<script lang="ts">
	import { _, json } from 'svelte-i18n';
	import type { DailyForecast as DailyForecastType } from '$lib/domain/weatherData';
	import { getCardinalDirection } from '$lib/domain/windSpeed';
	import { wmoEmoji } from '$lib/api/wmoWeatherCode';
	import { formatDayName, formatDayMonth } from '$lib/utils/date';
	import DualUnitText from './DualUnitText.svelte';
	import DailyTemperatureChart from './DailyTemperatureChart.svelte';

	interface Props {
		forecasts: DailyForecastType[];
		metricPrimary: boolean;
		localeTag: string;
		loc: (s: string) => string;
		onToggleUnits: () => void;
		utcOffsetSeconds: number;
	}

	let { forecasts, metricPrimary, localeTag, loc, onToggleUnits, utcOffsetSeconds }: Props = $props();

	let displayMode = $state('Temperature'); // Temperature, Wind, Precipitation
	let scrollLeft = $state(0);
	let containerWidth = $state(0);

	function toggleMode() {
		if (displayMode === 'Temperature') displayMode = 'Wind';
		else if (displayMode === 'Wind') displayMode = 'Precipitation';
		else displayMode = 'Temperature';
	}
</script>

<div class="daily-wrapper" bind:clientWidth={containerWidth}>
	<div class="cards-scroll" onscroll={(e) => { scrollLeft = e.currentTarget.scrollLeft; }}>
	<div class="cards-row">
		{#each forecasts as item}
			<div class="daily-card">
				<span class="day-name">{formatDayName(item.date, localeTag, utcOffsetSeconds, true)}</span>
				<span class="day-date">{loc(formatDayMonth(item.date, localeTag, utcOffsetSeconds))}</span>
				<button class="emoji-btn" onclick={toggleMode} aria-label={$_('android_only.cd_cycle_mode')}>
					{wmoEmoji(item.weatherCode)}
				</button>
				{#if displayMode === 'Temperature'}
					{@const [maxP, maxS] = item.tempMax.displayDual(metricPrimary)}
					{@const [minP, minS] = item.tempMin.displayDual(metricPrimary)}
					<DualUnitText
						primary={loc(`↓${minP} ↑${maxP}`)}
						secondary={loc(`↓${minS} ↑${maxS}`)}
						primarySize="13px"
						align="center"
						onClick={onToggleUnits}
					/>
				{:else if displayMode === 'Wind'}
					{@const [windP, windS] = item.windSpeed.displayDual(metricPrimary)}
					{@const dir = getCardinalDirection(item.windDeg, $json('cardinal_directions'))}
					<DualUnitText
						primary={loc(`${windP} ${dir}`)}
						secondary={loc(windS)}
						primarySize="13px"
						align="center"
						onClick={onToggleUnits}
					/>
				{:else if displayMode === 'Precipitation'}
					{@const [rainP, rainS] = item.precipitation.displayDual(metricPrimary)}
					<DualUnitText
						primary={loc(rainP)}
						secondary={loc(rainS)}
						primarySize="13px"
						align="center"
						onClick={onToggleUnits}
					/>
				{/if}
				<span class="daily-precip">
					{item.precipProbability > 0 ? loc(`${item.precipProbability}%`) : ''}
				</span>
			</div>
		{/each}
	</div>
	</div>

	{#if displayMode === 'Temperature'}
		<div class="chart-container">
			<DailyTemperatureChart
				{forecasts}
				{metricPrimary}
				itemWidth={96}
				itemSpacing={12}
				height={48}
				{scrollLeft}
				{containerWidth}
			/>
		</div>
	{/if}
</div>

<style>
	.daily-wrapper {
		display: flex;
		flex-direction: column;
		width: 100%;
	}

	.cards-scroll {
		overflow-x: auto;
		-webkit-overflow-scrolling: touch;
		padding: 8px 0;
	}

	.cards-scroll::-webkit-scrollbar {
		display: none;
	}

	.cards-row {
		display: flex;
		flex-wrap: nowrap;
		gap: 12px;
		margin-bottom: 4px;
	}

	.chart-container {
		pointer-events: none;
		margin-top: -8px; /* Pull up slightly to feel "attached" */
	}

	.daily-card {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 4px;
		padding: 12px;
		background: rgba(42, 31, 165, 0.45);
		border-radius: 16px;
		flex-shrink: 0;
		width: 96px;
	}

	.day-name {
		font-size: 12px;
		font-weight: 500;
		color: white;
		width: 100%;
		text-align: center;
	}

	.day-date {
		font-size: 9px;
		color: rgba(255,255,255,0.4);
		width: 100%;
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

	.daily-precip {
		font-size: 11px;
		color: #64B5F6;
		min-height: 14px;
		font-feature-settings: var(--font-features);
	}
</style>
