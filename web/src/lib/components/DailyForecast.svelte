<!-- SPDX-FileCopyrightText: Orinasa Njarasoa -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
<script lang="ts">
	import LevelBar from './LevelBar.svelte';
	import WeatherIcon from './WeatherIcon.svelte';
	import { dragScroll } from '#lib/actions/dragScroll.js';
	import { _, json } from 'svelte-i18n';
	import type { DailyForecast as DailyForecastType } from '#lib/domain/weatherData.js';
	import { getCardinalDirection } from '#lib/domain/windSpeed.js';
	import { wmoEmoji } from '#lib/api/wmoWeatherCode.js';
	import { formatDayName, formatDayMonth } from '#lib/utils/date.js';
	import DualUnitText from './DualUnitText.svelte';
	import HumidityDewPoint, { DEW_COLOR } from './HumidityDewPoint.svelte';
	import WindCompass from './WindCompass.svelte';
	import DailyTemperatureChart from './DailyTemperatureChart.svelte';
	import OverviewChart from './OverviewChart.svelte';
	import ChartScrub from './ChartScrub.svelte';
	import FeltValue from './FeltValue.svelte';
	import { dailyHeatIndex, dailyWindChill } from '#lib/domain/thermalIndices.js';
	import { HEAT_COLOR, CHILL_COLOR } from '#lib/utils/feltColors.js';
	import { shareItemsPerLine } from '#lib/utils/shareLayout.js';
	import { UV_TIER_COLORS, uvColorFor, type UvTier } from '#lib/domain/uv.js';
	import { AirQualityIndex, AQI_TIER_COLORS, type AqiStandard, type AqiTier } from '#lib/domain/airQuality.js';
	import type { HourlyAirQuality } from '#lib/domain/weatherData.js';
	import { dailyAirQuality } from '#lib/domain/dailyAirQuality.js';
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
		/** In the share image: the items wrap in rows, only the selected tab shows. */
		shareCopy?: boolean;
		/** The selected tab; the caller keeps it, so the share image shows the same tab. */
		displayMode?: string;
	}

	let { forecasts, metricPrimary, localeTag, loc, onToggleUnits, utcOffsetSeconds, airQuality = null, shareCopy = false, displayMode = $bindable('Temperature') }: Props = $props();

	const weekdayNames = $derived($json('weekday_names') as string[] | undefined);
	const monthNames = $derived($json('month_names') as string[] | undefined);

	let scrollEl = $state<HTMLElement | null>(null);
	// Share image: 5 items a line at most, so that chat apps do not shrink the image much
	let perLine = $derived(shareItemsPerLine(forecasts.length, 5));
	let gridWidth = $derived(perLine * 96 + (perLine - 1) * 12);
	let scrollLeft = $state(0);
	let containerWidth = $state(0);

	// Each day's heat index and wind chill; each tab shows only when one day has it
	let heatByItem = $derived(forecasts.map((d) => dailyHeatIndex(d)));
	let chillByItem = $derived(forecasts.map((d) => dailyWindChill(d)));

	let tabs = $derived([
		{ id: 'Temperature', label: $_('tab_conditions') },
		...(heatByItem.some((t) => t) ? [{ id: 'HeatIndex', label: $_('detail_heat_index') }] : []),
		...(chillByItem.some((t) => t) ? [{ id: 'WindChill', label: $_('detail_wind_chill') }] : []),
		{ id: 'Precipitation', label: $_('detail_precipitation') },
		{ id: 'Wind', label: $_('detail_wind') },
		{ id: 'Pressure', label: $_('detail_pressure') },
		{ id: 'Humidity', label: $_('detail_humidity') },
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
		if (displayMode === 'HeatIndex' || displayMode === 'WindChill') {
			const felt = displayMode === 'HeatIndex' ? heatByItem : chillByItem;
			const values = felt.map((t) => (t ? (metricPrimary ? t.celsius : t.fahrenheit) : null));
			return { values, freezingAt: metricPrimary ? 0 : 32, color: displayMode === 'HeatIndex' ? HEAT_COLOR : CHILL_COLOR, highLabel: dual(felt[indexOfMax(values)]?.displayDual(metricPrimary)), lowLabel: dual(felt[indexOfMin(values)]?.displayDual(metricPrimary)) };
		}
		if (displayMode === 'Humidity') {
			const values = forecasts.map((f) => f.humidity ?? null);
			const label = (i: number) => (values[i] != null ? loc(`${values[i]}%`) : undefined);
			const dews = forecasts.map((f) => (f.dewPoint && Number.isFinite(f.dewPoint.celsius) ? (metricPrimary ? f.dewPoint.celsius : f.dewPoint.fahrenheit) : null));
			return {
				values,
				color: '#64B5F6',
				highLabel: label(indexOfMax(values)),
				lowLabel: label(indexOfMin(values)),
				legend: [{ color: '#64B5F6', label: $_('detail_humidity') }, { color: DEW_COLOR, label: $_('detail_dewpoint') }],
				second: { values: dews, color: DEW_COLOR, highLabel: dual(forecasts[indexOfMax(dews)]?.dewPoint?.displayDual(metricPrimary)), lowLabel: dual(forecasts[indexOfMin(dews)]?.dewPoint?.displayDual(metricPrimary)) },
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

<div class="daily-wrapper" class:share={shareCopy} style:width={shareCopy ? `${gridWidth}px` : undefined} bind:clientWidth={containerWidth}>
	<ForecastTabs tabs={shareCopy ? tabs.filter((t) => t.id === displayMode) : tabs} selected={displayMode} onSelect={(id) => (displayMode = id)} />
	<div class="cards-scroll" bind:this={scrollEl} use:dragScroll onscroll={(e) => { scrollLeft = e.currentTarget.scrollLeft; }}>
	<div class="cards-row" style:grid-template-columns={shareCopy ? `repeat(${perLine}, 96px)` : undefined}>
		{#each forecasts as item, i}
			<div class="daily-card" class:today={i === 0}>
				<span class="day-name">{formatDayName(item.date, localeTag, utcOffsetSeconds, true, weekdayNames)}</span>
				<span class="day-date">{loc(formatDayMonth(item.date, localeTag, utcOffsetSeconds, monthNames))}</span>
				<WeatherIcon code={item.weatherCode} size={44} />
				{@render dailyCardValue(item, i)}
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

	<ChartScrub count={forecasts.length} itemWidth={96} itemSpacing={12} {scrollEl} label={(i) => `${formatDayName(forecasts[i].date, localeTag, utcOffsetSeconds, true, weekdayNames)} ${loc(formatDayMonth(forecasts[i].date, localeTag, utcOffsetSeconds, monthNames))}`}>
		{#if displayMode === 'Temperature'}
			<div class="chart-container">
				<DailyTemperatureChart
					{forecasts}
					{metricPrimary}
					itemWidth={96}
					itemSpacing={12}
					height={64}
					{scrollLeft}
					{containerWidth} showBand={!shareCopy}
				/>
			</div>
		{:else if chart}
			<div class="chart-container">
				<OverviewChart {...chart} itemWidth={96} itemSpacing={12} {scrollLeft} {containerWidth} showBand={!shareCopy} />
			</div>
		{/if}
	</ChartScrub>
</div>


<!-- The value under a daily card's icon for the tab displayMode. Android has the same in DailyCardValue (WeatherContent.kt). -->
{#snippet dailyCardValue(item: DailyForecastType, i: number)}
	{#if displayMode === 'Temperature'}
		{@render cardDualValue(item.tempMax.displayDual(metricPrimary))}
		<div class="range-track" aria-hidden="true">
			<div class="range-fill" style:bottom="{barPercent(item.tempMin.celsius)}%" style:top="{100 - barPercent(item.tempMax.celsius)}%"></div>
		</div>
		{@render cardDualValue(item.tempMin.displayDual(metricPrimary))}
	{:else if displayMode === 'Wind'}
		{@render cardDualValue(item.windSpeed.displayDual(metricPrimary))}
		<span class="wind-dir">
			<WindCompass degrees={item.windDeg} size={18} />
			{getCardinalDirection(item.windDeg, $json('cardinal_directions'))}
		</span>
	{:else if displayMode === 'Precipitation'}
		{@render cardDualValue(item.precipitation.displayDual(metricPrimary))}
	{:else if displayMode === 'Pressure'}
		{#if item.pressure && Number.isFinite(item.pressure.hPa)}
			{@render cardDualValue(item.pressure.displayDual(metricPrimary))}
		{:else}
			<span class="uv-value">--</span>
		{/if}
	{:else if displayMode === 'Humidity'}
		<HumidityDewPoint humidity={item.humidity} dewPoint={item.dewPoint} {metricPrimary} {loc} {onToggleUnits} />
	{:else if displayMode === 'HeatIndex' || displayMode === 'WindChill'}
		<FeltValue value={(displayMode === 'HeatIndex' ? heatByItem : chillByItem)[i]} {metricPrimary} {loc} {onToggleUnits} />
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
			<DualUnitText primary={loc(String(aq[0]))} secondary={loc(String(aq[1]))} primaryUnit={aqiUnits[0]} secondaryUnit={aqiUnits[1]} primarySize="0.8125rem" align="center" />
			<LevelBar color={AQI_TIER_COLORS[aqTier(aq[0])]} label={aqTierLabel(aq[0])} />
		{:else}
			<span class="uv-value">--</span>
		{/if}
	{/if}
{/snippet}

<!-- A value in both units on a forecast card; a tap swaps the units -->
{#snippet cardDualValue(dual: [string, string])}
	<DualUnitText primary={loc(dual[0])} secondary={loc(dual[1])} primarySize="0.8125rem" align="center" onClick={onToggleUnits} />
{/snippet}

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

	.share .cards-scroll {
		overflow: visible;
	}

	.share .cards-row {
		display: grid;
		row-gap: 12px;
	}

	/* scrollbar-width: Chrome ignores ::-webkit-scrollbar when an ancestor sets scrollbar-color */
	.cards-scroll {
		scrollbar-width: none;
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
		font-size: 0.75rem;
		font-weight: 500;
		color: white;
		width: 100%;
		text-align: center;
	}

	.day-date {
		font-size: 0.5625rem;
		color: rgba(255,255,255,0.65);
		width: 100%;
		text-align: center;
	}

	/* Compass pointing to where the wind comes from, with the direction's short name */
	.wind-dir {
		display: flex;
		align-items: center;
		gap: 4px;
		font-size: 0.6875rem;
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
		font-size: 0.9375rem;
		font-weight: 700;
		color: white;
		font-feature-settings: var(--font-features);
	}

	.daily-precip {
		font-size: 0.6875rem;
		color: #64B5F6;
		min-height: 14px;
		font-feature-settings: var(--font-features);
	}

	.daily-precip .drop {
		margin-right: 2px;
	}
</style>
