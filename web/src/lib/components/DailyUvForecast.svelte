<script lang="ts">
	import { json } from 'svelte-i18n';
	import type { DailyForecast as DailyForecastType } from '$lib/domain/weatherData';
	import { formatDayName, formatDayMonth } from '$lib/utils/date';
	import DailyUvChart from './DailyUvChart.svelte';
	import UvTierBadge from './UvTierBadge.svelte';

	interface Props {
		forecasts: DailyForecastType[];
		localeTag: string;
		loc: (s: string) => string;
		utcOffsetSeconds: number;
	}

	let { forecasts, localeTag, loc, utcOffsetSeconds }: Props = $props();

	const weekdayNames = $derived($json('weekday_names') as string[] | undefined);
	const monthNames = $derived($json('month_names') as string[] | undefined);

	const hasUv = (v: number | null): v is number => v != null && Number.isFinite(v);

	function uvLabel(uvIndex: number, labels: string[]): string {
		if (uvIndex < 3) return labels[0];
		if (uvIndex < 6) return labels[1];
		if (uvIndex < 8) return labels[2];
		if (uvIndex < 11) return labels[3];
		return labels[4];
	}
</script>

<div class="daily-list">
	{#if forecasts.length > 0}
		<DailyUvChart {forecasts} />
	{/if}
	<div class="uv-rows">
		{#each forecasts as item}
			<div class="daily-row">
				<span class="day-name">{formatDayName(item.date, localeTag, utcOffsetSeconds, false, weekdayNames)}</span>
				<span class="day-date">{loc(formatDayMonth(item.date, localeTag, utcOffsetSeconds, monthNames))}</span>
				{#if hasUv(item.uvIndexMax)}
					<UvTierBadge uvIndex={item.uvIndexMax} label={uvLabel(item.uvIndexMax, $json('uv_labels') as string[])} />
				{/if}
				<span class="uv-value">{hasUv(item.uvIndexMax) ? loc(item.uvIndexMax.toFixed(1)) : '--'}</span>
			</div>
		{/each}
	</div>
</div>

<style>
	.daily-list {
		display: flex;
		flex-direction: column;
	}

	.uv-rows {
		background: var(--surface-tile);
		border-radius: 16px;
		padding: 0 16px;
	}

	.daily-row {
		display: flex;
		align-items: center;
		gap: 8px;
		padding: 10px 0;
	}

	.daily-row + .daily-row {
		border-top: 1px solid var(--hairline);
	}

	.day-name {
		font-size: 14px;
		font-weight: 500;
		color: white;
		white-space: nowrap;
	}

	.day-date {
		flex: 1;
		min-width: 0;
		font-size: 12px;
		color: rgba(255,255,255,0.45);
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
	}

	.uv-value {
		width: 40px;
		text-align: end;
		font-family: var(--font-display);
		font-size: 14px;
		font-weight: 700;
		color: white;
		font-feature-settings: var(--font-features);
	}
</style>
