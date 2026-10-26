// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

import { describe, expect, it } from 'vitest';
import { aqiDialPosition, AirQualityIndex } from './airQuality';

describe('aqiDialPosition', () => {
	it('places a reading inside its level segment', () => {
		expect(aqiDialPosition(0, 'US')).toBeCloseTo(0.004);
		expect(aqiDialPosition(25, 'US')).toBeCloseTo(0.1);
		expect(aqiDialPosition(75, 'US')).toBeCloseTo(0.3);
		expect(aqiDialPosition(150, 'US')).toBeCloseTo(0.5);
		expect(aqiDialPosition(30, 'EUROPEAN')).toBeCloseTo(0.3);
	});

	it('agrees with the level the reading is given', () => {
		for (const [v, std] of [[50, 'US'], [51, 'US'], [100, 'US'], [101, 'US'], [19, 'EUROPEAN'], [20, 'EUROPEAN'], [99, 'EUROPEAN']] as const) {
			const tier = AirQualityIndex.tierFor(v, std);
			const segment = Math.floor(aqiDialPosition(v, std) * 5);
			expect(['good', 'moderate', 'unhealthy', 'very_unhealthy', 'hazardous'][Math.max(0, segment)]).toBe(tier);
		}
	});

	it('stops at the end of the dial', () => {
		expect(aqiDialPosition(900, 'US')).toBeCloseTo(0.996);
	});
});
