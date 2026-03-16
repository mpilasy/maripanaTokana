<script lang="ts">
	import { _ } from 'svelte-i18n';
	import type { WeatherData } from '$lib/domain/weatherData';
	import { getCardinalDirection } from '$lib/domain/windSpeed';
	import { wmoEmoji, wmoDescriptionKey } from '$lib/api/wmoWeatherCode';
	import { doFetchWeather } from '$lib/stores/weather';
	import { formatTime } from '$lib/utils/date';
	import DualUnitText from './DualUnitText.svelte';

	import { enableDevMode } from '$lib/stores/devMode';

	interface Props {
		data: WeatherData;
		metricPrimary: boolean;
		loc: (s: string) => string;
		onToggleUnits: () => void;
		onShare?: (el: HTMLElement) => void;
	}

	let { data, metricPrimary, loc, onToggleUnits, onShare }: Props = $props();

	let cardEl = $state<HTMLElement | null>(null);
	let copyrightTaps = $state(0);

	function handleCopyrightClick() {
		copyrightTaps++;
		if (copyrightTaps >= 5) {
			enableDevMode();
			copyrightTaps = 0;
		}
	}

	let isNight = $derived(data.timestamp < data.sunrise * 1000 || data.timestamp > data.sunset * 1000);
	let emoji = $derived(wmoEmoji(data.weatherCode, isNight));
	let description = $derived($_( wmoDescriptionKey(data.weatherCode)));
	let tempDual = $derived(data.temperature.displayDualMixed(metricPrimary));
	let feelsLikeDual = $derived(data.feelsLike.displayDual(metricPrimary));
	let maxDual = $derived(data.tempMax.displayDual(metricPrimary));
	let minDual = $derived(data.tempMin.displayDual(metricPrimary));
	let windDual = $derived(data.windSpeed.displayDual(metricPrimary));


	function handleShare(e: MouseEvent) {
		e.stopPropagation();
		if (cardEl && onShare) onShare(cardEl);
	}
</script>

<div class="hero-card" bind:this={cardEl}>
	<!-- svelte-ignore a11y_click_events_have_key_events -->
	<!-- svelte-ignore a11y_no_static_element_interactions -->
	<div class="card-actions">
		{#if onShare}
			<button class="share-btn" onclick={handleShare} aria-label="Share">
				<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
					<path d="M4 12v8a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8"/>
					<polyline points="16 6 12 2 8 6"/>
					<line x1="12" y1="2" x2="12" y2="15"/>
				</svg>
			</button>
		{/if}
		<button type="button" class="updated" onclick={() => doFetchWeather()}>
			<span>{$_('updated_time', { values: { time: loc(formatTime(data.timestamp)) } })}</span>
			<svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
				<path d="M23 4v6h-6"/>
				<path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/>
			</svg>
		</button>
	</div>

	<div class="hero-top">
		<div class="hero-weather">
			<span class="hero-emoji">{emoji}</span>
			<span class="hero-description">{description}</span>
		</div>
		<DualUnitText
			primary={loc(tempDual[0])}
			secondary={loc(tempDual[1])}
			primarySize="48px"
			align="end"
			onClick={onToggleUnits}
		/>
	</div>

	<div class="hero-bottom">
		<div class="hero-feels-like">
			<span class="label">{$_('feels_like')}</span>
			<DualUnitText
				primary={loc(feelsLikeDual[0])}
				secondary={loc(feelsLikeDual[1])}
				onClick={onToggleUnits}
			/>
		</div>
		<div class="hero-precip">
			{#if data.snow}
				{@const [snowP, snowS] = data.snow.displayDual(metricPrimary)}
				<DualUnitText
					primary={"\u2744\uFE0F " + loc(snowP)}
					secondary={loc(snowS)}
					align="end"
					onClick={onToggleUnits}
				/>
			{:else if data.rain}
				{@const [rainP, rainS] = data.rain.displayDual(metricPrimary)}
				<DualUnitText
					primary={"\uD83C\uDF27\uFE0F " + loc(rainP)}
					secondary={loc(rainS)}
					align="end"
					onClick={onToggleUnits}
				/>
			{:else}
				<span class="no-precip">{$_('no_precip')}</span>
			{/if}
		</div>
	</div>

	<div class="hero-extra">
		<div class="hero-highlow">
			<DualUnitText
				primary={"\u2191 " + loc(maxDual[0])}
				secondary={loc(maxDual[1])}
				onClick={onToggleUnits}
			/>
			<DualUnitText
				primary={"\u2193 " + loc(minDual[0])}
				secondary={loc(minDual[1])}
				onClick={onToggleUnits}
			/>
		</div>
		<div class="hero-wind">
			<DualUnitText
				primary={loc(windDual[0])}
				secondary={loc(windDual[1])}
				align="end"
				onClick={onToggleUnits}
			/>
			<span class="wind-direction">{loc(getCardinalDirection(data.windDeg, $_('cardinal_directions')))}</span>
		</div>
	</div>

	<div 
		class="copyright" 
		onclick={handleCopyrightClick}
		style="cursor: default; user-select: none;"
	>
		&copy; Orinasa Njarasoa
	</div>
</div>

<style>
	.hero-card {
		background: rgba(42, 31, 165, 0.8);
		border-radius: 20px;
		padding: 24px;
		margin-bottom: 24px;
		position: relative;
	}

	.card-actions {
		position: absolute;
		top: 12px;
		right: 12px;
		display: flex;
		align-items: center;
		gap: 8px;
	}

	.share-btn {
		background: rgba(255,255,255,0.1);
		border: none;
		border-radius: 50%;
		width: 32px;
		height: 32px;
		display: flex;
		align-items: center;
		justify-content: center;
		color: rgba(255,255,255,0.5);
		cursor: pointer;
		transition: background 0.2s, color 0.2s;
	}

	.share-btn:hover {
		background: rgba(255,255,255,0.2);
		color: rgba(255,255,255,0.8);
	}

	.share-btn:focus-visible {
		outline: 2px solid rgba(255, 255, 255, 0.5);
		outline-offset: 2px;
	}

	.updated {
		font-size: 11px;
		color: rgba(255,255,255,0.35);
		cursor: pointer;
		background: transparent;
		border: none;
		padding: 4px 6px;
		border-radius: 4px;
		transition: color 0.2s, background 0.2s;
		font-family: inherit;
		white-space: nowrap;
		display: flex;
		align-items: center;
		gap: 6px;
	}

	.updated svg {
		opacity: 0.8;
	}

	.updated:hover {
		color: rgba(255,255,255,0.7);
		background: rgba(255,255,255,0.05);
	}

	.updated:focus-visible {
		outline: 1px solid rgba(255, 255, 255, 0.5);
		outline-offset: 2px;
	}

	.updated:active {
		transform: scale(0.98);
	}

	.hero-top {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 16px;
	}

	.hero-weather {
		display: flex;
		flex-direction: column;
		align-items: center;
		flex: 1;
	}

	.hero-emoji {
		font-size: 48px;
		line-height: 1;
	}

	.hero-description {
		font-size: 16px;
		color: rgba(255,255,255,0.9);
		text-align: center;
		margin-top: 4px;
	}

	.hero-bottom {
		display: flex;
		justify-content: space-between;
		align-items: flex-start;
		margin-top: 16px;
	}

	.hero-feels-like {
		flex: 1;
	}

	.label {
		font-size: 14px;
		color: rgba(255,255,255,0.7);
		display: block;
		margin-bottom: 2px;
	}

	.hero-precip {
		text-align: end;
	}

	.no-precip {
		font-size: 14px;
		color: rgba(255,255,255,0.5);
	}

	.hero-extra {
		display: flex;
		justify-content: space-between;
		align-items: flex-start;
		margin-top: 12px;
	}

	.hero-highlow {
		display: flex;
		flex-direction: row;
		align-items: baseline;
		gap: 12px;
	}

	.hero-wind {
		text-align: end;
	}

	.wind-direction {
		font-size: 12px;
		color: rgba(255,255,255,0.6);
	}

	.copyright {
		text-align: center;
		font-size: 9px;
		color: rgba(255,255,255,0.2);
		margin-top: 7px;
	}
</style>
