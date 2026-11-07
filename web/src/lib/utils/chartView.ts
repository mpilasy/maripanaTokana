// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

// Pure functions of the chart view and the charts on the main page. Android has the same functions with the same
// rules in ChartViewMath.kt.

import { isDaytime } from '#lib/domain/weatherData.js';

const HOUR = 3_600_000;
const DAY = 24 * HOUR;

/** The most hours that the chart view shows (10 days). */
export const CHART_MAX_HOURS = 240;
/** The default, smallest and largest width of one hour, in px. */
export const CHART_SLOT_DEFAULT = 28;
export const CHART_SLOT_MIN = 12;
export const CHART_SLOT_MAX = 64;

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

/**
 * The hour under the centre line of the chart view. The scroll (`scrollPx`) is the position under the centre line,
 * in px from the start of the first hour. -1 when there is no hour.
 */
export function chartCenterIndex(scrollPx: number, slotPx: number, count: number): number {
	return chartIndexForX(scrollPx, slotPx, count);
}

/** The scroll that puts the middle of hour `index` under the centre line. */
export function chartScrollForIndex(index: number, slotPx: number): number {
	return (index + 0.5) * slotPx;
}

/** The scroll range: from the middle of the first hour to the middle of the last hour under the centre line. */
export function chartScrollRange(slotPx: number, count: number): { min: number; max: number } {
	if (count <= 0 || slotPx <= 0) return { min: 0, max: 0 };
	return { min: chartScrollForIndex(0, slotPx), max: chartScrollForIndex(count - 1, slotPx) };
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

/** A new hour width and scroll after a zoom. */
export interface ChartZoom {
	slotPx: number;
	scrollPx: number;
}

/**
 * Zooms the hour width by `factor`, kept inside `min` to `max`, and gives the scroll that keeps the hour at `anchorX`
 * (px from the start of the view) in the same place. The scroll is not less than 0.
 */
export function chartZoom(slotPx: number, factor: number, anchorX: number, scrollPx: number, min = CHART_SLOT_MIN, max = CHART_SLOT_MAX): ChartZoom {
	const next = Math.min(max, Math.max(min, slotPx * factor));
	const hourAtAnchor = (scrollPx + anchorX) / slotPx;
	return { slotPx: next, scrollPx: Math.max(0, hourAtAnchor * next - anchorX) };
}
