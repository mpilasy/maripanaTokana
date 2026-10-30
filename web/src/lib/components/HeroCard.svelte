<!-- SPDX-FileCopyrightText: Orinasa Njarasoa -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
<script lang="ts">
	import WeatherIcon from './WeatherIcon.svelte';
	import { isDaytime, type WeatherData } from '#lib/domain/weatherData.js';
	import { wmoDescriptionKey } from '#lib/api/wmoWeatherCode.js';
	import { precipOutlook } from '#lib/domain/precipOutlook.js';
	import { formatHourAtLocation, formatHourInDeviceTime, isRemoteTimezone } from '#lib/utils/date.js';
	import DualUnitText from './DualUnitText.svelte';
	import AqiTierBadge from './AqiTierBadge.svelte';
	import FeltIcon from './FeltIcon.svelte';
	import { frostbiteMinutes, heatIndex, windChill } from '#lib/domain/thermalIndices.js';
	import { HEAT_COLOR, CHILL_COLOR } from '#lib/utils/feltColors.js';
	import { _, json } from 'svelte-i18n';

	interface Props {
		data: WeatherData;
		metricPrimary: boolean;
		loc: (s: string) => string;
		onToggleUnits: () => void;
		onShare?: (el: HTMLElement) => void;
	}

	let { data, metricPrimary, loc, onToggleUnits, onShare }: Props = $props();

	let cardEl = $state<HTMLElement | null>(null);

	let isNight = $derived(!isDaytime(data.timestamp, data.sunrise * 1000, data.sunset * 1000));
	let description = $derived($_( wmoDescriptionKey(data.weatherCode)));
	let tempDual = $derived(data.temperature.displayDualMixed(metricPrimary));
	let maxDual = $derived(data.tempMax.displayDual(metricPrimary));
	let minDual = $derived(data.tempMin.displayDual(metricPrimary));
	// Half a day at a glance: the first forecast hour and every 4 hours after it, up to 12 hours later
	let nextHours = $derived([0, 4, 8, 12].map((i) => data.hourlyForecast[i]).filter((h) => h != null));

	// One line on rain or snow coming in the next 24 hours; nothing when they stay dry
	let outlook = $derived.by(() => {
		const o = precipOutlook(data.hourlyForecast, data.minutelyForecast ?? []);
		if (!o || o.kind === 'dry') return '';
		const kind = o.snow ? 'snow' : 'rain';
		if (o.kind === 'starts' || o.kind === 'stops') {
			const time = loc(formatHourAtLocation(o.time, data.utcOffsetSeconds));
			return $_(o.kind === 'starts' ? `outlook_${kind}_from` : `outlook_${kind}_until`, { values: { time } });
		}
		return $_(`outlook_${kind}_all_day`);
	});

	const AQI_TIERS = ['good', 'moderate', 'unhealthy', 'very_unhealthy', 'hazardous'];

	// Air quality only when it is not good, with both indices as on the air quality card
	let aqi = $derived(data.airQuality && data.airQuality.primaryTier !== 'good' ? data.airQuality : null);
	let aqiDual = $derived(aqi?.displayDual());
	let aqiUnits = $derived.by(() => {
		const us = $_('air_quality_us_aqi');
		const eu = $_('air_quality_eu_aqi');
		return aqi?.primaryStandard === 'EUROPEAN' ? [eu, us] : [us, eu];
	});

	// How the temperature feels, next to it: the heat index or the wind chill when one applies, else feels like
	let heat = $derived(heatIndex(data.temperature, data.humidity));
	let felt = $derived(heat ?? windChill(data.temperature, data.windSpeed));
	let feltDual = $derived((felt ?? data.feelsLike).displayDual(metricPrimary));
	let feltLabel = $derived($_(heat ? 'detail_heat_index' : felt ? 'detail_wind_chill' : 'feels_like'));
	let feltColor = $derived(heat ? HEAT_COLOR : felt ? CHILL_COLOR : 'rgba(255,255,255,0.6)');
	// How fast bare skin freezes at this wind chill (Environment Canada's bands)
	let frostbite = $derived.by(() => {
		const band = !heat && felt ? frostbiteMinutes(felt) : null;
		if (!band) return null;
		return band[0] === 0
			? $_('frostbite_under_minutes', { values: { minutes: band[1] } })
			: $_('frostbite_minutes', { values: { minutes: `${band[0]}\u2060–\u2060${band[1]}` } });
	});

	function isNightAt(time: number): boolean {
		let day = 0;
		for (let i = data.dailySunrise.length - 1; i >= 0; i--) {
			if (data.dailySunrise[i] <= time) { day = i; break; }
		}
		return !isDaytime(time, data.dailySunrise[day] ?? 0, data.dailySunset[day] ?? 0);
	}

	function handleShare(e: MouseEvent) {
		e.stopPropagation();
		if (cardEl && onShare) onShare(cardEl);
	}
</script>

<div class="hero-wrap">
	<div class="hero-card" bind:this={cardEl}>
		<div class="hero-condition">
			<WeatherIcon code={data.weatherCode} night={isNight} size={64} />
			<span class="hero-description">{description}</span>
		</div>

		<div class="hero-mid">
		<div class="hero-temp">
			<DualUnitText
				primary={loc(tempDual[0])}
				secondary={loc(tempDual[1])}
				primarySize="80px"
				secondaryScale={0.32}
				onClick={onToggleUnits}
			/>
		</div>

		<div class="hero-range">
			<DualUnitText
				primary={loc(`↓${minDual[0]} ↑${maxDual[0]}`)}
				secondary={loc(`↓${minDual[1]} ↑${maxDual[1]}`)}
				onClick={onToggleUnits}
			/>
			<div class="hero-felt">
				<span class="hero-felt-label" style:color={feltColor}>{feltLabel}</span>
				<!-- A thermometer with a plus for the heat index, with a minus for the wind chill, as tall as the two numbers -->
				<div class="hero-felt-values">
					<!-- 32 px: the height of the 15 px value and its other unit -->
					{#if felt}<FeltIcon kind={heat ? 'heat' : 'cold'} color={feltColor} size={32} />{/if}
					<DualUnitText primary={loc(feltDual[0])} secondary={loc(feltDual[1])} primarySize="15px" onClick={onToggleUnits} />
				</div>
				{#if frostbite}<span class="hero-felt-label hero-frostbite" style:color={feltColor}>{loc(frostbite)}</span>{/if}
			</div>
		</div>

		<!-- Rain or snow and air quality fill the space beside the °F and high/low lines -->
		{#if outlook || aqi}
			<div class="hero-side">
				{#if outlook}
					<p class="hero-outlook">{outlook}</p>
				{/if}
				{#if aqi}
					{#if aqiDual}
						<DualUnitText primary={loc(String(aqiDual[0]))} secondary={loc(String(aqiDual[1]))} primaryUnit={aqiUnits[0]} secondaryUnit={aqiUnits[1]} primarySize="14px" align="end" />
					{/if}
					<AqiTierBadge tier={aqi.primaryTier} label={($json('aqi_tier_labels') as string[])?.[AQI_TIERS.indexOf(aqi.primaryTier)] ?? ''} compact />
				{/if}
			</div>
		{/if}
		</div>

		{#if nextHours.length > 0}
			<div class="next-hours">
				{#each nextHours as hour (hour.time)}
					<div class="next-hour">
						<span class="next-time">{loc(formatHourAtLocation(hour.time, data.utcOffsetSeconds))}</span>
						<!-- The phone's own time too when the place is in another time zone, as in the hourly cards -->
						{#if isRemoteTimezone(data.utcOffsetSeconds)}
							<!-- The time is centred under the place's time; the phone icon hangs off its side -->
							<span class="next-device-time">{loc(formatHourInDeviceTime(hour.time))}<svg class="next-device-icon" viewBox="0 0 24 24" width="10" height="10" fill="currentColor" aria-hidden="true"><path d="M17 1.01L7 1c-1.1 0-2 .9-2 2v18c0 1.1.9 2 2 2h10c1.1 0 2-.9 2-2V3c0-1.1-.9-1.99-2-1.99zM17 19H7V5h10v14z"/></svg></span>
						{/if}
						<WeatherIcon code={hour.weatherCode} night={isNightAt(hour.time)} size={40} />
						<DualUnitText primary={loc(hour.temperature.displayDual(metricPrimary)[0])} secondary={loc(hour.temperature.displayDual(metricPrimary)[1])} primarySize="15px" align="center" onClick={onToggleUnits} />
					</div>
				{/each}
			</div>
		{/if}

		<div class="copyright">&copy; Orinasa Njarasoa</div>
	</div>

	{#if onShare}
		<button class="share-btn" onclick={handleShare} aria-label={$_('cd_share')}>
			<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
				<path d="M4 12v8a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8"/>
				<polyline points="16 6 12 2 8 6"/>
				<line x1="12" y1="2" x2="12" y2="15"/>
			</svg>
		</button>
	{/if}
</div>

<style>
	/* Share button sits outside the captured card so it never appears in share images. */
	.hero-wrap {
		position: relative;
	}

	.hero-card {
		position: relative;
		background: var(--hero-gradient);
		border: 1px solid var(--hairline);
		border-radius: 28px;
		padding: 20px 24px 8px;
	}

	.share-btn {
		position: absolute;
		top: 8px;
		inset-inline-end: 8px;
		background: var(--surface-tile);
		border: none;
		border-radius: 50%;
		width: 36px;
		height: 36px;
		display: flex;
		align-items: center;
		justify-content: center;
		color: rgba(255,255,255,0.75);
		cursor: pointer;
		transition: background 0.2s, color 0.2s;
	}

	.share-btn:hover {
		background: var(--surface-tile-highlight);
		color: white;
	}

	.share-btn:focus-visible {
		outline: 2px solid rgba(255, 255, 255, 0.5);
		outline-offset: 2px;
	}

	.hero-condition {
		display: flex;
		align-items: center;
		gap: 8px;
		padding-inline-end: 40px;
	}


	.hero-description {
		font-family: var(--font-body);
		font-size: 16px;
		font-weight: 500;
		color: rgba(255,255,255,0.9);
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
	}

	/* The big temperature spans the card; the side column sits beside the high/low lines and
	   rises beside the °F line, but never up beside the big digits */
	.hero-mid {
		display: grid;
		grid-template-columns: max-content minmax(0, 1fr);
		column-gap: 16px;
	}

	.hero-temp {
		grid-column: 1 / -1;
		margin-top: 8px;
	}

	.hero-range {
		grid-column: 1;
		grid-row: 2;
		margin-top: 8px;
	}

	.hero-side {
		grid-column: 2;
		grid-row: 2;
		align-self: end;
		margin-top: -28px;
		display: flex;
		flex-direction: column;
		align-items: flex-end;
		gap: 4px;
		text-align: end;
	}

	.hero-outlook {
		margin: 0;
		font-size: 14px;
		color: rgba(255,255,255,0.85);
	}

	.hero-felt {
		margin-top: 8px;
		display: flex;
		flex-direction: column;
	}

	.hero-felt-label {
		font-size: 12px;
	}

	.hero-felt-values {
		display: flex;
	}

	/* Wraps to the width of the values above, so it does not make the column wider */
	.hero-frostbite {
		width: 0;
		min-width: 100%;
	}

	.next-hours {
		display: flex;
		margin-top: 16px;
		padding: 12px 0;
		background: var(--surface-tile);
		border-radius: 16px;
	}

	.next-hour {
		flex: 1;
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 4px;
	}

	.next-hour + .next-hour {
		border-inline-start: 1px solid var(--hairline);
	}

	.next-device-time {
		position: relative;
		margin-top: -4px;
		font-size: 9px;
		color: rgba(255,255,255,0.65);
		font-feature-settings: var(--font-features);
	}

	.next-device-icon {
		position: absolute;
		inset-inline-end: 100%;
		top: 50%;
		transform: translateY(-50%);
		margin-inline-end: 4px;
		color: rgba(255, 255, 255, 0.5);
	}

	.next-time {
		font-size: 12px;
		color: rgba(255,255,255,0.6);
		font-feature-settings: var(--font-features);
	}

	.copyright {
		text-align: center;
		font-size: 9px;
		color: rgba(255,255,255,0.25);
		margin-top: 8px;
	}
</style>
