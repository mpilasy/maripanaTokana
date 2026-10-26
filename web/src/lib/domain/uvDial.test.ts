// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

import { describe, expect, it } from 'vitest';
import { uvDialPosition, uvTier } from './uv';

const TIERS = ['low', 'moderate', 'high', 'veryHigh', 'extreme'];

describe('uvDialPosition', () => {
	it('places a reading inside its level segment', () => {
		expect(uvDialPosition(1.5)).toBeCloseTo(0.1);
		expect(uvDialPosition(7)).toBeCloseTo(0.5);
		expect(uvDialPosition(9.5)).toBeCloseTo(0.7);
	});

	it('agrees with the level the reading is given', () => {
		for (const uv of [0, 2.9, 3, 5.9, 6, 7.9, 8, 10.9, 11, 20]) {
			expect(TIERS[Math.floor(uvDialPosition(uv) * 5)]).toBe(uvTier(uv));
		}
	});
});
