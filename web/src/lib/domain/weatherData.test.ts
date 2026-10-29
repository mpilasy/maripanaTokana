// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

import { describe, expect, it } from 'vitest';
import { isDaytime } from './weatherData.js';

const midnight = 1_793_232_000; // a day start, in seconds

describe('isDaytime', () => {
	it('is day between sunrise and sunset', () => {
		expect(isDaytime(midnight + 12 * 3600, midnight + 6 * 3600, midnight + 18 * 3600)).toBe(true);
		expect(isDaytime(midnight + 2 * 3600, midnight + 6 * 3600, midnight + 18 * 3600)).toBe(false);
	});
	it('has no day in a polar night (sunrise and sunset both 00:00)', () => {
		expect(isDaytime(midnight, midnight, midnight)).toBe(false);
		expect(isDaytime(midnight + 12 * 3600, midnight, midnight)).toBe(false);
	});
	it('is all day in a midnight sun', () => {
		expect(isDaytime(midnight, midnight, midnight + 86_400)).toBe(true);
		expect(isDaytime(midnight + 23 * 3600, midnight, midnight + 86_400)).toBe(true);
	});
});
