// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

import { describe, expect, it } from 'vitest';
import { moonArcProgress, moonIllumination, moonPhaseIndex, moonPosition, moonTimes } from './moon';

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

	it('arc progress: halfway between rise and set, -1 while down', () => {
		const days = [{ rise: 1000, set: 3000 }];
		expect(moonArcProgress(2000, days)).toBe(0.5);
		expect(moonArcProgress(3500, days)).toBe(-1);
	});
});
