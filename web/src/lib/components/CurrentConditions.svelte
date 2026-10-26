<!-- SPDX-FileCopyrightText: Orinasa Njarasoa -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
<script lang="ts">
	import WeatherIcon from './WeatherIcon.svelte';
	import { _, json } from 'svelte-i18n';
	import type { WeatherData } from '#lib/domain/weatherData.js';
	import { getCardinalDirection } from '#lib/domain/windSpeed.js';
	import { AQI_TIER_COLORS, aqiDialPosition } from '#lib/domain/airQuality.js';
	import { UV_TIER_COLORS, uvColorFor, uvDialPosition, type UvTier } from '#lib/domain/uv.js';
	import DetailCard from './DetailCard.svelte';
	import { formatHourAtLocation } from '#lib/utils/date.js';
	import Gauge from './Gauge.svelte';
	import WindCompass from './WindCompass.svelte';
	import SunArc from './SunArc.svelte';

	interface Props {
		data: WeatherData;
		metricPrimary: boolean;
		loc: (s: string) => string;
		onToggleUnits: () => void;
		onOpenAirQuality: () => void;
		/** 'now': temperature, rain, wind, pressure, humidity; 'sky': sun, air quality, UV, visibility. */
		part: 'now' | 'sky';
	}

	let { data, metricPrimary, loc, onToggleUnits, onOpenAirQuality, part }: Props = $props();

	const UV_TIERS: UvTier[] = ['low', 'moderate', 'high', 'veryHigh', 'extreme'];
	const AQI_TIERS = ['good', 'moderate', 'unhealthy', 'very_unhealthy', 'hazardous'];
	// Gauge scales: sea-level pressure rarely leaves 960-1050 hPa; AQI tops out at "hazardous"
	const PRESSURE_MIN_HPA = 960;
	const PRESSURE_MAX_HPA = 1050;

	// Sunrise and sunset in the place's own time, like the hourly cards and its clock
	function formatTime(epochSec: number): string {
		return formatHourAtLocation(epochSec * 1000, data.utcOffsetSeconds);
	}


	function getUvLabel(uv: number): string {
		const labels: string[] = $json('uv_labels') as string[];
		if (!Array.isArray(labels)) return '';
		if (uv < 3) return labels[0];
		if (uv < 6) return labels[1];
		if (uv < 8) return labels[2];
		if (uv < 11) return labels[3];
		return labels[4];
	}

	function visibilityDisplay(meters: number | null, metric: boolean): [string, string] {
		if (meters == null) return ['--', '--'];
		const km = (meters / 1000).toFixed(1);
		const mi = (meters / 1609.34).toFixed(2);
		return metric
			? [`${km} km`, `${mi} mi`]
			: [`${mi} mi`, `${km} km`];
	}

	let tempDual = $derived(data.temperature.displayDual(metricPrimary));
	let feelsLikeDual = $derived(data.feelsLike.displayDual(metricPrimary));
	let windDual = $derived(data.windSpeed.displayDual(metricPrimary));
	let gustDual = $derived(data.windGust?.displayDual(metricPrimary));
	let pressDual = $derived(data.pressure.displayDual(metricPrimary));
	let dewDual = $derived(data.dewPoint.displayDual(metricPrimary));
	let visDual = $derived(visibilityDisplay(data.visibility, metricPrimary));
	let cloudCover = $derived(`${$_('detail_cloud_cover')}: ${data.cloudCover != null ? loc(`${data.cloudCover}%`) : '--'}`);
</script>

<div class="conditions-grid">
	{#if part === 'now'}
	<!-- Temperature / Precipitation -->
	<DetailCard
		title={$_('detail_temperature')}
		value={loc(tempDual[0])}
		secondaryValue={loc(tempDual[1])}
		subtitle={`${$_('feels_like')} ${loc(feelsLikeDual[0])} · ${loc(feelsLikeDual[1])}`}
		{onToggleUnits}
	/>
	{#if data.snow}
		{@const [snowP, snowS] = data.snow.displayDual(metricPrimary)}
		<DetailCard title={$_('detail_precipitation')} value={loc(snowP)} secondaryValue={loc(snowS)} subtitle={cloudCover} {onToggleUnits}>
			{#snippet cornerSnippet()}<WeatherIcon code={71} size={44} />{/snippet}
		</DetailCard>
	{:else if data.rain}
		{@const [rainP, rainS] = data.rain.displayDual(metricPrimary)}
		<DetailCard title={$_('detail_precipitation')} value={loc(rainP)} secondaryValue={loc(rainS)} subtitle={cloudCover} {onToggleUnits}>
			{#snippet cornerSnippet()}<WeatherIcon code={61} size={44} />{/snippet}
		</DetailCard>
	{:else}
		<DetailCard title={$_('detail_precipitation')} value={$_('no_precip')} subtitle={cloudCover} />
	{/if}

	<!-- Wind / Gust -->
	<DetailCard
		title={$_('detail_wind')}
		value={loc(windDual[0])}
		secondaryValue={loc(windDual[1])}
		subtitle={data.windDeg != null ? loc(`${getCardinalDirection(data.windDeg, $json('cardinal_directions'))} (${data.windDeg}°)`) : '--'}
		{onToggleUnits}
	>
		{#snippet dialSnippet()}
			{#if data.windDeg != null}
				<WindCompass degrees={data.windDeg} size={56} />
			{/if}
		{/snippet}
	</DetailCard>
	<DetailCard
		title={$_('detail_wind_gust')}
		value={gustDual ? loc(gustDual[0]) : '--'}
		secondaryValue={gustDual ? loc(gustDual[1]) : undefined}
		{onToggleUnits}
	/>

	<!-- Pressure / Humidity -->
	<DetailCard
		title={$_('detail_pressure')}
		value={loc(pressDual[0])}
		secondaryValue={loc(pressDual[1])}
		{onToggleUnits}
	>
		{#snippet dialSnippet()}
			<Gauge fraction={(data.pressure.hPa - PRESSURE_MIN_HPA) / (PRESSURE_MAX_HPA - PRESSURE_MIN_HPA)} color="#9B8FFF" size={56} />
		{/snippet}
	</DetailCard>
	<DetailCard
		title={$_('detail_humidity')}
		value={data.humidity != null ? loc(`${data.humidity}%`) : '--'}
		subtitle={`${$_('detail_dewpoint')} ${loc(dewDual[0])} · ${loc(dewDual[1])}`}
		fillPercent={data.humidity ?? undefined}
	/>

	{:else}
	<!-- Sun / Air quality (the sun spans the row when there is no air-quality reading) -->
	<div class="sun-tile" class:wide={!data.airQuality}>
		<span class="tile-title">{$_('detail_sun')}</span>
		<SunArc sunrise={data.sunrise} sunset={data.sunset} now={Date.now() / 1000} />
		<div class="sun-times">
			<span><span class="sun-label">{$_('detail_sunrise')}</span> {loc(formatTime(data.sunrise))}</span>
			<span><span class="sun-label">{$_('detail_sunset')}</span> {loc(formatTime(data.sunset))}</span>
		</div>
	</div>
	{#if data.airQuality}
		{@const aqi = data.airQuality}
		{@const [aqiP, aqiS] = aqi.displayDual()}
		{@const us = $_('air_quality_us_aqi')}
		{@const eu = $_('air_quality_eu_aqi')}
		<DetailCard
			title={$_('detail_air_quality')}
			value={loc(aqiP)}
			secondaryValue={loc(aqiS)}
			unit={aqi.primaryStandard === 'EUROPEAN' ? eu : us}
			secondaryUnit={aqi.primaryStandard === 'EUROPEAN' ? us : eu}
			subtitle={($json('aqi_tier_labels') as string[])?.[AQI_TIERS.indexOf(aqi.primaryTier)] ?? ''}
			onClick={onOpenAirQuality}
		>
			{#snippet dialSnippet()}
				<Gauge fraction={aqiDialPosition(aqi.primaryStandard === 'EUROPEAN' ? aqi.europeanValue : aqi.usValue, aqi.primaryStandard)} color={AQI_TIER_COLORS[aqi.primaryTier]}
					segments={AQI_TIERS.map((t) => AQI_TIER_COLORS[t as keyof typeof AQI_TIER_COLORS])} size={56} />
			{/snippet}
		</DetailCard>
	{/if}

	<!-- UV Index (current reading) / Visibility -->
	<DetailCard
		title={$_('detail_uv_index')}
		value={data.uvIndex != null ? loc(data.uvIndex.toFixed(1)) : '--'}
		subtitle={data.uvIndex != null ? getUvLabel(data.uvIndex) : undefined}
	>
		{#snippet dialSnippet()}
			{#if data.uvIndex != null}
				<Gauge fraction={uvDialPosition(data.uvIndex)} color={uvColorFor(data.uvIndex)}
					segments={UV_TIERS.map((t) => UV_TIER_COLORS[t])} size={56} />
			{/if}
		{/snippet}
	</DetailCard>
	<DetailCard
		title={$_('detail_visibility')}
		value={loc(visDual[0])}
		secondaryValue={loc(visDual[1])}
		{onToggleUnits}
	/>
	{/if}
</div>

<style>
	.conditions-grid {
		display: grid;
		grid-template-columns: 1fr 1fr;
		grid-auto-rows: auto;
		gap: 10px;
	}

	.sun-tile {
		background: var(--surface-tile);
		border-radius: 16px;
		padding: 16px;
		display: flex;
		flex-direction: column;
		gap: 6px;
		min-width: 0;
	}

	.sun-tile.wide {
		grid-column: span 2;
	}

	.tile-title {
		font-size: 13px;
		color: rgba(255, 255, 255, 0.6);
	}

	.sun-times {
		display: flex;
		justify-content: space-between;
		gap: 8px;
		font-family: var(--font-display);
		font-size: 15px;
		font-weight: 700;
		color: white;
		font-feature-settings: var(--font-features);
	}

	.sun-times > span {
		display: flex;
		flex-direction: column;
	}

	.sun-times > span:last-child {
		text-align: end;
	}

	.sun-label {
		font-family: var(--font-body, inherit);
		font-size: 11px;
		font-weight: 400;
		color: rgba(255, 255, 255, 0.5);
	}

</style>
