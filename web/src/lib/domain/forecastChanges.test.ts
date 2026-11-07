// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

import { describe, expect, it } from 'vitest';
import { forecastChanges, snapshotToCompare, type ForecastSnapshot, type SnapshotDay } from './forecastChanges';

const DATES = ['2026-11-07', '2026-11-08', '2026-11-09', '2026-11-10', '2026-11-11', '2026-11-12', '2026-11-13', '2026-11-14'];

function day(i: number, values: Partial<SnapshotDay> = {}): SnapshotDay {
	return { date: DATES[i], highC: 20, lowC: 10, rainMm: 0, windMaxKmh: 10, ...values };
}

function snapshot(days: SnapshotDay[], savedAt = 0): ForecastSnapshot {
	return { savedAt, days };
}

const base = DATES.map((_, i) => day(i));

describe('forecastChanges', () => {
	it('finds no change in the same forecast', () => {
		expect(forecastChanges(snapshot(base), snapshot(base))).toEqual([]);
	});

	it('finds a high 3 °C warmer or cooler, not less', () => {
		const current = base.map((d, i) => (i === 1 ? { ...d, highC: 23 } : i === 2 ? { ...d, highC: 17.5 } : i === 3 ? { ...d, highC: 17 } : d));
		expect(forecastChanges(snapshot(base), snapshot(current))).toEqual([
			{ date: DATES[1], kind: 'high_warmer', value: 3 },
			{ date: DATES[3], kind: 'high_cooler', value: 3 },
		]);
	});

	it('finds rain that comes, rain that goes, and more or less rain', () => {
		const old = base.map((d, i) => (i === 2 ? { ...d, rainMm: 3 } : i === 3 ? { ...d, rainMm: 10 } : i === 4 ? { ...d, rainMm: 10 } : i === 5 ? { ...d, rainMm: 20 } : d));
		const current = old.map((d, i) => (i === 1 ? { ...d, rainMm: 1 } : i === 2 ? { ...d, rainMm: 0.5 } : i === 3 ? { ...d, rainMm: 16 } : i === 4 ? { ...d, rainMm: 4 } : i === 5 ? { ...d, rainMm: 27 } : d));
		expect(forecastChanges(snapshot(old), snapshot(current))).toEqual([
			{ date: DATES[1], kind: 'rain_now', value: 1 },
			{ date: DATES[2], kind: 'dry_now', value: 0.5 },
			{ date: DATES[3], kind: 'rain_more', value: 16 },
		{ date: DATES[4], kind: 'rain_less', value: 4 },
		]);
		// Day 5: 7 mm more is less than half of 20 mm, so no change (and at most 4 lines)
	});

	it('finds a maximum wind 15 km/h stronger or weaker', () => {
		const current = base.map((d, i) => (i === 1 ? { ...d, windMaxKmh: 25 } : i === 2 ? { ...d, windMaxKmh: 0 } : i === 3 ? { ...d, windMaxKmh: 24 } : d));
		const old = base.map((d, i) => (i === 2 ? { ...d, windMaxKmh: 15 } : d));
		expect(forecastChanges(snapshot(old), snapshot(current))).toEqual([
			{ date: DATES[1], kind: 'wind_stronger', value: 25 },
			{ date: DATES[2], kind: 'wind_weaker', value: 0 },
		]);
	});

	it('orders by date, then high, rain, wind, and keeps 4', () => {
		const current = base.map((d, i) => (i === 1 ? { ...d, highC: 25, rainMm: 5, windMaxKmh: 40 } : i === 2 ? { ...d, highC: 10, rainMm: 8 } : d));
		expect(forecastChanges(snapshot(base), snapshot(current)).map((c) => `${c.date} ${c.kind}`)).toEqual([
			`${DATES[1]} high_warmer`,
			`${DATES[1]} rain_now`,
			`${DATES[1]} wind_stronger`,
			`${DATES[2]} high_cooler`,
		]);
	});

	it('compares only tomorrow up to 6 days ahead, and only dates in both', () => {
		const current = base.map((d, i) => (i === 0 || i === 7 ? { ...d, highC: 30 } : d));
		expect(forecastChanges(snapshot(base), snapshot(current))).toEqual([]);
		// The old forecast started a day earlier: its dates are matched by date, not by position
		const old = snapshot([day(0, { date: '2026-11-06' }), ...base.slice(0, 7).map((d, i) => (i === 1 ? { ...d, highC: 14 } : d))]);
		expect(forecastChanges(old, snapshot(base))).toEqual([{ date: DATES[1], kind: 'high_warmer', value: 6 }]);
	});

	it('leaves out missing values', () => {
		const current = base.map((d, i) => (i === 1 ? { ...d, highC: null, rainMm: null, windMaxKmh: null } : d));
		const old = base.map((d, i) => (i === 1 ? { ...d, highC: 0, rainMm: 50, windMaxKmh: 90 } : d));
		expect(forecastChanges(snapshot(old), snapshot(current))).toEqual([]);
	});
});

describe('snapshotToCompare', () => {
	const HOUR = 3_600_000;
	const now = 100 * HOUR;
	it('takes the one closest to 24 hours old', () => {
		const list = [snapshot([], now - 30 * HOUR), snapshot([], now - 23 * HOUR), snapshot([], now - 20 * HOUR)];
		expect(snapshotToCompare(list, now)?.savedAt).toBe(now - 23 * HOUR);
	});
	it('takes none outside 18 to 36 hours', () => {
		expect(snapshotToCompare([snapshot([], now - 17 * HOUR), snapshot([], now - 37 * HOUR)], now)).toBeNull();
		expect(snapshotToCompare([snapshot([], now - 36 * HOUR)], now)?.savedAt).toBe(now - 36 * HOUR);
	});
});
