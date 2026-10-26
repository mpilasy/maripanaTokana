<!-- SPDX-FileCopyrightText: Orinasa Njarasoa -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
<script lang="ts">
	import LevelBar from './LevelBar.svelte';
	import WeatherIcon from './WeatherIcon.svelte';
	import { dragScroll } from '$lib/actions/dragScroll';
	import { _, json } from 'svelte-i18n';
	import type { DailyForecast as DailyForecastType } from '$lib/domain/weatherData';
	import { getCardinalDirection } from '$lib/domain/windSpeed';
	import { wmoEmoji } from '$lib/api/wmoWeatherCode';
	import { formatDayName, formatDayMonth } from '$lib/utils/date';
	import DualUnitText from './DualUnitText.svelte';
	import WindCompass from './WindCompass.svelte';
	import DailyTemperatureChart from './DailyTemperatureChart.svelte';
	import OverviewChart from './OverviewChart.svelte';
	import { UV_TIER_COLORS, uvColorFor, type UvTier } from '$lib/domain/uv';
	import { AirQualityIndex, AQI_TIER_COLORS, type AqiStandard, type AqiTier } from '$lib/domain/airQuality';
	import type { HourlyAirQuality } from '$lib/domain/weatherData';
	import { dailyAirQuality } from '$lib/domain/dailyAirQuality';
	import ForecastTabs from './ForecastTabs.svelte';

	interface Props {
		forecasts: DailyForecastType[];
		metricPrimary: boolean;
		localeTag: string;
		loc: (s: string) => string;
		onToggleUnits: () => void;
		utcOffsetSeconds: number;
		/** Hourly air quality, summed up per day for its tab; the tab is left out without it. */
		airQuality?: { forecasts: HourlyAirQuality[]; primaryStandard: AqiStandard } | null;
	}

	let { forecasts, metricPrimary, localeTag, loc, onToggleUnits, utcOffsetSeconds, airQuality = null }: Props = $props();

	const weekdayNames = $derived($json('weekday_names') as string[] | undefined);
	const monthNames = $derived($json('month_names') as string[] | undefined);

	let displayMode = $state('Temperature'); // Temperature, Wind, Precipitation, Uv
	let scrollLeft = $state(0);
	let containerWidth = $state(0);

	let tabs = $derived([
		{ id: 'Temperature', label: $_('tab_conditions') },
		{ id: 'Precipitation', label: $_('detail_precipitation') },
		{ id: 'Wind', label: $_('detail_wind') },
		{ id: 'Pressure', label: $_('detail_pressure') },
		{ id: 'Uv', label: $_('detail_uv_index') },
		...(airQuality ? [{ id: 'AirQuality', label: $_('detail_air_quality') }] : []),
	]);

	const AQI_TIERS: AqiTier[] = ['good', 'moderate', 'unhealthy', 'very_unhealthy', 'hazardous'];
	let aqiUnits = $derived.by(() => {
		const us = $_('air_quality_us_aqi');
		const eu = $_('air_quality_eu_aqi');
		return airQuality?.primaryStandard === 'EUROPEAN' ? [eu, us] : [us, eu];
	});
	let aqByDay = $derived(airQuality ? dailyAirQuality(forecasts.map((f) => f.date), airQuality.forecasts, airQuality.primaryStandard) : []);

	function aqTier(value: number): AqiTier {
		return AirQualityIndex.tierFor(value, airQuality?.primaryStandard ?? 'US');
	}

	function aqTierLabel(value: number): string {
		const labels = $json('aqi_tier_labels') as string[] | undefined;
		return Array.isArray(labels) ? labels[AQI_TIERS.indexOf(aqTier(value))] ?? '' : '';
	}

	// Each day's low-to-high bar is placed on one scale shared by the whole week
	let weekLow = $derived(Math.min(...forecasts.map((f) => f.tempMin.celsius).filter(Number.isFinite)));
	let weekHigh = $derived(Math.max(...forecasts.map((f) => f.tempMax.celsius).filter(Number.isFinite)));
	const UV_TIERS: UvTier[] = ['low', 'moderate', 'high', 'veryHigh', 'extreme'];

	// The chart under the cards for every tab but Conditions, which has its own temperature chart
	let chart = $derived.by(() => {
		const indexOfMax = (vs: (number | null)[]) => vs.reduce<number>((best, v, i) => (v != null && (best < 0 || v > vs[best]!) ? i : best), -1);
		const indexOfMin = (vs: (number | null)[]) => vs.reduce<number>((best, v, i) => (v != null && (best < 0 || v < vs[best]!) ? i : best), -1);
		const dual = (pair: [string, string] | undefined) => (pair ? loc(pair.join(' · ')) : undefined);
		if (displayMode === 'Wind') {
			const values = forecasts.map((f) => (metricPrimary ? f.windSpeed.metersPerSecond : f.windSpeed.mph));
			return { values, fromZero: true, color: '#9B8FFF', highLabel: dual(forecasts[indexOfMax(values)]?.windSpeed.displayDual(metricPrimary)), lowLabel: dual(forecasts[indexOfMin(values)]?.windSpeed.displayDual(metricPrimary)) };
		}
		if (displayMode === 'Pressure') {
			// The day's mean, a reading whose trend from day to day matters, so a line
			const values = forecasts.map((f) => (f.pressure && Number.isFinite(f.pressure.hPa) ? (metricPrimary ? f.pressure.hPa : f.pressure.inHg) : null));
			return { values, color: '#9B8FFF', highLabel: dual(forecasts[indexOfMax(values)]?.pressure?.displayDual(metricPrimary)), lowLabel: dual(forecasts[indexOfMin(values)]?.pressure?.displayDual(metricPrimary)) };
		}
		if (displayMode === 'Precipitation') {
			const values = forecasts.map((f) => f.precipitation.mm);
			return { values, style: 'bars' as const, color: '#64B5F6', highLabel: dual(forecasts[indexOfMax(values)]?.precipitation.displayDual(metricPrimary)), emptyLabel: $_('no_precip') };
		}
		if (displayMode === 'AirQuality') {
			const values = aqByDay.map((v) => v?.[0] ?? null);
			const label = (i: number) => {
				const v = aqByDay[i];
				return v ? loc(`${v[0]} ${aqiUnits[0]} · ${v[1]} ${aqiUnits[1]}`) : undefined;
			};
			const labels = $json('aqi_tier_labels') as string[] | undefined;
			return {
				fromZero: true,
				values,
				pointColors: values.map((v) => (v != null ? AQI_TIER_COLORS[aqTier(v)] : 'transparent')),
				highLabel: label(indexOfMax(values)),
				lowLabel: label(indexOfMin(values)),
				legend: Array.isArray(labels) ? AQI_TIERS.map((t, i) => ({ color: AQI_TIER_COLORS[t], label: labels[i] })) : undefined,
			};
		}
		if (displayMode === 'Uv') {
			const values = forecasts.map((f) => (f.uvIndexMax != null && Number.isFinite(f.uvIndexMax) ? f.uvIndexMax : null));
			const label = (i: number) => (values[i] != null ? loc(`UV ${values[i]!.toFixed(1)}`) : undefined);
			const labels = $json('uv_labels') as string[] | undefined;
			return {
				fromZero: true,
				values,
				pointColors: values.map((v) => (v != null ? uvColorFor(v) : 'transparent')),
				highLabel: label(indexOfMax(values)),
				lowLabel: label(indexOfMin(values)),
				legend: Array.isArray(labels) ? UV_TIERS.map((t, i) => ({ color: UV_TIER_COLORS[t], label: labels[i] })) : undefined,
			};
		}
		return null;
	});

	function uvLabel(uv: number): string {
		const labels = $json('uv_labels') as string[] | undefined;
		if (!Array.isArray(labels)) return '';
		return labels[uv < 3 ? 0 : uv < 6 ? 1 : uv < 8 ? 2 : uv < 11 ? 3 : 4];
	}

	function barPercent(celsius: number): number {
		const span = weekHigh - weekLow;
		return span > 0 && Number.isFinite(celsius) ? ((celsius - weekLow) / span) * 100 : 50;
	}
</script>

<div class="daily-wrapper" bind:clientWidth={containerWidth}>
	<ForecastTabs {tabs} selected={displayMode} onSelect={(id) => (displayMode = id)} />
	<div class="cards-scroll" use:dragScroll onscroll={(e) => { scrollLeft = e.currentTarget.scrollLeft; }}>
	<div class="cards-row">
		{#each forecasts as item, i}
			<div class="daily-card" class:today={i === 0}>
				<span class="day-name">{formatDayName(item.date, localeTag, utcOffsetSeconds, true, weekdayNames)}</span>
				<span class="day-date">{loc(formatDayMonth(item.date, localeTag, utcOffsetSeconds, monthNames))}</span>
				<WeatherIcon code={item.weatherCode} size={44} />
				{#if displayMode === 'Temperature'}
					{@const [maxP, maxS] = item.tempMax.displayDual(metricPrimary)}
					{@const [minP, minS] = item.tempMin.displayDual(metricPrimary)}
					<DualUnitText primary={loc(maxP)} secondary={loc(maxS)} primarySize="13px" align="center" onClick={onToggleUnits} />
					<div class="range-track" aria-hidden="true">
						<div class="range-fill" style:bottom="{barPercent(item.tempMin.celsius)}%" style:top="{100 - barPercent(item.tempMax.celsius)}%"></div>
					</div>
					<DualUnitText primary={loc(minP)} secondary={loc(minS)} primarySize="13px" align="center" onClick={onToggleUnits} />
				{:else if displayMode === 'Wind'}
					{@const [windP, windS] = item.windSpeed.displayDual(metricPrimary)}
					<DualUnitText
						primary={loc(windP)}
						secondary={loc(windS)}
						primarySize="13px"
						align="center"
						onClick={onToggleUnits}
					/>
					<span class="wind-dir">
						<WindCompass degrees={item.windDeg} size={18} />
						{getCardinalDirection(item.windDeg, $json('cardinal_directions'))}
					</span>
				{:else if displayMode === 'Precipitation'}
					{@const [rainP, rainS] = item.precipitation.displayDual(metricPrimary)}
					<DualUnitText
						primary={loc(rainP)}
						secondary={loc(rainS)}
						primarySize="13px"
						align="center"
						onClick={onToggleUnits}
					/>
				{:else if displayMode === 'Pressure'}
					{#if item.pressure && Number.isFinite(item.pressure.hPa)}
						{@const [pressP, pressS] = item.pressure.displayDual(metricPrimary)}
						<DualUnitText primary={loc(pressP)} secondary={loc(pressS)} primarySize="13px" align="center" onClick={onToggleUnits} />
					{:else}
						<span class="uv-value">--</span>
					{/if}
				{:else if displayMode === 'Uv'}
					{#if item.uvIndexMax != null && Number.isFinite(item.uvIndexMax)}
						<span class="uv-value">{loc(item.uvIndexMax.toFixed(1))}</span>
						<LevelBar color={uvColorFor(item.uvIndexMax)} label={uvLabel(item.uvIndexMax)} />
					{:else}
						<span class="uv-value">--</span>
					{/if}
				{:else if displayMode === 'AirQuality'}
					{@const aq = aqByDay[i]}
					{#if aq}
						<DualUnitText primary={loc(String(aq[0]))} secondary={loc(String(aq[1]))} primaryUnit={aqiUnits[0]} secondaryUnit={aqiUnits[1]} primarySize="13px" align="center" />
						<LevelBar color={AQI_TIER_COLORS[aqTier(aq[0])]} label={aqTierLabel(aq[0])} />
					{:else}
						<span class="uv-value">--</span>
					{/if}
				{/if}
				<!-- Chance of precipitation, only where it describes the weather -->
				<span class="daily-precip">
					{#if item.precipProbability > 0 && (displayMode === 'Temperature' || displayMode === 'Precipitation')}
						<svg class="drop" viewBox="0 0 24 24" width="9" height="9" fill="currentColor" aria-hidden="true"><path d="M12 2c-5.33 4.55-8 8.48-8 11.8 0 4.98 3.8 8.2 8 8.2s8-3.22 8-8.2c0-3.32-2.67-7.25-8-11.8z"/></svg>{loc(`${item.precipProbability}%`)}
					{/if}
				</span>
				{#if displayMode === 'Temperature'}
					<span class="night-icon">{#if item.nightWeatherCode != null}<WeatherIcon code={item.nightWeatherCode} night size={36} />{/if}</span>
				{/if}
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
				height={64}
				{scrollLeft}
				{containerWidth}
			/>
		</div>
	{:else if chart}
		<div class="chart-container">
			<OverviewChart {...chart} itemWidth={96} itemSpacing={12} {scrollLeft} {containerWidth} />
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
		background: none;
		border-radius: 16px;
		flex-shrink: 0;
		width: 96px;
	}

	.daily-card.today {
		background: var(--surface-tile);
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

	/* Compass pointing to where the wind comes from, with the direction's short name */
	.wind-dir {
		display: flex;
		align-items: center;
		gap: 4px;
		font-size: 11px;
		color: rgba(255, 255, 255, 0.7);
	}

	/* Keeps the cards the same height when a night has no icon */
	.night-icon {
		min-height: 30px;
	}

	.range-track {
		position: relative;
		width: 6px;
		height: 56px;
		margin: 2px 0;
		border-radius: 3px;
		background: rgba(255, 255, 255, 0.08);
	}

	.range-fill {
		position: absolute;
		left: 0;
		right: 0;
		min-height: 6px;
		border-radius: 3px;
		background: linear-gradient(to top, #64B5F6, #FF7043);
	}

	.uv-value {
		font-family: var(--font-display);
		font-size: 15px;
		font-weight: 700;
		color: white;
		font-feature-settings: var(--font-features);
	}

	.daily-precip {
		font-size: 11px;
		color: #64B5F6;
		min-height: 14px;
		font-feature-settings: var(--font-features);
	}

	.daily-precip .drop {
		margin-right: 2px;
	}
</style>
