<!-- SPDX-FileCopyrightText: Orinasa Njarasoa -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
<script lang="ts">
	import { heatIndex, windChill } from '#lib/domain/thermalIndices.js';
	import { HEAT_COLOR, CHILL_COLOR } from '#lib/utils/feltColors.js';
	import WeatherIcon from './WeatherIcon.svelte';
	import { _, json } from 'svelte-i18n';
	import { isDaytime, type WeatherData } from '#lib/domain/weatherData.js';
	import { getCardinalDirection } from '#lib/domain/windSpeed.js';
	import { AQI_TIER_COLORS, aqiDialPosition, type AirQualityIndex } from '#lib/domain/airQuality.js';
	import { UV_TIER_COLORS, uvColorFor, uvDialPosition, type UvTier } from '#lib/domain/uv.js';
	import DetailCard from './DetailCard.svelte';
	import { formatDayMonth, formatHourAtLocation } from '#lib/utils/date.js';
	import { localeIndex } from '#lib/stores/preferences.js';
	import { SUPPORTED_LOCALES } from '#lib/i18n/index.js';
	import Gauge from './Gauge.svelte';
	import SunArc from './SunArc.svelte';
	import SkyArc from './SkyArc.svelte';
	import MoonPhaseIcon from './MoonPhaseIcon.svelte';
	import DialValue from './DialValue.svelte';
	import CompassDial from './CompassDial.svelte';
	import LabeledValue from './LabeledValue.svelte';
	import AqiTierBadge from './AqiTierBadge.svelte';
	import { moonArcProgress, moonIllumination, moonPhaseIndex, moonPosition, moonTimes, polarPeriod, sunPosition } from '#lib/domain/moon.js';
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
	// How it feels: the heat index or the wind chill when one applies, else feels like
	let heat = $derived(heatIndex(data.temperature, data.humidity));
	let felt = $derived(heat ?? windChill(data.temperature, data.windSpeed));
	let feltDual = $derived((felt ?? data.feelsLike).displayDual(metricPrimary));
	let feltLabel = $derived($_(heat ? 'detail_heat_index' : felt ? 'detail_wind_chill' : 'feels_like'));
	let windDual = $derived(data.windSpeed.displayDual(metricPrimary));
	let gustDual = $derived(data.windGust?.displayDual(metricPrimary));
	let pressDual = $derived(data.pressure.displayDual(metricPrimary));
	let dewDual = $derived(data.dewPoint.displayDual(metricPrimary));
	let visDual = $derived(visibilityDisplay(data.visibility, metricPrimary));
	let precipDual = $derived((data.snow ?? data.rain)?.displayDual(metricPrimary));
	let nowSec = $derived(Date.now() / 1000);

	/** "2.9 m/s" -> ["2.9", "m/s"]: the number for the middle of a dial and its unit under it. */
	function splitUnit(text: string): [string, string] {
		const i = text.lastIndexOf(' ');
		return [text.slice(0, i), text.slice(i + 1)];
	}

	// Polar night (the sun stays down all day) or midnight sun (up all day): no rise or set, as for the moon
	let polarNight = $derived(data.sunset <= data.sunrise);
	let midnightSun = $derived(data.sunset - data.sunrise >= 86_400);
	let polar = $derived(polarNight || midnightSun);
	// Then the tile shows the first and last day of that period in place of the arc's times
	let period = $derived(polar ? polarPeriod(Date.now(), data.latitude, midnightSun) : null);
	// In a midnight sun, where the sun is now: it goes round the sky, so its direction changes all day
	let sunAzimuth = $derived(midnightSun ? Math.round(sunPosition(Date.now(), data.latitude, data.longitude).azimuth) % 360 : null);
	const dayMonth = (ms: number) => loc(formatDayMonth(ms, SUPPORTED_LOCALES[$localeIndex].tag, data.utcOffsetSeconds, $json('month_names') as string[] | undefined));
	// The length of the day (hours and minutes) in the middle of the sun's arc, or the polar night or midnight sun
	let daylight = $derived.by(() => {
		if (polarNight) return $_('polar_night');
		if (midnightSun) return $_('midnight_sun');
		const minutes = Math.floor((data.sunset - data.sunrise) / 60);
		const h = Math.floor(minutes / 60);
		const m = minutes % 60;
		return $_('daylight_duration', {
			values: {
				hours: $_(h === 1 ? 'daylight_hours_one' : 'daylight_hours_other', { values: { count: h } }),
				minutes: $_(m === 1 ? 'daylight_minutes_one' : 'daylight_minutes_other', { values: { count: m } }),
			},
		});
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
	{#if part === 'now'}{@render nowTiles()}{:else}{@render skyTiles()}{/if}
</div>

<!-- Every card: the title, one graphic in the middle with the values in it, labels in the bottom row -->
{#snippet nowTiles()}
	{@render temperatureTile()}
	{@render precipitationTile()}
	{@render windTile()}
	{@render gustTile()}
	{@render pressureTile()}
	{@render humidityTile()}
{/snippet}

<!-- The temperature on a dial between today's low and high, and how it feels -->
{#snippet temperatureTile()}
	<DetailCard title={$_('detail_temperature')} value={loc(tempDual[0])} {onToggleUnits}>
		{#snippet centerDialSnippet()}
			<!-- Where now is between today's low and high -->
			{@const low = data.tempMin.celsius}
			{@const high = data.tempMax.celsius}
			<Gauge fraction={high > low ? (data.temperature.celsius - low) / (high - low) : 0.5} color="#FFB74D">
				<DialValue value={loc(tempDual[0])} secondary={loc(tempDual[1])} />
			</Gauge>
		{/snippet}
		{#snippet footerSnippet()}
			<LabeledValue label={feltLabel} value={loc(feltDual[0])} secondary={loc(feltDual[1])} centered ltr icon={heat ? { kind: 'heat', color: HEAT_COLOR } : felt ? { kind: 'cold', color: CHILL_COLOR } : null} />
		{/snippet}
	</DetailCard>
{/snippet}

<!-- The rain or snow now, and the chance of precipitation in the next hour -->
{#snippet precipitationTile()}
	<DetailCard title={$_('detail_precipitation')} value={precipDual ? loc(precipDual[0]) : $_('no_precip')} onToggleUnits={precipDual ? onToggleUnits : undefined}>
		{#snippet centerDialSnippet()}
			<div class="precip-center">
				<WeatherIcon code={data.snow ? 71 : data.rain ? 61 : data.weatherCode} night={!isDaytime(nowSec, data.sunrise, data.sunset)} size={56} />
				{#if precipDual}
					<DialValue value={loc(precipDual[0])} secondary={loc(precipDual[1])} fill={false} />
				{:else}
					<DialValue value={$_('no_precip')} fill={false} />
				{/if}
			</div>
		{/snippet}
		{#snippet footerSnippet()}
			<!-- The chance of precipitation in the next hour, as on the forecast cards -->
			<span class="precip-chance">
				{#if (data.hourlyForecast[0]?.precipProbability ?? 0) > 0}
					<svg viewBox="0 0 24 24" width="11" height="11" fill="currentColor" aria-hidden="true"><path d="M12 2c-5.33 4.55-8 8.48-8 11.8 0 4.98 3.8 8.2 8 8.2s8-3.22 8-8.2c0-3.32-2.67-7.25-8-11.8z"/></svg>{loc(`${data.hourlyForecast[0].precipProbability}%`)}
				{/if}
			</span>
		{/snippet}
	</DetailCard>
{/snippet}

<!-- The wind speed on a compass that shows where it comes from -->
{#snippet windTile()}
	<DetailCard title={$_('detail_wind')} value={loc(windDual[0])} {onToggleUnits}>
		{#snippet centerDialSnippet()}
			{@const [n, u] = splitUnit(windDual[0])}
			{#if data.windDeg != null}
				<CompassDial degrees={data.windDeg}><DialValue value={loc(n)} unit={u} secondary={loc(windDual[1])} /></CompassDial>
			{:else}
				<div class="square"><DialValue value={loc(n)} unit={u} secondary={loc(windDual[1])} /></div>
			{/if}
		{/snippet}
		{#snippet footerSnippet()}
			{#if data.windDeg != null}
				<span class="footer-center">{loc(`${getCardinalDirection(data.windDeg, $json('cardinal_directions'))} (${data.windDeg}°)`)}</span>
			{/if}
		{/snippet}
	</DetailCard>
{/snippet}

<!-- The wind gusts on a dial -->
{#snippet gustTile()}
	<DetailCard title={$_('detail_wind_gust')} value={gustDual ? loc(gustDual[0]) : '--'} {onToggleUnits}>
		{#snippet centerDialSnippet()}
			{#if data.windGust && gustDual}
				{@const [n, u] = splitUnit(gustDual[0])}
				<!-- Gusts rarely pass 30 m/s -->
				<Gauge fraction={data.windGust.metersPerSecond / 30} color="#64B5F6">
					<DialValue value={loc(n)} unit={u} secondary={loc(gustDual[1])} />
				</Gauge>
			{/if}
		{/snippet}
	</DetailCard>
{/snippet}

<!-- The sea-level pressure on a dial -->
{#snippet pressureTile()}
	<DetailCard title={$_('detail_pressure')} value={loc(pressDual[0])} {onToggleUnits}>
		{#snippet centerDialSnippet()}
			{@const [n, u] = splitUnit(pressDual[0])}
			<Gauge fraction={(data.pressure.hPa - PRESSURE_MIN_HPA) / (PRESSURE_MAX_HPA - PRESSURE_MIN_HPA)} color="#9B8FFF">
				<DialValue value={loc(n)} unit={u} secondary={loc(pressDual[1])} />
			</Gauge>
		{/snippet}
	</DetailCard>
{/snippet}

<!-- The humidity on a dial, and the dew point -->
{#snippet humidityTile()}
	<DetailCard title={$_('detail_humidity')} value={data.humidity != null ? loc(`${data.humidity}%`) : '--'} {onToggleUnits}>
		{#snippet centerDialSnippet()}
			{#if data.humidity != null}
				<Gauge fraction={data.humidity / 100} color="#64B5F6">
					<DialValue value={loc(`${data.humidity}%`)} />
				</Gauge>
			{/if}
		{/snippet}
		{#snippet footerSnippet()}
			<LabeledValue label={$_('detail_dewpoint')} value={loc(dewDual[0])} secondary={loc(dewDual[1])} centered ltr />
		{/snippet}
	</DetailCard>
{/snippet}

<!-- Sun / Moon, air quality / UV, cloud cover / visibility (without air quality: UV / cloud cover, then visibility) -->
{#snippet skyTiles()}
	{@render sunTile()}
	{@render moonTile()}
	{#if data.airQuality}
		{@render airQualityTile(data.airQuality)}
		{@render uvTile()}
		{@render cloudCoverTile()}
		{@render visibilityTile()}
	{:else}
		{@render uvTile()}
		{@render cloudCoverTile()}
		<div class="wide">{@render visibilityTile()}</div>
	{/if}
{/snippet}

<!-- The sun's arc and times, or the polar night or midnight sun with its dates -->
{#snippet sunTile()}
	<div class="sky-tile">
		<span class="tile-title">{$_('detail_sun')}</span>
		<!-- The arc right under the title, then the times, so the sun's and moon's arcs and times line up -->
		<div class="sky-content" class:polar>
		<div class="arc-box">
			<!-- No arc in a polar night or midnight sun: the sun does not cross the horizon -->
			{#if !polar}<SunArc sunrise={data.sunrise} sunset={data.sunset} now={Date.now() / 1000} />{/if}
			{#if daylight}<span class="under-arc mid-arc" class:polar>{loc(daylight)}</span>{/if}
		</div>
		<div class="sky-times">
			{#if period}
				<span><span class="sky-label">{$_('polar_since')}</span> {dayMonth(period.since)}</span>
				<span><span class="sky-label">{$_('polar_until')}</span> {dayMonth(period.until)}</span>
			{:else}
				<span><span class="sky-label">{$_('detail_sunrise')}</span> {loc(formatTime(data.sunrise))}</span>
				<span><span class="sky-label">{$_('detail_sunset')}</span> {loc(formatTime(data.sunset))}</span>
			{/if}
		</div>
		{#if sunAzimuth != null}
			<LabeledValue label={$_('detail_azimuth')} value={loc(`${getCardinalDirection(sunAzimuth, $json('cardinal_directions'))} (${sunAzimuth}°)`)} centered />
		{/if}
		</div>
	</div>
{/snippet}

<!-- The moon, laid out like the sun: its arc, the part lit, rise and set today, phase and direction -->
{#snippet moonTile()}
	<div class="sky-tile">
		<span class="tile-title">{$_('detail_moon')}</span>
		<div class="sky-content">
		<div class="arc-box">
			<SkyArc t={moon.arcT} progressColor="#CFC8E8">
				{#snippet body(x, y)}
					<circle cx={x} cy={y} r="7" fill="#3A3570" />
					<path d={moonLitPath(moon.illumination.phase, moon.southern, x, y, 7)} fill="#F2EBD3" />
				{/snippet}
			</SkyArc>
			<span class="under-arc mid-arc">{loc(`${Math.round(moon.illumination.fraction * 100)}%`)}</span>
		</div>
		<div class="sky-times">
			<span><span class="sky-label">{$_('detail_moonrise')}</span> {moon.today.rise != null ? loc(formatHourAtLocation(moon.today.rise, data.utcOffsetSeconds)) : '--'}</span>
			<span><span class="sky-label">{$_('detail_moonset')}</span> {moon.today.set != null ? loc(formatHourAtLocation(moon.today.set, data.utcOffsetSeconds)) : '--'}</span>
		</div>
		<!-- The direction as a labelled value, like the times; the phase at the end -->
		<div class="moon-bottom">
			<LabeledValue label={$_('detail_azimuth')} value={loc(`${getCardinalDirection(moonAzimuth, $json('cardinal_directions'))} (${moonAzimuth}°)`)} />
			<span class="moon-phase">
				<MoonPhaseIcon phase={moon.illumination.phase} southern={moon.southern} size={16} />
				{($json('moon_phases') as string[])?.[moonPhaseIndex(moon.illumination.phase)] ?? ''}
			</span>
		</div>
		</div>
	</div>
{/snippet}

<!-- The air quality on a five-level dial; a tap opens its details -->
{#snippet airQualityTile(aqi: AirQualityIndex)}
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
{/snippet}

<!-- The UV index on the same five-level dial as air quality -->
{#snippet uvTile()}
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

<!-- The cloud cover on a dial -->
{#snippet cloudCoverTile()}
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

<!-- The visibility on a log-scale dial -->
{#snippet visibilityTile()}
	<DetailCard title={$_('detail_visibility')} value={loc(visDual[0])} {onToggleUnits}>
		{#snippet centerDialSnippet()}
			{#if data.visibility != null}
				{@const [n, u] = splitUnit(visDual[0])}
				<!-- On a log scale, 100 m to 20 km: each step down matters more than one up -->
				<Gauge fraction={Math.log10(Math.max(100, data.visibility) / 100) / Math.log10(200)} color="#80DEEA">
					<DialValue value={loc(n)} unit={u} secondary={loc(visDual[1])} />
				</Gauge>
			{/if}
		{/snippet}
	</DetailCard>
{/snippet}

<style>
	.conditions-grid {
		display: grid;
		grid-template-columns: 1fr 1fr;
		grid-auto-rows: auto;
		gap: 10px;
	}

	.sky-tile {
		position: relative;
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
	.sky-content {
		flex: 1;
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 6px;
	}

	.moon-bottom {
		margin-top: auto;
		align-self: stretch;
		display: flex;
		justify-content: space-between;
		align-items: flex-end;
		gap: 6px;
	}

	.precip-center {
		width: 100%;
		aspect-ratio: 1;
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		gap: 6px;
	}

	.square {
		position: relative;
		width: 100%;
		aspect-ratio: 1;
	}

	.precip-chance {
		display: flex;
		align-items: center;
		justify-content: center;
		gap: 2px;
		min-height: 17px;
		font-size: 13px;
		color: #64B5F6;
		font-feature-settings: var(--font-features);
	}

	.footer-center {
		display: block;
		text-align: center;
		font-size: 13px;
		color: rgba(255, 255, 255, 0.7);
	}

	.sky-content > .arc-box,
	.sky-content > .sky-times {
		align-self: stretch;
	}

	.arc-box {
		position: relative;
	}

	/* Polar night or midnight sun: the text and the dates in the middle of the tile */
	/* Over the whole tile, so the block is in its middle and not only under the title */
	.sky-content.polar {
		position: absolute;
		inset: 0;
		padding: 16px;
		justify-content: center;
		gap: 12px;
	}

	.sky-content.polar > .arc-box {
		align-self: stretch;
	}

	.sky-content.polar .mid-arc {
		position: static;
		display: block;
		transform: none;
	}

	/* The dates side by side when they fit, else one under the other */
	.sky-content.polar > .sky-times {
		justify-content: center;
		flex-wrap: wrap;
		gap: 6px 16px;
	}

	.sky-content.polar > .sky-times > span {
		align-items: center;
	}

	.under-arc {
		position: absolute;
		/* Long texts (Malagasy) take more than one line inside the arc */
		left: 20%;
		right: 20%;
		bottom: 15%;
		text-align: center;
		line-height: 1.15;
		white-space: pre-line;
		font-family: var(--font-display);
		font-size: 13px;
		font-weight: 700;
		color: rgba(255, 255, 255, 0.8);
		font-feature-settings: var(--font-features);
	}

	/* The length of the day and the moon's part lit, in the middle of the arc */
	.mid-arc {
		bottom: auto;
		top: 50%;
		transform: translateY(-50%);
	}

	/* Polar night or midnight sun: the text alone, larger */
	.mid-arc.polar {
		font-size: 16px;
	}

	.moon-phase {
		display: flex;
		flex-direction: column;
		align-items: flex-end;
		gap: 2px;
		font-size: 12px;
		line-height: 14px;
		text-align: end;
		color: rgba(255, 255, 255, 0.7);
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
