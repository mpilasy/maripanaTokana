<!-- SPDX-FileCopyrightText: Orinasa Njarasoa -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
<script lang="ts">
	import { _, json } from 'svelte-i18n';
	import SettingsPage from './SettingsPage.svelte';
	import WeatherIcon from './WeatherIcon.svelte';
	import DualUnitText from './DualUnitText.svelte';
	import type { ChartHour, HourlyAirQuality } from '#lib/domain/weatherData.js';
	import { AirQualityIndex, AQI_TIER_COLORS, type AqiStandard, type AqiTier } from '#lib/domain/airQuality.js';
	import { getCardinalDirection, showWindArrow } from '#lib/domain/windSpeed.js';
	import { uvColorFor, uvTier } from '#lib/domain/uv.js';
	import { monotonePath } from '#lib/utils/monotonePath.js';
	import { runsOf, splitDual } from '#lib/utils/dualLabel.js';
	import { temperatureColor } from '#lib/utils/tempColors.js';
	import { ChartStyle } from '#lib/utils/chartStyle.js';
	import { formatDayName, formatHourAtLocation } from '#lib/utils/date.js';
	import { prefersReducedMotion } from '#lib/utils/motion.js';
	import { moonLitPath } from '#lib/utils/moonPath.js';
	import { moonIllumination, nextMoonPhase } from '#lib/domain/moon.js';
	import {
		CHART_SLOT, chartAirQuality, chartDayExtremes, chartDaySpans, chartDomain, chartHours, chartIndexForX, chartLineIndex, chartLineOffset, chartNightRuns, chartScrollForIndex,
		CHART_LABEL_SIZES, chartLabelFit, chartTileRows, chartTiles, chartValueAt, chartVisibleRange,
		type ChartDomain, type ChartTile,
	} from '#lib/utils/chartView.js';

	/**
	 * Up to 240 hours of the forecast in stacked panels that scroll sideways together, with the values of the
	 * selected hour at the top. It opens at the current hour. Android has the same view in ChartViewScreen.kt.
	 */
	interface Props {
		hours: ChartHour[];
		utcOffsetSeconds: number;
		/** South of the equator, where the moon is seen mirrored. */
		southern: boolean;
		placeName: string;
		metricPrimary: boolean;
		localeTag: string;
		/** The hourly air quality; empty when the place has no air quality. */
		hourlyAirQuality: HourlyAirQuality[];
		/** The standard of the air quality values that the panel and the tile show first. */
		aqiStandard: AqiStandard;
		loc: (s: string) => string;
		onToggleUnits: () => void;
		onBack: () => void;
	}

	let { hours, utcOffsetSeconds, southern, placeName, metricPrimary, localeTag, hourlyAirQuality, aqiStandard, loc, onToggleUnits, onBack }: Props = $props();

	// Sizes in px, the same as on Android: the axis column, the gap between panels, the title line,
	// the room above the wind line for the direction arrows, the room above and under the temperature
	// line of each panel for the labels of the day's high and low (and above the rain bars), the weather icons, and the side margin of the panel bands
	const AXIS_WIDTH = 52;
	const PANEL_GAP = 10;
	const TITLE_HEIGHT = 22;
	const ARROW_ROW = 14;
	const LABEL_ROW = 12;
	const ICON_SIZE = 26;
	const BAND_INSET = 6;

	// The panels from top to bottom, with their height; the value panels have a title line at the top.
	// A time axis is at the top (`timeTop`, mirrored) and at the bottom (`time`). The air quality panel is there
	// only when an hour has air quality.
	type PanelId = 'timeTop' | 'icons' | 'temp' | 'precip' | 'wind' | 'pressure' | 'humidity' | 'airQuality' | 'time';
	/** A panel: its top and height, and the part where the values go (`plotTop` to `plotBottom`). */
	interface Panel { top: number; height: number; plotTop: number; plotBottom: number }
	const isTitled = (id: PanelId) => id !== 'timeTop' && id !== 'icons' && id !== 'time';
	function layout(withAirQuality: boolean) {
		const heights: [PanelId, number][] = [['timeTop', 36], ['icons', 46], ['temp', 156], ['precip', 100], ['wind', 128], ['pressure', 104], ['humidity', 104]];
		if (withAirQuality) heights.push(['airQuality', 104]);
		heights.push(['time', 36]);
		const panels = {} as Record<PanelId, Panel>;
		let top = 0;
		for (const [id, height] of heights) {
			// Each value panel has a label row above and under its primary line; the precipitation only above its bars
			const labelRow = isTitled(id) && id !== 'precip' ? LABEL_ROW : 0;
			const labelTop = isTitled(id) ? LABEL_ROW : 0;
			const plotTop = top + (isTitled(id) ? TITLE_HEIGHT : 0) + (id === 'wind' ? ARROW_ROW : 0) + labelTop + 4;
			panels[id] = { top, height, plotTop, plotBottom: top + height - 4 - labelRow };
			top += height + PANEL_GAP;
		}
		const ids = heights.map(([id]) => id);
		const last = panels[withAirQuality ? 'airQuality' : 'humidity'];
		return {
			ids,
			panels,
			totalHeight: top - PANEL_GAP,
			// Night shading, the selection column and the vertical lines run from the top of the icons to the bottom of
			// the last value panel
			chartsTop: panels.icons.top,
			chartsBottom: last.top + last.height,
			topAxisBottom: panels.timeTop.top + panels.timeTop.height,
			// The vertical lines (midnight) go only through the plot area of each panel, with a break at the
			// gaps and title lines, so that each chart shows where it starts and ends. The time axis has its own short ticks.
			lineSegments: ids.filter((id) => id !== 'timeTop' && id !== 'time').map((id) => panels[id]),
			// The rounded band behind each titled panel
			bands: ids.filter(isTitled).map((id) => panels[id]),
		};
	}

	// The data can be some hours old: start again at the current hour
	const openedAt = Date.now();
	let shown = $derived.by(() => {
		const range = chartHours(hours.map((h) => h.time), openedAt);
		return hours.slice(range.start, range.end);
	});
	let count = $derived(shown.length);

	// The air quality of each hour: the entry less than 30 minutes away, or null
	let aqEntries = $derived.by(() => {
		const indexes = chartAirQuality(shown.map((h) => h.time), hourlyAirQuality.map((a) => a.time));
		return indexes.map((i) => (i == null ? null : hourlyAirQuality[i]));
	});
	const aqPrimary = (a: HourlyAirQuality) => (aqiStandard === 'EUROPEAN' ? a.europeanValue : a.usValue);
	let aqValues = $derived(aqEntries.map((a) => (a ? (Number.isFinite(aqPrimary(a)) ? aqPrimary(a) : null) : null)));
	let hasAirQuality = $derived(aqValues.some((v) => v != null));
	const aqTier = (v: number): AqiTier => AirQualityIndex.tierFor(v, aqiStandard);

	let { ids: panelIds, panels, totalHeight, chartsTop, chartsBottom, topAxisBottom, lineSegments, bands } = $derived(layout(hasAirQuality));

	const slot = CHART_SLOT;
	let scrollLeft = $state(0);
	let viewport = $state(0);
	let selected = $state(0);
	let scroller = $state<HTMLElement | null>(null);

	// The browser's scroll is the start of the view, in px from the start of the first hour. `lineX` is the position
	// under the selection line, which moves from the start of the view to the middle and, at the end, to the end of the
	// view (chartLineOffset).
	let lineX = $derived(scrollLeft + chartLineOffset(scrollLeft, slot, count, viewport));
	let hoursLeft = $derived(scrollLeft);
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

	// Each scale fits all the chart hours, so that it stays the same while the hours scroll
	let tempDomain = $derived(chartDomain([...temps, ...dews], false, metricPrimary ? 5 : 9));
	let precipDomain = $derived(chartDomain(precips, true, metricPrimary ? 1 : 0.04));
	let windDomain = $derived(chartDomain([...winds, ...gusts], true, metricPrimary ? 5 : 10));
	let pressureDomain = $derived(chartDomain(pressures, false, metricPrimary ? 4 : 0.12));
	let aqDomain = $derived(hasAirQuality ? chartDomain(aqValues, true, 50) : null);
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

	// A dot on each line at the selection line
	let selectedDots = $derived.by(() => {
		const dots: { y: number }[] = [];
		const add = (values: (number | null)[], d: ChartDomain | null, p: Panel) => {
			const v = chartValueAt(values, lineX, slot);
			if (d && v != null) dots.push({ y: y(v, d, p) });
		};
		add(dews, tempDomain, panels.temp);
		add(temps, tempDomain, panels.temp);
		add(chances, PERCENT_DOMAIN, panels.precip);
		add(gusts, windDomain, panels.wind);
		add(winds, windDomain, panels.wind);
		add(pressures, pressureDomain, panels.pressure);
		add(humidities, PERCENT_DOMAIN, panels.humidity);
		if (hasAirQuality) add(aqValues, aqDomain, panels.airQuality);
		return dots;
	});

	const localHour = (t: number) => new Date(t + utcOffsetSeconds * 1000).getUTCHours();
	// Icons, wind arrows and hour labels every 4 local hours (00, 04, 08…)
	const LABEL_HOURS = 4;
	let labelled = $derived(drawIndexes.filter((i) => localHour(shown[i].time) % LABEL_HOURS === 0));

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

	// The moon of each day: the phase to draw, and on the day of a new or full moon the index of its name in
	// moon_phases (0 or 4). That day shows the moon new or full; the other days show it at the place's noon
	let moons = $derived(
		days.map((d) => {
			const end = d.date + 24 * 3_600_000;
			if (nextMoonPhase(d.date, 0.5) < end) return { phase: 0.5, event: 4 };
			if (nextMoonPhase(d.date, 0) < end) return { phase: 0, event: 0 };
			return { phase: moonIllumination(d.date + 12 * 3_600_000).phase, event: null };
		}),
	);
	// The moon after each day label of the time axes: its radius, and the gap on each side of it
	const MOON_R = 5;
	const MOON_GAP = 4;
	// The day labels of the time axes (the day at the start stays in view): the date, the moon of the day and, on the day
	// of a new or full moon, its name, when they fit
	let dayLabels = $derived.by(() => {
		// Measured again when the fonts are in
		void fontsReady;
		const names = ($json('moon_phases') as string[] | undefined) ?? [];
		return shownDays.flatMap((d) => {
			const date = dayLabel(d.date);
			const left = Math.max(d.left, hoursLeft) + 4;
			const room = d.right - 4 - left;
			const dateWidth = textWidth(date, 'bold', 10, '--font-display');
			if (dateWidth > room) return [];
			const { phase, event } = moons[d.k];
			const moonX = left + dateWidth + MOON_GAP + MOON_R;
			const nameX = moonX + MOON_R + MOON_GAP;
			const name = event != null ? (names[event] ?? '') : '';
			const withMoon = moonX + MOON_R - left <= room;
			const withName = withMoon && name !== '' && nameX + textWidth(name, 'normal', 10, '--font-body') - left <= room;
			return [{ key: d.startIndex, left, date, phase, moonX: withMoon ? moonX : null, name: withName ? name : null, nameX }];
		});
	});

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
		...(hasAirQuality ? axisLabels(aqDomain, panels.airQuality, null, 0) : []),
	]);
	let grid = $derived([
		...(tempDomain?.ticks.map((v) => y(v, tempDomain!, panels.temp)) ?? []),
		...(precipDomain?.ticks.map((v) => y(v, precipDomain!, panels.precip)) ?? []),
		...(windDomain?.ticks.map((v) => y(v, windDomain!, panels.wind)) ?? []),
		...(pressureDomain?.ticks.map((v) => y(v, pressureDomain!, panels.pressure)) ?? []),
		...PERCENT_DOMAIN.ticks.map((v) => y(v, PERCENT_DOMAIN, panels.humidity)),
		...(hasAirQuality ? (aqDomain?.ticks.map((v) => y(v, aqDomain!, panels.airQuality)) ?? []) : []),
	]);

	const aqiLabel = (standard: AqiStandard) => $_(standard === 'EUROPEAN' ? 'air_quality_eu_aqi' : 'air_quality_us_aqi');

	// Panel titles: the name and both units (primary first), and the name of the second line in its colour
	const units = (metric: string, imperial: string) => (metricPrimary ? `${metric} · ${imperial}` : `${imperial} · ${metric}`);
	let titles = $derived<{ id: PanelId; title: string; second?: { text: string; color: string } }[]>([
		{ id: 'temp', title: `${$_('detail_temperature')}  ${units('°C', '°F')}`, second: { text: $_('detail_dewpoint'), color: ChartStyle.dewPoint } },
		{ id: 'precip', title: `${$_('detail_precipitation')}  ${units('mm', 'in')}`, second: { text: $_('precip_chance', { values: { value: '%' } }), color: ChartStyle.chance } },
		{ id: 'wind', title: `${$_('detail_wind')}  ${units('m/s', 'mph')}`, second: { text: $_('detail_wind_gust'), color: ChartStyle.gust } },
		{ id: 'pressure', title: `${$_('detail_pressure')}  ${units('hPa', 'inHg')}` },
		{ id: 'humidity', title: `${$_('detail_humidity')}  %` },
		...(hasAirQuality ? [{ id: 'airQuality' as PanelId, title: `${$_('detail_air_quality')}  ${aqiLabel(aqiStandard)}` }] : []),
	]);

	// The air quality line has the colour of the level of the hour at the start of each segment: a gradient along the
	// hours with a hard change at the middle of each hour where the level changes
	let aqStops = $derived.by(() => {
		const width = Math.max(1, count * slot);
		const stops: { offset: number; color: string }[] = [];
		let last: string | null = null;
		aqValues.forEach((v, i) => {
			if (v == null) return;
			const color = AQI_TIER_COLORS[aqTier(v)];
			if (color === last) return;
			const offset = last == null ? 0 : cx(i) / width;
			if (last != null) stops.push({ offset, color: last });
			stops.push({ offset, color });
			last = color;
		});
		return stops;
	});
	// Where the air quality stops (about 5 days): the hour before the first missing hour after the data
	let aqEnd = $derived.by(() => {
		const first = aqValues.findIndex((v) => v != null);
		if (first < 0) return -1;
		const gap = aqValues.findIndex((v, i) => i > first && v == null);
		return gap < 0 ? -1 : gap - 1;
	});

	// The day's high and low of the primary line of each panel, labelled at their points; the low is left out when it is
	// the same hour as the high, because it would sit on the same point
	const LABEL_GAP = 2;
	// The colours of the high and low labels, as on the temperature chart of the main page
	const HIGH_COLOR = '#FF8A65';
	const LOW_COLOR = '#64B5F6';
	// The baseline of a 9 px label under a point: the label's top is `LABEL_GAP` under the point
	const LABEL_UNDER = LABEL_GAP + 7;
	let labelCanvas: CanvasRenderingContext2D | null = null;
	/** The width of `text` in px, at `weight` and `size` px in the font of the CSS variable `fontVar`; 0 on the server. */
	function textWidth(text: string, weight: string, size: number, fontVar: string): number {
		if (typeof document === 'undefined') return 0;
		labelCanvas ??= document.createElement('canvas').getContext('2d');
		if (!labelCanvas) return 0;
		const family = getComputedStyle(document.documentElement).getPropertyValue(fontVar).trim() || 'sans-serif';
		labelCanvas.font = `${weight} ${size}px ${family}`;
		return labelCanvas.measureText(text).width;
	}
	/** The width of a high or low label: the primary unit at 9 px bold, the other unit at 0.8em and weight 500. */
	function extremeLabelWidth(primary: string, secondary: string | null): number {
		return textWidth(primary, 'bold', 9, '--font-display') + (secondary ? textWidth(' ' + secondary, '500', 7.2, '--font-display') : 0);
	}
	/**
	 * The labels of the day's high and low of one line (`id` keeps the keys apart); `text` gives the label of an hour.
	 * A label stays inside the part of the plot in view.
	 */
	function dayExtremeLabels(id: PanelId, values: (number | null)[], d: ChartDomain | null, color: (i: number, high: boolean) => string, text: (i: number) => string | null) {
		if (!d) return [];
		const out: { key: string; x: number; y: number; color: string; primary: string; secondary: string | null }[] = [];
		const add = (high: boolean, i: number) => {
			if (i < drawn.start || i >= drawn.end || values[i] == null) return;
			const label = text(i);
			if (label == null) return;
			const [p, s] = splitDual(label);
			const primary = loc(p);
			const secondary = s ? loc(s) : null;
			const py = y(values[i]!, d, panels[id]);
			let x = cx(i);
			if (viewport > 0) {
				const half = extremeLabelWidth(primary, secondary) / 2 + 2;
				x = Math.max(hoursLeft + half, Math.min(x, hoursLeft + viewport - half));
			}
			out.push({
				key: `${id}${high ? 'high' : 'low'}${i}`,
				x,
				y: high ? py - LABEL_GAP : py + LABEL_UNDER,
				color: color(i, high),
				primary,
				secondary,
			});
		};
		for (const e of chartDayExtremes(values, days)) {
			add(true, e.high);
			if (e.low !== e.high) add(false, e.low);
		}
		return out;
	}
	let extremeLabels = $derived.by(() => {
		// Measured again when the fonts are in
		void fontsReady;
		return [
			...dayExtremeLabels('temp', temps, tempDomain, (_, high) => (high ? HIGH_COLOR : LOW_COLOR), (i) => shown[i].temperature?.displayDual(metricPrimary).join(' · ') ?? null),
			...dayExtremeLabels('wind', winds, windDomain, () => ChartStyle.wind, (i) => shown[i].windSpeed?.displayDual(metricPrimary).join(' · ') ?? null),
			...dayExtremeLabels('pressure', pressures, pressureDomain, () => ChartStyle.pressure, (i) => shown[i].pressure?.displayDual(metricPrimary).join(' · ') ?? null),
			...dayExtremeLabels('humidity', humidities, PERCENT_DOMAIN, () => ChartStyle.humidity, (i) => (shown[i].humidity != null ? `${shown[i].humidity}%` : null)),
			...(hasAirQuality ? dayExtremeLabels('airQuality', aqValues, aqDomain, (i) => AQI_TIER_COLORS[aqTier(aqValues[i]!)], (i) => String(aqValues[i])) : []),
		];
	});

	// The value of the highest bar of each day (any number of hours), above the bar
	let precipLabels = $derived.by(() => {
		if (!precipDomain) return [];
		const out: { key: string; i: number; y: number; primary: string; secondary: string | null }[] = [];
		for (const e of chartDayExtremes(precips, days, 1)) {
			const i = e.high;
			if (i < drawn.start || i >= drawn.end || precips[i] == null || precips[i]! <= 0) continue;
			const [primary, secondary] = splitDual(shown[i].precipitation!.displayDual(metricPrimary).join(' · '));
			out.push({ key: `p${i}`, i, y: y(precips[i]!, precipDomain, panels.precip) - LABEL_GAP, primary: loc(primary), secondary: secondary ? loc(secondary) : null });
		}
		return out;
	});

	let summary = $derived($_('chart_view_summary', { values: { hours: loc(String(count)) } }));

	/** One value tile of the selection card: its label, and the value as a pair of units, as text, or on a pill. */
	interface Cell {
		label: string;
		dual?: [string, string];
		text?: string;
		pill?: { background: string; color: string };
		/** The chance of rain, localised, under the precipitation. */
		chance?: string;
		direction?: string;
		/** The tile has a line under the value, which keeps its height when it is empty. */
		extraLine?: boolean;
	}
	// The tiles of the chart, the same for every hour, so the card keeps its size when the selection moves
	let tiles = $derived(chartTiles(shown, hasAirQuality));
	const TILE_LABEL_KEYS: Record<ChartTile, string> = {
		temperature: 'detail_temperature', dewPoint: 'detail_dewpoint', precipitation: 'detail_precipitation', wind: 'detail_wind',
		windGust: 'detail_wind_gust', pressure: 'detail_pressure', humidity: 'detail_humidity', uvIndex: 'detail_uv_index',
		airQuality: 'detail_air_quality',
	};
	let labels = $derived(tiles.map((tile) => $_(TILE_LABEL_KEYS[tile])));
	// The label size of the card (chartLabelFit), measured with the font of the labels. The strips share their rows
	// with their cells (subgrid), so the values line up when a label takes 2 lines
	let rowsEl = $state<HTMLElement | null>(null);
	let rowsWidth = $state(0);
	let fontsReady = $state(false);
	$effect(() => {
		document.fonts?.ready.then(() => (fontsReady = true));
	});
	let labelFit = $derived.by(() => {
		void fontsReady;
		const context = rowsEl ? document.createElement('canvas').getContext('2d') : null;
		if (!rowsEl || !context || rowsWidth <= 0) return { size: CHART_LABEL_SIZES[0], lines: labels.map(() => 1) };
		const style = getComputedStyle(rowsEl);
		const family = style.getPropertyValue('--font-body').trim() || style.fontFamily;
		// The width of a label in each cell: the strips less their padding, shared by the cells, less the cell padding
		const content = rowsWidth - 2 * 12;
		const widths = chartTileRows(labels.length).flatMap((n) => Array<number>(n).fill(content / n - 2 * 2));
		return chartLabelFit(labels, widths, (text, size) => {
			context.font = `${size}px ${family}`;
			return context.measureText(text).width;
		});
	});
	// The values of the selected hour in rows of up to 5 that are as even as possible; a value that the hour does not
	// have shows a dash
	let rows = $derived.by(() => {
		const h = selectedHour;
		if (!h) return [];
		const missing: [string, string] = ['–', '\u00a0'];
		const dual = (pair: [string, string] | undefined): [string, string] => (pair ? [loc(pair[0]), loc(pair[1])] : missing);
		const aq = aqEntries[Math.min(selected, count - 1)];
		const values = tiles.map((tile): Omit<Cell, 'label'> => {
			switch (tile) {
				case 'temperature':
					return { dual: dual(h.temperature?.displayDual(metricPrimary)), pill: h.temperature ? { background: temperatureColor(h.temperature.celsius), color: '#1A1A1A' } : undefined };
				case 'dewPoint':
					return { dual: dual(h.dewPoint?.displayDual(metricPrimary)) };
				case 'precipitation': {
					const chance = h.precipProbability != null && h.precipProbability > 0 ? loc(`${h.precipProbability}%`) : undefined;
					return { dual: dual(h.precipitation?.displayDual(metricPrimary)), chance, extraLine: true };
				}
				case 'wind': {
					const direction = h.windSpeed && h.windDeg != null && showWindArrow(h.windSpeed) ? getCardinalDirection(h.windDeg, $json('cardinal_directions')) : undefined;
					return { dual: dual(h.windSpeed?.displayDual(metricPrimary)), direction, extraLine: true };
				}
				case 'windGust':
					return { dual: dual(h.windGust?.displayDual(metricPrimary)) };
				case 'pressure':
					return { dual: dual(h.pressure?.displayDual(metricPrimary)) };
				case 'humidity':
					return { text: h.humidity != null ? loc(`${h.humidity}%`) : missing[0] };
				case 'uvIndex': {
					if (h.uvIndex == null || !Number.isFinite(h.uvIndex)) return { text: missing[0] };
					const badge = uvBadge(h.uvIndex);
					return { text: badge.text, pill: { background: badge.background, color: badge.color } };
				}
				case 'airQuality': {
					// The primary standard in a pill of its tier colour; the panel title names the standard
					if (!aq) return { text: missing[0] };
					const primary = aqPrimary(aq);
					return { text: loc(String(primary)), pill: aqiPill(aqTier(primary)) };
				}
			}
		});
		const cells = values.map((v, i): Cell => ({ ...v, label: labels[i] }));
		let start = 0;
		return chartTileRows(cells.length).map((size) => cells.slice(start, (start += size)));
	});

	/** The colours of an air quality level on a pill: dark text on the light colours, as on Android. */
	function aqiPill(tier: AqiTier) {
		return { background: AQI_TIER_COLORS[tier], color: tier === 'good' || tier === 'moderate' ? '#1A1A1A' : 'white' };
	}

	/** The UV index as shown (rounded), and the colours of its level, from the shown value. */
	function uvBadge(uv: number) {
		const shownValue = Math.round(uv);
		return { text: loc(String(shownValue)), background: uvColorFor(shownValue), color: uvTier(shownValue) === 'moderate' ? '#1A1A1A' : 'white' };
	}

	// Gestures, as on Android. The selected hour is the hour under the selection line. One finger scrolls the hours
	// (the browser's own scrolling, with momentum), and the page up and down; a mouse drags the hours. The wheel with
	// Shift, or a sideways wheel or trackpad, scrolls the hours. A tap or click scrolls the hour under it to the line.
	const SLOP = 8;
	let press: { id: number; x: number; y: number; scroll: number; mouse: boolean; moved: boolean } | null = null;

	/** On each scroll: a new hour under the selection line becomes the selection, with a short vibrate. */
	function onScroll() {
		scrollLeft = scroller?.scrollLeft ?? 0;
		const i = chartLineIndex(scrollLeft, slot, count, viewport);
		if (i < 0 || i === selected) return;
		selected = i;
		navigator.vibrate?.(5);
	}

	/** Scrolls the middle of hour `i` to the selection line. */
	function centre(i: number, smooth: boolean) {
		scroller?.scrollTo({ left: chartScrollForIndex(i, slot, count, viewport), behavior: smooth && !prefersReducedMotion() ? 'smooth' : 'auto' });
	}
	// When the view opens, the browser's scroll is 0: the middle of the current hour (the first) is under the line,
	// and the selection is already that hour, so there is no vibrate

	function onPointerDown(e: PointerEvent) {
		// A second finger is not a tap
		if (press) return (press = null);
		if (e.pointerType === 'mouse' && e.button !== 0) return;
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
		press = null;
		if (!tap || !scroller) return;
		const i = chartIndexForX(e.clientX - scroller.getBoundingClientRect().left + scroller.scrollLeft, slot, count);
		if (i >= 0) centre(i, true);
	}

	// Shift and the wheel: the hours scroll (a sideways wheel or trackpad scrolls them without help). Not passive, so
	// that it can stop the page scroll
	$effect(() => {
		const el = scroller;
		if (!el) return;
		const wheel = (e: WheelEvent) => {
			if (!e.shiftKey || Math.abs(e.deltaY) <= Math.abs(e.deltaX)) return;
			e.preventDefault();
			el.scrollLeft += e.deltaY;
		};
		el.addEventListener('wheel', wheel, { passive: false });
		return () => el.removeEventListener('wheel', wheel);
	});

	// Keys (web only): Left and Right scroll one hour (so the selection moves by one), Home and End go to the first
	// and last hour
	function onKeyDown(e: KeyboardEvent) {
		if (count === 0) return;
		// From the hour under the line now: the scroll event that updates the selection may not have come yet
		const s = scroller?.scrollLeft ?? 0;
		const current = chartLineIndex(s, slot, count, viewport);
		let next: number;
		if (e.key === 'ArrowLeft') next = Math.max(0, current - 1);
		else if (e.key === 'ArrowRight') next = Math.min(count - 1, current + 1);
		else if (e.key === 'Home') next = 0;
		else if (e.key === 'End') next = count - 1;
		else return;
		e.preventDefault();
		centre(next, false);
	}
</script>

<SettingsPage title={placeName.trim() || $_('chart_view_title')} subtitle={placeName.trim() ? $_('chart_view_title') : undefined} stacked {onBack}>

	<!-- The selected hour's values; a screen reader reads them again when the selection moves, a tap swaps the units -->
	<button type="button" class="card selection" onclick={onToggleUnits} aria-describedby="chart-units-hint">
		<span class="sel-live" aria-live="polite">
			{#if selectedHour}
				<span class="sel-title">
					<span class="sel-time">{hourLabel(selectedHour)}</span>
					<WeatherIcon code={selectedHour.weatherCode} night={selectedHour.isNight} size={32} />
				</span>
				<span class="sel-rows" bind:this={rowsEl} bind:clientWidth={rowsWidth}>
					{#each rows as row, r (r)}
						<span class="sel-strip">
							{#each row as c, k (k)}
								<span class="sel-cell">
									<span class="sel-label" style:font-size="{labelFit.size}px">{c.label}</span>
									{#if c.dual}
										{#if c.pill}
											<span class="pill sel-temp" style:background={c.pill.background}>
												<DualUnitText primary={c.dual[0]} secondary={c.dual[1]} primarySize="0.8125rem" align="center" />
											</span>
										{:else}
											<DualUnitText primary={c.dual[0]} secondary={c.dual[1]} primarySize="0.8125rem" align="center" />
										{/if}
									{:else if c.text}
										<bdi dir="ltr" class="sel-text" class:pill={c.pill} style:background={c.pill?.background} style:color={c.pill?.color}>{c.text}</bdi>
									{/if}
									{#if c.chance}
										<span class="sel-sub sel-chance">
											<span aria-hidden="true"><svg class="sel-drop" viewBox="0 0 24 24" width="9" height="9" fill="currentColor"><path d="M12 2c-5.33 4.55-8 8.48-8 11.8 0 4.98 3.8 8.2 8 8.2s8-3.22 8-8.2c0-3.32-2.67-7.25-8-11.8z"/></svg>{c.chance}</span>
											<span class="sr-only">{$_('precip_chance', { values: { value: c.chance } })}</span>
										</span>
									{/if}
									{#if c.direction}<span class="sel-sub sel-direction">{c.direction}</span>{/if}
									{#if c.extraLine && !c.chance && !c.direction}<span class="sel-sub" aria-hidden="true">&nbsp;</span>{/if}
								</span>
							{/each}
						</span>
					{/each}
				</span>
			{/if}
		</span>
	</button>
	<span id="chart-units-hint" hidden>{$_('cd_toggle_units')}</span>

	<!-- Time runs left to right in every language, with the axis column on the left -->
	<div class="card chart-card">
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
		<!-- A rounded band behind each titled panel and its scale labels, under everything else; it does not scroll -->
		<div class="bands" aria-hidden="true">
			{#each bands as b (b.top)}
				<div class="band" style:top="{b.top}px" style:height="{b.height}px" style:inset-inline="{BAND_INSET}px" style:background={ChartStyle.panelBand} style:border-radius="{ChartStyle.panelBandRadius}px"></div>
			{/each}
		</div>

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
			onpointercancel={() => (press = null)}
			role="presentation"
		>
			<div class="content" style:width="{count * slot}px" style:height="{totalHeight}px">
			<div class="hours" style:width="{count * slot}px" style:height="{totalHeight}px">
				<svg width={count * slot} height={totalHeight} aria-hidden="true">
					<defs>
						{#each panelIds as id (id)}
							<clipPath id="chart-clip-{id}"><rect x="0" y={panels[id].top} width={count * slot} height={panels[id].height} /></clipPath>
						{/each}
					</defs>
					<!-- A vertical line at `x` through the plot area of each panel, with a break between panels.
					     Declared in the svg, so that its lines are svg elements -->
					{#snippet verticalLine(x: number, stroke: string, width: number)}
						{#each lineSegments as p (p.top)}
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

					<!-- The selected hour: a soft column over the panels, under the lines -->
					{#if count > 0}
						<rect x={lineX - slot / 2} y={chartsTop} width={slot} height={chartsBottom - chartsTop} rx={ChartStyle.selectionColumnRadius} fill={ChartStyle.selectionColumn} />
					{/if}

					<!-- The UV index under the icons, in the colours of its level -->
					{#each labelled as i (i)}
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
					{#each precipLabels as l (l.key)}
						<text x={cx(l.i)} y={l.y} text-anchor="middle" direction="ltr" font-size="9" font-weight="bold" fill={ChartStyle.precipitation} style:font-family="var(--font-display)" style:font-feature-settings="var(--font-features)">{l.primary}{#if l.secondary}<tspan font-size="0.8em" font-weight="500" fill-opacity="0.7">{" " + l.secondary}</tspan>{/if}</text>
					{/each}

					<!-- Wind and gusts on one scale, with the direction every 4 hours -->
					<g clip-path="url(#chart-clip-wind)">
						{#if windDomain}
							<path d={curve(gusts, windDomain, panels.wind)} fill="none" stroke={ChartStyle.gust} stroke-width={ChartStyle.thinLineWidth} stroke-dasharray={ChartStyle.gustDash} />
							<path d={curve(winds, windDomain, panels.wind)} fill="none" stroke={ChartStyle.wind} stroke-width={ChartStyle.lineWidth} />
						{/if}
					</g>
					{#each labelled as i (i)}
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

					<!-- The air quality in the primary standard, each segment in the colour of the level of the hour at its start;
					     no line where the hours have no air quality, and a note where the data stops -->
					{#if hasAirQuality && aqDomain}
						<defs>
							<linearGradient id="chart-aq-gradient" gradientUnits="userSpaceOnUse" x1="0" y1="0" x2={Math.max(1, count * slot)} y2="0">
								{#each aqStops as stop, k (k)}
									<stop offset={stop.offset} stop-color={stop.color} />
								{/each}
							</linearGradient>
						</defs>
						<path clip-path="url(#chart-clip-airQuality)" d={curve(aqValues, aqDomain, panels.airQuality)} fill="none" stroke="url(#chart-aq-gradient)" stroke-width={ChartStyle.lineWidth} />
						{#if aqEnd >= 0}
							<!-- It stays at the start of the plot when the last hour with air quality is out of view -->
							<text x={Math.max(cx(aqEnd), hoursLeft) + 8} y={(panels.airQuality.plotTop + panels.airQuality.plotBottom) / 2} dominant-baseline="central" font-size="11" fill="white" fill-opacity="0.5">{$_('chart_view_no_data')}</text>
						{/if}
					{/if}

					<!-- Time axes: an hour label every 4 hours, and the day at each midnight (the day at the start stays in view).
					     The bottom axis has its ticks, then the hours, then the days; the top axis is the same, mirrored. -->
					{#each labelled as i (i)}
						{@const hourText = loc(formatHourAtLocation(shown[i].time, utcOffsetSeconds))}
						<line x1={cx(i)} x2={cx(i)} y1={panels.time.top} y2={panels.time.top + 3} stroke="white" stroke-opacity="0.3" stroke-width="1" />
						<text x={cx(i)} y={panels.time.top + 13} text-anchor="middle" font-size="10" fill="white" fill-opacity="0.6">{hourText}</text>
						<line x1={cx(i)} x2={cx(i)} y1={topAxisBottom - 3} y2={topAxisBottom} stroke="white" stroke-opacity="0.3" stroke-width="1" />
						<text x={cx(i)} y={topAxisBottom - 6} text-anchor="middle" font-size="10" fill="white" fill-opacity="0.6">{hourText}</text>
					{/each}
					{#each dayLabels as l (l.key)}
						{#each [panels.time.top + 30, panels.timeTop.top + 12] as baseline (baseline)}
							<text x={l.left} y={baseline} font-size="10" font-weight="bold" fill="white" style:font-family="var(--font-display)">{l.date}</text>
							{#if l.moonX != null}
								<circle cx={l.moonX} cy={baseline - 3.5} r={MOON_R} fill="#3A3570" />
								<path d={moonLitPath(l.phase, southern, l.moonX, baseline - 3.5, MOON_R)} fill="#F2EBD3" />
							{/if}
							{#if l.name}
								<text x={l.nameX} y={baseline} font-size="10" fill="white" fill-opacity="0.7" style:font-family="var(--font-body)">{l.name}</text>
							{/if}
						{/each}
					{/each}

					<!-- A dot on each line at the selected hour -->
					{#if count > 0}
						{#each selectedDots as dot, k (k)}
							<circle cx={lineX} cy={dot.y} r={ChartStyle.selectionDotRadius} fill={ChartStyle.selectionDot} />
						{/each}
					{/if}

					<!-- The high and low of each day (12 hours or more) on the primary line of each panel, on top of every line and dot -->
					{#each extremeLabels as l (l.key)}
						<text x={l.x} y={l.y} text-anchor="middle" direction="ltr" font-size="9" font-weight="bold" fill={l.color} style:font-family="var(--font-display)" style:font-feature-settings="var(--font-features)">{l.primary}{#if l.secondary}<tspan font-size="0.8em" font-weight="500" fill-opacity="0.7">{" " + l.secondary}</tspan>{/if}</text>
					{/each}
				</svg>

				<!-- Weather icons every 4 local hours (night icons at night) -->
				{#each labelled as i (i)}
					{@const h = shown[i]}
					<div class="icon" style:left="{cx(i) - ICON_SIZE / 2}px" style:top="{panels.icons.top}px" style:width="{ICON_SIZE}px" style:height="{ICON_SIZE}px">
						<WeatherIcon code={h.weatherCode} night={h.isNight} size={ICON_SIZE} decorative />
					</div>
				{/each}
			</div>
			</div>
		</div>

		<!-- Titles at the start of each panel, and the chance of rain scale at the end of its panel; they do not scroll -->
		<div class="overlay" aria-hidden="true">
			{#each titles as t (t.id)}
				<div class="panel-title" style:top="{panels[t.id].top}px" style:height="{TITLE_HEIGHT}px" style:left="{BAND_INSET + 10}px" dir="auto">
					<span class="title-main">{t.title}</span>{#if t.second}<span class="title-second" style:color={t.second.color}>{'   ● ' + t.second.text}</span>{/if}
				</div>
			{/each}
			{#each [50, 100] as v (v)}
				<span class="chance-label" style:top="{y(v, PERCENT_DOMAIN, panels.precip)}px">{loc(`${v}%`)}</span>
			{/each}
		</div>
	</div>
	</div>
</SettingsPage>

<style>

	/* Glass cards as the section cards of the main screen; the page has 16 px at the sides, the cards 12 */
	.card {
		display: block;
		box-sizing: border-box;
		margin-inline: -4px;
		background: var(--surface-section);
		border: 1px solid var(--hairline);
		border-radius: 20px;
	}

	.selection {
		width: calc(100% + 8px);
		margin-bottom: 8px;
		padding: 0;
		color: white;
		font: inherit;
		text-align: start;
		cursor: pointer;
	}

	.sel-live {
		display: block;
	}

	.sel-title {
		display: flex;
		align-items: center;
		gap: 8px;
		min-height: 48px;
		padding-inline: 16px 12px;
	}

	.sel-time {
		flex: 1;
		min-width: 0;
		font-family: var(--font-body);
		font-size: 1.0625rem;
		font-weight: 700;
		color: white;
		font-feature-settings: var(--font-features);
	}

	.sel-rows {
		display: flex;
		flex-direction: column;
		gap: 6px;
		padding: 0 12px 12px;
	}

	/* A strip as the next hours of the hero card. The cells share its rows (subgrid): the labels in one strip get the
	   height of the tallest one, so the values line up when a label takes 2 lines */
	.sel-strip {
		display: grid;
		grid-auto-flow: column;
		grid-auto-columns: 1fr;
		grid-template-rows: auto auto auto;
		padding: 8px 0;
		background: var(--surface-tile);
		border-radius: 16px;
	}

	.sel-cell {
		position: relative;
		grid-row: span 3;
		min-width: 0;
		display: grid;
		grid-template-rows: subgrid;
		justify-items: center;
		align-items: start;
		padding: 0 2px;
		text-align: center;
	}

	.sel-cell + .sel-cell::before {
		content: '';
		position: absolute;
		inset-block: 4px;
		inset-inline-start: 0;
		width: 1px;
		background: var(--hairline);
	}

	/* The label at the size of the card (labelFit), on up to 2 lines */
	.sel-label {
		display: -webkit-box;
		-webkit-box-orient: vertical;
		-webkit-line-clamp: 2;
		line-clamp: 2;
		overflow: hidden;
		margin-bottom: 2px;
		font-family: var(--font-body);
		line-height: 1.2;
		color: rgba(255, 255, 255, 0.6);
	}

	/* The temperature and the UV index on a pill, moved up by its padding so its text lines up with the values next to it;
	   dark text on the light colours of the scale */
	.pill {
		margin-top: -2px;
		padding: 2px 10px;
		border-radius: 12px;
	}

	.sel-temp :global(.primary),
	.sel-temp :global(.secondary) {
		color: #1A1A1A;
	}

	.sel-text {
		font-family: var(--font-display);
		font-size: 0.8125rem;
		font-weight: 700;
		line-height: 1.15;
		color: white;
		font-feature-settings: var(--font-features);
	}

	.sel-sub {
		font-size: 0.6875rem;
		line-height: 14px;
		font-feature-settings: var(--font-features);
	}

	.sel-chance {
		color: #64B5F6;
	}

	.sel-drop {
		margin-inline-end: 2px;
	}

	.sel-direction {
		color: rgba(255, 255, 255, 0.7);
	}

	.chart-card {
		padding: 8px 0;
		overflow: hidden;
	}

	.chart {
		position: relative;
		display: flex;
		outline: none;
	}

	.chart:focus-visible {
		outline: 2px solid rgba(255, 255, 255, 0.8);
		outline-offset: -2px;
	}

	.bands {
		position: absolute;
		inset: 0;
		pointer-events: none;
	}

	.band {
		position: absolute;
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
		/* The browser scrolls the hours and the page (with momentum) */
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
		left: 0;
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

	/* Centred in the title line; the units stay in the title text */
	.panel-title {
		position: absolute;
		display: flex;
		align-items: center;
		white-space: pre;
		font-family: var(--font-body);
	}

	.title-main {
		font-size: 13px;
		font-weight: 700;
		color: rgba(255, 255, 255, 0.9);
	}

	.title-second {
		font-size: 11px;
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
