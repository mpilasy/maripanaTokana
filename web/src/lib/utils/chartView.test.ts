// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

import { describe, expect, it } from 'vitest';
import { Temperature } from '#lib/domain/temperature.js';
import { WindSpeed } from '#lib/domain/windSpeed.js';
import type { ChartHour } from '#lib/domain/weatherData.js';
import { chartAirQuality, chartLabelFit, chartTileRows, chartTiles, chartDayExtremes, chartDaylight, chartDaySpans, chartDomain, chartHours, chartIndexForX, chartLineIndex, chartLineOffset, chartMaxScroll, chartScrollForIndex, chartValueAt, chartMidnights, chartNightRuns, chartVisibleRange } from './chartView';

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

// The selection line (28 px an hour, 240 hours, a plot of 308 px: the line moves 140 px to the middle)
describe('chartLineOffset', () => {
	it('moves from the start to the middle to the end', () => {
		const max = chartMaxScroll(28, 240, 308);
		expect(max).toBe(240 * 28 - 308);
		expect(chartLineOffset(0, 28, 240, 308)).toBe(14);
		expect(chartLineOffset(70, 28, 240, 308)).toBe(84);
		expect(chartLineOffset(140, 28, 240, 308)).toBe(154);
		expect(chartLineOffset(3000, 28, 240, 308)).toBe(154);
		expect(chartLineOffset(max - 140, 28, 240, 308)).toBe(154);
		expect(chartLineOffset(max, 28, 240, 308)).toBe(294);
	});
	it('works with a short scroll, and stays on the first hour when all the hours fit', () => {
		expect(chartLineOffset(0, 28, 20, 308)).toBe(14);
		expect(chartLineOffset(126, 28, 20, 308)).toBe(154);
		expect(chartLineOffset(252, 28, 20, 308)).toBe(294);
		expect(chartMaxScroll(28, 10, 308)).toBe(0);
		expect(chartLineOffset(0, 28, 10, 308)).toBe(14);
		expect(chartScrollForIndex(5, 28, 10, 308)).toBe(0);
	});
});

describe('chartLineIndex and chartScrollForIndex', () => {
	it('gives the first and last hour at the ends', () => {
		expect(chartLineIndex(0, 28, 240, 308)).toBe(0);
		expect(chartLineIndex(chartMaxScroll(28, 240, 308), 28, 240, 308)).toBe(239);
		expect(chartLineIndex(0, 28, 0, 308)).toBe(-1);
	});
	it('puts the middle of an hour under the line', () => {
		for (const count of [20, 240]) {
			for (const i of [0, 1, 3, 6, 10, count / 2, count - 7, count - 2, count - 1]) {
				const scroll = chartScrollForIndex(i, 28, count, 308);
				expect(scroll + chartLineOffset(scroll, 28, count, 308)).toBeCloseTo((i + 0.5) * 28, 2);
				expect(chartLineIndex(scroll, 28, count, 308)).toBe(i);
			}
		}
		expect(chartScrollForIndex(0, 28, 240, 308)).toBe(0);
		expect(chartScrollForIndex(239, 28, 240, 308)).toBeCloseTo(chartMaxScroll(28, 240, 308), 2);
	});
});

describe('chartValueAt', () => {
	it('gives the value of an hour at its middle', () => {
		expect(chartValueAt([1, 5, 9], 14, 28)).toBe(1);
		expect(chartValueAt([1, 5, 9], 42, 28)).toBe(5);
	});
	it('is on the straight line between two hours', () => {
		expect(chartValueAt([1, 5, 9], 28, 28)).toBe(3);
		expect(chartValueAt([1, 5, 9], 63, 28)).toBe(8);
	});
	it('stays at the first and last value outside the hours', () => {
		expect(chartValueAt([1, 5, 9], 0, 28)).toBe(1);
		expect(chartValueAt([1, 5, 9], 28 * 10, 28)).toBe(9);
	});
	it('takes the nearer hour when one is missing, else null', () => {
		expect(chartValueAt([1, null, 9], 30, 28)).toBeNull();
		expect(chartValueAt([1, null, 9], 20, 28)).toBe(1);
		expect(chartValueAt([1, null, 9], 60, 28)).toBe(9);
		expect(chartValueAt([], 10, 28)).toBeNull();
	});
});

describe('chartDayExtremes', () => {
	const span = (startIndex: number, endIndex: number) => ({ startIndex, endIndex });
	it('gives the highest and the lowest index of each day', () => {
		const values = Array.from({ length: 24 }, (_, i) => 10 + Math.sin((i / 24) * 2 * Math.PI) * 5);
		expect(chartDayExtremes(values, [span(0, 24)])).toEqual([{ high: 6, low: 18 }]);
	});
	it('takes the middle one of the tied indexes', () => {
		const values = [1, 5, 5, 5, 5, 3, 0, 0, 0, 2, 4, 4];
		expect(chartDayExtremes(values, [span(0, 12)])).toEqual([{ high: 3, low: 7 }]);
		expect(chartDayExtremes([1, 9, 9, 0, 0, 5, 5, 5, 5, 5, 5, 5], [span(0, 12)])).toEqual([{ high: 2, low: 4 }]);
	});
	it('leaves out a day with fewer hours than the minimum', () => {
		const values = Array.from({ length: 36 }, (_, i) => i);
		expect(chartDayExtremes(values, [span(0, 11), span(11, 36)])).toEqual([{ high: 35, low: 11 }]);
		expect(chartDayExtremes(values, [span(0, 11)], 11)).toEqual([{ high: 10, low: 0 }]);
	});
	it('ignores missing values and leaves out a day with none', () => {
		const values: (number | null)[] = [null, 4, NaN, 7, ...Array(8).fill(null), ...Array(12).fill(null)];
		expect(chartDayExtremes(values, [span(0, 12), span(12, 24)])).toEqual([{ high: 3, low: 1 }]);
	});
	it('gives nothing without days', () => {
		expect(chartDayExtremes([1, 2, 3], [])).toEqual([]);
	});
});

describe('chartAirQuality', () => {
	it('matches each hour to the entry less than 30 minutes away', () => {
		const aq = [T0 + 10 * 60_000, T0 + HOUR - 5 * 60_000, T0 + 3 * HOUR];
		expect(chartAirQuality(hours(4), aq)).toEqual([0, 1, null, 2]);
	});
	it('matches at exactly 30 minutes away, as in a UTC+5:30 zone', () => {
		expect(chartAirQuality([T0], [T0 + 30 * 60_000, T0 - 30 * 60_000])).toEqual([0]);
	});
	it('gives null at more than 30 minutes away', () => {
		expect(chartAirQuality([T0], [T0 + 31 * 60_000, T0 - 31 * 60_000])).toEqual([null]);
	});
	it('takes the first entry that is near', () => {
		expect(chartAirQuality([T0], [T0 + 20 * 60_000, T0])).toEqual([0]);
	});
	it('gives null for each hour without air quality', () => {
		expect(chartAirQuality(hours(3), [])).toEqual([null, null, null]);
		expect(chartAirQuality([], hours(3))).toEqual([]);
	});
});

describe('chartTiles', () => {
	const hour = (time: number): ChartHour => ({
		time, temperature: Temperature.fromCelsius(10), dewPoint: null, weatherCode: 0, isNight: false, precipitation: null,
		precipProbability: null, windSpeed: null, windDeg: null, windGust: null, pressure: null, humidity: 50, uvIndex: null,
	});
	it('keeps each value that one hour has', () => {
		const list = [hour(T0), { ...hour(T0 + HOUR), windGust: WindSpeed.fromMetersPerSecond(5), uvIndex: NaN }];
		expect(chartTiles(list, false)).toEqual(['temperature', 'windGust', 'humidity']);
		expect(chartTiles(list, true)).toEqual(['temperature', 'windGust', 'humidity', 'airQuality']);
		expect(chartTiles([], false)).toEqual([]);
	});
});

describe('chartLabelFit', () => {
	// Each character is half the size wide
	const measure = (text: string, size: number) => (text.length * size) / 2;
	it('keeps the largest size that fits', () => {
		expect(chartLabelFit(['Wind', "Tahan'ny tara-masoandro"], [60, 78], measure)).toEqual({ size: 11, lines: [1, 2] });
	});
	it('makes all labels smaller when a word is too wide', () => {
		// "Precipitation" is 71.5 wide at 11 and 58.5 at 9
		expect(chartLabelFit(['Wind', 'Precipitation'], [60, 60], measure)).toEqual({ size: 9, lines: [1, 1] });
	});
	it('gives the smallest size when no size fits', () => {
		// 3 lines at every size
		expect(chartLabelFit(['Air quality index'], [40], measure)).toEqual({ size: 8, lines: [2] });
		expect(chartLabelFit([], [], measure)).toEqual({ size: 11, lines: [] });
	});
});

describe('chartTileRows', () => {
	it('spreads the tiles over as few rows of at most 5 as possible', () => {
		expect(chartTileRows(9)).toEqual([5, 4]);
		expect(chartTileRows(8)).toEqual([4, 4]);
		expect(chartTileRows(7)).toEqual([4, 3]);
		expect(chartTileRows(6)).toEqual([3, 3]);
		expect(chartTileRows(10)).toEqual([5, 5]);
		expect(chartTileRows(11)).toEqual([4, 4, 3]);
	});
	it('keeps up to 5 tiles in one row', () => {
		expect(chartTileRows(1)).toEqual([1]);
		expect(chartTileRows(5)).toEqual([5]);
	});
	it('gives no row without tiles', () => {
		expect(chartTileRows(0)).toEqual([]);
	});
});
