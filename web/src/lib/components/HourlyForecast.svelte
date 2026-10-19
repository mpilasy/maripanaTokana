<script lang="ts">
	import LevelBar from './LevelBar.svelte';
	import WeatherIcon from './WeatherIcon.svelte';
	import { dragScroll } from '$lib/actions/dragScroll';
	import { _, json } from 'svelte-i18n';
	import type { HourlyForecast as HourlyForecastType } from '$lib/domain/weatherData';
	import { getCardinalDirection } from '$lib/domain/windSpeed';
	import { wmoEmoji } from '$lib/api/wmoWeatherCode';
	import { formatHourInDeviceTime, formatHourAtLocation, isRemoteTimezone } from '$lib/utils/date';
	import type { HourlyAirQuality } from '$lib/domain/weatherData';
	import { AirQualityIndex, AQI_TIER_COLORS, type AqiStandard, type AqiTier } from '$lib/domain/airQuality';
	import DualUnitText from './DualUnitText.svelte';
	import WindCompass from './WindCompass.svelte';
	import TemperatureChart from './TemperatureChart.svelte';
	import ForecastTabs from './ForecastTabs.svelte';
	import OverviewChart from './OverviewChart.svelte';
	import { UV_TIER_COLORS, uvColorFor, type UvTier } from '$lib/domain/uv';

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
	}

	let { forecasts, metricPrimary, dailySunrise, dailySunset, loc, onToggleUnits, utcOffsetSeconds, airQuality = null }: Props = $props();

	let displayMode = $state('Temperature'); // Temperature, Wind, Precipitation, Pressure, AirQuality
	let scrollLeft = $state(0);
	let containerWidth = $state(0);

	let isRemote = $derived(isRemoteTimezone(utcOffsetSeconds));

	let tabs = $derived([
		{ id: 'Temperature', label: $_('tab_conditions') },
		{ id: 'Wind', label: $_('detail_wind') },
		{ id: 'Precipitation', label: $_('detail_precipitation') },
		{ id: 'Pressure', label: $_('detail_pressure') },
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
			return { values, style: 'bars' as const, color: '#64B5F6', highLabel: dual(forecasts[indexOfMax(values)]?.precipitation.displayDual(metricPrimary)) };
		}
		if (displayMode === 'Pressure') {
			const values = forecasts.map((f) => (metricPrimary ? f.pressure.hPa : f.pressure.inHg));
			return { values, color: '#9B8FFF', highLabel: dual(forecasts[indexOfMax(values)]?.pressure.displayDual(metricPrimary)), lowLabel: dual(forecasts[indexOfMin(values)]?.pressure.displayDual(metricPrimary)) };
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

<div class="hourly-wrapper" bind:clientWidth={containerWidth}>
	<ForecastTabs {tabs} selected={displayMode} onSelect={(id) => (displayMode = id)} />
	<div class="cards-scroll" use:dragScroll onscroll={(e) => { scrollLeft = e.currentTarget.scrollLeft; }}>
	<div class="cards-row">
		{#each forecasts as item, i}
			<div class="hourly-card" class:now={i === 0}>
				<span class="hour">{loc(formatHourAtLocation(item.time, utcOffsetSeconds))}</span>
				{#if isRemote}
					<div class="hour-device-container">
						<span class="hour-device-text">{loc(formatHourInDeviceTime(item.time))}</span>
						<span class="hour-device-icon">📱</span>
					</div>
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
				<span class="precip-prob">
					{item.precipProbability > 0 ? loc(`${item.precipProbability}%`) : ''}
				</span>
			</div>
		{/each}
	</div>
	</div>

	{#if displayMode === 'Temperature'}
		<div class="chart-container">
			<TemperatureChart
				{forecasts}
				{metricPrimary}
				itemWidth={104}
				itemSpacing={12}
				height={48}
				{scrollLeft}
				{containerWidth}
			/>
		</div>
	{:else if chart}
		<div class="chart-container">
			<OverviewChart {...chart} itemWidth={104} itemSpacing={12} {scrollLeft} {containerWidth} />
		</div>
	{/if}
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

	.hour-device-container {
		position: relative;
		width: 100%;
		display: flex;
		justify-content: center;
		align-items: center;
		margin-top: -3px;
	}

	.hour-device-text {
		font-size: 9px;
		color: rgba(255,255,255,0.35);
		font-feature-settings: var(--font-features);
		text-align: center;
	}

	.hour-device-icon {
		position: absolute;
		right: 0;
		font-size: 8px;
		opacity: 0.35;
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
</style>
