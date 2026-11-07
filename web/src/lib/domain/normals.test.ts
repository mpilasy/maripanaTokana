// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

import { describe, expect, it } from 'vitest';
import { dailyNormals, dayOfYear, normalFor, placeIsoDate } from './normals';

/** Every date from `start` for `days` days, as ISO dates. */
function isoDates(start: string, days: number): string[] {
	const t = Date.parse(`${start}T00:00:00Z`);
	return Array.from({ length: days }, (_, i) => new Date(t + i * 86_400_000).toISOString().substring(0, 10));
}

describe('dayOfYear', () => {
	it('gives Mar 1 the same day in all years', () => {
		expect(dayOfYear('2023-03-01')).toBe(61);
		expect(dayOfYear('2024-03-01')).toBe(61);
		expect(dayOfYear('2024-02-29')).toBe(60);
		expect(dayOfYear('2025-01-01')).toBe(1);
		expect(dayOfYear('2025-12-31')).toBe(366);
	});
});

describe('dailyNormals', () => {
	it('takes the mean of the 15 days around each day over all years', () => {
		// Two years where the high is the day of the month and the low is 10 less
		const dates = [...isoDates('2023-06-01', 30), ...isoDates('2024-06-01', 30)];
		const max = dates.map((d) => parseInt(d.substring(8, 10), 10));
		const min = max.map((v) => v - 10);
		const normals = dailyNormals(dates, max, min);
		// Jun 15: Jun 8 to Jun 22, mean 15
		expect(normalFor(normals, '2026-06-15')).toEqual({ max: 15, min: 5 });
		// Jun 2: only Jun 1 to Jun 9 have data, mean 5
		expect(normalFor(normals, '2026-06-02')!.max).toBeCloseTo(5);
	});

	it('goes across the year end', () => {
		const dates = ['2023-12-30', '2024-01-03'];
		const normals = dailyNormals(dates, [10, 20], [0, 4]);
		expect(normalFor(normals, '2026-01-01')).toEqual({ max: 15, min: 2 });
		expect(normalFor(normals, '2026-12-31')).toEqual({ max: 15, min: 2 });
	});

	it('gives Feb 29 the normal of the days around it', () => {
		const normals = dailyNormals(['2023-02-28', '2023-03-01'], [10, 12], [2, 4]);
		expect(normalFor(normals, '2028-02-29')).toEqual({ max: 11, min: 3 });
	});

	it('leaves out missing values and gives null for a day without data', () => {
		const normals = dailyNormals(['2023-06-01', '2023-06-02'], [20, null], [10, 12]);
		expect(normalFor(normals, '2026-06-01')).toEqual({ max: 20, min: 11 });
		expect(normalFor(normals, '2026-12-01')).toBeNull();
	});
});

describe('placeIsoDate', () => {
	it('gives the date at the place', () => {
		// 22:00 UTC on Nov 6 is Nov 7 at UTC+3
		expect(placeIsoDate(Date.UTC(2026, 10, 6, 22), 3 * 3600)).toBe('2026-11-07');
		expect(placeIsoDate(Date.UTC(2026, 10, 6, 22), 0)).toBe('2026-11-06');
	});
});
