<script lang="ts">
	import { _, json } from 'svelte-i18n';
	import type { WeatherData } from '$lib/domain/weatherData';
	import { getCardinalDirection } from '$lib/domain/windSpeed';
	import DetailCard from './DetailCard.svelte';
	import UvTierBadge from './UvTierBadge.svelte';

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
	let minDual = $derived(data.tempMin.displayDual(metricPrimary));
	let maxDual = $derived(data.tempMax.displayDual(metricPrimary));
	let windDual = $derived(data.windSpeed.displayDual(metricPrimary));
	let gustDual = $derived(data.windGust?.displayDual(metricPrimary));
	let pressDual = $derived(data.pressure.displayDual(metricPrimary));
	let dewDual = $derived(data.dewPoint.displayDual(metricPrimary));
	let visDual = $derived(visibilityDisplay(data.visibility, metricPrimary));
	let cloudCover = $derived(`${$_('detail_cloud_cover')}: ${data.cloudCover != null ? loc(`${data.cloudCover}%`) : '--'}`);
</script>

<div class="conditions-grid">
	<!-- Temperature / Precipitation -->
	<DetailCard
		title={$_('detail_temperature')}
		value={loc(tempDual[0])}
		secondaryValue={loc(tempDual[1])}
		subtitle={`${$_('feels_like')} ${loc(feelsLikeDual[0])}`}
		{onToggleUnits}
	/>
	{#if data.snow}
		{@const [snowP, snowS] = data.snow.displayDual(metricPrimary)}
		<DetailCard title={$_('detail_precipitation')} value={"\u2744\uFE0F " + loc(snowP)} secondaryValue={loc(snowS)} subtitle={cloudCover} {onToggleUnits} />
	{:else if data.rain}
		{@const [rainP, rainS] = data.rain.displayDual(metricPrimary)}
		<DetailCard title={$_('detail_precipitation')} value={"\uD83C\uDF27\uFE0F " + loc(rainP)} secondaryValue={loc(rainS)} subtitle={cloudCover} {onToggleUnits} />
	{:else}
		<DetailCard title={$_('detail_precipitation')} value={$_('no_precip')} subtitle={cloudCover} />
	{/if}

	<!-- Low / High -->
	<DetailCard title={$_('detail_min_temp')} value={"\u2193 " + loc(minDual[0])} secondaryValue={loc(minDual[1])} {onToggleUnits} />
	<DetailCard title={$_('detail_max_temp')} value={"\u2191 " + loc(maxDual[0])} secondaryValue={loc(maxDual[1])} {onToggleUnits} />

	<!-- Wind / Gust -->
	<DetailCard
		title={$_('detail_wind')}
		value={loc(windDual[0])}
		secondaryValue={loc(windDual[1])}
		subtitle={data.windDeg != null ? loc(`${getCardinalDirection(data.windDeg, $json('cardinal_directions'))} (${data.windDeg}°)`) : '--'}
		{onToggleUnits}
	/>
	<DetailCard
		title={$_('detail_wind_gust')}
		value={gustDual ? loc(gustDual[0]) : '--'}
		secondaryValue={gustDual ? loc(gustDual[1]) : undefined}
		{onToggleUnits}
	/>

	<!-- Sunrise / Sunset -->
	<DetailCard title={$_('detail_sunrise')} value={loc(formatTime(data.sunrise))} />
	<DetailCard title={$_('detail_sunset')} value={loc(formatTime(data.sunset))} />

	<!-- Pressure / Humidity -->
	<DetailCard
		title={$_('detail_pressure')}
		value={loc(pressDual[0])}
		secondaryValue={loc(pressDual[1])}
		{onToggleUnits}
	/>
	<DetailCard
		title={$_('detail_humidity')}
		value={data.humidity != null ? loc(`${data.humidity}%`) : '--'}
		subtitle={`${$_('detail_dewpoint')} ${loc(dewDual[0])} · ${loc(dewDual[1])}`}
	/>

	<!-- UV Index (current reading) / Visibility — Air Quality lives on its own forecast card -->
	<DetailCard
		title={$_('detail_uv_index')}
		value={data.uvIndex != null ? loc(data.uvIndex.toFixed(1)) : '--'}
	>
		{#snippet subtitleSnippet()}
			{#if data.uvIndex != null}
				<span><UvTierBadge uvIndex={data.uvIndex} label={getUvLabel(data.uvIndex)} /></span>
			{/if}
		{/snippet}
	</DetailCard>
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
		gap: 10px;
	}
</style>
