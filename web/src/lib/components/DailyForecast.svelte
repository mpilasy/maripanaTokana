<script lang="ts">
	import { _ } from 'svelte-i18n';
	import type { DailyForecast as DailyForecastType } from '$lib/domain/weatherData';
	import { wmoEmoji, wmoDescriptionKey } from '$lib/api/wmoWeatherCode';
	import DualUnitText from './DualUnitText.svelte';

	const CARDINAL = ['N','NNE','NE','ENE','E','ESE','SE','SSE','S','SSW','SW','WSW','W','WNW','NW','NNW'];
	const DISPLAY_MODES = 5; // 0=emoji+desc, 1=hi-lo, 2=wind, 3=precip, 4=pressure

	interface Props {
		forecasts: DailyForecastType[];
		metricPrimary: boolean;
		localeTag: string;
		loc: (s: string) => string;
		onToggleUnits: () => void;
	}

	let { forecasts, metricPrimary, localeTag, loc, onToggleUnits }: Props = $props();

	let displayMode = $state(0);

	function cycleMode() {
		displayMode = (displayMode + 1) % DISPLAY_MODES;
	}

	function formatDayName(millis: number): string {
		return new Intl.DateTimeFormat(localeTag, { weekday: 'long' }).format(new Date(millis));
	}

	function formatDayMonth(millis: number): string {
		return new Intl.DateTimeFormat(localeTag, { day: 'numeric', month: 'short' }).format(new Date(millis));
	}

	function cardinalDir(deg: number): string {
		return CARDINAL[((deg % 360 + 360) % 360 * 16 / 360) % 16];
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
			<!-- svelte-ignore a11y_click_events_have_key_events -->
			<!-- svelte-ignore a11y_no_static_element_interactions -->
			<span class="daily-weather" onclick={cycleMode}>
				{#if displayMode === 0}
					{wmoEmoji(item.weatherCode)} {$_(wmoDescriptionKey(item.weatherCode))}
				{:else if displayMode === 1}
					{loc(`\u2191${maxP} \u2193${minP}`)}
				{:else if displayMode === 2}
					{@const [windP] = item.windSpeedMax.displayDual(metricPrimary)}
					{loc(windP)} {cardinalDir(item.windDeg)}
				{:else if displayMode === 3}
					{@const [precipP] = item.precipitationSum.displayDual(metricPrimary)}
					{loc(precipP)}
				{:else}
					—
				{/if}
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
		cursor: pointer;
		user-select: none;
		-webkit-tap-highlight-color: transparent;
		font-feature-settings: var(--font-features);
	}

	.daily-precip {
		font-size: 11px;
		color: #64B5F6;
		min-width: 30px;
		text-align: end;
		font-feature-settings: var(--font-features);
	}
</style>
