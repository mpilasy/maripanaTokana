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
	import SkyArc from './SkyArc.svelte';
	import MoonPhaseIcon from './MoonPhaseIcon.svelte';
	import DialValue from './DialValue.svelte';
	import AqiTierBadge from './AqiTierBadge.svelte';
	import { moonArcProgress, moonIllumination, moonPhaseIndex, moonPosition, moonTimes } from '#lib/domain/moon.js';
	import { moonLitPath } from '#lib/utils/moonPath.js';

	interface Props {
		data: WeatherData;
		metricPrimary: boolean;
		loc: (s: string) => string;
		onToggleUnits: () => void;
		onOpenAirQuality: () => void;
		/** 'now': temperature, rain, wind, pressure, humidity; 'sky': sun, moon, air quality, UV, cloud cover, visibility. */
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

	// The length of the day (hours:minutes), shown under the sun's arc
	let daylight = $derived.by(() => {
		if (data.sunset <= data.sunrise) return null;
		const minutes = Math.floor((data.sunset - data.sunrise) / 60);
		return `${Math.floor(minutes / 60)}:${String(minutes % 60).padStart(2, '0')}`;
	});

	// The moon at the place now; today runs from its midnight
	let moon = $derived.by(() => {
		const now = Date.now();
		const offsetMs = data.utcOffsetSeconds * 1000;
		const dayStart = Math.floor((now + offsetMs) / 86_400_000) * 86_400_000 - offsetMs;
		const days = [-1, 0, 1].map((d) => moonTimes(dayStart + d * 86_400_000, data.latitude, data.longitude));
		const illumination = moonIllumination(now);
		const position = moonPosition(now, data.latitude, data.longitude);
		return { illumination, position, today: days[1], arcT: moonArcProgress(now, days), southern: data.latitude < 0 };
	});
	let moonAzimuth = $derived(Math.round(moon.position.azimuth) % 360);
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
		<DetailCard title={$_('detail_precipitation')} value={loc(snowP)} secondaryValue={loc(snowS)} {onToggleUnits}>
			{#snippet cornerSnippet()}<WeatherIcon code={71} size={44} />{/snippet}
		</DetailCard>
	{:else if data.rain}
		{@const [rainP, rainS] = data.rain.displayDual(metricPrimary)}
		<DetailCard title={$_('detail_precipitation')} value={loc(rainP)} secondaryValue={loc(rainS)} {onToggleUnits}>
			{#snippet cornerSnippet()}<WeatherIcon code={61} size={44} />{/snippet}
		</DetailCard>
	{:else}
		<DetailCard title={$_('detail_precipitation')} value={$_('no_precip')} />
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
		{#snippet centerDialSnippet()}
			<Gauge fraction={(data.pressure.hPa - PRESSURE_MIN_HPA) / (PRESSURE_MAX_HPA - PRESSURE_MIN_HPA)} color="#9B8FFF">
				<DialValue value={loc(pressDual[0].slice(0, pressDual[0].lastIndexOf(' ')))} unit={pressDual[0].slice(pressDual[0].lastIndexOf(' ') + 1)} secondary={loc(pressDual[1])} />
			</Gauge>
		{/snippet}
	</DetailCard>
	<DetailCard
		title={$_('detail_humidity')}
		value={data.humidity != null ? loc(`${data.humidity}%`) : '--'}
		subtitle={`${$_('detail_dewpoint')}\n${loc(dewDual[0])} · ${loc(dewDual[1])}`}
		fillPercent={data.humidity ?? undefined}
	/>

	{:else}
	<!-- Sun / Moon, air quality / UV, cloud cover / visibility (without air quality: UV / cloud cover, then visibility) -->
	<div class="sky-tile">
		<span class="tile-title">{$_('detail_sun')}</span>
		<div class="arc-box">
			<SunArc sunrise={data.sunrise} sunset={data.sunset} now={Date.now() / 1000} />
			{#if daylight}<span class="under-arc">{loc(daylight)}</span>{/if}
		</div>
		<div class="sky-times">
			<span><span class="sky-label">{$_('detail_sunrise')}</span> {loc(formatTime(data.sunrise))}</span>
			<span><span class="sky-label">{$_('detail_sunset')}</span> {loc(formatTime(data.sunset))}</span>
		</div>
	</div>
	<!-- The moon, laid out like the sun: its arc, the part lit, rise and set today, phase and direction -->
	<div class="sky-tile">
		<span class="tile-title">{$_('detail_moon')}</span>
		<div class="arc-box">
			<SkyArc t={moon.arcT} progressColor="#CFC8E8">
				{#snippet body(x, y)}
					<circle cx={x} cy={y} r="7" fill="#3A3570" />
					<path d={moonLitPath(moon.illumination.phase, moon.southern, x, y, 7)} fill="#F2EBD3" />
				{/snippet}
			</SkyArc>
			<span class="under-arc">{loc(`${Math.round(moon.illumination.fraction * 100)}%`)}</span>
		</div>
		<div class="sky-times">
			<span><span class="sky-label">{$_('detail_moonrise')}</span> {moon.today.rise != null ? loc(formatHourAtLocation(moon.today.rise, data.utcOffsetSeconds)) : '--'}</span>
			<span><span class="sky-label">{$_('detail_moonset')}</span> {moon.today.set != null ? loc(formatHourAtLocation(moon.today.set, data.utcOffsetSeconds)) : '--'}</span>
		</div>
		<span class="moon-phase">
			<MoonPhaseIcon phase={moon.illumination.phase} southern={moon.southern} size={18} />
			{($json('moon_phases') as string[])?.[moonPhaseIndex(moon.illumination.phase)] ?? ''}
		</span>
		<!-- Fainter while the moon is under the horizon -->
		<span class="moon-azimuth" class:down={moon.position.altitude < 0}>{loc(`${$_('detail_azimuth')} ${getCardinalDirection(moonAzimuth, $json('cardinal_directions'))} (${moonAzimuth}°)`)}</span>
	</div>

	{#if data.airQuality}
		{@const aqi = data.airQuality}
		{@const [aqiP, aqiS] = aqi.displayDual()}
		{@const us = $_('air_quality_us_aqi')}
		{@const eu = $_('air_quality_eu_aqi')}
		{@const european = aqi.primaryStandard === 'EUROPEAN'}
		<DetailCard title={$_('detail_air_quality')} value={loc(aqiP)} onClick={onOpenAirQuality}>
			{#snippet centerDialSnippet()}
				<Gauge fraction={aqiDialPosition(european ? aqi.europeanValue : aqi.usValue, aqi.primaryStandard)} color={AQI_TIER_COLORS[aqi.primaryTier]}
					segments={AQI_TIERS.map((t) => AQI_TIER_COLORS[t as keyof typeof AQI_TIER_COLORS])}>
					<DialValue value={loc(aqiP)} unit={european ? eu : us} secondary={loc(`${aqiS} ${european ? us : eu}`)}>
						{#snippet badge()}
							<AqiTierBadge tier={aqi.primaryTier} label={($json('aqi_tier_labels') as string[])?.[AQI_TIERS.indexOf(aqi.primaryTier)] ?? ''} />
						{/snippet}
					</DialValue>
				</Gauge>
			{/snippet}
		</DetailCard>
	{/if}
	{@render uvCard()}
	{#if data.airQuality}
		{@render cloudCard()}
		{@render visibilityCard()}
	{:else}
		{@render cloudCard()}
		<div class="wide">{@render visibilityCard()}</div>
	{/if}

	{#snippet uvCard()}
		<DetailCard title={$_('detail_uv_index')} value={data.uvIndex != null ? loc(data.uvIndex.toFixed(1)) : '--'}>
			{#snippet centerDialSnippet()}
				{#if data.uvIndex != null}
					<!-- Same five-level dial as air quality, the dot at the reading's place in its level -->
					<Gauge fraction={uvDialPosition(data.uvIndex)} color={uvColorFor(data.uvIndex)} segments={UV_TIERS.map((t) => UV_TIER_COLORS[t])}>
						<DialValue value={loc(data.uvIndex.toFixed(1))}>
							{#snippet badge()}<AqiTierBadge color={uvColorFor(data.uvIndex!)} label={getUvLabel(data.uvIndex!)} />{/snippet}
						</DialValue>
					</Gauge>
				{/if}
			{/snippet}
		</DetailCard>
	{/snippet}

	{#snippet cloudCard()}
		<DetailCard title={$_('detail_cloud_cover')} value={data.cloudCover != null ? loc(`${data.cloudCover}%`) : '--'}>
			{#snippet centerDialSnippet()}
				{#if data.cloudCover != null}
					<Gauge fraction={data.cloudCover / 100} color="#B0BEC5">
						<DialValue value={loc(`${data.cloudCover}%`)} />
					</Gauge>
				{/if}
			{/snippet}
		</DetailCard>
	{/snippet}

	{#snippet visibilityCard()}
		<DetailCard
			title={$_('detail_visibility')}
			value={loc(visDual[0])}
			secondaryValue={loc(visDual[1])}
			{onToggleUnits}
		/>
	{/snippet}
	{/if}
</div>

<style>
	.conditions-grid {
		display: grid;
		grid-template-columns: 1fr 1fr;
		grid-auto-rows: auto;
		gap: 10px;
	}

	.sky-tile {
		background: var(--surface-tile);
		border-radius: 16px;
		padding: 16px;
		display: flex;
		flex-direction: column;
		gap: 6px;
		min-width: 0;
	}

	.wide {
		grid-column: span 2;
		display: flex;
	}

	/* Under the arc, just above the horizon line */
	.arc-box {
		position: relative;
	}

	.under-arc {
		position: absolute;
		left: 0;
		right: 0;
		bottom: 23%;
		text-align: center;
		font-family: var(--font-display);
		font-size: 13px;
		font-weight: 700;
		color: rgba(255, 255, 255, 0.8);
		font-feature-settings: var(--font-features);
	}

	.moon-phase {
		display: flex;
		align-items: center;
		gap: 6px;
		font-size: 12px;
		line-height: 14px;
		color: rgba(255, 255, 255, 0.7);
	}

	.moon-azimuth {
		font-size: 12px;
		color: rgba(255, 255, 255, 0.7);
	}

	.moon-azimuth.down {
		color: rgba(255, 255, 255, 0.4);
	}

	.tile-title {
		font-size: 13px;
		color: rgba(255, 255, 255, 0.6);
	}

	.sky-times {
		display: flex;
		justify-content: space-between;
		gap: 8px;
		font-family: var(--font-display);
		font-size: 15px;
		font-weight: 700;
		color: white;
		font-feature-settings: var(--font-features);
	}

	.sky-times > span {
		display: flex;
		flex-direction: column;
	}

	.sky-times > span:last-child {
		text-align: end;
	}

	.sky-label {
		font-family: var(--font-body, inherit);
		font-size: 11px;
		font-weight: 400;
		color: rgba(255, 255, 255, 0.5);
	}

</style>
