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
		expect(computeActivityIndices(weather(20, 45, 3, 0, 2))).toEqual({ running: 'EXCELLENT', laundry: 'EXCELLENT', uvSafety: 'EXCELLENT' });
	});

	it('gives poor running and laundry tiers in rain', () => {
		const indices = computeActivityIndices(weather(15, 90, 3, 5, 2));
		expect(indices.running).toBe('POOR');
		expect(indices.laundry).toBe('POOR');
	});

	it('gives a poor UV tier at a high UV index', () => {
		expect(computeActivityIndices(weather(15, 50, 3, 0, 9)).uvSafety).toBe('POOR');
	});
});
