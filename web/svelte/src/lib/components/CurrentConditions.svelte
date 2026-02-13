<script lang="ts">
	import { _ } from 'svelte-i18n';
	import type { WeatherData } from '$shared/domain/weatherData';
	import DetailCard from './DetailCard.svelte';

	interface Props {
		data: WeatherData;
		metricPrimary: boolean;
		loc: (s: string) => string;
		onToggleUnits: () => void;
	}

	let { data, metricPrimary, loc, onToggleUnits }: Props = $props();

	function formatTime(epochSec: number): string {
		const d = new Date(epochSec * 1000);
		return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
	}

	function getCardinalDirection(deg: number): string {
		const dirs: string[] = $_('cardinal_directions') as unknown as string[];
		if (!Array.isArray(dirs)) return '';
		const idx = ((deg % 360 + 360) % 360 * 16 / 360) % 16;
		return dirs[Math.round(idx)] ?? '';
	}

	function getUvLabel(uv: number): string {
		const labels: string[] = $_('uv_labels') as unknown as string[];
		if (!Array.isArray(labels)) return '';
		if (uv < 3) return labels[0];
		if (uv < 6) return labels[1];
		if (uv < 8) return labels[2];
		if (uv < 11) return labels[3];
		return labels[4];
	}

	function visibilityDisplay(meters: number, metric: boolean): [string, string] {
		const km = (meters / 1000).toFixed(1);
		const mi = (meters / 1609.34).toFixed(2);
		return metric
			? [`${km} km`, `${mi} mi`]
			: [`${mi} mi`, `${km} km`];
	}

	let tempDual = $derived(data.temperature.displayDual(metricPrimary));
	let feelsLikeDual = $derived(data.feelsLike.displayDual(metricPrimary));
	let minDual = $derived(data.tempMin.displayDual(metricPrimary));
	let maxDual = $derived(data.tempMax.displayDual(metricPrimary));
	let windDual = $derived(data.windSpeed.displayDual(metricPrimary));
	let gustDual = $derived(data.windGust?.displayDual(metricPrimary));
	let pressDual = $derived(data.pressure.displayDual(metricPrimary));
	let dewDual = $derived(data.dewPoint.displayDual(metricPrimary));
	let visDual = $derived(visibilityDisplay(data.visibility, metricPrimary));
</script>

<div class="conditions-grid">
	<!-- High / Low merged card -->
	<!-- svelte-ignore a11y_click_events_have_key_events -->
	<!-- svelte-ignore a11y_no_static_element_interactions -->
	<div class="merged-card highlow-card" onclick={onToggleUnits}>
		<span class="highlow-arrow">↓</span>
		<span class="merged-values">
			<span class="merged-primary">{loc(minDual[0])}</span>
			<span class="merged-secondary">{loc(minDual[1])}</span>
		</span>
		<span class="merged-label">{$_('detail_high_low')}</span>
		<span class="merged-values merged-values-end">
			<span class="merged-primary">{loc(maxDual[0])}</span>
			<span class="merged-secondary">{loc(maxDual[1])}</span>
		</span>
		<span class="highlow-arrow">↑</span>
	</div>

	<!-- Wind merged card -->
	<!-- svelte-ignore a11y_click_events_have_key_events -->
	<!-- svelte-ignore a11y_no_static_element_interactions -->
	<div class="merged-card wind-merged-card" onclick={onToggleUnits}>
		<div class="wind-side">
			<span class="merged-values">
				<span class="merged-primary">{loc(windDual[0])}</span>
				<span class="merged-secondary">{loc(windDual[1])}</span>
			</span>
			<span class="wind-subtitle">{loc(`${getCardinalDirection(data.windDeg)} (${data.windDeg}°)`)}</span>
		</div>
		<span class="merged-label">{$_('detail_wind')}</span>
		<div class="wind-side wind-side-end">
			{#if gustDual}
				<span class="merged-values merged-values-end">
					<span class="merged-primary">{loc(gustDual[0])}</span>
					<span class="merged-secondary">{loc(gustDual[1])}</span>
				</span>
				<span class="wind-subtitle">{$_('detail_wind_gust')}</span>
			{/if}
		</div>
	</div>

	<!-- Sunrise / Sunset merged card -->
	<div class="merged-card sun-card">
		<div class="sun-side">
			<span class="sun-time">{loc(formatTime(data.sunrise))}</span>
			<span class="sun-label">{$_('detail_sunrise')}</span>
		</div>
		<span class="sun-icon">☀️</span>
		<div class="sun-side sun-side-end">
			<span class="sun-time">{loc(formatTime(data.sunset))}</span>
			<span class="sun-label">{$_('detail_sunset')}</span>
		</div>
	</div>

	<!-- Temperature + Precipitation merged card -->
	<!-- svelte-ignore a11y_click_events_have_key_events -->
	<!-- svelte-ignore a11y_no_static_element_interactions -->
	<div class="merged-card temp-precip-card" onclick={onToggleUnits}>
		<div class="tp-side">
			<span class="card-title">{$_('detail_temperature')}</span>
			<span class="tp-values">
				<span class="tp-primary">{loc(tempDual[0])}</span>
				<span class="tp-secondary">{loc(tempDual[1])}</span>
			</span>
			<span class="feels-label">{$_('feels_like')}</span>
			<span class="tp-values">
				<span class="feels-primary">{loc(feelsLikeDual[0])}</span>
				<span class="feels-secondary">{loc(feelsLikeDual[1])}</span>
			</span>
		</div>
		<div class="tp-side tp-side-end">
			<span class="card-title">{$_('detail_precipitation')}</span>
			{#if data.snow}
				{@const [snowP, snowS] = data.snow.displayDual(metricPrimary)}
				<span class="tp-values tp-values-end">
					<span class="tp-primary">{"\u2744\uFE0F " + loc(snowP)}</span>
					<span class="tp-secondary">{loc(snowS)}</span>
				</span>
			{:else if data.rain}
				{@const [rainP, rainS] = data.rain.displayDual(metricPrimary)}
				<span class="tp-values tp-values-end">
					<span class="tp-primary">{"\uD83C\uDF27\uFE0F " + loc(rainP)}</span>
					<span class="tp-secondary">{loc(rainS)}</span>
				</span>
			{:else}
				<span class="tp-primary">{$_('no_precip')}</span>
			{/if}
			<span class="feels-label">{$_('detail_cloud_cover')}</span>
			<span class="feels-primary">{loc(`${data.cloudCover}%`)}</span>
		</div>
	</div>

	<DetailCard
		title={$_('detail_pressure')}
		value={loc(pressDual[0])}
		secondaryValue={loc(pressDual[1])}
		{onToggleUnits}
	/>
	<!-- Humidity + Dew point combined card -->
	<div class="detail-card humidity-card">
		<span class="card-title">{$_('detail_humidity')}</span>
		<span class="card-value">{loc(`${data.humidity}%`)}</span>
		<span class="dew-label">{$_('detail_dewpoint')}</span>
		<!-- svelte-ignore a11y_click_events_have_key_events -->
		<!-- svelte-ignore a11y_no_static_element_interactions -->
		<span class="dew-values" onclick={onToggleUnits}>
			<span class="dew-primary">{loc(dewDual[0])}</span>
			<span class="dew-secondary">{loc(dewDual[1])}</span>
		</span>
	</div>

	<DetailCard
		title={$_('detail_uv_index')}
		value={loc(data.uvIndex.toFixed(1))}
		subtitle={getUvLabel(data.uvIndex)}
	/>
	<DetailCard
		title={$_('detail_visibility')}
		value={loc(visDual[0])}
		secondaryValue={loc(visDual[1])}
		{onToggleUnits}
	/>
</div>

<style>
	.conditions-grid {
		display: grid;
		grid-template-columns: 1fr 1fr;
		grid-auto-rows: auto;
		gap: 16px;
	}

	.merged-card {
		background: rgba(42, 31, 165, 0.6);
		border-radius: 16px;
		padding: 16px;
		display: flex;
		flex-direction: row;
		gap: 8px;
		grid-column: 1 / -1;
		align-items: center;
		justify-content: space-between;
		cursor: pointer;
	}

	.merged-label {
		font-size: 12px;
		color: rgba(255,255,255,0.5);
		text-align: center;
	}

	.merged-values {
		display: flex;
		align-items: baseline;
		gap: 4px;
	}

	.merged-values-end {
		justify-content: flex-end;
	}

	.merged-primary {
		font-family: var(--font-display);
		font-size: 18px;
		font-weight: 700;
		color: white;
		font-feature-settings: var(--font-features);
	}

	.merged-secondary {
		font-family: var(--font-display);
		font-size: 12px;
		color: rgba(255,255,255,0.55);
		font-feature-settings: var(--font-features);
	}

	.highlow-arrow {
		font-size: 24px;
		color: rgba(255,255,255,0.7);
	}

	.wind-merged-card {
		cursor: pointer;
	}

	.wind-side {
		display: flex;
		flex-direction: column;
		gap: 2px;
		min-width: 0;
	}

	.wind-side-end {
		align-items: flex-end;
	}

	.wind-subtitle {
		font-size: 12px;
		color: rgba(255,255,255,0.6);
	}

	.sun-card {
		cursor: default;
	}

	.sun-side {
		display: flex;
		flex-direction: column;
		gap: 2px;
	}

	.sun-side-end {
		align-items: flex-end;
	}

	.sun-time {
		font-family: var(--font-display);
		font-size: 18px;
		font-weight: 700;
		color: white;
		font-feature-settings: var(--font-features);
	}

	.sun-label {
		font-size: 12px;
		color: rgba(255,255,255,0.6);
	}

	.sun-icon {
		font-size: 24px;
	}

	.temp-precip-card {
		align-items: flex-start;
	}

	.tp-side {
		display: flex;
		flex-direction: column;
		gap: 4px;
	}

	.tp-side-end {
		align-items: flex-end;
	}

	.tp-values {
		display: flex;
		align-items: baseline;
		gap: 4px;
	}

	.tp-values-end {
		justify-content: flex-end;
	}

	.tp-primary {
		font-family: var(--font-display);
		font-size: 18px;
		font-weight: 700;
		color: white;
		font-feature-settings: var(--font-features);
	}

	.tp-secondary {
		font-family: var(--font-display);
		font-size: 12px;
		color: rgba(255,255,255,0.55);
		font-feature-settings: var(--font-features);
	}

	.feels-label {
		font-size: 12px;
		color: rgba(255,255,255,0.5);
	}

	.feels-primary {
		font-family: var(--font-display);
		font-size: 13px;
		font-weight: 700;
		color: white;
	}

	.feels-secondary {
		font-family: var(--font-display);
		font-size: 12px;
		color: rgba(255,255,255,0.55);
		margin-left: 4px;
	}

	.humidity-card {
		background: rgba(42, 31, 165, 0.6);
		border-radius: 16px;
		padding: 16px;
		display: flex;
		flex-direction: column;
		gap: 8px;
		flex: 1;
	}

	.card-title {
		font-size: 14px;
		color: rgba(255,255,255,0.7);
	}

	.card-value {
		font-family: var(--font-display);
		font-size: 20px;
		font-weight: 700;
		color: white;
		font-feature-settings: var(--font-features);
	}

	.dew-label {
		font-size: 12px;
		color: rgba(255,255,255,0.5);
	}

	.dew-values {
		cursor: pointer;
	}

	.dew-primary {
		font-family: var(--font-display);
		font-size: 13px;
		font-weight: 700;
		color: white;
	}

	.dew-secondary {
		font-family: var(--font-display);
		font-size: 12px;
		color: rgba(255,255,255,0.55);
		margin-left: 4px;
	}
</style>
