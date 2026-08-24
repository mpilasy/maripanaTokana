<script lang="ts">
	import { _ } from 'svelte-i18n';
	import type { MinutelyForecast } from '$lib/domain/weatherData';
	import { Precipitation } from '$lib/domain/precipitation';

	interface Props {
		items: MinutelyForecast[];
		localizeDigits: (val: string) => string;
	}

	let { items, localizeDigits }: Props = $props();

	function interpolatePrecip(sorted: MinutelyForecast[], targetTime: number): number {
		let result = 0;
		if (sorted.length === 0) {
			result = 0;
		} else if (targetTime <= sorted[0].time) {
			result = sorted[0].precipitation.mm;
		} else if (targetTime >= sorted[sorted.length - 1].time) {
			result = sorted[sorted.length - 1].precipitation.mm;
		} else {
			const idx = sorted.findLastIndex((i) => i.time <= targetTime);
			if (idx !== -1 && idx < sorted.length - 1) {
				const t1 = sorted[idx].time;
				const t2 = sorted[idx + 1].time;
				const p1 = sorted[idx].precipitation.mm;
				const p2 = sorted[idx + 1].precipitation.mm;
				if (t2 > t1) {
					const ratio = (targetTime - t1) / (t2 - t1);
					result = p1 + ratio * (p2 - p1);
				} else {
					result = p1;
				}
			} else if (idx !== -1) {
				result = sorted[idx].precipitation.mm;
			} else {
				result = 0;
			}
		}
		return result;
	}

	function sampleEvery10Minutes(rawItems: MinutelyForecast[]): MinutelyForecast[] {
		let result: MinutelyForecast[] = [];
		if (rawItems.length === 0) {
			result = [];
		} else {
			const sorted = [...rawItems].sort((a, b) => a.time - b.time);
			const now = Date.now();
			const startTime = Math.floor(now / (10 * 60 * 1000)) * (10 * 60 * 1000);
			const points: MinutelyForecast[] = [];
			for (let k = 0; k <= 12; k++) {
				const targetTime = startTime + k * 10 * 60 * 1000;
				const precipMm = interpolatePrecip(sorted, targetTime);
				points.push({
					time: targetTime,
					precipitation: Precipitation.fromMm(precipMm),
				});
			}
			result = points;
		}
		return result;
	}

	let displayItems = $derived(sampleEvery10Minutes(items));
	let maxPrecipMm = $derived(Math.max(...displayItems.map((i) => i.precipitation.mm), 0.5));

	let firstPrecipIndex = $derived(displayItems.findIndex((i) => i.precipitation.mm > 0.05));
	let minutesUntilStart = $derived(firstPrecipIndex > 0 ? firstPrecipIndex * 10 : 0);

	let headlineText = $derived.by(() => {
		let result = '';
		if (displayItems.length === 0 || displayItems.every((i) => i.precipitation.mm <= 0.05)) {
			result = $_('nowcast_no_precip');
		} else if (firstPrecipIndex === 0) {
			result = $_('nowcast_ongoing');
		} else if (firstPrecipIndex > 0) {
			result = $_('nowcast_starts_in', { values: { minutes: localizeDigits(minutesUntilStart.toString()) } });
		} else {
			result = $_('nowcast_no_precip');
		}
		return result;
	});

	function formatTime(epochMs: number): string {
		const d = new Date(epochMs);
		const hh = d.getHours().toString().padStart(2, '0');
		const mm = d.getMinutes().toString().padStart(2, '0');
		return localizeDigits(`${hh}:${mm}`);
	}
</script>

<div class="nowcast-card">
	<div class="header-row">
		<span class="icon">🌧️</span>
		<span class="headline">{headlineText}</span>
	</div>

	{#if displayItems.length > 0}
		<div class="sparkline-container">
			{#each displayItems as item}
				{@const fraction = Math.max(0.05, Math.min(1.0, item.precipitation.mm / maxPrecipMm))}
				{@const isRaining = item.precipitation.mm > 0.05}
				<div class="bar-column">
					<div class="bar-wrapper">
						<div
							class="bar {isRaining ? 'active' : ''}"
							style="height: {fraction * 100}%"
						></div>
					</div>
					<span class="time-label">{formatTime(item.time)}</span>
				</div>
			{/each}
		</div>
	{/if}
</div>

<style>
	.nowcast-card {
		background: rgba(42, 31, 165, 0.6);
		backdrop-filter: blur(12px);
		-webkit-backdrop-filter: blur(12px);
		border-radius: 16px;
		padding: 16px;
		display: flex;
		flex-direction: column;
		gap: 16px;
	}

	.header-row {
		display: flex;
		align-items: center;
		gap: 8px;
	}

	.icon {
		font-size: 1.1rem;
	}

	.headline {
		color: #ffffff;
		font-size: 0.95rem;
		font-weight: 500;
	}

	.sparkline-container {
		display: flex;
		justify-content: space-between;
		align-items: flex-end;
		height: 90px;
		width: 100%;
		gap: 2px;
	}

	.bar-column {
		flex: 1;
		display: flex;
		flex-direction: column;
		align-items: center;
		height: 100%;
		justify-content: flex-end;
	}

	.bar-wrapper {
		height: 60px;
		width: 100%;
		display: flex;
		align-items: flex-end;
		justify-content: center;
	}

	.bar {
		width: 6px;
		border-radius: 3px;
		background: rgba(255, 255, 255, 0.2);
		transition: height 0.3s ease;
	}

	.bar.active {
		background: #38bdf8;
		box-shadow: 0 0 6px rgba(56, 189, 248, 0.4);
	}

	.time-label {
		font-size: 0.6rem;
		color: rgba(255, 255, 255, 0.7);
		margin-top: 6px;
		white-space: nowrap;
	}
</style>
