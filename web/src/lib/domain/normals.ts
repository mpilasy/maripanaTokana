// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

/** The normal high and low of one day of the year, in °C. */
export interface DayNormal {
	max: number;
	min: number;
}

/** Days on each side of a day that go into its normal. */
const WINDOW_DAYS = 7;
/** Days in the leap-year calendar that holds every month and day, Feb 29 included. */
const DAYS_IN_YEAR = 366;
const MONTH_START = [0, 31, 60, 91, 121, 152, 182, 213, 244, 274, 305, 335];

/**
 * The day of the year (1–366) of an ISO date ("2025-03-01"), in a leap-year calendar: Mar 1 is
 * always day 61, so a date has the same day in all years. In other years, day 60 (Feb 29) has no data.
 */
export function dayOfYear(isoDate: string): number {
	const month = parseInt(isoDate.substring(5, 7), 10);
	const day = parseInt(isoDate.substring(8, 10), 10);
	return MONTH_START[month - 1] + day;
}

/**
 * The normal high and low per day of the year: the mean of the daily highs and of the daily lows of all
 * years in the 15 days around the day (7 on each side, across the year end). Index 0 is day 1;
 * null for a day without data. Feb 29 takes its normal from the days around it (Feb 28, Mar 1, ...).
 */
export function dailyNormals(dates: string[], max: (number | null)[], min: (number | null)[]): (DayNormal | null)[] {
	const sumMax = new Array<number>(DAYS_IN_YEAR).fill(0);
	const countMax = new Array<number>(DAYS_IN_YEAR).fill(0);
	const sumMin = new Array<number>(DAYS_IN_YEAR).fill(0);
	const countMin = new Array<number>(DAYS_IN_YEAR).fill(0);
	dates.forEach((date, i) => {
		const slot = dayOfYear(date) - 1;
		const high = max[i];
		const low = min[i];
		if (high != null && Number.isFinite(high)) { sumMax[slot] += high; countMax[slot]++; }
		if (low != null && Number.isFinite(low)) { sumMin[slot] += low; countMin[slot]++; }
	});
	return Array.from({ length: DAYS_IN_YEAR }, (_, slot) => {
		let hs = 0, hc = 0, ls = 0, lc = 0;
		for (let d = -WINDOW_DAYS; d <= WINDOW_DAYS; d++) {
			const s = (slot + d + DAYS_IN_YEAR) % DAYS_IN_YEAR;
			hs += sumMax[s]; hc += countMax[s];
			ls += sumMin[s]; lc += countMin[s];
		}
		return hc > 0 && lc > 0 ? { max: hs / hc, min: ls / lc } : null;
	});
}

/** The normal of an ISO date ("2026-11-07"); null when there is none. */
export function normalFor(normals: (DayNormal | null)[], isoDate: string): DayNormal | null {
	return normals[dayOfYear(isoDate) - 1] ?? null;
}

/** The ISO date ("2026-11-07") at the place of a time (epoch millis), with the place's UTC offset. */
export function placeIsoDate(epochMillis: number, utcOffsetSeconds: number): string {
	return new Date(epochMillis + utcOffsetSeconds * 1000).toISOString().substring(0, 10);
}

/** The smallest difference (°C) between the forecast high and the normal high that the hero shows. */
export const NORMAL_DELTA_MIN_C = 2;
