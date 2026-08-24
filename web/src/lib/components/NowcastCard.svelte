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
	let maxPrecipMm = $derived(Math.max(...displayItems.map((i) => i.precipitation.mm), 0.1));
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

	const totalHeight = 95;
	const chartHeight = 77;

	function generateMonotonePath(pts: { x: number; y: number }[]): string {
		const n = pts.length;
		if (n < 2) return '';
		if (n === 2) return `M ${pts[0].x.toFixed(1)} ${pts[0].y.toFixed(1)} L ${pts[1].x.toFixed(1)} ${pts[1].y.toFixed(1)}`;

		const dx = new Float64Array(n - 1);
		const dy = new Float64Array(n - 1);
		const ms = new Float64Array(n - 1);

		for (let i = 0; i < n - 1; i++) {
			dx[i] = Math.max(0.0001, pts[i + 1].x - pts[i].x);
			dy[i] = pts[i + 1].y - pts[i].y;
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

		let d = `M ${pts[0].x.toFixed(1)} ${pts[0].y.toFixed(1)}`;
		for (let i = 0; i < n - 1; i++) {
			const h = dx[i];
			const p1 = pts[i];
			const p2 = pts[i + 1];
			const cp1X = (p1.x + h / 3).toFixed(1);
			const cp1Y = (p1.y + (ds[i] * h) / 3).toFixed(1);
			const cp2X = (p2.x - h / 3).toFixed(1);
			const cp2Y = (p2.y - (ds[i + 1] * h) / 3).toFixed(1);
			d += ` C ${cp1X} ${cp1Y}, ${cp2X} ${cp2Y}, ${p2.x.toFixed(1)} ${p2.y.toFixed(1)}`;
		}
		return d;
	}

	let points = $derived.by(() => {
		if (displayItems.length <= 1) return [];
		return displayItems.map((item) => {
			const ratioX = (item.time - startTime) / durationMs;
			const x = ratioX * containerWidth;
			const fraction = Math.max(0, Math.min(1, item.precipitation.mm / maxPrecipMm));
			const y = chartHeight - fraction * (chartHeight - 16);
			return { x, y };
		});
	});

	let maxPointInfo = $derived.by(() => {
		if (points.length === 0) return null;
		const minY = Math.min(...points.map(p => p.y));
		const maxIndices = points.map((p, i) => p.y === minY ? i : -1).filter(i => i !== -1);
		const midIdx = maxIndices[Math.floor(maxIndices.length / 2)];
		const pt = points[midIdx];
		const x = Math.max(20, Math.min(containerWidth - 20, pt.x));
		const y = Math.max(12, Math.min(chartHeight - 12, pt.y - 4));
		return { x, y };
	});

	let minPointInfo = $derived.by(() => {
		if (points.length === 0) return null;
		const maxY = Math.max(...points.map(p => p.y));
		const minIndices = points.map((p, i) => p.y === maxY ? i : -1).filter(i => i !== -1);
		const midIdx = minIndices[Math.floor(minIndices.length / 2)];
		const pt = points[midIdx];
		const x = Math.max(20, Math.min(containerWidth - 20, pt.x));
		const y = chartHeight - 4;
		return { x, y };
	});

	let showMinLegend = $derived.by(() => {
		if (!maxPointInfo || !minPointInfo) return false;
		const hOverlap = Math.abs(maxPointInfo.x - minPointInfo.x) < 40;
		const vOverlap = Math.abs(maxPointInfo.y - minPointInfo.y) < 14;
		return !(hOverlap && vOverlap);
	});

	let pathD = $derived(generateMonotonePath(points));

	let areaPathD = $derived.by(() => {
		if (points.length < 2 || !pathD) return '';
		return `${pathD} L ${points[points.length - 1].x.toFixed(1)} ${chartHeight} L ${points[0].x.toFixed(1)} ${chartHeight} Z`;
	});
</script>

<div class="nowcast-card">
	<div class="header-row">
		<span class="icon">🌧️</span>
		<span class="headline">{headlineText}</span>
	</div>

	{#if hasPrecipitation && displayItems.length > 1}
		<div class="chart-section">
			<div class="chart-container" bind:this={container}>
				<svg width="100%" height={totalHeight} viewBox="0 0 {containerWidth} {totalHeight}">
					<defs>
						<linearGradient id="nowcastGradient" x1="0" y1="0" x2="0" y2="1">
							<stop offset="0%" stop-color="#38bdf8" stop-opacity="0.4" />
							<stop offset="100%" stop-color="#38bdf8" stop-opacity="0" />
						</linearGradient>
					</defs>
					{#if points.length > 1}
						<!-- Top dashed guideline -->
						<line x1="0" y1="4" x2={containerWidth} y2="4" stroke="rgba(255, 255, 255, 0.2)" stroke-dasharray="4 4" />
						<!-- Bottom baseline -->
						<line x1="0" y1={chartHeight} x2={containerWidth} y2={chartHeight} stroke="rgba(255, 255, 255, 0.2)" />
						
						<!-- 30-min vertical tick lines & aligned X-axis labels -->
						{#each ticks30Min as tickTime}
							{@const ratioX = Math.max(0, Math.min(1, (tickTime - startTime) / durationMs))}
							{@const tickX = ratioX * containerWidth}
							<line x1={tickX} y1="0" x2={tickX} y2={chartHeight} stroke="rgba(255, 255, 255, 0.12)" stroke-dasharray="4 4" />
							<text x={tickX} y={totalHeight - 2} text-anchor="middle" font-size="9" fill="rgba(255, 255, 255, 0.7)">{formatTime(tickTime)}</text>
						{/each}

						<!-- Area fill and smooth Bezier line stroke -->
						<path d={areaPathD} fill="url(#nowcastGradient)" />
						<path d={pathD} fill="none" stroke="#38bdf8" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />

						<!-- Max legend on peak curve point -->
						{#if maxPointInfo}
							<text x={maxPointInfo.x} y={maxPointInfo.y} text-anchor="middle" font-size="9" font-weight="bold" fill="#38bdf8">{yLabelText}</text>
						{/if}

						<!-- Min legend on baseline min point -->
						{#if minPointInfo && showMinLegend}
							<text x={minPointInfo.x} y={minPointInfo.y} text-anchor="middle" font-size="9" fill="rgba(255, 255, 255, 0.4)">{yZeroText}</text>
						{/if}
					{/if}
				</svg>
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

	.chart-container {
		position: relative;
		display: flex;
		flex-direction: column;
		width: 100%;
	}

	svg {
		overflow: visible;
		width: 100%;
		display: block;
	}
</style>
