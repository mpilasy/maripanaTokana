<script lang="ts">
	import { _ } from 'svelte-i18n';
	import type { MinutelyForecast } from '$lib/domain/weatherData';
	import { Precipitation } from '$lib/domain/precipitation';
	import { metricPrimary } from '$lib/stores/preferences';
	import { onMount } from 'svelte';

	interface Props {
		items: MinutelyForecast[];
		localizeDigits: (val: string) => string;
	}

	let { items, localizeDigits }: Props = $props();

	let containerWidth = $state(100);
	let container: HTMLDivElement | undefined = $state();

	onMount(() => {
		if (!container) return;
		const observer = new ResizeObserver((entries) => {
			for (const entry of entries) {
				containerWidth = entry.contentRect.width;
			}
		});
		observer.observe(container);
		return () => observer.disconnect();
	});

	let nowMs = $state(Date.now());
	let displayItems = $derived(items.filter((i) => i.time >= nowMs - 15 * 60 * 1000 && i.time <= nowMs + 2 * 3600 * 1000));
	let hasPrecipitation = $derived(displayItems.some((i) => i.precipitation.mm > 0.05));
	let maxPrecipMm = $derived(Math.max(...displayItems.map((i) => i.precipitation.mm), 0.5));
	let maxPrecipObj = $derived(Precipitation.fromMm(maxPrecipMm));
	let yLabelText = $derived(localizeDigits($metricPrimary ? maxPrecipObj.displayMetric() : maxPrecipObj.displayImperial()));
	let yZeroText = $derived(localizeDigits($metricPrimary ? '0.0 mm' : '0.00 in'));

	let firstPrecipIndex = $derived(displayItems.findIndex((i) => i.precipitation.mm > 0.05));
	let minutesUntilStart = $derived.by(() => {
		let result = 0;
		if (firstPrecipIndex > 0 && displayItems.length > 0) {
			const diffMs = displayItems[firstPrecipIndex].time - nowMs;
			result = Math.max(1, Math.round(diffMs / (60 * 1000)));
		}
		return result;
	});

	let headlineText = $derived.by(() => {
		let result = '';
		if (displayItems.length === 0 || !hasPrecipitation) {
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

	function generate30MinTicks(startTime: number, endTime: number): number[] {
		let result: number[] = [];
		if (endTime <= startTime) {
			result = [];
		} else {
			const intervalMs = 30 * 60 * 1000;
			const firstTick = Math.ceil(startTime / intervalMs) * intervalMs;
			const ticks: number[] = [];
			let curr = firstTick;
			while (curr <= endTime) {
				ticks.push(curr);
				curr += intervalMs;
			}
			result = ticks;
		}
		return result;
	}

	let startTime = $derived(displayItems.length ? displayItems[0].time : 0);
	let endTime = $derived(displayItems.length ? displayItems[displayItems.length - 1].time : 1);
	let durationMs = $derived(Math.max(1, endTime - startTime));
	let ticks30Min = $derived(generate30MinTicks(startTime, endTime));

	const height = 70;
	let points = $derived.by(() => {
		if (displayItems.length <= 1) return [];
		return displayItems.map((item) => {
			const ratioX = (item.time - startTime) / durationMs;
			const x = ratioX * containerWidth;
			const fraction = Math.max(0, Math.min(1, item.precipitation.mm / maxPrecipMm));
			const y = height - 2 - fraction * (height - 14);
			return { x, y };
		});
	});

	let pathD = $derived.by(() => {
		const n = points.length;
		if (n <= 1) return '';
		if (n === 2) return `M ${points[0].x} ${points[0].y} L ${points[1].x} ${points[1].y}`;

		const dx = new Float64Array(n - 1);
		const dy = new Float64Array(n - 1);
		const ms = new Float64Array(n - 1);

		for (let i = 0; i < n - 1; i++) {
			dx[i] = Math.max(0.0001, points[i + 1].x - points[i].x);
			dy[i] = points[i + 1].y - points[i].y;
			ms[i] = dy[i] / dx[i];
		}

		const ds = new Float64Array(n);
		ds[0] = ms[0];
		ds[n - 1] = ms[n - 2];

		for (let i = 1; i < n - 1; i++) {
			if (ms[i - 1] * ms[i] <= 0) {
				ds[i] = 0;
			} else {
				ds[i] = (ms[i - 1] + ms[i]) / 2;
			}
		}

		for (let i = 0; i < n - 1; i++) {
			if (ms[i] === 0) {
				ds[i] = 0;
				ds[i + 1] = 0;
			} else {
				const alpha = ds[i] / ms[i];
				const beta = ds[i + 1] / ms[i];
				const dist = alpha * alpha + beta * beta;
				if (dist > 9) {
					const tau = 3 / Math.sqrt(dist);
					ds[i] = tau * alpha * ms[i];
					ds[i + 1] = tau * beta * ms[i];
				}
			}
		}

		let d = `M ${points[0].x} ${points[0].y}`;
		for (let i = 0; i < n - 1; i++) {
			const h = dx[i];
			const p1 = points[i];
			const p2 = points[i + 1];
			const cp1X = p1.x + h / 3;
			const cp1Y = p1.y + (ds[i] * h) / 3;
			const cp2X = p2.x - h / 3;
			const cp2Y = p2.y - (ds[i + 1] * h) / 3;
			d += ` C ${cp1X} ${cp1Y}, ${cp2X} ${cp2Y}, ${p2.x} ${p2.y}`;
		}
		return d;
	});

	let areaPathD = $derived.by(() => {
		if (points.length === 0 || !pathD) return '';
		return `${pathD} L ${points[points.length - 1].x} ${height - 2} L ${points[0].x} ${height - 2} Z`;
	});
</script>

<div class="nowcast-card">
	<div class="header-row">
		<span class="icon">🌧️</span>
		<span class="headline">{headlineText}</span>
	</div>

	{#if hasPrecipitation && displayItems.length > 1}
		<div class="chart-section">
			<!-- Vertical Y-axis scale header -->
			<div class="y-axis-labels">
				<span class="y-label max-label">{yLabelText}</span>
				<span class="y-label zero-label">{yZeroText}</span>
			</div>

			<div class="chart-container" bind:this={container}>
				<svg width="100%" height={height} viewBox="0 0 {containerWidth} {height}" preserveAspectRatio="none">
					<defs>
						<linearGradient id="nowcastGradient" x1="0" y1="0" x2="0" y2="1">
							<stop offset="0%" stop-color="#38bdf8" stop-opacity="0.4" />
							<stop offset="100%" stop-color="#38bdf8" stop-opacity="0" />
						</linearGradient>
					</defs>
					{#if points.length > 0}
						<!-- Top dashed guideline -->
						<line x1="0" y1="6" x2={containerWidth} y2="6" stroke="rgba(255, 255, 255, 0.2)" stroke-dasharray="4 4" />
						<!-- Bottom baseline -->
						<line x1="0" y1={height - 2} x2={containerWidth} y2={height - 2} stroke="rgba(255, 255, 255, 0.2)" />
						
						<!-- Area fill and Monotone Cubic Spline line stroke -->
						<path d={areaPathD} fill="url(#nowcastGradient)" />
						<path d={pathD} fill="none" stroke="#38bdf8" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
					{/if}
				</svg>

				<!-- 30-minute horizontal X-axis labels -->
				<div class="x-axis-labels">
					{#each ticks30Min as tickTime}
						<span class="x-label">{formatTime(tickTime)}</span>
					{/each}
				</div>
			</div>
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
		gap: 12px;
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

	.chart-section {
		display: flex;
		flex-direction: column;
		gap: 4px;
		width: 100%;
	}

	.y-axis-labels {
		display: flex;
		justify-content: space-between;
		align-items: center;
		width: 100%;
	}

	.y-label {
		font-size: 0.65rem;
		font-family: inherit;
	}

	.max-label {
		color: rgba(255, 255, 255, 0.8);
	}

	.zero-label {
		color: rgba(255, 255, 255, 0.4);
	}

	.chart-container {
		display: flex;
		flex-direction: column;
		gap: 4px;
		width: 100%;
	}

	svg {
		overflow: visible;
	}

	.x-axis-labels {
		display: flex;
		justify-content: space-between;
		width: 100%;
	}

	.x-label {
		font-size: 0.6rem;
		color: rgba(255, 255, 255, 0.7);
		white-space: nowrap;
	}
</style>
