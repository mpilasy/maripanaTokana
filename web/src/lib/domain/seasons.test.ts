// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

import { describe, expect, it } from 'vitest';
import { daysUntil, declination, eventTime, noonOffset, noonRange, seasonNameIndex, seasonsAround } from './seasons';

// Reference values from Meeus's examples and from the US Naval Observatory, as in SeasonsTest.kt
const MINUTE = 60_000;

describe('seasons', () => {
	it('declination', () => {
		// Meeus example 25.a: 1992 October 13 at 0h, -7° 47' 06"
		expect(declination(Date.parse('1992-10-13T00:00:00Z'))).toBeCloseTo(-7.78507, 3);
		// Near the solstices the sun is over the tropics
		expect(declination(Date.parse('2026-12-21T20:50:00Z'))).toBeCloseTo(-23.44, 1);
		expect(declination(Date.parse('2027-06-21T14:11:00Z'))).toBeCloseTo(23.44, 1);
	});

	it('event times', () => {
		// Meeus example 27.a: the June solstice of 1962, 21:25 dynamical time
		expect(Math.abs(eventTime(1962, 1) + 69_000 - Date.parse('1962-06-21T21:25:08Z'))).toBeLessThan(MINUTE);
		// US Naval Observatory, UTC
		expect(Math.abs(eventTime(2024, 3) - Date.parse('2024-12-21T09:20Z'))).toBeLessThan(2 * MINUTE);
		expect(Math.abs(eventTime(2026, 3) - Date.parse('2026-12-21T20:50Z'))).toBeLessThan(2 * MINUTE);
		expect(Math.abs(eventTime(2027, 0) - Date.parse('2027-03-20T20:25Z'))).toBeLessThan(2 * MINUTE);
		expect(Math.abs(eventTime(2030, 1) - Date.parse('2030-06-21T07:31Z'))).toBeLessThan(2 * MINUTE);
	});

	it('noon offset and range', () => {
		// At the equator on the June solstice the noon sun is 23.44° to the north (negative)
		expect(noonOffset(0, Date.parse('2027-06-21T12:00:00Z'))).toBeCloseTo(-23.44, 1);
		// Roseville (45° N): 21.6° to 68.4° to the south; the equator: 23.44° north to 23.44° south
		expect(noonRange(45)).toEqual([45 - 23.44, 45 + 23.44]);
		expect(noonRange(0)).toEqual([-23.44, 23.44]);
		// Antananarivo (18.9° S): 4.5° south to 42.3° north, the one nearer overhead first
		const [near, far] = noonRange(-18.9);
		expect(near).toBeCloseTo(4.54, 2);
		expect(far).toBeCloseTo(-42.34, 2);
	});

	it('season names and days until', () => {
		expect(seasonNameIndex(3, 45)).toBe(3); // December: winter solstice in the north
		expect(seasonNameIndex(3, -18.9)).toBe(1); // summer solstice in the south
		expect(seasonNameIndex(0, -18.9)).toBe(2); // March: autumn equinox in the south
		// Days at the place: 23:00 to 01:00 the next day is one day, 01:00 to 23:00 the same day is none
		const offset = -6 * 3600;
		expect(daysUntil(Date.parse('2026-11-10T05:00:00Z'), Date.parse('2026-11-10T07:00:00Z'), offset)).toBe(1);
		expect(daysUntil(Date.parse('2026-11-10T07:00:00Z'), Date.parse('2026-11-11T05:00:00Z'), offset)).toBe(0);
	});

	it('around', () => {
		const a = seasonsAround(Date.parse('2026-11-10T00:00:00Z'));
		expect(a.previous.kind).toBe(2);
		expect(a.next.kind).toBe(3);
		expect(a.next.time).toBe(eventTime(2026, 3));
		// Across the new year: the December solstice before, the March equinox after
		const b = seasonsAround(Date.parse('2027-01-05T00:00:00Z'));
		expect(b.previous).toEqual({ time: eventTime(2026, 3), kind: 3 });
		expect(b.next).toEqual({ time: eventTime(2027, 0), kind: 0 });
	});
});
