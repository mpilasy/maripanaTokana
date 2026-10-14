<script lang="ts">
	import { _, json } from 'svelte-i18n';
	import type { WeatherData } from '$lib/domain/weatherData';
	import { getCardinalDirection } from '$lib/domain/windSpeed';
	import { wmoEmoji, wmoDescriptionKey } from '$lib/api/wmoWeatherCode';
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
	let emoji = $derived(wmoEmoji(data.weatherCode, isNight));
	let description = $derived($_( wmoDescriptionKey(data.weatherCode)));
	let tempDual = $derived(data.temperature.displayDualMixed(metricPrimary));
	let feelsLikeDual = $derived(data.feelsLike.displayDual(metricPrimary));
	let maxDual = $derived(data.tempMax.displayDual(metricPrimary));
	let minDual = $derived(data.tempMin.displayDual(metricPrimary));
	let windDual = $derived(data.windSpeed.displayDual(metricPrimary));
	let windDir = $derived(data.windDeg != null ? getCardinalDirection(data.windDeg, $json('cardinal_directions')) : '--');

	function handleShare(e: MouseEvent) {
		e.stopPropagation();
		if (cardEl && onShare) onShare(cardEl);
	}
</script>

<div class="hero-wrap">
	<div class="hero-card" bind:this={cardEl}>
		<div class="hero-condition">
			<span class="hero-emoji">{emoji}</span>
			<span class="hero-description">{description}</span>
		</div>

		<div class="hero-main">
			<div class="hero-temp">
				<DualUnitText
					primary={loc(tempDual[0])}
					secondary={loc(tempDual[1])}
					primarySize="64px"
					secondaryScale={0.4}
					onClick={onToggleUnits}
				/>
			</div>
			<div class="hero-precip">
				{#if data.snow}
					{@const [snowP, snowS] = data.snow.displayDual(metricPrimary)}
					<DualUnitText
						primary={"❄️ " + loc(snowP)}
						secondary={loc(snowS)}
						align="end"
						onClick={onToggleUnits}
					/>
				{:else if data.rain}
					{@const [rainP, rainS] = data.rain.displayDual(metricPrimary)}
					<DualUnitText
						primary={"🌧️ " + loc(rainP)}
						secondary={loc(rainS)}
						align="end"
						onClick={onToggleUnits}
					/>
				{:else}
					<span class="no-precip">{$_('no_precip')}</span>
				{/if}
			</div>
		</div>

		<!-- Stat strip: feels like · low/high · wind -->
		<div class="hero-stats">
			<div class="stat">
				<span class="stat-label">{$_('feels_like')}</span>
				<DualUnitText
					primary={loc(feelsLikeDual[0])}
					secondary={loc(feelsLikeDual[1])}
					align="center"
					onClick={onToggleUnits}
				/>
			</div>
			<div class="stat-divider"></div>
			<div class="stat stat-wide">
				<DualUnitText
					primary={loc(`↓${minDual[0]} ↑${maxDual[0]}`)}
					secondary={loc(`↓${minDual[1]} ↑${maxDual[1]}`)}
					align="center"
					onClick={onToggleUnits}
				/>
			</div>
			<div class="stat-divider"></div>
			<div class="stat">
				<span class="stat-label">{$_('detail_wind')} · {loc(windDir)}</span>
				<DualUnitText
					primary={loc(windDual[0])}
					secondary={loc(windDual[1])}
					align="center"
					onClick={onToggleUnits}
				/>
			</div>
		</div>

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
		margin-bottom: 16px;
	}

	.hero-card {
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

	.hero-emoji {
		font-size: 22px;
		line-height: 1;
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

	.hero-main {
		display: flex;
		align-items: flex-end;
		justify-content: space-between;
		gap: 12px;
		margin-top: 4px;
	}

	/* Temperature keeps its natural width; precipitation takes (and wraps within) the rest. */
	.hero-temp {
		flex: none;
	}

	.hero-precip {
		flex: 1;
		min-width: 0;
		padding-bottom: 4px;
		text-align: end;
	}

	.no-precip {
		font-size: 13px;
		color: rgba(255,255,255,0.6);
		background: var(--surface-tile);
		border-radius: 999px;
		padding: 4px 10px;
		display: inline-block;
	}

	.hero-stats {
		display: flex;
		align-items: stretch;
		margin-top: 16px;
		padding: 12px 0;
		background: var(--surface-tile);
		border-radius: 16px;
	}

	.stat {
		flex: 1;
		min-width: 0;
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		gap: 2px;
		padding: 0 4px;
	}

	.stat-wide {
		flex: 1.2;
	}

	.stat-label {
		font-size: 12px;
		color: rgba(255,255,255,0.6);
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
		max-width: 100%;
	}

	.stat-divider {
		width: 1px;
		margin: 4px 0;
		background: var(--hairline);
	}

	.copyright {
		text-align: center;
		font-size: 9px;
		color: rgba(255,255,255,0.25);
		margin-top: 8px;
	}
</style>
