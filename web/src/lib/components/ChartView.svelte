<!-- SPDX-FileCopyrightText: Orinasa Njarasoa -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
<script lang="ts">
	import { tick } from 'svelte';
	import { _, json } from 'svelte-i18n';
	import SettingsPage from './SettingsPage.svelte';
	import WeatherIcon from './WeatherIcon.svelte';
	import type { ChartHour } from '#lib/domain/weatherData.js';
	import { getCardinalDirection, showWindArrow } from '#lib/domain/windSpeed.js';
	import { uvColorFor, uvTier } from '#lib/domain/uv.js';
	import { monotonePath } from '#lib/utils/monotonePath.js';
	import { runsOf } from '#lib/utils/dualLabel.js';
	import { ChartStyle } from '#lib/utils/chartStyle.js';
	import { formatDayName, formatHourAtLocation } from '#lib/utils/date.js';
	import {
		CHART_SLOT_DEFAULT, chartDaySpans, chartDomain, chartHours, chartIndexForX, chartNightRuns, chartVisibleRange, chartZoom,
		type ChartDomain, type ChartZoom,
	} from '#lib/utils/chartView.js';

	/**
	 * Up to 240 hours of the forecast in stacked panels that scroll sideways together, with the values of the
	 * selected hour at the top. It opens at the current hour. Android has the same view in ChartViewScreen.kt.
	 */
	interface Props {
		hours: ChartHour[];
		utcOffsetSeconds: number;
		placeName: string;
		metricPrimary: boolean;
		localeTag: string;
		loc: (s: string) => string;
		onToggleUnits: () => void;
		onBack: () => void;
	}

	let { hours, utcOffsetSeconds, placeName, metricPrimary, localeTag, loc, onToggleUnits, onBack }: Props = $props();

	// Sizes in px, the same as on Android: the axis column, the gap between panels, the title line,
	// the room above the wind line for the direction arrows, and the weather icons
	const AXIS_WIDTH = 52;
	const PANEL_GAP = 6;
	const TITLE_HEIGHT = 16;
	const ARROW_ROW = 14;
	const ICON_SIZE = 26;

	// The panels from top to bottom, with their height; the value panels have a title line at the top
	type PanelId = 'icons' | 'temp' | 'precip' | 'wind' | 'pressure' | 'humidity' | 'time';
	const PANEL_HEIGHTS: [PanelId, number][] = [['icons', 46], ['temp', 132], ['precip', 88], ['wind', 104], ['pressure', 80], ['humidity', 80], ['time', 36]];
	/** A panel: its top and height, and the part where the values go (`plotTop` to `plotBottom`). */
	interface Panel { top: number; height: number; plotTop: number; plotBottom: number }
	const { panels, totalHeight } = (() => {
		const result = {} as Record<PanelId, Panel>;
		let top = 0;
		for (const [id, height] of PANEL_HEIGHTS) {
			const titled = id !== 'icons' && id !== 'time';
			const plotTop = top + (titled ? TITLE_HEIGHT : 0) + (id === 'wind' ? ARROW_ROW : 0) + 4;
			result[id] = { top, height, plotTop, plotBottom: top + height - 4 };
			top += height + PANEL_GAP;
		}
		return { panels: result, totalHeight: top - PANEL_GAP };
	})();
	const chartsBottom = panels.humidity.top + panels.humidity.height;
	// The vertical lines (midnight, selected hour) go only through the plot area of each panel, with a break at the
	// gaps and title lines, so that each chart shows where it starts and ends. The time axis has its own short ticks.
	const LINE_SEGMENTS = (['icons', 'temp', 'precip', 'wind', 'pressure', 'humidity'] as PanelId[]).map((id) => panels[id]);

	// The data can be some hours old: start again at the current hour
	const openedAt = Date.now();
	let shown = $derived.by(() => {
		const range = chartHours(hours.map((h) => h.time), openedAt);
		return hours.slice(range.start, range.end);
	});
	let count = $derived(shown.length);

	let slot = $state(CHART_SLOT_DEFAULT);
	let scrollLeft = $state(0);
	let viewport = $state(0);
	let selected = $state(0);
	let scroller = $state<HTMLElement | null>(null);

	// Lines are drawn for the hours in view and 2 more on each side; the scales fit the hours in view only
	let drawn = $derived(chartVisibleRange(scrollLeft, viewport, slot, count));
	let inView = $derived(chartVisibleRange(scrollLeft, viewport, slot, count, 0));
	let drawIndexes = $derived(Array.from({ length: Math.max(0, drawn.end - drawn.start) }, (_, k) => drawn.start + k));
	let selectedHour = $derived(shown[Math.min(selected, count - 1)]);

	const weekdayNames = $derived($json('weekday_names') as string[] | undefined);
	const monthNames = $derived($json('month_names') as string[] | undefined);

	// The values of each hour in the primary unit, as the charts draw them
	const finite = (v: number | null | undefined) => (v != null && Number.isFinite(v) ? v : null);
	let temps = $derived(shown.map((h) => (h.temperature ? finite(metricPrimary ? h.temperature.celsius : h.temperature.fahrenheit) : null)));
	let dews = $derived(shown.map((h) => (h.dewPoint ? finite(metricPrimary ? h.dewPoint.celsius : h.dewPoint.fahrenheit) : null)));
	let precips = $derived(shown.map((h) => (h.precipitation ? finite(metricPrimary ? h.precipitation.mm : h.precipitation.inches) : null)));
	let chances = $derived(shown.map((h) => finite(h.precipProbability)));
	let winds = $derived(shown.map((h) => (h.windSpeed ? finite(metricPrimary ? h.windSpeed.metersPerSecond : h.windSpeed.mph) : null)));
	let gusts = $derived(shown.map((h) => (h.windGust ? finite(metricPrimary ? h.windGust.metersPerSecond : h.windGust.mph) : null)));
	let pressures = $derived(shown.map((h) => (h.pressure ? finite(metricPrimary ? h.pressure.hPa : h.pressure.inHg) : null)));
	let humidities = $derived(shown.map((h) => finite(h.humidity)));

	const part = (vs: (number | null)[]) => vs.slice(inView.start, inView.end);
	let tempDomain = $derived(chartDomain([...part(temps), ...part(dews)], false, metricPrimary ? 5 : 9));
	let precipDomain = $derived(chartDomain(part(precips), true, metricPrimary ? 1 : 0.04));
	let windDomain = $derived(chartDomain([...part(winds), ...part(gusts)], true, metricPrimary ? 5 : 10));
	let pressureDomain = $derived(chartDomain(part(pressures), false, metricPrimary ? 4 : 0.12));
	/** The fixed 0-100 % scale of the chance of rain and the humidity. */
	const PERCENT_DOMAIN: ChartDomain = { min: 0, max: 100, ticks: [0, 50, 100] };

	/** The middle of hour `i`. */
	const cx = (i: number) => (i + 0.5) * slot;
	function y(v: number, d: ChartDomain, p: Panel): number {
		return d.max > d.min ? p.plotBottom - ((v - d.min) / (d.max - d.min)) * (p.plotBottom - p.plotTop) : p.plotBottom;
	}

	/** The curve through the drawn values; a missing value leaves a gap. */
	function curve(values: (number | null)[], d: ChartDomain | null, p: Panel): string {
		if (!d) return '';
		const pts = drawIndexes.flatMap((i) => (values[i] != null ? [{ x: cx(i), y: y(values[i]!, d, p), i }] : []));
		return runsOf(pts).filter((r) => r.length > 1).map((r) => monotonePath(r)).join(' ');
	}

	// The lines that get a dot at the selected hour
	let selectedDots = $derived.by(() => {
		const dots: { y: number }[] = [];
		const add = (values: (number | null)[], d: ChartDomain | null, p: Panel) => {
			const v = values[selected];
			if (d && v != null) dots.push({ y: y(v, d, p) });
		};
		add(dews, tempDomain, panels.temp);
		add(temps, tempDomain, panels.temp);
		add(chances, PERCENT_DOMAIN, panels.precip);
		add(gusts, windDomain, panels.wind);
		add(winds, windDomain, panels.wind);
		add(pressures, pressureDomain, panels.pressure);
		add(humidities, PERCENT_DOMAIN, panels.humidity);
		return dots;
	});

	const localHour = (t: number) => new Date(t + utcOffsetSeconds * 1000).getUTCHours();
	// Icons and wind arrows every 3 local hours (00, 03, 06…); hour labels every 3 h at 20 px an hour or more, else every 6 h
	let everyThird = $derived(drawIndexes.filter((i) => localHour(shown[i].time) % 3 === 0));
	let labelStep = $derived(slot >= 20 ? 3 : 6);

	// Each local day, from the middle of its first hour to the middle of the next day's first hour
	let days = $derived.by(() => {
		const spans = chartDaySpans(shown.map((h) => h.time), utcOffsetSeconds);
		return spans.map((s, k) => ({
			...s,
			k,
			left: k === 0 ? 0 : cx(s.startIndex),
			right: spans[k + 1] ? cx(spans[k + 1].startIndex) : cx(s.endIndex - 1) + slot / 2,
		}));
	});
	let shownDays = $derived(days.filter((d) => d.right >= scrollLeft && d.left <= scrollLeft + viewport));

	// The runs of night hours that reach the drawn hours
	let nights = $derived(chartNightRuns(shown.map((h) => h.isNight)).filter((r) => r.end > drawn.start && r.start < drawn.end));

	const shortMonths = new Map<string, Intl.DateTimeFormat>();
	/** "EEE d MMM" at the place, for example "Sat 7 Nov". */
	function dayLabel(time: number): string {
		const d = new Date(time + utcOffsetSeconds * 1000);
		let month: string;
		if (Intl.DateTimeFormat.supportedLocalesOf([localeTag]).length > 0 || !monthNames) {
			let f = shortMonths.get(localeTag);
			if (!f) {
				f = new Intl.DateTimeFormat(localeTag, { month: 'short', timeZone: 'UTC' });
				shortMonths.set(localeTag, f);
			}
			month = f.format(d);
		} else {
			const name = monthNames[d.getUTCMonth()];
			month = localeTag === 'ne' ? name : name.substring(0, 3);
		}
		return loc(`${formatDayName(time, localeTag, utcOffsetSeconds, true, weekdayNames)} ${d.getUTCDate()} ${month}`);
	}

	/** "EEE d MMM, HH:mm" at the place. */
	function hourLabel(h: ChartHour): string {
		return `${dayLabel(h.time)}, ${loc(formatHourAtLocation(h.time, utcOffsetSeconds))}`;
	}

	/**
	 * The labels of a scale in the axis column: the primary unit, with the other unit (`toOther`, with
	 * `otherDecimals`) small under it. A label that would cover the one above it is left out.
	 */
	function axisLabels(d: ChartDomain | null, p: Panel, toOther: ((v: number) => number) | null, otherDecimals: number, suffix = '') {
		if (!d) return [];
		const step = d.ticks.length > 1 ? d.ticks[1] - d.ticks[0] : 1;
		let decimals = 3;
		for (let k = 0; k <= 3; k++) {
			if (Math.abs(step * 10 ** k - Math.round(step * 10 ** k)) < 1e-6) {
				decimals = k;
				break;
			}
		}
		const height = toOther ? 20 : 10;
		const labels: { top: number; primary: string; secondary: string | null }[] = [];
		let lastTop = Infinity;
		for (const v of d.ticks) {
			const top = Math.min(Math.max(y(v, d, p) - 6, p.plotTop - 6), p.plotBottom + 4 - height);
			if (top + height > lastTop) continue;
			labels.push({ top, primary: loc(v.toFixed(decimals) + suffix), secondary: toOther ? loc(toOther(v).toFixed(otherDecimals)) : null });
			lastTop = top;
		}
		return labels;
	}

	let axes = $derived([
		...axisLabels(tempDomain, panels.temp, metricPrimary ? (c) => (c * 9) / 5 + 32 : (f) => ((f - 32) * 5) / 9, 0),
		...axisLabels(precipDomain, panels.precip, metricPrimary ? (v) => v * 0.03937 : (v) => v / 0.03937, metricPrimary ? 2 : 1),
		...axisLabels(windDomain, panels.wind, metricPrimary ? (v) => v * 2.23694 : (v) => v / 2.23694, metricPrimary ? 0 : 1),
		...axisLabels(pressureDomain, panels.pressure, metricPrimary ? (v) => v * 0.02953 : (v) => v / 0.02953, metricPrimary ? 2 : 0),
		...axisLabels(PERCENT_DOMAIN, panels.humidity, null, 0, '%'),
	]);
	let grid = $derived([
		...(tempDomain?.ticks.map((v) => y(v, tempDomain!, panels.temp)) ?? []),
		...(precipDomain?.ticks.map((v) => y(v, precipDomain!, panels.precip)) ?? []),
		...(windDomain?.ticks.map((v) => y(v, windDomain!, panels.wind)) ?? []),
		...(pressureDomain?.ticks.map((v) => y(v, pressureDomain!, panels.pressure)) ?? []),
		...PERCENT_DOMAIN.ticks.map((v) => y(v, PERCENT_DOMAIN, panels.humidity)),
	]);

	// Panel titles: the name and both units (primary first), and the name of the second line in its colour
	const units = (metric: string, imperial: string) => (metricPrimary ? `${metric} · ${imperial}` : `${imperial} · ${metric}`);
	let titles = $derived<{ id: PanelId; title: string; second?: { text: string; color: string } }[]>([
		{ id: 'temp', title: `${$_('detail_temperature')}  ${units('°C', '°F')}`, second: { text: $_('detail_dewpoint'), color: ChartStyle.dewPoint } },
		{ id: 'precip', title: `${$_('detail_precipitation')}  ${units('mm', 'in')}`, second: { text: $_('precip_chance', { values: { value: '%' } }), color: ChartStyle.chance } },
		{ id: 'wind', title: `${$_('detail_wind')}  ${units('m/s', 'mph')}`, second: { text: $_('detail_wind_gust'), color: ChartStyle.gust } },
		{ id: 'pressure', title: `${$_('detail_pressure')}  ${units('hPa', 'inHg')}` },
		{ id: 'humidity', title: `${$_('detail_humidity')}  %` },
	]);

	let summary = $derived($_('chart_view_summary', { values: { hours: loc(String(count)) } }));

	/** The UV index as shown (rounded), and the colours of its level, from the shown value. */
	function uvBadge(uv: number) {
		const shownValue = Math.round(uv);
		return { text: loc(String(shownValue)), background: uvColorFor(shownValue), color: uvTier(shownValue) === 'moderate' ? '#1A1A1A' : 'white' };
	}

	// Gestures, as on Android. A one-finger drag that starts sideways scrolls the hours, else the page
	// (the browser does this). A tap selects the hour under it after the double-tap wait; a double tap resets
	// the zoom to 28 around the tapped hour. A long press, then a drag, moves the selection without scrolling.
	// Two fingers zoom, keeping the hour between them in place; Ctrl and the mouse wheel zoom too.
	const SLOP = 8;
	const LONG_PRESS_MS = 500;
	const DOUBLE_TAP_MS = 300;
	let press: { id: number; x: number; y: number; scroll: number; mouse: boolean; moved: boolean } | null = null;
	let scrubbing = false;
	let longPressTimer: ReturnType<typeof setTimeout> | undefined;
	let pendingTap: { timer: ReturnType<typeof setTimeout>; x: number } | null = null;
	let pinch: { distance: number } | null = null;
	// The scroll that a zoom asked for, until the browser has it
	let zoomScroll: number | null = null;

	function viewX(clientX: number): number {
		return scroller ? clientX - scroller.getBoundingClientRect().left : 0;
	}

	function selectAt(clientX: number, feedback: boolean) {
		const i = chartIndexForX(viewX(clientX) + (scroller?.scrollLeft ?? 0), slot, count);
		if (i < 0 || i === selected) return;
		selected = i;
		if (feedback) navigator.vibrate?.(5);
	}

	async function applyZoom(z: ChartZoom) {
		const scroll = Math.min(z.scrollPx, Math.max(0, count * z.slotPx - viewport));
		slot = z.slotPx;
		zoomScroll = scroll;
		await tick();
		if (!scroller || zoomScroll !== scroll) return;
		scroller.scrollLeft = scroll;
		scrollLeft = scroller.scrollLeft;
		zoomScroll = null;
	}

	const currentScroll = () => zoomScroll ?? scroller?.scrollLeft ?? 0;

	function endPress() {
		clearTimeout(longPressTimer);
		press = null;
		scrubbing = false;
	}

	function onPointerDown(e: PointerEvent) {
		// A second finger is a pinch, not a tap
		if (press) return endPress();
		if (pinch || (e.pointerType === 'mouse' && e.button !== 0)) return;
		press = { id: e.pointerId, x: e.clientX, y: e.clientY, scroll: scroller?.scrollLeft ?? 0, mouse: e.pointerType === 'mouse', moved: false };
		longPressTimer = setTimeout(() => {
			if (!press || press.moved) return;
			scrubbing = true;
			scroller?.setPointerCapture(press.id);
			navigator.vibrate?.(10);
			selectAt(press.x, false);
		}, LONG_PRESS_MS);
	}

	function onPointerMove(e: PointerEvent) {
		if (!press || e.pointerId !== press.id) return;
		if (scrubbing) return selectAt(e.clientX, true);
		const dx = e.clientX - press.x;
		if (!press.moved && Math.hypot(dx, e.clientY - press.y) > SLOP) {
			press.moved = true;
			clearTimeout(longPressTimer);
			if (press.mouse) scroller?.setPointerCapture(e.pointerId);
		}
		if (press.moved && press.mouse && scroller) scroller.scrollLeft = press.scroll - dx;
	}

	function onPointerUp(e: PointerEvent) {
		if (!press || e.pointerId !== press.id) return;
		const tap = !scrubbing && !press.moved;
		endPress();
		if (!tap) return;
		if (pendingTap && Math.abs(e.clientX - pendingTap.x) < 24) {
			clearTimeout(pendingTap.timer);
			pendingTap = null;
			applyZoom(chartZoom(slot, CHART_SLOT_DEFAULT / slot, viewX(e.clientX), currentScroll()));
			return;
		}
		if (pendingTap) clearTimeout(pendingTap.timer);
		const x = e.clientX;
		pendingTap = { x, timer: setTimeout(() => { pendingTap = null; selectAt(x, false); }, DOUBLE_TAP_MS) };
	}

	const distance = (t: TouchList) => Math.hypot(t[0].clientX - t[1].clientX, t[0].clientY - t[1].clientY);

	// Touch and wheel listeners that can stop the browser's own scroll and zoom (not passive)
	$effect(() => {
		const el = scroller;
		if (!el) return;
		const touchStart = (e: TouchEvent) => {
			if (e.touches.length !== 2) return;
			endPress();
			pinch = { distance: distance(e.touches) };
		};
		const touchMove = (e: TouchEvent) => {
			if (pinch && e.touches.length >= 2) {
				e.preventDefault();
				const d = distance(e.touches);
				const anchor = viewX((e.touches[0].clientX + e.touches[1].clientX) / 2);
				applyZoom(chartZoom(slot, d / pinch.distance, anchor, currentScroll()));
				pinch.distance = d;
			} else if (scrubbing && e.cancelable) {
				e.preventDefault();
			}
		};
		const touchEnd = (e: TouchEvent) => {
			if (e.touches.length < 2) pinch = null;
		};
		const wheel = (e: WheelEvent) => {
			if (!e.ctrlKey) return;
			e.preventDefault();
			applyZoom(chartZoom(slot, Math.exp(-e.deltaY * 0.01), viewX(e.clientX), currentScroll()));
		};
		el.addEventListener('touchstart', touchStart, { passive: true });
		el.addEventListener('touchmove', touchMove, { passive: false });
		el.addEventListener('touchend', touchEnd);
		el.addEventListener('touchcancel', touchEnd);
		el.addEventListener('wheel', wheel, { passive: false });
		return () => {
			el.removeEventListener('touchstart', touchStart);
			el.removeEventListener('touchmove', touchMove);
			el.removeEventListener('touchend', touchEnd);
			el.removeEventListener('touchcancel', touchEnd);
			el.removeEventListener('wheel', wheel);
			if (pendingTap) clearTimeout(pendingTap.timer);
			clearTimeout(longPressTimer);
		};
	});

	/** Scrolls so that hour `i` is in view. */
	function reveal(i: number) {
		if (!scroller) return;
		const left = i * slot;
		if (left < scroller.scrollLeft) scroller.scrollLeft = left;
		else if (left + slot > scroller.scrollLeft + viewport) scroller.scrollLeft = left + slot - viewport;
	}

	// Keys (web only): Left and Right move the selection, Home and End go to the first and last hour, + and − zoom
	function onKeyDown(e: KeyboardEvent) {
		if (count === 0) return;
		let next = selected;
		if (e.key === 'ArrowLeft') next = Math.max(0, selected - 1);
		else if (e.key === 'ArrowRight') next = Math.min(count - 1, selected + 1);
		else if (e.key === 'Home') next = 0;
		else if (e.key === 'End') next = count - 1;
		else if (e.key === '+' || e.key === '=' || e.key === '-' || e.key === '_') {
			e.preventDefault();
			const anchor = Math.min(viewport, Math.max(0, cx(selected) - scrollLeft));
			applyZoom(chartZoom(slot, e.key === '+' || e.key === '=' ? 1.25 : 0.8, anchor, currentScroll()));
			return;
		} else return;
		e.preventDefault();
		selected = next;
		reveal(next);
	}
</script>

<SettingsPage title={$_('chart_view_title')} subtitle={placeName} {onBack}>
	<p class="hint">{$_('chart_view_hint')}</p>

	<!-- The selected hour's values; a screen reader reads them again when the selection moves, a tap swaps the units -->
	<button type="button" class="selection" onclick={onToggleUnits} aria-describedby="chart-units-hint">
		<span class="selection-row" aria-live="polite">
			{#if selectedHour}
				{@const h = selectedHour}
				<span class="sel-time">{hourLabel(h)}</span>
				<WeatherIcon code={h.weatherCode} night={h.isNight} size={28} />
				{#if h.temperature}{@render value(null, h.temperature.displayDual(metricPrimary))}{/if}
				{#if h.dewPoint}{@render value($_('detail_dewpoint'), h.dewPoint.displayDual(metricPrimary))}{/if}
				{#if h.precipitation}
					{@render value($_('detail_precipitation'), h.precipitation.displayDual(metricPrimary), h.precipProbability != null ? $_('precip_chance', { values: { value: loc(`${h.precipProbability}%`) } }) : undefined)}
				{/if}
				{#if h.windSpeed}
					{@render value($_('detail_wind'), h.windSpeed.displayDual(metricPrimary), h.windDeg != null && showWindArrow(h.windSpeed) ? getCardinalDirection(h.windDeg, $json('cardinal_directions')) : undefined)}
				{/if}
				{#if h.windGust}{@render value($_('detail_wind_gust'), h.windGust.displayDual(metricPrimary))}{/if}
				{#if h.pressure}{@render value($_('detail_pressure'), h.pressure.displayDual(metricPrimary))}{/if}
				{#if h.humidity != null}{@render value($_('detail_humidity'), null, loc(`${h.humidity}%`))}{/if}
				{#if h.uvIndex != null && Number.isFinite(h.uvIndex)}
					{@const badge = uvBadge(h.uvIndex)}
					<span class="sel-value"><span class="sel-label">{$_('detail_uv_index')}</span> <span class="uv-pill" style:background={badge.background} style:color={badge.color}>{badge.text}</span></span>
				{/if}
			{/if}
		</span>
	</button>
	<span id="chart-units-hint" hidden>{$_('cd_toggle_units')}</span>

	<!-- Time runs left to right in every language, with the axis column on the left -->
	<div
		class="chart"
		dir="ltr"
		role="slider"
		tabindex="0"
		aria-label={summary}
		aria-valuemin={0}
		aria-valuemax={Math.max(0, count - 1)}
		aria-valuenow={selected}
		aria-valuetext={selectedHour ? hourLabel(selectedHour) : undefined}
		onkeydown={onKeyDown}
		style:height="{totalHeight}px"
	>
		<!-- The axis column: the values of each scale in the primary unit, with the other unit small under them -->
		<div class="axis" style:width="{AXIS_WIDTH}px">
			{#each axes as l, k (k)}
				<div class="tick" style:top="{l.top}px">
					<span class="tick-primary">{l.primary}</span>
					{#if l.secondary}<span class="tick-secondary">{l.secondary}</span>{/if}
				</div>
			{/each}
		</div>

		<div
			class="scroller"
			bind:this={scroller}
			bind:clientWidth={viewport}
			onscroll={() => { scrollLeft = scroller?.scrollLeft ?? 0; }}
			onpointerdown={onPointerDown}
			onpointermove={onPointerMove}
			onpointerup={onPointerUp}
			onpointercancel={endPress}
			role="presentation"
		>
			<div class="content" style:width="{count * slot}px" style:height="{totalHeight}px">
				<svg width={count * slot} height={totalHeight} aria-hidden="true">
					<defs>
						{#each PANEL_HEIGHTS as [id] (id)}
							<clipPath id="chart-clip-{id}"><rect x="0" y={panels[id].top} width={count * slot} height={panels[id].height} /></clipPath>
						{/each}
					</defs>
					<!-- A vertical line at `x` through the plot area of each panel, with a break between panels.
					     Declared in the svg, so that its lines are svg elements -->
					{#snippet verticalLine(x: number, stroke: string, width: number)}
						{#each LINE_SEGMENTS as p (p.top)}
							<line x1={x} x2={x} y1={p.plotTop} y2={p.plotBottom} {stroke} stroke-width={width} />
						{/each}
					{/snippet}
					<!-- Days: every other one a little lighter, and a stronger line at each midnight in every panel -->
					{#each shownDays as d (d.startIndex)}
						{#if d.k % 2 === 1}
							<rect x={d.left} y="0" width={d.right - d.left} height={totalHeight} fill="white" fill-opacity="0.03" />
						{/if}
						{#if d.k > 0}
							{@render verticalLine(d.left, ChartStyle.midnight, ChartStyle.midnightWidth)}
						{/if}
					{/each}
					<!-- Night hours, faintly darker -->
					{#each nights as run (run.start)}
						<rect x={run.start * slot} y="0" width={(run.end - run.start) * slot} height={chartsBottom} fill={ChartStyle.night} />
					{/each}
					{#each grid as gy, k (k)}
						<line x1="0" x2={count * slot} y1={gy} y2={gy} stroke="white" stroke-opacity="0.07" stroke-width="1" />
					{/each}

					<!-- The UV index under the icons, in the colours of its level -->
					{#each everyThird as i (i)}
						{@const uv = shown[i].uvIndex}
						<!-- From 1 as shown (rounded): 0.6 shows 1 -->
						{#if uv != null && Number.isFinite(uv) && Math.round(uv) >= 1}
							{@const badge = uvBadge(uv)}
							<rect x={cx(i) - 10} y={panels.icons.top + ICON_SIZE + 2} width="20" height="13" rx="6.5" fill={badge.background} />
							<text x={cx(i)} y={panels.icons.top + ICON_SIZE + 12} text-anchor="middle" font-size="9" font-weight="bold" fill={badge.color}>{badge.text}</text>
						{/if}
					{/each}

					<!-- Temperature with the dew point on the same scale, and a dashed line at freezing -->
					{#if tempDomain}
						{@const freezing = metricPrimary ? 0 : 32}
						<g clip-path="url(#chart-clip-temp)">
							{#if freezing > tempDomain.min && freezing < tempDomain.max}
								<line x1="0" x2={count * slot} y1={y(freezing, tempDomain, panels.temp)} y2={y(freezing, tempDomain, panels.temp)} stroke={ChartStyle.freezing} stroke-width={ChartStyle.freezingWidth} stroke-dasharray={ChartStyle.freezingDash} />
							{/if}
							<path d={curve(dews, tempDomain, panels.temp)} fill="none" stroke={ChartStyle.dewPoint} stroke-width={ChartStyle.thinLineWidth} />
							<path d={curve(temps, tempDomain, panels.temp)} fill="none" stroke={ChartStyle.temperature} stroke-width={ChartStyle.lineWidth} />
						</g>
					{/if}

					<!-- Precipitation as bars, the chance of rain as a thin line on 0-100 % -->
					<g clip-path="url(#chart-clip-precip)">
						{#if precipDomain}
							{#each drawIndexes as i (i)}
								{#if precips[i] != null && precips[i]! > 0}
									{@const top = y(precips[i]!, precipDomain, panels.precip)}
									{@const barWidth = Math.max(2, slot * 0.6)}
									<rect x={cx(i) - barWidth / 2} y={top} width={barWidth} height={panels.precip.plotBottom - top} rx="1.5" fill={ChartStyle.precipitation} />
								{/if}
							{/each}
						{/if}
						<path d={curve(chances, PERCENT_DOMAIN, panels.precip)} fill="none" stroke={ChartStyle.chance} stroke-width={ChartStyle.chanceWidth} />
					</g>

					<!-- Wind and gusts on one scale, with the direction every 3 hours -->
					<g clip-path="url(#chart-clip-wind)">
						{#if windDomain}
							<path d={curve(gusts, windDomain, panels.wind)} fill="none" stroke={ChartStyle.gust} stroke-width={ChartStyle.thinLineWidth} stroke-dasharray={ChartStyle.gustDash} />
							<path d={curve(winds, windDomain, panels.wind)} fill="none" stroke={ChartStyle.wind} stroke-width={ChartStyle.lineWidth} />
						{/if}
					</g>
					{#each everyThird as i (i)}
						{@const h = shown[i]}
						{#if h.windSpeed && h.windDeg != null && showWindArrow(h.windSpeed)}
							<!-- Points to where the wind comes from, as the compass of the wind cards -->
							<path d="M 0 -5 L 3.5 4 L 0 2 L -3.5 4 Z" fill="white" fill-opacity="0.75"
								transform="translate({cx(i)} {panels.wind.plotTop - 4 - ARROW_ROW / 2}) rotate({h.windDeg})" />
						{/if}
					{/each}

					{#if pressureDomain}
						<path clip-path="url(#chart-clip-pressure)" d={curve(pressures, pressureDomain, panels.pressure)} fill="none" stroke={ChartStyle.pressure} stroke-width={ChartStyle.lineWidth} />
					{/if}

					<path clip-path="url(#chart-clip-humidity)" d={curve(humidities, PERCENT_DOMAIN, panels.humidity)} fill="none" stroke={ChartStyle.humidity} stroke-width={ChartStyle.lineWidth} />

					<!-- Time axis: an hour label every 3 or 6 hours, and the day at each midnight (the day at the start stays in view) -->
					{#each drawIndexes as i (i)}
						{#if localHour(shown[i].time) % labelStep === 0}
							<line x1={cx(i)} x2={cx(i)} y1={panels.time.top} y2={panels.time.top + 3} stroke="white" stroke-opacity="0.3" stroke-width="1" />
							<text x={cx(i)} y={panels.time.top + 13} text-anchor="middle" font-size="10" fill="white" fill-opacity="0.6">{loc(formatHourAtLocation(shown[i].time, utcOffsetSeconds))}</text>
						{/if}
					{/each}
					{#each shownDays as d (d.startIndex)}
						{@const label = dayLabel(d.date)}
						{@const left = Math.max(d.left, scrollLeft) + 4}
						<!-- The width is an estimate (about 6.5 px a letter at 11 px bold) -->
						{#if left + label.length * 6.5 + 4 <= d.right}
							<text x={left} y={panels.time.top + 30} font-size="11" font-weight="bold" fill="white">{label}</text>
						{/if}
					{/each}

					<!-- The selected hour in every panel, with a dot on each line -->
					{#if count > 0 && selected >= drawn.start && selected < drawn.end}
						{@render verticalLine(cx(selected), ChartStyle.selection, ChartStyle.selectionWidth)}
						{#each selectedDots as dot, k (k)}
							<circle cx={cx(selected)} cy={dot.y} r={ChartStyle.selectionDotRadius} fill={ChartStyle.selectionDot} />
						{/each}
					{/if}
				</svg>

				<!-- Weather icons every 3 local hours (night icons at night) -->
				{#each everyThird as i (i)}
					{@const h = shown[i]}
					<div class="icon" style:left="{cx(i) - ICON_SIZE / 2}px" style:top="{panels.icons.top}px" style:width="{ICON_SIZE}px" style:height="{ICON_SIZE}px">
						<WeatherIcon code={h.weatherCode} night={h.isNight} size={ICON_SIZE} decorative />
					</div>
				{/each}
			</div>
		</div>

		<!-- Titles at the start of each panel, and the chance of rain scale at the end of its panel; they do not scroll -->
		<div class="overlay" aria-hidden="true" style:left="{AXIS_WIDTH}px">
			{#each titles as t (t.id)}
				<div class="panel-title" style:top="{panels[t.id].top + 1}px" dir="auto">
					<span class="title-main">{t.title}</span>{#if t.second}<span style:color={t.second.color}>{'   ● ' + t.second.text}</span>{/if}
				</div>
			{/each}
			{#each [50, 100] as v (v)}
				<span class="chance-label" style:top="{y(v, PERCENT_DOMAIN, panels.precip)}px">{loc(`${v}%`)}</span>
			{/each}
		</div>
	</div>
</SettingsPage>

<!-- One value of the selection strip: its name, then the value in both units (and more, such as the chance of rain) -->
{#snippet value(label: string | null, pair: [string, string] | null, extra?: string)}
	<span class="sel-value">
		{#if label}<span class="sel-label">{label}</span>{' '}{/if}
		{#if pair}<bdi dir="ltr"><span class="sel-primary">{loc(pair[0])}</span> <span class="sel-secondary">{loc(pair[1])}</span></bdi>{/if}
		{#if extra}<span class="sel-extra">{' ' + extra}</span>{/if}
	</span>
{/snippet}

<style>
	.hint {
		margin: -8px 4px 8px;
		font-size: 0.75rem;
		color: rgba(255, 255, 255, 0.55);
	}

	.selection {
		display: block;
		width: 100%;
		min-height: 52px;
		margin-bottom: 8px;
		padding: 6px 10px;
		border: none;
		border-radius: 12px;
		background: rgba(255, 255, 255, 0.06);
		color: white;
		font: inherit;
		text-align: start;
		cursor: pointer;
	}

	.selection-row {
		display: flex;
		flex-wrap: wrap;
		align-items: center;
		gap: 2px 12px;
	}

	.sel-time {
		font-size: 0.8125rem;
		font-weight: 700;
		font-feature-settings: var(--font-features);
	}

	.sel-value {
		white-space: nowrap;
	}

	.sel-label {
		font-size: 0.6875rem;
		color: rgba(255, 255, 255, 0.55);
	}

	.sel-primary {
		font-family: var(--font-display);
		font-size: 0.8125rem;
		font-weight: 700;
		font-feature-settings: var(--font-features);
	}

	.sel-secondary {
		font-family: var(--font-display);
		font-size: 0.625rem;
		color: rgba(255, 255, 255, 0.6);
		font-feature-settings: var(--font-features);
	}

	.sel-extra {
		font-size: 0.75rem;
		color: rgba(255, 255, 255, 0.8);
		font-feature-settings: var(--font-features);
	}

	.uv-pill {
		padding: 0 6px;
		border-radius: 6px;
		font-family: var(--font-display);
		font-size: 0.75rem;
		font-weight: 700;
		font-feature-settings: var(--font-features);
	}

	.chart {
		position: relative;
		display: flex;
		margin: 0 -8px;
		border-radius: 12px;
		outline: none;
	}

	.chart:focus-visible {
		outline: 2px solid rgba(255, 255, 255, 0.8);
		outline-offset: 2px;
	}

	.axis {
		position: relative;
		flex: none;
		pointer-events: none;
	}

	.tick {
		position: absolute;
		right: 4px;
		display: flex;
		flex-direction: column;
		align-items: flex-end;
		line-height: 10px;
		font-family: var(--font-display);
		font-feature-settings: var(--font-features);
	}

	.tick-primary {
		font-size: 10px;
		color: rgba(255, 255, 255, 0.8);
	}

	.tick-secondary {
		font-size: 8px;
		color: rgba(255, 255, 255, 0.5);
	}

	.scroller {
		flex: 1;
		min-width: 0;
		overflow-x: auto;
		overflow-y: hidden;
		/* The browser keeps one-finger scrolling both ways; pinch and double tap come to the chart */
		touch-action: pan-x pan-y;
		user-select: none;
		-webkit-user-select: none;
		scrollbar-width: none;
	}

	.scroller::-webkit-scrollbar {
		display: none;
	}

	.content {
		position: relative;
	}

	.content svg {
		position: absolute;
		inset: 0;
		display: block;
	}

	.icon {
		position: absolute;
		display: flex;
		align-items: center;
		justify-content: center;
		pointer-events: none;
	}

	.overlay {
		position: absolute;
		inset: 0;
		overflow: hidden;
		pointer-events: none;
	}

	.panel-title {
		position: absolute;
		left: 6px;
		white-space: pre;
		font-size: 10px;
		line-height: 14px;
	}

	.title-main {
		font-weight: 600;
		color: rgba(255, 255, 255, 0.75);
	}

	.chance-label {
		position: absolute;
		right: 2px;
		transform: translateY(-50%);
		font-family: var(--font-display);
		font-size: 8px;
		color: rgba(255, 255, 255, 0.6);
		font-feature-settings: var(--font-features);
	}
</style>
