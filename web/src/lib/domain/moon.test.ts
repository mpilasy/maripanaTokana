// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

import { describe, expect, it } from 'vitest';
import { moonArcProgress, moonIllumination, moonPhaseIndex, moonPosition, moonTimes, nextMoonPhase, polarPeriod } from './moon';

// Reference values from SunCalc's own tests (2013-03-05 UTC, 50.5° N, 30.5° E), as in MoonTest.kt
const lat = 50.5;
const lon = 30.5;
const date = Date.parse('2013-03-05T00:00:00Z');

describe('moon', () => {
	it('position', () => {
		const p = moonPosition(date, lat, lon);
		expect(p.azimuth).toBeCloseTo((-0.9783999522438226 * 180) / Math.PI + 180, 6);
		expect(p.altitude).toBeCloseTo((0.014551482243892251 * 180) / Math.PI, 6);
	});

	it('illumination', () => {
		const i = moonIllumination(date);
		expect(i.fraction).toBeCloseTo(0.4848068202456373, 9);
		expect(i.phase).toBeCloseTo(0.7548368838538762, 9);
		expect(moonPhaseIndex(i.phase)).toBe(6); // last quarter
	});

	it('times', () => {
		const t = moonTimes(Date.parse('2013-03-04T00:00:00Z'), lat, lon);
		expect(Math.abs(t.rise! - Date.parse('2013-03-04T23:54:29Z'))).toBeLessThan(1000);
		expect(Math.abs(t.set! - Date.parse('2013-03-04T07:47:58Z'))).toBeLessThan(1000);
	});

	it('phase index wraps at new moon', () => {
		expect(moonPhaseIndex(0.98)).toBe(0);
		expect(moonPhaseIndex(0.5)).toBe(4);
	});

	it('next phase finds the new and full moon', () => {
		// January 2024: new moon on the 11th at 11:57 UTC, full moon on the 25th at 17:54 UTC; the formulas are
		// good to some hours
		const from = Date.parse('2024-01-01T00:00:00Z');
		expect(Math.abs(nextMoonPhase(from, 0) - Date.parse('2024-01-11T11:57:00Z'))).toBeLessThan(8 * 3_600_000);
		expect(Math.abs(nextMoonPhase(from, 0.5) - Date.parse('2024-01-25T17:54:00Z'))).toBeLessThan(8 * 3_600_000);
		// From just after a full moon: the next one, about a month later
		const full = nextMoonPhase(from, 0.5);
		expect(Math.abs(nextMoonPhase(full + 60_000, 0.5) - full - 29.5 * 86_400_000)).toBeLessThan(86_400_000);
	});

	it('arc progress: halfway between rise and set, -1 while down', () => {
		const days = [{ rise: 1000, set: 3000 }];
		expect(moonArcProgress(2000, days)).toBe(0.5);
		expect(moonArcProgress(3500, days)).toBe(-1);
	});
});

describe('polarPeriod', () => {
	const day = (iso: string) => Date.parse(iso + 'T12:00:00Z');
	const near = (ms: number, iso: string) => expect(Math.abs(ms - day(iso))).toBeLessThanOrEqual(2 * 86_400_000);
	it('finds the polar night at Alert', () => {
		const p = polarPeriod(day('2026-10-29'), 82.5, false);
		near(p.since, '2026-10-14');
		near(p.until, '2027-02-27');
	});
	it('finds the midnight sun at the South Pole', () => {
		const p = polarPeriod(day('2026-10-29'), -89.99, true);
		near(p.since, '2026-09-22');
		near(p.until, '2027-03-21');
	});
});
