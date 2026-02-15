<script lang="ts">
	import type { HourlyForecast as HourlyForecastType } from '$shared/domain/weatherData';
	import { wmoEmoji } from '$shared/api/wmoWeatherCode';
	import DualUnitText from './DualUnitText.svelte';

	interface Props {
		forecasts: HourlyForecastType[];
		metricPrimary: boolean;
		dailySunrise: number[];
		dailySunset: number[];
		loc: (s: string) => string;
		onToggleUnits: () => void;
	}

	let { forecasts, metricPrimary, dailySunrise, dailySunset, loc, onToggleUnits }: Props = $props();

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
			<span class="emoji">{wmoEmoji(item.weatherCode, isNightForHour(item.time))}</span>
			<DualUnitText
				primary={loc(tempP)}
				secondary={loc(tempS)}
				primarySize="14px"
				align="center"
				onClick={onToggleUnits}
			/>
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

	.precip-prob {
		font-size: 11px;
		color: #64B5F6;
		min-height: 14px;
		font-feature-settings: var(--font-features);
	}
</style>
