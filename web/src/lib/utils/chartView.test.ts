// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

import { describe, expect, it } from 'vitest';
import { chartDaylight, chartDaySpans, chartDomain, chartHours, chartIndexForX, chartMidnights, chartNightRuns, chartVisibleRange, chartZoom } from './chartView';

const HOUR = 3_600_000;
const T0 = Date.UTC(2026, 10, 7, 0, 0); // 2026-11-07 00:00 UTC
const hours = (n: number, from = T0) => Array.from({ length: n }, (_, i) => from + i * HOUR);

describe('chartHours', () => {
	it('starts at the hour that contains now', () => {
		expect(chartHours(hours(48), T0 + 5 * HOUR + 20 * 60_000)).toEqual({ start: 5, end: 48 });
	});
	it('starts at an hour that begins exactly now', () => {
		expect(chartHours(hours(48), T0 + 5 * HOUR)).toEqual({ start: 5, end: 48 });
	});
	it('starts at the first hour when now is before the data', () => {
		expect(chartHours(hours(10), T0 - 3 * HOUR)).toEqual({ start: 0, end: 10 });
	});
	it('keeps at most 240 hours', () => {
		expect(chartHours(hours(264), T0 + 30 * 60_000)).toEqual({ start: 0, end: 240 });
		expect(chartHours(hours(264), T0 + 10 * HOUR)).toEqual({ start: 10, end: 250 });
	});
	it('takes another maximum', () => {
		expect(chartHours(hours(48), T0, 12)).toEqual({ start: 0, end: 12 });
	});
	it('is empty when every hour has ended or there is no hour', () => {
		expect(chartHours(hours(5), T0 + 5 * HOUR)).toEqual({ start: 5, end: 5 });
		expect(chartHours([], T0)).toEqual({ start: 0, end: 0 });
	});
});

describe('chartDaySpans', () => {
	it('splits the hours at local midnight, with the place offset', () => {
		// UTC+3: local midnight is 21:00 UTC
		const spans = chartDaySpans(hours(48, T0 + 18 * HOUR), 3 * 3600);
		expect(spans.map((s) => [s.startIndex, s.endIndex])).toEqual([[0, 3], [3, 27], [27, 48]]);
		expect(spans[1].date).toBe(Date.UTC(2026, 10, 8) - 3 * HOUR);
	});
	it('uses a negative offset', () => {
		// UTC-5: local midnight is 05:00 UTC
		const spans = chartDaySpans(hours(30), -5 * 3600);
		expect(spans.map((s) => [s.startIndex, s.endIndex])).toEqual([[0, 5], [5, 29], [29, 30]]);
		expect(spans[0].date).toBe(Date.UTC(2026, 10, 6) + 5 * HOUR);
	});
	it('gives a day of 23 hours when an hour is missing (start of summer time)', () => {
		// The local times skip 02:00: the second day has 23 hours
		const times = [...hours(24), ...hours(24, T0 + 24 * HOUR).filter((_, i) => i !== 2), ...hours(5, T0 + 48 * HOUR)];
		expect(chartDaySpans(times, 0).map((s) => s.endIndex - s.startIndex)).toEqual([24, 23, 5]);
	});
	it('gives a day of 25 hours when an hour comes two times (end of summer time)', () => {
		const day2 = hours(24, T0 + 24 * HOUR);
		const times = [...hours(24), ...day2.slice(0, 3), day2[2] + 30 * 60_000, ...day2.slice(3), T0 + 48 * HOUR];
		expect(chartDaySpans(times, 0).map((s) => s.endIndex - s.startIndex)).toEqual([24, 25, 1]);
	});
	it('is empty with no hour', () => {
		expect(chartDaySpans([], 3600)).toEqual([]);
	});
});

describe('chartDaylight', () => {
	const sunrises = [T0 + 6 * HOUR, T0 + 30 * HOUR];
	const sunsets = [T0 + 18 * HOUR, T0 + 42 * HOUR];
	it('marks the hours outside sunrise to sunset as night', () => {
		const night = chartDaylight(hours(48), sunrises, sunsets);
		expect(night.slice(0, 6).every(Boolean)).toBe(true);
		expect(night.slice(6, 19).some(Boolean)).toBe(false);
		expect(night.slice(19, 30).every(Boolean)).toBe(true);
		expect(night[30]).toBe(false);
		expect(night[43]).toBe(true);
	});
	it('makes every hour night in a polar night and day in a midnight sun', () => {
		expect(chartDaylight(hours(24), [T0], [T0]).every(Boolean)).toBe(true);
		expect(chartDaylight(hours(24), [T0], [T0 + 24 * HOUR]).some(Boolean)).toBe(false);
	});
	it('makes every hour night with no sun times', () => {
		expect(chartDaylight(hours(3), [], [])).toEqual([true, true, true]);
	});
});

describe('chartMidnights', () => {
	it('gives the first hour of each local day after the first one', () => {
		// 30 hours from 22:00 UTC; at UTC+3 local midnight is hour 23
		expect(chartMidnights(hours(30, T0 - 2 * HOUR), 3 * 3600)).toEqual([23]);
	});
	it('does not count the first hour, even at midnight', () => {
		expect(chartMidnights(hours(50), 0)).toEqual([24, 48]);
	});
	it('is empty within one day or with no hour', () => {
		expect(chartMidnights(hours(10), 0)).toEqual([]);
		expect(chartMidnights([], 0)).toEqual([]);
	});
});

describe('chartNightRuns', () => {
	it('gives each run of night hours, in order', () => {
		expect(chartNightRuns([true, true, false, false, true, false, true, true])).toEqual([
			{ start: 0, end: 2 }, { start: 4, end: 5 }, { start: 6, end: 8 },
		]);
	});
	it('is empty with no night hour', () => {
		expect(chartNightRuns([false, false])).toEqual([]);
		expect(chartNightRuns([])).toEqual([]);
	});
});

describe('chartVisibleRange', () => {
	it('gives the hours in view and 2 more on each side', () => {
		expect(chartVisibleRange(280, 336, 28, 240)).toEqual({ start: 8, end: 24 });
	});
	it('stays inside the hours', () => {
		expect(chartVisibleRange(0, 336, 28, 240)).toEqual({ start: 0, end: 14 });
		expect(chartVisibleRange(28 * 230, 336, 28, 240)).toEqual({ start: 228, end: 240 });
		expect(chartVisibleRange(-50, 100, 28, 5, 0)).toEqual({ start: 0, end: 4 });
	});
	it('takes another overscan', () => {
		expect(chartVisibleRange(28, 56, 28, 240, 0)).toEqual({ start: 1, end: 3 });
	});
	it('is empty with no hour', () => {
		expect(chartVisibleRange(0, 300, 28, 0)).toEqual({ start: 0, end: 0 });
	});
});

describe('chartIndexForX', () => {
	it('gives the hour under x', () => {
		expect(chartIndexForX(0, 28, 240)).toBe(0);
		expect(chartIndexForX(27.9, 28, 240)).toBe(0);
		expect(chartIndexForX(28, 28, 240)).toBe(1);
		expect(chartIndexForX(100, 12, 240)).toBe(8);
	});
	it('stays inside the hours', () => {
		expect(chartIndexForX(-10, 28, 240)).toBe(0);
		expect(chartIndexForX(28 * 500, 28, 240)).toBe(239);
	});
	it('is -1 with no hour', () => {
		expect(chartIndexForX(10, 28, 0)).toBe(-1);
	});
});

describe('chartDomain', () => {
	it('puts the ends on round ticks', () => {
		expect(chartDomain([3.2, 11.7, 7], false, 0)).toEqual({ min: 0, max: 15, ticks: [0, 5, 10, 15] });
		expect(chartDomain([12.5, 17.5], false, 0)).toEqual({ min: 12, max: 18, ticks: [12, 14, 16, 18] });
	});
	it('ignores nulls and values that are not numbers', () => {
		expect(chartDomain([null, 1012, undefined, NaN, 1018], false, 0)).toEqual(chartDomain([1012, 1018], false, 0));
	});
	it('is null with no value', () => {
		expect(chartDomain([null, NaN], true, 1)).toBeNull();
		expect(chartDomain([], false, 1)).toBeNull();
	});
	it('starts at 0 with fromZero', () => {
		const d = chartDomain([4, 9], true, 0)!;
		expect(d.min).toBe(0);
		expect(d.max).toBeGreaterThanOrEqual(9);
		expect(d.ticks[0]).toBe(0);
	});
	it('is at least minSpan high, up from 0 with fromZero', () => {
		expect(chartDomain([0, 0, 0], true, 1)).toEqual({ min: 0, max: 1, ticks: [0, 0.5, 1] });
	});
	it('is at least minSpan high, around the middle without fromZero', () => {
		const d = chartDomain([20, 20], false, 4)!;
		expect(d.min).toBeLessThanOrEqual(18);
		expect(d.max).toBeGreaterThanOrEqual(22);
		expect(d.max - d.min).toBeLessThan(8);
	});
	it('uses steps of 1, 2 or 5 only, never 2.5', () => {
		expect(chartDomain([0, 7], false, 0)).toEqual({ min: 0, max: 10, ticks: [0, 5, 10] });
		expect(chartDomain([1020.4, 1023.1], false, 4)).toEqual({ min: 1018, max: 1024, ticks: [1018, 1020, 1022, 1024] });
		expect(chartDomain([29.9, 30.0], false, 0.12)!.ticks).toEqual([29.85, 29.9, 29.95, 30, 30.05]);
	});
	it('gives clean ticks for small and negative values', () => {
		expect(chartDomain([-7.4, 2.1], false, 0)).toEqual({ min: -10, max: 5, ticks: [-10, -5, 0, 5] });
		expect(chartDomain([0.03, 0.27], false, 0)!.ticks).toEqual([0, 0.1, 0.2, 0.3]);
	});
});

describe('chartZoom', () => {
	it('keeps the hour at the anchor in place', () => {
		// Hour 15 is at x = 100 in view (scroll 320 + 100 = 420 = 15 * 28)
		const z = chartZoom(28, 2, 100, 320);
		expect(z.slotPx).toBe(56);
		expect((z.scrollPx + 100) / z.slotPx).toBeCloseTo(15);
	});
	it('stays inside 12 and 64', () => {
		expect(chartZoom(28, 10, 0, 0).slotPx).toBe(64);
		expect(chartZoom(28, 0.1, 0, 0).slotPx).toBe(12);
		expect(chartZoom(30, 1, 0, 0, 40, 50).slotPx).toBe(40);
	});
	it('keeps the anchor at the limit too', () => {
		const z = chartZoom(28, 10, 50, 1000);
		expect((z.scrollPx + 50) / 64).toBeCloseTo((1000 + 50) / 28);
	});
	it('does not scroll before the start', () => {
		expect(chartZoom(28, 0.5, 200, 10).scrollPx).toBe(0);
	});
});
