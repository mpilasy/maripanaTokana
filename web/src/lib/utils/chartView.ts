// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

// Pure functions of the chart view and the charts on the main page. Android has the same functions with the same
// rules in ChartViewMath.kt.

import { isDaytime, type ChartHour } from '#lib/domain/weatherData.js';

const HOUR = 3_600_000;
const DAY = 24 * HOUR;

/** The most hours that the chart view shows (10 days). */
export const CHART_MAX_HOURS = 240;
/** The width of one hour, in px. */
export const CHART_SLOT = 28;

/** A range of indexes: `start` is in it, `end` is not. */
export interface IndexRange {
	start: number;
	end: number;
}

/**
 * The hours to show: from the hour that contains `nowMs` (the first hour that has not ended), at most `max`.
 * `times` are the start of each hour in epoch millis, in order. Empty when every hour has ended.
 */
export function chartHours(times: number[], nowMs: number, max = CHART_MAX_HOURS): IndexRange {
	const found = times.findIndex((t) => t + HOUR > nowMs);
	const start = found === -1 ? times.length : found;
	return { start, end: Math.min(times.length, start + Math.max(0, max)) };
}

/** One local day of the hours: `startIndex` is in it, `endIndex` is not; `date` is its local midnight (epoch millis). */
export interface ChartDaySpan {
	startIndex: number;
	endIndex: number;
	date: number;
}

/**
 * The hours in groups of one local day each, in order. A day is the place's calendar day (UTC + `utcOffsetSec`).
 * A day can have fewer or more than 24 hours (the first and last day, a change to or from summer time).
 */
export function chartDaySpans(times: number[], utcOffsetSec: number): ChartDaySpan[] {
	const offset = utcOffsetSec * 1000;
	const spans: ChartDaySpan[] = [];
	times.forEach((t, i) => {
		const date = Math.floor((t + offset) / DAY) * DAY - offset;
		const last = spans[spans.length - 1];
		if (last && last.date === date) last.endIndex = i + 1;
		else spans.push({ startIndex: i, endIndex: i + 1, date });
	});
	return spans;
}

/** The first hour of each local day after the first one: where a chart draws a midnight line. */
export function chartMidnights(times: number[], utcOffsetSec: number): number[] {
	return chartDaySpans(times, utcOffsetSec).slice(1).map((s) => s.startIndex);
}

/** The runs of night hours in `night` (true at night), in order. */
export function chartNightRuns(night: boolean[]): IndexRange[] {
	const runs: IndexRange[] = [];
	night.forEach((isNight, i) => {
		if (!isNight) return;
		const last = runs[runs.length - 1];
		if (last && last.end === i) last.end = i + 1;
		else runs.push({ start: i, end: i + 1 });
	});
	return runs;
}

/**
 * Whether each hour is at night, with the same rule as the hourly forecast: the hour takes the last sunrise
 * at or before it, and it is night when it is not between that sunrise and the sunset of the same day.
 * An hour before the first sunrise uses the first day.
 */
export function chartDaylight(times: number[], sunrises: number[], sunsets: number[]): boolean[] {
	return times.map((t) => {
		let day = 0;
		for (let i = sunrises.length - 1; i >= 0; i--) {
			if (sunrises[i] <= t) {
				day = i;
				break;
			}
		}
		return !isDaytime(t, sunrises[day] ?? 0, sunsets[day] ?? 0);
	});
}

/**
 * The hours to draw: the hours in view (scrolled `scrollPx`, `viewportPx` wide, `slotPx` an hour) and `overscan`
 * more on each side, kept inside 0 to `count`.
 */
export function chartVisibleRange(scrollPx: number, viewportPx: number, slotPx: number, count: number, overscan = 2): IndexRange {
	if (count <= 0 || slotPx <= 0) return { start: 0, end: 0 };
	const first = Math.floor(Math.max(0, scrollPx) / slotPx);
	const last = Math.ceil((Math.max(0, scrollPx) + Math.max(0, viewportPx)) / slotPx);
	return { start: Math.max(0, first - overscan), end: Math.min(count, Math.max(first + 1, last) + overscan) };
}

/** The hour at `x` px from the start of the hours (`slotPx` an hour), kept inside 0 to `count` - 1; -1 when there is no hour. */
export function chartIndexForX(x: number, slotPx: number, count: number): number {
	if (count <= 0 || slotPx <= 0) return -1;
	return Math.min(count - 1, Math.max(0, Math.floor(x / slotPx)));
}

// The selection line starts on the first hour at the start of the plot. While the hours scroll the first half plot
// width, it moves to the middle of the plot and stays there; at the end it moves on to the last hour at the end of the
// plot. So the plot never has empty space. The scroll is the start of the plot, in px from the start of the first hour.

/** The largest scroll, with `viewPx` the plot width: 0 when all the hours fit in the plot. */
export function chartMaxScroll(slotPx: number, count: number, viewPx: number): number {
	return Math.max(0, count * slotPx - viewPx);
}

/** The position of the selection line, in px from the start of the plot, at `scrollPx`. */
export function chartLineOffset(scrollPx: number, slotPx: number, count: number, viewPx: number): number {
	const max = chartMaxScroll(slotPx, count, viewPx);
	// The distance from the middle of the first hour to the middle of the plot, and the scroll that moves the line on it
	const travel = Math.max(0, (viewPx - slotPx) / 2);
	const ramp = Math.min(travel, max / 2);
	if (ramp <= 0) return slotPx / 2;
	const s = Math.min(max, Math.max(0, scrollPx));
	return slotPx / 2 + travel * (Math.min(1, s / ramp) + 1 - Math.min(1, (max - s) / ramp));
}

/** The hour under the selection line, at `scrollPx`; -1 when there is no hour. */
export function chartLineIndex(scrollPx: number, slotPx: number, count: number, viewPx: number): number {
	return chartIndexForX(scrollPx + chartLineOffset(scrollPx, slotPx, count, viewPx), slotPx, count);
}

/** The scroll that puts the middle of hour `index` under the selection line, kept inside the scroll range. */
export function chartScrollForIndex(index: number, slotPx: number, count: number, viewPx: number): number {
	const max = chartMaxScroll(slotPx, count, viewPx);
	const travel = Math.max(0, (viewPx - slotPx) / 2);
	const ramp = Math.min(travel, max / 2);
	if (ramp <= 0) return 0;
	// Three straight parts: the line moves to the middle, stays there, and moves to the end
	const x = index * slotPx;
	const speed = 1 + travel / ramp;
	let s: number;
	if (x <= ramp * speed) s = x / speed;
	else if (x <= max - ramp + travel) s = x - travel;
	else s = (x - 2 * travel + (travel * max) / ramp) / speed;
	return Math.min(max, Math.max(0, s));
}

/**
 * The value at `x` px from the start of the hours (`slotPx` an hour, each value at the middle of its hour), on the
 * straight line between the two hours around it. With one of the two missing, the value of the nearer hour, or null.
 */
export function chartValueAt(values: (number | null)[], x: number, slotPx: number): number | null {
	if (values.length === 0 || slotPx <= 0) return null;
	const pos = Math.min(values.length - 1, Math.max(0, x / slotPx - 0.5));
	const a = Math.floor(pos);
	const b = Math.min(values.length - 1, a + 1);
	const va = values[a];
	const vb = values[b];
	if (va != null && vb != null) return va + (vb - va) * (pos - a);
	return values[Math.round(pos)] ?? null;
}

/** The highest and the lowest value of one day: their indexes in the values. */
export interface ChartDayExtreme {
	high: number;
	low: number;
}

/** The middle one of the tied indexes, as the temperature chart of the main page. */
function middleOf(indexes: number[]): number {
	return indexes[Math.floor(indexes.length / 2)];
}

/**
 * For each day in `daySpans` with at least `minHours` hours: the index of its highest and of its lowest value
 * (nulls and values that are not numbers are ignored). With ties, the middle one of the tied indexes.
 * A day with fewer hours, or with no value, gives nothing.
 */
export function chartDayExtremes(
	values: (number | null | undefined)[],
	daySpans: { startIndex: number; endIndex: number }[],
	minHours = 12,
): ChartDayExtreme[] {
	const result: ChartDayExtreme[] = [];
	for (const span of daySpans) {
		if (span.endIndex - span.startIndex < minHours) continue;
		let hi = -Infinity;
		let lo = Infinity;
		for (let i = span.startIndex; i < span.endIndex; i++) {
			const v = values[i];
			if (v == null || !Number.isFinite(v)) continue;
			if (v > hi) hi = v;
			if (v < lo) lo = v;
		}
		if (lo > hi) continue;
		const highs: number[] = [];
		const lows: number[] = [];
		for (let i = span.startIndex; i < span.endIndex; i++) {
			if (values[i] === hi) highs.push(i);
			if (values[i] === lo) lows.push(i);
		}
		result.push({ high: middleOf(highs), low: middleOf(lows) });
	}
	return result;
}

/** A vertical scale: the bottom and top values and the values that get a label (from bottom to top). */
export interface ChartDomain {
	min: number;
	max: number;
	ticks: number[];
}

/** A step of 1, 2 or 5 times a power of 10, at least `rough`. */
function niceStep(rough: number): number {
	const power = Math.pow(10, Math.floor(Math.log10(rough)));
	const step = [1, 2, 5, 10].find((m) => m * power >= rough * (1 - 1e-9)) ?? 10;
	return step * power;
}

/** A value with the floating-point noise of a sum of steps removed. */
function clean(v: number): number {
	return Number(v.toPrecision(12));
}

/**
 * The vertical scale of `values` (nulls and values that are not numbers are ignored). The scale is at least `minSpan`
 * high (around the middle of the values; up from 0 with `fromZero`), and its ends are on round ticks, about 3 steps
 * apart. With `fromZero`, the scale starts at 0 or lower. Null when there is no value.
 */
export function chartDomain(values: (number | null | undefined)[], fromZero: boolean, minSpan: number): ChartDomain | null {
	let lo = Infinity;
	let hi = -Infinity;
	for (const v of values) {
		if (v == null || !Number.isFinite(v)) continue;
		if (v < lo) lo = v;
		if (v > hi) hi = v;
	}
	if (lo > hi) return null;
	if (fromZero) lo = Math.min(lo, 0);
	const span = Math.max(minSpan, 0);
	if (hi - lo < span) {
		if (fromZero && lo === 0) {
			hi = span;
		} else {
			// Around the middle; also with fromZero when a value is under 0
			const mid = (lo + hi) / 2;
			lo = mid - span / 2;
			hi = mid + span / 2;
		}
	}
	if (hi === lo) hi = lo + 1;
	const step = niceStep((hi - lo) / 3);
	const min = clean(Math.floor(lo / step + 1e-9) * step);
	const max = clean(Math.ceil(hi / step - 1e-9) * step);
	const ticks: number[] = [];
	for (let k = 0; min + k * step <= max + step * 1e-9; k++) ticks.push(clean(min + k * step));
	return { min, max, ticks };
}

/**
 * For each hour of `hourTimes`, the index in `aqTimes` of the first air quality entry less than 30 minutes away, as the
 * hourly forecast matches them; null when no entry is near.
 */
export function chartAirQuality(hourTimes: number[], aqTimes: number[]): (number | null)[] {
	return hourTimes.map((t) => {
		const i = aqTimes.findIndex((a) => Math.abs(a - t) < HOUR / 2);
		return i >= 0 ? i : null;
	});
}

/** The value tiles of the selection card, in their order. */
export const CHART_TILES = ['temperature', 'dewPoint', 'precipitation', 'wind', 'windGust', 'pressure', 'humidity', 'uvIndex', 'airQuality'] as const;
export type ChartTile = (typeof CHART_TILES)[number];

/**
 * The tiles of the selection card: each value that at least one of `hours` has. The card keeps these tiles for every
 * hour (an hour without the value shows a dash), so it keeps the same size when the selection moves.
 */
export function chartTiles(hours: ChartHour[], hasAirQuality: boolean): ChartTile[] {
	const has: Record<ChartTile, () => boolean> = {
		temperature: () => hours.some((h) => h.temperature != null),
		dewPoint: () => hours.some((h) => h.dewPoint != null),
		precipitation: () => hours.some((h) => h.precipitation != null),
		wind: () => hours.some((h) => h.windSpeed != null),
		windGust: () => hours.some((h) => h.windGust != null),
		pressure: () => hours.some((h) => h.pressure != null),
		humidity: () => hours.some((h) => h.humidity != null),
		uvIndex: () => hours.some((h) => h.uvIndex != null && Number.isFinite(h.uvIndex)),
		airQuality: () => hasAirQuality,
	};
	return CHART_TILES.filter((tile) => has[tile]());
}

/** The label size of the selection card in px, and the number of lines of each label at that size. */
export interface ChartLabelFit {
	size: number;
	lines: number[];
}

/** The label sizes that the selection card tries, from the largest. */
export const CHART_LABEL_SIZES = [11, 10.5, 10, 9.5, 9, 8.5, 8];

/**
 * The largest label size at which each label fits its width: no word is wider than the width (so no word breaks) and
 * the label takes at most 2 lines. All labels get the same size, so the card looks even. `measure` gives the width of a
 * text at a size, in the unit of `widths`. When no size fits, the smallest size, and the labels end with an ellipsis.
 */
export function chartLabelFit(labels: string[], widths: number[], measure: (text: string, size: number) => number): ChartLabelFit {
	// The lines of a label when the words fill each line in turn; null when a word is wider than the width
	const lines = (label: string, width: number, size: number): number | null => {
		let count = 1;
		let line = '';
		for (const word of label.split(' ').filter((w) => w.length > 0)) {
			if (measure(word, size) > width) return null;
			const next = line === '' ? word : `${line} ${word}`;
			if (line === '' || measure(next, size) <= width) line = next;
			else {
				count++;
				line = word;
			}
		}
		return count;
	};
	for (const size of CHART_LABEL_SIZES) {
		const counts = labels.map((label, i) => lines(label, widths[i], size));
		if (counts.every((c) => c != null && c <= 2)) return { size, lines: counts as number[] };
	}
	const size = CHART_LABEL_SIZES[CHART_LABEL_SIZES.length - 1];
	return { size, lines: labels.map((label, i) => Math.min(2, lines(label, widths[i], size) ?? 2)) };
}

/**
 * The number of tiles in each row of the selection card, for `n` tiles: as few rows of at most 5 as possible, with the
 * tiles spread evenly and filled in order (9 gives 5, 4; 7 gives 4, 3).
 */
export function chartTileRows(n: number): number[] {
	if (n <= 0) return [];
	const rows = Math.ceil(n / 5);
	const perRow = Math.ceil(n / rows);
	return Array.from({ length: rows }, (_, r) => Math.min(perRow, n - r * perRow)).filter((s) => s > 0);
}
