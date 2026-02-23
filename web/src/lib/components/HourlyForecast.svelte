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
		<div class="hourly-card">
			<span class="hour">{loc(formatHour(item.time))}</span>
			<span class="emoji">{wmoEmoji(item.weatherCode, isNightForHour(item.time))}</span>
			<!-- svelte-ignore a11y_click_events_have_key_events -->
			<!-- svelte-ignore a11y_no_static_element_interactions -->
			<span class="value-area" onclick={cycleMode}>
				{#if displayMode === 0}
					<DualUnitText
						primary={loc(tempP)}
						secondary={loc(tempS)}
						primarySize="14px"
						align="center"
						onClick={onToggleUnits}
					/>
				{:else if displayMode === 1}
					{@const [windP] = item.windSpeed.displayDual(metricPrimary)}
					<span class="data-value data-small">{loc(windP)}</span>
					<span class="data-sub">{cardinalDir(item.windDeg)}</span>
				{:else if displayMode === 2}
					{@const [precipP] = item.precipitation.displayDual(metricPrimary)}
					<span class="data-value">{loc(precipP)}</span>
				{:else}
					{@const [pressP] = item.pressure.displayDual(metricPrimary)}
					<span class="data-value data-small">{loc(pressP)}</span>
				{/if}
			</span>
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

	.emoji {
		font-size: 20px;
	}

	.value-area {
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		min-height: 28px;
		cursor: pointer;
		user-select: none;
		-webkit-tap-highlight-color: transparent;
	}

	.data-value {
		font-family: var(--font-display);
		font-size: 13px;
		font-weight: 700;
		color: white;
		white-space: nowrap;
		font-feature-settings: var(--font-features);
	}

	.data-small {
		font-size: 11px;
	}

	.data-sub {
		font-family: var(--font-display);
		font-size: 10px;
		color: rgba(255,255,255,0.55);
		font-feature-settings: var(--font-features);
	}

	.precip-prob {
		font-size: 11px;
		color: #64B5F6;
		min-height: 14px;
		font-feature-settings: var(--font-features);
	}
</style>
