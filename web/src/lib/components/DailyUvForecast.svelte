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
	{#each forecasts as item}
		<div class="daily-row">
			<div class="day-info">
				<span class="day-name">{formatDayName(item.date, localeTag, utcOffsetSeconds)}</span>
				<span class="day-date">{loc(formatDayMonth(item.date, localeTag, utcOffsetSeconds))}</span>
			</div>
			<span class="uv-value">{loc(item.uvIndexMax.toFixed(1))}</span>
			<UvTierBadge uvIndex={item.uvIndexMax} label={uvLabel(item.uvIndexMax, $json('uv_labels') as string[])} />
		</div>
	{/each}
</div>

<style>
	.daily-list {
		display: flex;
		flex-direction: column;
		gap: 8px;
	}

	.daily-row {
		display: flex;
		align-items: center;
		gap: 8px;
		padding: 12px 16px;
		background: rgba(42, 31, 165, 0.3);
		border-radius: 12px;
	}

	.day-info {
		display: flex;
		flex-direction: column;
		width: 100px;
		flex-shrink: 0;
	}

	.day-name {
		font-size: 14px;
		font-weight: 500;
		color: white;
	}

	.day-date {
		font-size: 10px;
		color: rgba(255,255,255,0.4);
	}

	.uv-value {
		flex: 1;
		text-align: right;
		font-size: 14px;
		font-weight: 500;
		color: white;
	}
</style>
