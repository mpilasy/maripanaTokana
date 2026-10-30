// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

import { describe, it, expect } from 'vitest';
import { nhcLevel, nhcStats } from './nhc';

describe('nhcLevel', () => {
	it('makes a major hurricane an emergency', () => {
		expect(nhcLevel('HU', 96)).toBe('emergency');
		expect(nhcLevel('HU', 95)).toBe('warning');
	});

	it('makes a tropical storm a warning and other storms a watch', () => {
		expect(nhcLevel('TS', 50)).toBe('warning');
		expect(nhcLevel('TD', 30)).toBe('watch');
	});
});

describe('nhcStats', () => {
	it('joins the winds, pressure and movement in one sentence', () => {
		expect(nhcStats({ pressure: '980', movementSpeed: 12, movementDir: 315 }, 65)).toBe(
			'Winds ~75 mph, central pressure 980 mb, moving NW at 12 mph.'
		);
	});

	it('says stationary when the storm does not move', () => {
		expect(nhcStats({ movementSpeed: 0 }, 0)).toBe('Stationary.');
	});

	it('is empty when nothing is known', () => {
		expect(nhcStats({}, 0)).toBe('');
	});
});
