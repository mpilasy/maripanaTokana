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
	import UvTierBadge from './UvTierBadge.svelte';
	import { uvTier } from '#lib/domain/uv.js';
	import { temperatureTrend } from '#lib/domain/temperatureTrend.js';
	import { temperatureColor } from '#lib/utils/tempColors.js';
	import FeltIcon from './FeltIcon.svelte';
	import { frostbiteMinutes, heatIndex, windChill } from '#lib/domain/thermalIndices.js';
	import { HEAT_COLOR, CHILL_COLOR } from '#lib/utils/feltColors.js';
	import { NORMAL_DELTA_MIN_C } from '#lib/domain/normals.js';
	import { _, json } from 'svelte-i18n';

	interface Props {
		data: WeatherData;
		/** The place's name, for the share button's label. */
		placeName: string;
		metricPrimary: boolean;
		loc: (s: string) => string;
		onToggleUnits: () => void;
		onShare?: (el: HTMLElement) => void;
	}

	let { data, placeName, metricPrimary, loc, onToggleUnits, onShare }: Props = $props();

	let cardEl = $state<HTMLElement | null>(null);

	let isNight = $derived(!isDaytime(data.timestamp, data.sunrise * 1000, data.sunset * 1000));
	let description = $derived($_( wmoDescriptionKey(data.weatherCode)));
	let tempDual = $derived(data.temperature.displayDualMixed(metricPrimary));
	let maxDual = $derived(data.tempMax.displayDual(metricPrimary));
	let minDual = $derived(data.tempMin.displayDual(metricPrimary));
	// Half a day at a glance: the first forecast hour and every 4 hours after it, up to 12 hours later
	let nextHours = $derived([0, 4, 8, 12].map((i) => data.hourlyForecast[i]).filter((h) => h != null));
	/** The chance of precipitation (%) from which an hour of that row shows a drop. */
	const RAIN_MARK_MIN_PERCENT = 30;

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

	// The largest change of temperature in the next 6 hours, in the chip next to the current temperature
	let trend = $derived(temperatureTrend(data.temperature, data.hourlyForecast, metricPrimary));
	let trendChip = $derived.by(() => {
		if (!trend) return null;
		// Both units come from the exact change, so that they agree
		const change = Math.abs(trend.temperature.celsius - data.temperature.celsius);
		const c = `${Math.round(change)}°C`;
		const f = `${Math.round(change * 9 / 5)}°F`;
		const [primary, secondary] = metricPrimary ? [c, f] : [f, c];
		const time = loc(formatHourAtLocation(trend.time, data.utcOffsetSeconds));
		const up = trend.direction === 'up';
		return {
			arrow: up ? '↑' : '↓',
			primary: loc(primary),
			secondary: loc(secondary),
			time,
			color: up ? HEAT_COLOR : CHILL_COLOR,
			label: $_(up ? 'temp_trend_rise' : 'temp_trend_fall', {
				values: { delta: loc(`${primary.replace('°', ' °')} (${secondary.replace('°', ' °')})`), time },
			}),
		};
	});

	// Today's high against the normal high, when they differ by 2 °C or more
	let normalLine = $derived.by(() => {
		const normal = data.normals?.[0];
		const high = data.dailyForecast[0]?.tempMax.celsius ?? data.tempMax.celsius;
		if (!normal || !Number.isFinite(high)) return null;
		const delta = high - normal.max;
		if (Math.abs(delta) < NORMAL_DELTA_MIN_C) return null;
		// Both units come from the exact difference, so that they agree
		const c = `${Math.round(Math.abs(delta))} °C`;
		const f = `${Math.round(Math.abs(delta) * 9 / 5)} °F`;
		const [primary, secondary] = metricPrimary ? [c, f] : [f, c];
		// No-break spaces: the line can wrap, but never inside "2 °C (4 °F)"
		const nbsp = (t: string) => t.replace(/ /g, '\u00A0');
		const key = delta > 0 ? 'high_vs_normal_above' : 'high_vs_normal_below';
		// The sentence around the difference, which shows in two sizes
		const marker = '\u0001';
		const [before, after = ''] = $_(key, { values: { delta: marker } }).split(marker);
		return {
			before,
			after,
			primary: loc(nbsp(primary)),
			secondary: loc(nbsp(` (${secondary})`)),
			color: delta > 0 ? HEAT_COLOR : CHILL_COLOR,
			label: $_(key, { values: { delta: loc(`${primary} (${secondary})`) } }),
		};
	});

	// The chip goes beside the big digits when it fits there, else at the end of the °F line
	let tempRowWidth = $state(0);
	let tempValueWidth = $state(0);
	let trendBesideWidth = $state(0);
	let trendBeside = $derived(tempRowWidth === 0 || tempValueWidth + 8 + trendBesideWidth <= tempRowWidth);

	const AQI_TIERS = ['good', 'moderate', 'unhealthy', 'very_unhealthy', 'hazardous'];

	// Air quality only when it is not good, with both indices as on the air quality card
	let aqi = $derived(data.airQuality && data.airQuality.primaryTier !== 'good' ? data.airQuality : null);
	let aqiDual = $derived(aqi?.displayDual());
	let aqiUnits = $derived.by(() => {
		const us = $_('air_quality_us_aqi');
		const eu = $_('air_quality_eu_aqi');
		return aqi?.primaryStandard === 'EUROPEAN' ? [eu, us] : [us, eu];
	});

	const UV_TIERS = ['low', 'moderate', 'high', 'veryHigh', 'extreme'];

	// The UV index only when it is moderate or higher; at night it is low or missing
	let uv = $derived(data.uvIndex != null && uvTier(data.uvIndex) !== 'low' ? data.uvIndex : null);
	let uvLabel = $derived(uv == null ? '' : (($json('uv_labels') as string[])?.[UV_TIERS.indexOf(uvTier(uv))] ?? ''));

	// The normal line uses the width up to the air quality and UV column, or the full width without that column
	let hasSide = $derived(aqi != null || uv != null);
	let midWidth = $state(0);
	let sideWidth = $state(0);
	const MID_COLUMN_GAP = 16;
	let normalWidth = $derived(hasSide && midWidth > 0 && sideWidth > 0 ? midWidth - sideWidth - MID_COLUMN_GAP : null);

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
			<WeatherIcon code={data.weatherCode} night={isNight} size={64} decorative />
			<span class="hero-description">{description}</span>
		</div>
		<!-- Rain or snow coming, on its own line under the condition -->
		{#if outlook}
			<p class="hero-outlook">{outlook}</p>
		{/if}

		<div class="hero-mid" bind:clientWidth={midWidth}>
		<div class="hero-temp" bind:clientWidth={tempRowWidth}>
			<div bind:clientWidth={tempValueWidth}>
				<DualUnitText
					primary={loc(tempDual[0])}
					secondary={loc(tempDual[1])}
					primarySize="5rem"
					secondaryScale={0.32}
					onClick={onToggleUnits}
				/>
			</div>
			{#if trendChip}
				{#snippet trendContent()}
					<bdi dir="ltr" class="hero-trend-main">{trendChip.arrow}{trendChip.primary}</bdi>
					<bdi dir="ltr" class="hero-trend-sub">{trendChip.secondary}</bdi>
					<span class="hero-trend-sub">{trendChip.time}</span>
				{/snippet}
				<!-- Read as one sentence; a tap changes the units, as the values next to it -->
				<button type="button" class="hero-trend" class:hero-trend-low={!trendBeside} style:--trend={trendChip.color} onclick={onToggleUnits} aria-label={trendChip.label}>
					{@render trendContent()}
				</button>
				<!-- Not shown: it gives the width of the chip beside the big digits -->
				<div class="hero-trend hero-trend-probe" aria-hidden="true" bind:clientWidth={trendBesideWidth}>
					{@render trendContent()}
				</div>
			{/if}
		</div>

		<div class="hero-range" class:hero-range-full={!hasSide}>
			<DualUnitText
				primary={loc(`↓${minDual[0]} ↑${maxDual[0]}`)}
				secondary={loc(`↓${minDual[1]} ↑${maxDual[1]}`)}
				onClick={onToggleUnits}
			/>
			{#if normalLine}
				<!-- Read as one sentence; a tap changes the units, as the values above it -->
				<button type="button" class="hero-normal" style:--normal={normalLine.color} style:width={normalWidth ? `${normalWidth}px` : undefined} onclick={onToggleUnits} aria-label={normalLine.label}>
					<span aria-hidden="true">{normalLine.before}<span class="hero-normal-delta"><bdi dir="ltr" class="hero-normal-main">{normalLine.primary}</bdi><bdi dir="ltr" class="hero-normal-sub">{normalLine.secondary}</bdi></span>{normalLine.after}</span>
				</button>
			{/if}
			<div class="hero-felt">
				<span class="hero-felt-label" style:color={feltColor}>{feltLabel}</span>
				<!-- A thermometer with a plus for the heat index, with a minus for the wind chill, as tall as the two numbers -->
				<div class="hero-felt-values">
					<!-- 32 px: the height of the 15 px value and its other unit -->
					{#if felt}<FeltIcon kind={heat ? 'heat' : 'cold'} color={feltColor} size={32} />{/if}
					<DualUnitText primary={loc(feltDual[0])} secondary={loc(feltDual[1])} primarySize="0.9375rem" onClick={onToggleUnits} />
				</div>
				{#if frostbite}<span class="hero-felt-label hero-frostbite" style:color={feltColor}>{loc(frostbite)}</span>{/if}
			</div>
		</div>

		<!-- Air quality and UV fill the space beside the °F and high/low lines; air quality first -->
		{#if aqi || uv != null}
			<div class="hero-side" class:hero-side-low={trendChip && !trendBeside} bind:clientWidth={sideWidth}>
				{#if aqi}
					{#if aqiDual}
						<DualUnitText primary={loc(String(aqiDual[0]))} secondary={loc(String(aqiDual[1]))} primaryUnit={aqiUnits[0]} secondaryUnit={aqiUnits[1]} primarySize="0.875rem" align="end" />
					{/if}
					<AqiTierBadge tier={aqi.primaryTier} label={($json('aqi_tier_labels') as string[])?.[AQI_TIERS.indexOf(aqi.primaryTier)] ?? ''} compact />
				{/if}
				{#if uv != null}
					<!-- A short "UV" on screen, because the full name does not fit on one line in all languages -->
					<div class="hero-uv" class:hero-uv-stacked={aqi}>
						<span class="sr-only">{$_('detail_uv_index')} {loc(uv.toFixed(1))}</span>
						<bdi dir="ltr" aria-hidden="true"><span class="hero-uv-unit">UV</span> {loc(uv.toFixed(1))}</bdi>
					</div>
					<UvTierBadge uvIndex={uv} label={uvLabel} compact />
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
						<!-- A likely chance of precipitation under the icon; the space stays when there is none, so the slots line up -->
						<span class="next-precip">
							{#if hour.precipProbability >= RAIN_MARK_MIN_PERCENT}
								<span aria-hidden="true"><svg class="next-drop" viewBox="0 0 24 24" width="9" height="9" fill="currentColor"><path d="M12 2c-5.33 4.55-8 8.48-8 11.8 0 4.98 3.8 8.2 8 8.2s8-3.22 8-8.2c0-3.32-2.67-7.25-8-11.8z"/></svg>{loc(`${hour.precipProbability}%`)}</span>
								<span class="sr-only">{$_('precip_chance', { values: { value: loc(`${hour.precipProbability}%`) } })}</span>
							{/if}
						</span>
						<!-- The temperature on a pill in its colour on the temperature scale -->
						<div class="temp-pill" style:background={temperatureColor(hour.temperature.celsius)}>
							<DualUnitText primary={loc(hour.temperature.displayDual(metricPrimary)[0])} secondary={loc(hour.temperature.displayDual(metricPrimary)[1])} primarySize="0.9375rem" align="center" onClick={onToggleUnits} />
						</div>
					</div>
				{/each}
			</div>
		{/if}

		<!-- Only in share images: captureAndShare shows it in its copy of the page -->
		<div class="copyright" data-share-only>&copy; Orinasa Njarasoa</div>
	</div>

	{#if onShare}
		<button class="share-btn touch-target" onclick={handleShare} aria-label={`${$_('cd_share')}: ${placeName}`}>
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
		font-size: 1rem;
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
		position: relative;
		display: flex;
		align-items: flex-start;
		column-gap: 8px;
	}

	/* 3.2rem tall, centred on the 5rem line of the big digits (line height 1.15) */
	.hero-trend {
		height: 3.2rem;
		margin-top: calc((5rem * 1.15 - 3.2rem) / 2);
		padding: 0 8px;
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		border: none;
		border-radius: 12px;
		/* A tint of the colour, with the text in a lighter shade of it for a contrast of 4.5:1 or more */
		background: color-mix(in srgb, var(--trend) 20%, transparent);
		color: color-mix(in srgb, var(--trend) 50%, white);
		font-family: var(--font-display);
		font-feature-settings: var(--font-features);
		line-height: 1.2;
		cursor: pointer;
	}

	/* When the big digits leave no space: on one line at the end of the °F line, centred on it
	   (that line is 5rem × 0.32 with a line height of about 1.2) */
	.hero-trend.hero-trend-low {
		position: absolute;
		inset-inline-end: 0;
		bottom: calc(5rem * 0.32 * 0.6);
		transform: translateY(50%);
		height: auto;
		margin-top: 0;
		padding: 2px 8px;
		flex-direction: row;
		align-items: baseline;
		gap: 6px;
	}

	.hero-trend-probe {
		position: absolute;
		top: 0;
		inset-inline-start: 0;
		visibility: hidden;
		pointer-events: none;
	}

	.hero-trend:focus-visible {
		outline: 2px solid rgba(255, 255, 255, 0.5);
		outline-offset: 2px;
	}

	.hero-trend-main {
		font-size: 0.875rem;
		font-weight: 700;
	}

	.hero-trend-sub {
		font-size: 0.6875rem;
	}

	.hero-range {
		grid-column: 1;
		grid-row: 2;
		margin-top: 8px;
	}

	/* No air quality or UV column: the start column has the full width */
	.hero-range.hero-range-full {
		grid-column: 1 / -1;
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
		/* As wide as its content, so that the normal line can measure the space beside it */
		justify-self: end;
	}

	/* The chip is at the end of the °F line, so the side column does not rise beside that line */
	.hero-side.hero-side-low {
		margin-top: 0;
	}

	.hero-uv {
		font-family: var(--font-display);
		font-size: 0.875rem;
		font-weight: 700;
		line-height: 1.15;
		color: white;
		font-feature-settings: var(--font-features);
	}

	/* More space between the air quality block and the UV block */
	.hero-uv-stacked {
		margin-top: 4px;
	}

	/* As the unit labels of the air quality values */
	.hero-uv-unit {
		font-size: calc(0.875rem * 0.55);
		font-weight: 400;
		opacity: 0.75;
	}

	.hero-outlook {
		margin: 4px 0 0;
		text-align: start;
		font-size: 0.875rem;
		color: rgba(255,255,255,0.85);
	}

	/* At least as wide as the high/low line; a measured width (see normalWidth) takes it up to the side
	   column. The text is a lighter shade of the colour, for a contrast of 4.5:1 or more, as on the trend chip */
	.hero-normal {
		width: 0;
		min-width: 100%;
		margin-top: 4px;
		padding: 0;
		border: none;
		background: none;
		text-align: start;
		font-family: var(--font-body);
		font-size: 0.75rem;
		line-height: 1.3;
		color: color-mix(in srgb, var(--normal) 50%, white);
		cursor: pointer;
	}

	.hero-normal:focus-visible {
		outline: 2px solid rgba(255, 255, 255, 0.5);
		outline-offset: 2px;
		border-radius: 4px;
	}

	.hero-normal-delta {
		white-space: nowrap;
	}

	.hero-normal-main {
		font-family: var(--font-display);
		font-size: 0.8125rem;
		font-weight: 700;
		font-feature-settings: var(--font-features);
	}

	.hero-normal-sub {
		font-family: var(--font-display);
		font-size: 0.625rem;
		font-feature-settings: var(--font-features);
	}

	.hero-felt {
		margin-top: 8px;
		display: flex;
		flex-direction: column;
	}

	.hero-felt-label {
		font-size: 0.75rem;
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

	.temp-pill {
		padding: 2px 10px;
		border-radius: 12px;
	}

	/* Dark text on the light colours of the scale */
	.next-hour .temp-pill :global(.primary),
	.next-hour .temp-pill :global(.secondary) {
		color: #1A1A1A;
	}

	/* As tall as one line of its text whether or not it shows */
	.next-precip {
		min-height: 14px;
		font-size: 0.6875rem;
		line-height: 14px;
		color: #64B5F6;
		font-feature-settings: var(--font-features);
	}

	.next-drop {
		margin-inline-end: 2px;
	}

	.next-device-time {
		position: relative;
		margin-top: -4px;
		font-size: 0.5625rem;
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
		font-size: 0.75rem;
		color: rgba(255,255,255,0.6);
		font-feature-settings: var(--font-features);
	}

	.copyright {
		text-align: center;
		font-size: 0.5625rem;
		color: rgba(255,255,255,0.25);
		margin-top: 8px;
		display: none;
	}
</style>
