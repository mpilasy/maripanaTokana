<!-- SPDX-FileCopyrightText: Orinasa Njarasoa -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
<script lang="ts">
	import LevelBar from './LevelBar.svelte';
	import WeatherIcon from './WeatherIcon.svelte';
	import { dragScroll } from '#lib/actions/dragScroll.js';
	import { _, json } from 'svelte-i18n';
	import type { HourlyForecast as HourlyForecastType } from '#lib/domain/weatherData.js';
	import { getCardinalDirection } from '#lib/domain/windSpeed.js';
	import { wmoEmoji } from '#lib/api/wmoWeatherCode.js';
	import { formatHourInDeviceTime, formatHourAtLocation, isRemoteTimezone } from '#lib/utils/date.js';
	import type { HourlyAirQuality } from '#lib/domain/weatherData.js';
	import { AirQualityIndex, AQI_TIER_COLORS, type AqiStandard, type AqiTier } from '#lib/domain/airQuality.js';
	import DualUnitText from './DualUnitText.svelte';
	import HumidityDewPoint, { DEW_COLOR } from './HumidityDewPoint.svelte';
	import WindCompass from './WindCompass.svelte';
	import TemperatureChart from './TemperatureChart.svelte';
	import ForecastTabs from './ForecastTabs.svelte';
	import OverviewChart from './OverviewChart.svelte';
	import ChartScrub from './ChartScrub.svelte';
	import FeltValue from './FeltValue.svelte';
	import { heatIndex, windChill } from '#lib/domain/thermalIndices.js';
	import { HEAT_COLOR, CHILL_COLOR } from '#lib/utils/feltColors.js';
	import { shareItemsPerLine } from '#lib/utils/shareLayout.js';
	import { UV_TIER_COLORS, uvColorFor, type UvTier } from '#lib/domain/uv.js';

	interface Props {
		forecasts: HourlyForecastType[];
		metricPrimary: boolean;
		dailySunrise: number[];
		dailySunset: number[];
		loc: (s: string) => string;
		onToggleUnits: () => void;
		utcOffsetSeconds: number;
		/** Hourly air quality for its tab; the tab is left out without it. */
		airQuality?: { forecasts: HourlyAirQuality[]; primaryStandard: AqiStandard } | null;
		/** In the share image: the items wrap in rows, only the selected tab shows. */
		shareCopy?: boolean;
		/** The selected tab; the caller keeps it, so the share image shows the same tab. */
		displayMode?: string;
	}

	let { forecasts, metricPrimary, dailySunrise, dailySunset, loc, onToggleUnits, utcOffsetSeconds, airQuality = null, shareCopy = false, displayMode = $bindable('Temperature') }: Props = $props();

	let scrollEl = $state<HTMLElement | null>(null);
	// Share image: 6 items a line at most, so that chat apps do not shrink the image much
	let perLine = $derived(shareItemsPerLine(forecasts.length, 6));
	let gridWidth = $derived(perLine * 104 + (perLine - 1) * 12);
	let scrollLeft = $state(0);
	let containerWidth = $state(0);

	let isRemote = $derived(isRemoteTimezone(utcOffsetSeconds));

	// Each hour's heat index and wind chill; each tab shows only when one hour has it
	let heatByItem = $derived(forecasts.map((f) => heatIndex(f.temperature, f.humidity)));
	let chillByItem = $derived(forecasts.map((f) => windChill(f.temperature, f.windSpeed)));

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
	const UV_TIERS: UvTier[] = ['low', 'moderate', 'high', 'veryHigh', 'extreme'];

	function uvLabel(uv: number): string {
		const labels = $json('uv_labels') as string[] | undefined;
		if (!Array.isArray(labels)) return '';
		return labels[uv < 3 ? 0 : uv < 6 ? 1 : uv < 8 ? 2 : uv < 11 ? 3 : 4];
	}
	let aqiUnits = $derived.by(() => {
		const us = $_('air_quality_us_aqi');
		const eu = $_('air_quality_eu_aqi');
		return airQuality?.primaryStandard === 'EUROPEAN' ? [eu, us] : [us, eu];
	});

	// Each hour's air quality as [primary, secondary] index, matched by time; null when missing
	let aqByHour = $derived(forecasts.map((f) => {
		const aq = airQuality?.forecasts.find((a) => Math.abs(a.time - f.time) < 30 * 60 * 1000);
		if (!aq) return null;
		return airQuality!.primaryStandard === 'EUROPEAN' ? [aq.europeanValue, aq.usValue] : [aq.usValue, aq.europeanValue];
	}));

	function aqTier(value: number): AqiTier {
		return AirQualityIndex.tierFor(value, airQuality?.primaryStandard ?? 'US');
	}

	function aqTierLabel(value: number): string {
		const labels = $json('aqi_tier_labels') as string[] | undefined;
		return Array.isArray(labels) ? labels[AQI_TIERS.indexOf(aqTier(value))] ?? '' : '';
	}

	// The chart under the cards for every tab but Conditions, which has its own temperature chart
	let chart = $derived.by(() => {
		const indexOfMax = (vs: (number | null)[]) => vs.reduce<number>((best, v, i) => (v != null && (best < 0 || v > vs[best]!) ? i : best), -1);
		const indexOfMin = (vs: (number | null)[]) => vs.reduce<number>((best, v, i) => (v != null && (best < 0 || v < vs[best]!) ? i : best), -1);
		const dual = (pair: [string, string] | undefined) => (pair ? loc(pair.join(' · ')) : undefined);
		if (displayMode === 'Wind') {
			const values = forecasts.map((f) => (metricPrimary ? f.windSpeed.metersPerSecond : f.windSpeed.mph));
			return { values, fromZero: true, color: '#9B8FFF', highLabel: dual(forecasts[indexOfMax(values)]?.windSpeed.displayDual(metricPrimary)), lowLabel: dual(forecasts[indexOfMin(values)]?.windSpeed.displayDual(metricPrimary)) };
		}
		if (displayMode === 'Precipitation') {
			const values = forecasts.map((f) => f.precipitation.mm);
			return { values, style: 'bars' as const, color: '#64B5F6', highLabel: dual(forecasts[indexOfMax(values)]?.precipitation.displayDual(metricPrimary)), emptyLabel: $_('no_precip') };
		}
		if (displayMode === 'Pressure') {
			const values = forecasts.map((f) => (metricPrimary ? f.pressure.hPa : f.pressure.inHg));
			return { values, color: '#9B8FFF', highLabel: dual(forecasts[indexOfMax(values)]?.pressure.displayDual(metricPrimary)), lowLabel: dual(forecasts[indexOfMin(values)]?.pressure.displayDual(metricPrimary)) };
		}
		if (displayMode === 'HeatIndex' || displayMode === 'WindChill') {
			const felt = displayMode === 'HeatIndex' ? heatByItem : chillByItem;
			const values = felt.map((t) => (t ? (metricPrimary ? t.celsius : t.fahrenheit) : null));
			return { values, color: displayMode === 'HeatIndex' ? HEAT_COLOR : CHILL_COLOR, highLabel: dual(felt[indexOfMax(values)]?.displayDual(metricPrimary)), lowLabel: dual(felt[indexOfMin(values)]?.displayDual(metricPrimary)) };
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
			const values = forecasts.map((f) => (f.uvIndex != null && Number.isFinite(f.uvIndex) ? f.uvIndex : null));
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
		if (displayMode === 'AirQuality') {
			const values = aqByHour.map((v) => v?.[0] ?? null);
			const label = (i: number) => {
				const v = aqByHour[i];
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
		return null;
	});

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

<div class="hourly-wrapper" class:share={shareCopy} style:width={shareCopy ? `${gridWidth}px` : undefined} bind:clientWidth={containerWidth}>
	<ForecastTabs tabs={shareCopy ? tabs.filter((t) => t.id === displayMode) : tabs} selected={displayMode} onSelect={(id) => (displayMode = id)} />
	<div class="cards-scroll" bind:this={scrollEl} use:dragScroll onscroll={(e) => { scrollLeft = e.currentTarget.scrollLeft; }}>
	<div class="cards-row" style:grid-template-columns={shareCopy ? `repeat(${perLine}, 104px)` : undefined}>
		{#each forecasts as item, i}
			<div class="hourly-card" class:now={i === 0}>
				<span class="hour">{loc(formatHourAtLocation(item.time, utcOffsetSeconds))}</span>
				{#if isRemote}
					<!-- The phone's time centred under the place's; the phone icon hangs off its side -->
					<span class="hour-device-text">{loc(formatHourInDeviceTime(item.time))}<svg class="hour-device-icon" viewBox="0 0 24 24" width="10" height="10" fill="currentColor" aria-hidden="true"><path d="M17 1.01L7 1c-1.1 0-2 .9-2 2v18c0 1.1.9 2 2 2h10c1.1 0 2-.9 2-2V3c0-1.1-.9-1.99-2-1.99zM17 19H7V5h10v14z"/></svg></span>
				{/if}
				<WeatherIcon code={item.weatherCode} night={isNightForHour(item.time)} size={44} />
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
					<DualUnitText
						primary={loc(windP)}
						secondary={loc(windS)}
						primarySize="14px"
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
				{:else if displayMode === 'Humidity'}
					<HumidityDewPoint humidity={item.humidity} dewPoint={item.dewPoint} {metricPrimary} {loc} {onToggleUnits} />
				{:else if displayMode === 'HeatIndex' || displayMode === 'WindChill'}
					<FeltValue value={(displayMode === 'HeatIndex' ? heatByItem : chillByItem)[i]} {metricPrimary} {loc} {onToggleUnits} />
				{:else if displayMode === 'Uv'}
					{#if item.uvIndex != null && Number.isFinite(item.uvIndex)}
						<span class="uv-value">{loc(item.uvIndex.toFixed(1))}</span>
						<LevelBar color={uvColorFor(item.uvIndex)} label={uvLabel(item.uvIndex)} />
					{:else}
						<span class="no-data">--</span>
					{/if}
				{:else if displayMode === 'AirQuality'}
					{@const aq = aqByHour[i]}
					{#if aq}
						<DualUnitText primary={loc(String(aq[0]))} secondary={loc(String(aq[1]))} primaryUnit={aqiUnits[0]} secondaryUnit={aqiUnits[1]} primarySize="14px" align="center" />
						<LevelBar color={AQI_TIER_COLORS[aqTier(aq[0])]} label={aqTierLabel(aq[0])} />
					{:else}
						<span class="no-data">--</span>
					{/if}
				{/if}
				<!-- Chance of precipitation, only where it describes the weather -->
				<span class="precip-prob">
					{#if item.precipProbability > 0 && (displayMode === 'Temperature' || displayMode === 'Precipitation')}
						<svg class="drop" viewBox="0 0 24 24" width="9" height="9" fill="currentColor" aria-hidden="true"><path d="M12 2c-5.33 4.55-8 8.48-8 11.8 0 4.98 3.8 8.2 8 8.2s8-3.22 8-8.2c0-3.32-2.67-7.25-8-11.8z"/></svg>{loc(`${item.precipProbability}%`)}
					{/if}
				</span>
			</div>
		{/each}
	</div>
	</div>

	<ChartScrub count={forecasts.length} itemWidth={104} itemSpacing={12} {scrollEl} label={(i) => loc(formatHourAtLocation(forecasts[i].time, utcOffsetSeconds))}>
		{#if displayMode === 'Temperature'}
			<div class="chart-container">
				<TemperatureChart
					{forecasts}
					{metricPrimary}
					itemWidth={104}
					itemSpacing={12}
					height={64}
					{scrollLeft}
					{containerWidth} showBand={!shareCopy}
				/>
			</div>
		{:else if chart}
			<div class="chart-container">
				<OverviewChart {...chart} itemWidth={104} itemSpacing={12} {scrollLeft} {containerWidth} showBand={!shareCopy} />
			</div>
		{/if}
	</ChartScrub>
</div>

<style>
	.hourly-wrapper {
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

	.hourly-card {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 4px;
		padding: 12px;
		background: none;
		border-radius: 16px;
		flex-shrink: 0;
		width: 104px;
	}

	.hourly-card.now {
		background: var(--surface-tile);
	}

	.hour {
		font-size: 12px;
		color: rgba(255,255,255,0.7);
		font-feature-settings: var(--font-features);
		width: 100%;
		text-align: center;
	}

	.hour-device-text {
		position: relative;
		margin-top: -3px;
		font-size: 9px;
		color: rgba(255,255,255,0.4);
		font-feature-settings: var(--font-features);
	}

	.hour-device-icon {
		position: absolute;
		inset-inline-end: 100%;
		top: 50%;
		transform: translateY(-50%);
		margin-inline-end: 4px;
		color: rgba(255, 255, 255, 0.5);
	}

	/* Compass pointing to where the wind comes from, with the direction's short name */
	.wind-dir {
		display: flex;
		align-items: center;
		gap: 4px;
		font-size: 11px;
		color: rgba(255, 255, 255, 0.7);
	}

	.uv-value {
		font-family: var(--font-display);
		font-size: 15px;
		font-weight: 700;
		color: white;
		font-feature-settings: var(--font-features);
	}

	.no-data {
		color: rgba(255, 255, 255, 0.5);
	}

	.precip-prob {
		font-size: 11px;
		color: #64B5F6;
		min-height: 14px;
		font-feature-settings: var(--font-features);
	}

	.precip-prob .drop {
		margin-right: 2px;
	}
</style>
