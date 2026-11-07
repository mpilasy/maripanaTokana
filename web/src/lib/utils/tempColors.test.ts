// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

import { describe, expect, it } from 'vitest';
import { temperatureColor } from './tempColors';

describe('temperatureColor', () => {
	it('gives the stop colour on a stop', () => {
		expect(temperatureColor(-20)).toBe('#B39DDB');
		expect(temperatureColor(0)).toBe('#4FC3F7');
		expect(temperatureColor(15)).toBe('#81C784');
		expect(temperatureColor(40)).toBe('#E57373');
	});

	it('mixes the two stops around the temperature', () => {
		// Half way from #81C784 (15 °C) to #DCE775 (20 °C)
		expect(temperatureColor(17.5)).toBe('#AFD77D');
		// A quarter of the way from #4FC3F7 (0 °C) to #4DD0E1 (10 °C)
		expect(temperatureColor(2.5)).toBe('#4FC6F2');
	});

	it('keeps the end colours outside the scale', () => {
		expect(temperatureColor(-45)).toBe('#B39DDB');
		expect(temperatureColor(52)).toBe('#E57373');
		expect(temperatureColor(NaN)).toBe('#B39DDB');
	});
});
