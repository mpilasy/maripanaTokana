<!-- SPDX-FileCopyrightText: Orinasa Njarasoa -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
<script lang="ts">
	import { _, json } from 'svelte-i18n';
	import { equalRowTitles } from '#lib/utils/equalRowTitles.js';
	import { getCardinalDirection } from '#lib/domain/windSpeed.js';
	import { Temperature } from '#lib/domain/temperature.js';
	import { nextTides, seaHeightDual, type SeaConditions } from '#lib/domain/sea.js';
	import { formatHourAtLocation } from '#lib/utils/date.js';
	import DetailCard from './DetailCard.svelte';
	import WindCompass from './WindCompass.svelte';
	import DialValue from './DialValue.svelte';

	interface Props {
		sea: SeaConditions;
		utcOffsetSeconds: number;
		metricPrimary: boolean;
		loc: (s: string) => string;
		onToggleUnits: () => void;
	}

	let { sea, utcOffsetSeconds, metricPrimary, loc, onToggleUnits }: Props = $props();

	let tides = $derived(nextTides(sea.levelTimes, sea.levels, Date.now()));
	// The next two tides in the order they come
	let tideLines = $derived(
		[tides.high && { key: 'tide_high', time: tides.high.time }, tides.low && { key: 'tide_low', time: tides.low.time }]
			.filter((t) => t != null)
			.sort((a, b) => a.time - b.time)
			.map((t) => $_(t.key, { values: { time: loc(formatHourAtLocation(t.time, utcOffsetSeconds)) } }))
	);
	let tempDual = $derived(sea.seaTemperatureC != null ? Temperature.fromCelsius(sea.seaTemperatureC).displayDual(metricPrimary) : null);
	let levelDual = $derived(sea.seaLevelM != null ? seaHeightDual(sea.seaLevelM, metricPrimary) : null);
</script>

<div class="sea-grid" use:equalRowTitles>
	{#if sea.waveHeightM != null}
		{@render waveTile($_('sea_waves'), sea.waveHeightM, sea.wavePeriodS, sea.waveDirection)}
	{/if}
	{#if sea.swellHeightM != null}
		{@render waveTile($_('sea_swell'), sea.swellHeightM, sea.swellPeriodS, sea.swellDirection)}
	{/if}
	{#if tempDual}
		<DetailCard title={$_('sea_temperature')} value={loc(tempDual[0])} {onToggleUnits}>
			{#snippet centerDialSnippet()}
				<div class="square"><DialValue value={loc(tempDual[0])} secondary={loc(tempDual[1])} /></div>
			{/snippet}
		</DetailCard>
	{/if}
	{#if levelDual || tideLines.length > 0}
		<!-- The sea level now, and the next high and low tide in the place's time -->
		<DetailCard title={$_('sea_tide')} value={levelDual ? loc(levelDual[0]) : '--'} secondaryValue={levelDual ? loc(levelDual[1]) : undefined} onToggleUnits={levelDual ? onToggleUnits : undefined}>
			{#snippet footerSnippet()}
				{#each tideLines as line (line)}
					<span class="footer-center">{line}</span>
				{/each}
			{/snippet}
		</DetailCard>
	{/if}
</div>
<p class="sea-note">{$_('sea_note')}</p>

<!-- The height of the waves or the swell, its period, and where they come from, as on Android: no dial -->
{#snippet waveTile(title: string, heightM: number, periodS: number | null, direction: number | null)}
	{@const dual = seaHeightDual(heightM, metricPrimary)}
	<DetailCard {title} value={loc(dual[0])} secondaryValue={loc(dual[1])} subtitle={periodS != null ? loc($_('sea_period', { values: { seconds: Math.round(periodS) } })) : undefined} {onToggleUnits}>
		{#snippet trailingSnippet()}
			{#if direction != null}
				<span class="sea-dir">
					<WindCompass degrees={direction} size={18} />
					{getCardinalDirection(direction, $json('cardinal_directions'))}
				</span>
			{/if}
		{/snippet}
	</DetailCard>
{/snippet}

<style>
	/* As the tiles of the other detail sections */
	.sea-grid {
		display: grid;
		grid-template-columns: 1fr 1fr;
		grid-auto-rows: auto;
		gap: 10px;
	}

	.square {
		position: relative;
		width: 100%;
		aspect-ratio: 1;
	}

	.footer-center {
		display: block;
		text-align: center;
		font-size: 0.8125rem;
		color: rgba(255, 255, 255, 0.7);
	}

	/* A small compass to where the waves come from, with the direction's short name, as on the forecast cards */
	.sea-dir {
		display: flex;
		align-items: center;
		gap: 4px;
		font-size: 0.8125rem;
		color: rgba(255, 255, 255, 0.7);
	}

	.sea-note {
		margin: 8px 4px 0;
		font-size: 0.6875rem;
		color: rgba(255, 255, 255, 0.55);
	}
</style>
