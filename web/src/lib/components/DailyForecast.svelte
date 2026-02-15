<script lang="ts">
	import { _ } from 'svelte-i18n';
	import type { DailyForecast as DailyForecastType } from '$lib/domain/weatherData';
	import { wmoEmoji, wmoDescriptionKey } from '$lib/api/wmoWeatherCode';
	import DualUnitText from './DualUnitText.svelte';

	interface Props {
		forecasts: DailyForecastType[];
		metricPrimary: boolean;
		localeTag: string;
		loc: (s: string) => string;
		onToggleUnits: () => void;
	}

	let { forecasts, metricPrimary, localeTag, loc, onToggleUnits }: Props = $props();

	function formatDayName(millis: number): string {
		return new Intl.DateTimeFormat(localeTag, { weekday: 'long' }).format(new Date(millis));
	}

	function formatDayMonth(millis: number): string {
		return new Intl.DateTimeFormat(localeTag, { day: 'numeric', month: 'short' }).format(new Date(millis));
	}
</script>

<div class="daily-list">
	{#each forecasts as item}
		{@const [maxP, maxS] = item.tempMax.displayDual(metricPrimary)}
		{@const [minP, minS] = item.tempMin.displayDual(metricPrimary)}
		<div class="daily-row">
			<div class="day-info">
				<span class="day-name">{formatDayName(item.date)}</span>
				<span class="day-date">{loc(formatDayMonth(item.date))}</span>
			</div>
			<span class="daily-weather">
				{wmoEmoji(item.weatherCode)} {$_(wmoDescriptionKey(item.weatherCode))}
			</span>
			<span class="daily-precip">
				{item.precipProbability > 0 ? loc(`${item.precipProbability}%`) : ''}
			</span>
			<DualUnitText
				primary={loc(`\u2191${maxP} \u2193${minP}`)}
				secondary={loc(`\u2191${maxS} \u2193${minS}`)}
				primarySize="13px"
				onClick={onToggleUnits}
			/>
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

	.daily-weather {
		font-size: 12px;
		color: rgba(255,255,255,0.7);
		flex: 1;
		min-width: 0;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.daily-precip {
		font-size: 11px;
		color: #64B5F6;
		min-width: 30px;
		text-align: end;
		font-feature-settings: var(--font-features);
	}
</style>
