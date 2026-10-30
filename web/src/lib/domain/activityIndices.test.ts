// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

import { describe, expect, it } from 'vitest';
import { computeActivityIndices } from './activityIndices';
import type { WeatherData } from './weatherData';

// Only the fields the indices read; the same cases as ActivityIndicesTest.kt
function weather(tempC = 15, humidity = 50, windMs = 3, rainMm = 0, uvIndex = 2): WeatherData {
	return {
		temperature: { celsius: tempC },
		humidity,
		windSpeed: { metersPerSecond: windMs },
		rain: rainMm > 0 ? { mm: rainMm } : null,
		snow: null,
		uvIndex
	} as unknown as WeatherData;
}

describe('computeActivityIndices', () => {
	it('gives excellent tiers in ideal conditions', () => {
		expect(computeActivityIndices(weather(20, 45, 3, 0, 2), true)).toEqual({ running: 'EXCELLENT', laundry: 'EXCELLENT', uvSafety: 'EXCELLENT' });
	});

	it('gives poor running and laundry tiers in rain', () => {
		const indices = computeActivityIndices(weather(15, 90, 3, 5, 2), true);
		expect(indices.running).toBe('POOR');
		expect(indices.laundry).toBe('POOR');
	});

	it('gives a poor UV tier at a high UV index by day', () => {
		expect(computeActivityIndices(weather(15, 50, 3, 0, 9), true).uvSafety).toBe('POOR');
	});

	it('gives a not needed UV tier at night, also at a high UV index', () => {
		expect(computeActivityIndices(weather(15, 50, 3, 0, 2), false).uvSafety).toBe('NOT_NEEDED');
		expect(computeActivityIndices(weather(15, 50, 3, 0, 9), false).uvSafety).toBe('NOT_NEEDED');
	});

	it('keeps the running and laundry tiers at night', () => {
		const indices = computeActivityIndices(weather(20, 45, 3, 0, 2), false);
		expect(indices.running).toBe('EXCELLENT');
		expect(indices.laundry).toBe('EXCELLENT');
	});
});
