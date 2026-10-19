<script lang="ts">
	import WeatherIcon from './WeatherIcon.svelte';
	import { _ } from 'svelte-i18n';
	import type { WeatherData } from '$lib/domain/weatherData';
	import { wmoDescriptionKey } from '$lib/api/wmoWeatherCode';
	import { precipOutlook } from '$lib/domain/precipOutlook';
	import { formatHourAtLocation, formatHourInDeviceTime, isRemoteTimezone } from '$lib/utils/date';
	import DualUnitText from './DualUnitText.svelte';

	interface Props {
		data: WeatherData;
		metricPrimary: boolean;
		loc: (s: string) => string;
		onToggleUnits: () => void;
		onShare?: (el: HTMLElement) => void;
	}

	let { data, metricPrimary, loc, onToggleUnits, onShare }: Props = $props();

	let cardEl = $state<HTMLElement | null>(null);

	let isNight = $derived(data.timestamp < data.sunrise * 1000 || data.timestamp > data.sunset * 1000);
	let description = $derived($_( wmoDescriptionKey(data.weatherCode)));
	let tempDual = $derived(data.temperature.displayDualMixed(metricPrimary));
	let maxDual = $derived(data.tempMax.displayDual(metricPrimary));
	let minDual = $derived(data.tempMin.displayDual(metricPrimary));
	let nextHours = $derived(data.hourlyForecast.slice(0, 3));

	// One line on what the next 24 hours hold for rain or snow
	let outlook = $derived.by(() => {
		const o = precipOutlook(data.hourlyForecast, data.minutelyForecast ?? []);
		if (!o) return '';
		const kind = o.snow ? 'snow' : 'rain';
		if (o.kind === 'starts' || o.kind === 'stops') {
			const time = loc(formatHourAtLocation(o.time, data.utcOffsetSeconds));
			return $_(o.kind === 'starts' ? `outlook_${kind}_from` : `outlook_${kind}_until`, { values: { time } });
		}
		return o.kind === 'dry' ? $_('outlook_dry') : $_(`outlook_${kind}_all_day`);
	});

	function isNightAt(time: number): boolean {
		let day = 0;
		for (let i = data.dailySunrise.length - 1; i >= 0; i--) {
			if (data.dailySunrise[i] <= time) { day = i; break; }
		}
		return time < (data.dailySunrise[day] ?? 0) || time > (data.dailySunset[day] ?? 0);
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
		</div>

		{#if outlook}
			<p class="hero-outlook">{outlook}</p>
		{/if}

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

	.hero-temp {
		margin-top: 8px;
	}

	.hero-range {
		margin-top: 8px;
	}

	.hero-outlook {
		margin: 16px 0 0;
		font-size: 15px;
		color: rgba(255,255,255,0.85);
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
		color: rgba(255,255,255,0.4);
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
