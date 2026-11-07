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
	import { prefersReducedMotion } from '#lib/utils/motion.js';
	import {
		CHART_SLOT_DEFAULT, chartCenterIndex, chartDaySpans, chartDomain, chartHours, chartIndexForX, chartNightRuns, chartScrollForIndex, chartScrollRange,
		chartValueAt, chartVisibleRange, chartZoom,
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

	// The panels from top to bottom, with their height; the value panels have a title line at the top.
	// A time axis is at the top (`timeTop`, mirrored) and at the bottom (`time`).
	type PanelId = 'timeTop' | 'icons' | 'temp' | 'precip' | 'wind' | 'pressure' | 'humidity' | 'time';
	const PANEL_HEIGHTS: [PanelId, number][] = [['timeTop', 36], ['icons', 46], ['temp', 132], ['precip', 88], ['wind', 104], ['pressure', 80], ['humidity', 80], ['time', 36]];
	/** A panel: its top and height, and the part where the values go (`plotTop` to `plotBottom`). */
	interface Panel { top: number; height: number; plotTop: number; plotBottom: number }
	const { panels, totalHeight } = (() => {
		const result = {} as Record<PanelId, Panel>;
		let top = 0;
		for (const [id, height] of PANEL_HEIGHTS) {
			const titled = id !== 'timeTop' && id !== 'icons' && id !== 'time';
			const plotTop = top + (titled ? TITLE_HEIGHT : 0) + (id === 'wind' ? ARROW_ROW : 0) + 4;
			result[id] = { top, height, plotTop, plotBottom: top + height - 4 };
			top += height + PANEL_GAP;
		}
		return { panels: result, totalHeight: top - PANEL_GAP };
	})();
	// Night shading runs from the top of the icons to the bottom of the humidity panel
	const chartsTop = panels.icons.top;
	const chartsBottom = panels.humidity.top + panels.humidity.height;
	const topAxisBottom = panels.timeTop.top + panels.timeTop.height;
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

	// The hours have half the view of space before and after them (less half an hour), so that the browser's scroll
	// range runs from the middle of the first hour to the middle of the last hour under the centre line (chartScrollRange):
	// a fling ends with the end hour exactly under it. `centerX` is the position under the centre line, in px from the
	// start of the first hour (the scroll of chartCenterIndex), and the view starts at `hoursLeft`.
	let edgeSpace = $derived((viewport - slot) / 2);
	let centerX = $derived(scrollLeft + slot / 2);
	let hoursLeft = $derived(scrollLeft - edgeSpace);
	// Lines are drawn for the hours in view and 2 more on each side
	let drawn = $derived(chartVisibleRange(hoursLeft, viewport, slot, count));
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

	// Each scale fits all the chart hours, so that it stays the same while the hours scroll or zoom
	let tempDomain = $derived(chartDomain([...temps, ...dews], false, metricPrimary ? 5 : 9));
	let precipDomain = $derived(chartDomain(precips, true, metricPrimary ? 1 : 0.04));
	let windDomain = $derived(chartDomain([...winds, ...gusts], true, metricPrimary ? 5 : 10));
	let pressureDomain = $derived(chartDomain(pressures, false, metricPrimary ? 4 : 0.12));
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

	// A dot on each line at the centre line
	let selectedDots = $derived.by(() => {
		const dots: { y: number }[] = [];
		const add = (values: (number | null)[], d: ChartDomain | null, p: Panel) => {
			const v = chartValueAt(values, centerX, slot);
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
	let shownDays = $derived(days.filter((d) => d.right >= hoursLeft && d.left <= hoursLeft + viewport));

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

	// Gestures, as on Android. The selected hour is the hour under the fixed centre line. One finger scrolls the hours
	// (the browser's own scrolling, with momentum), and the page up and down; a mouse drags the hours. The wheel with
	// Shift, or a sideways wheel or trackpad, scrolls the hours. A tap or click scrolls the hour under it to the centre,
	// after the double-tap wait. Pinch, Ctrl and the wheel, and a double tap (back to 28) zoom around the centre line,
	// so the selected hour stays selected.
	const SLOP = 8;
	const DOUBLE_TAP_MS = 300;
	let press: { id: number; x: number; y: number; scroll: number; mouse: boolean; moved: boolean } | null = null;
	let pendingTap: { timer: ReturnType<typeof setTimeout>; x: number } | null = null;
	let pinchDistance: number | null = null;
	// The centre position that a zoom asked for, until the browser has it
	let zoomCenter: number | null = null;

	function viewX(clientX: number): number {
		return scroller ? clientX - scroller.getBoundingClientRect().left : 0;
	}

	/** On each scroll: a new hour under the centre line becomes the selection, with a short vibrate. */
	function onScroll() {
		scrollLeft = scroller?.scrollLeft ?? 0;
		const i = chartCenterIndex(scrollLeft + slot / 2, slot, count);
		if (i < 0 || i === selected) return;
		selected = i;
		navigator.vibrate?.(5);
	}

	/** Scrolls the middle of hour `i` to the centre line. The browser's scroll is the centre position less half an hour. */
	function centre(i: number, smooth: boolean) {
		scroller?.scrollTo({ left: chartScrollForIndex(i, slot) - slot / 2, behavior: smooth && !prefersReducedMotion() ? 'smooth' : 'auto' });
	}
	// When the view opens, the browser's scroll is 0: the middle of the current hour (the first) is under the line,
	// and the selection is already that hour, so there is no vibrate

	/** Applies a zoom; `z.scrollPx` is the new centre position, kept inside the scroll range. */
	async function applyZoom(z: ChartZoom) {
		const range = chartScrollRange(z.slotPx, count);
		const center = Math.min(range.max, Math.max(range.min, z.scrollPx));
		slot = z.slotPx;
		zoomCenter = center;
		await tick();
		if (!scroller || zoomCenter !== center) return;
		scroller.scrollLeft = center - z.slotPx / 2;
		scrollLeft = scroller.scrollLeft;
		zoomCenter = null;
	}

	const currentCenter = () => zoomCenter ?? (scroller?.scrollLeft ?? 0) + slot / 2;

	/** Zooms by `factor` around the centre line: the position under it stays there. */
	function zoomAtCentre(factor: number) {
		applyZoom(chartZoom(slot, factor, 0, currentCenter()));
	}

	function endPress() {
		press = null;
	}

	function onPointerDown(e: PointerEvent) {
		// A second finger is a pinch, not a tap
		if (press) return endPress();
		if (pinchDistance != null || (e.pointerType === 'mouse' && e.button !== 0)) return;
		press = { id: e.pointerId, x: e.clientX, y: e.clientY, scroll: scroller?.scrollLeft ?? 0, mouse: e.pointerType === 'mouse', moved: false };
	}

	function onPointerMove(e: PointerEvent) {
		if (!press || e.pointerId !== press.id) return;
		const dx = e.clientX - press.x;
		if (!press.moved && Math.hypot(dx, e.clientY - press.y) > SLOP) {
			press.moved = true;
			if (press.mouse) scroller?.setPointerCapture(e.pointerId);
		}
		// A mouse drags the hours, as in the app's other rows
		if (press.moved && press.mouse && scroller) scroller.scrollLeft = press.scroll - dx;
	}

	function onPointerUp(e: PointerEvent) {
		if (!press || e.pointerId !== press.id) return;
		const tap = !press.moved;
		endPress();
		if (!tap) return;
		if (pendingTap && Math.abs(e.clientX - pendingTap.x) < 24) {
			clearTimeout(pendingTap.timer);
			pendingTap = null;
			zoomAtCentre(CHART_SLOT_DEFAULT / slot);
			return;
		}
		if (pendingTap) clearTimeout(pendingTap.timer);
		const x = e.clientX;
		pendingTap = {
			x,
			timer: setTimeout(() => {
				pendingTap = null;
				const i = chartIndexForX(viewX(x) + (scroller?.scrollLeft ?? 0) - edgeSpace, slot, count);
				if (i >= 0) centre(i, true);
			}, DOUBLE_TAP_MS),
		};
	}

	const distance = (t: TouchList) => Math.hypot(t[0].clientX - t[1].clientX, t[0].clientY - t[1].clientY);

	// Touch and wheel listeners that can stop the browser's own scroll and zoom (not passive)
	$effect(() => {
		const el = scroller;
		if (!el) return;
		const touchStart = (e: TouchEvent) => {
			if (e.touches.length !== 2) return;
			endPress();
			pinchDistance = distance(e.touches);
		};
		const touchMove = (e: TouchEvent) => {
			if (pinchDistance == null || e.touches.length < 2) return;
			if (e.cancelable) e.preventDefault();
			const d = distance(e.touches);
			zoomAtCentre(d / pinchDistance);
			pinchDistance = d;
		};
		const touchEnd = (e: TouchEvent) => {
			if (e.touches.length < 2) pinchDistance = null;
		};
		const wheel = (e: WheelEvent) => {
			if (e.ctrlKey) {
				e.preventDefault();
				zoomAtCentre(Math.exp(-e.deltaY * 0.01));
			} else if (e.shiftKey && Math.abs(e.deltaY) > Math.abs(e.deltaX)) {
				// Shift and the wheel: the hours scroll (a sideways wheel or trackpad scrolls them without help)
				e.preventDefault();
				el.scrollLeft += e.deltaY;
			}
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
		};
	});

	// Keys (web only): Left and Right scroll one hour (so the selection moves by one), Home and End go to the first
	// and last hour, + and − zoom around the centre line
	function onKeyDown(e: KeyboardEvent) {
		if (count === 0) return;
		// From the hour under the line now: the scroll event that updates the selection may not have come yet
		const current = chartCenterIndex(currentCenter(), slot, count);
		let next: number;
		if (e.key === 'ArrowLeft') next = Math.max(0, current - 1);
		else if (e.key === 'ArrowRight') next = Math.min(count - 1, current + 1);
		else if (e.key === 'Home') next = 0;
		else if (e.key === 'End') next = count - 1;
		else if (e.key === '+' || e.key === '=' || e.key === '-' || e.key === '_') {
			e.preventDefault();
			zoomAtCentre(e.key === '+' || e.key === '=' ? 1.25 : 0.8);
			return;
		} else return;
		e.preventDefault();
		centre(next, false);
	}
</script>

<SettingsPage title={$_('chart_view_title')} subtitle={placeName} {onBack}>

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
			onscroll={onScroll}
			onpointerdown={onPointerDown}
			onpointermove={onPointerMove}
			onpointerup={onPointerUp}
			onpointercancel={endPress}
			role="presentation"
		>
			<div class="content" style:width="{count * slot + 2 * edgeSpace}px" style:height="{totalHeight}px">
			<div class="hours" style:left="{edgeSpace}px" style:width="{count * slot}px" style:height="{totalHeight}px">
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
						<rect x={run.start * slot} y={chartsTop} width={(run.end - run.start) * slot} height={chartsBottom - chartsTop} fill={ChartStyle.night} />
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

					<!-- Time axes: an hour label every 3 or 6 hours, and the day at each midnight (the day at the start stays in view).
					     The bottom axis has its ticks, then the hours, then the days; the top axis is the same, mirrored. -->
					{#each drawIndexes as i (i)}
						{#if localHour(shown[i].time) % labelStep === 0}
							{@const hourText = loc(formatHourAtLocation(shown[i].time, utcOffsetSeconds))}
							<line x1={cx(i)} x2={cx(i)} y1={panels.time.top} y2={panels.time.top + 3} stroke="white" stroke-opacity="0.3" stroke-width="1" />
							<text x={cx(i)} y={panels.time.top + 13} text-anchor="middle" font-size="10" fill="white" fill-opacity="0.6">{hourText}</text>
							<line x1={cx(i)} x2={cx(i)} y1={topAxisBottom - 3} y2={topAxisBottom} stroke="white" stroke-opacity="0.3" stroke-width="1" />
							<text x={cx(i)} y={topAxisBottom - 6} text-anchor="middle" font-size="10" fill="white" fill-opacity="0.6">{hourText}</text>
						{/if}
					{/each}
					{#each shownDays as d (d.startIndex)}
						{@const label = dayLabel(d.date)}
						{@const left = Math.max(d.left, hoursLeft) + 4}
						<!-- The width is an estimate (about 6.5 px a letter at 11 px bold) -->
						{#if left + label.length * 6.5 + 4 <= d.right}
							<text x={left} y={panels.time.top + 30} font-size="11" font-weight="bold" fill="white">{label}</text>
							<text x={left} y={panels.timeTop.top + 12} font-size="11" font-weight="bold" fill="white">{label}</text>
						{/if}
					{/each}

					<!-- The fixed centre line in every panel, with a dot on each line; the selected hour is the one under it -->
					{#if count > 0}
						{@render verticalLine(centerX, ChartStyle.selection, ChartStyle.selectionWidth)}
						{#each selectedDots as dot, k (k)}
							<circle cx={centerX} cy={dot.y} r={ChartStyle.selectionDotRadius} fill={ChartStyle.selectionDot} />
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
		/* The browser scrolls the hours and the page (with momentum); pinch and double tap come to the chart */
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

	/* The hours, after half the view of space (less half an hour) */
	.hours {
		position: absolute;
		top: 0;
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
