// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

import { describe, expect, it } from 'vitest';
import { computeActivityIndices } from './activityIndices';
import type { WeatherData } from './weatherData';

// Only the fields the indices read; the same cases as ActivityIndicesTest.kt
function weather(tempC = 15, humidity = 50, windMs = 3, rainMm = 0, uvIndex = 2,
	more: { dewC?: number; weatherCode?: number; visibility?: number | null; gustMs?: number | null; dailyChances?: number[] } = {}): WeatherData {
	return {
		temperature: { celsius: tempC },
		humidity,
		windSpeed: { metersPerSecond: windMs },
		rain: rainMm > 0 ? { mm: rainMm } : null,
		snow: null,
		uvIndex,
		dewPoint: { celsius: more.dewC ?? tempC - 2 },
		weatherCode: more.weatherCode ?? 0,
		visibility: more.visibility === undefined ? 10000 : more.visibility,
		windGust: more.gustMs != null ? { metersPerSecond: more.gustMs } : null,
		// The chance of rain or snow (%) of today, tomorrow and the day after
		dailyForecast: (more.dailyChances ?? []).map((chance) => ({ precipProbability: chance, precipitation: { mm: 0 } }))
	} as unknown as WeatherData;
}

describe('computeActivityIndices', () => {
	it('gives excellent tiers in ideal conditions', () => {
		expect(computeActivityIndices(weather(20, 45, 3, 0, 2), true)).toEqual({
			running: 'EXCELLENT', laundry: 'EXCELLENT', uvSafety: 'EXCELLENT', skinComfort: 'EXCELLENT', carWash: 'EXCELLENT', driving: 'EXCELLENT'
		});
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

	const tiers = (data: WeatherData) => computeActivityIndices(data, true);

	it('gives the skin comfort tier from the dew point and humidity', () => {
		expect(tiers(weather(15, 60, 3, 0, 2, { dewC: 10 })).skinComfort).toBe('EXCELLENT');
		expect(tiers(weather(15, 60, 3, 0, 2, { dewC: 2 })).skinComfort).toBe('GOOD');
		expect(tiers(weather(15, 60, 3, 0, 2, { dewC: -5 })).skinComfort).toBe('FAIR');
		expect(tiers(weather(15, 60, 3, 0, 2, { dewC: -12 })).skinComfort).toBe('POOR');
		// Desert heat: the dew point is not low, but the air is very dry
		expect(tiers(weather(35, 18, 3, 0, 2, { dewC: 6 })).skinComfort).toBe('POOR');
	});

	it('gives the car wash tier from the next days', () => {
		expect(tiers(weather(15, 50, 3, 0, 2, { dailyChances: [0, 10, 10] })).carWash).toBe('EXCELLENT');
		expect(tiers(weather(15, 50, 3, 0, 2, { dailyChances: [0, 10, 25] })).carWash).toBe('GOOD');
		expect(tiers(weather(15, 50, 3, 0, 2, { dailyChances: [0, 10, 60] })).carWash).toBe('FAIR');
		expect(tiers(weather(15, 50, 3, 0, 2, { dailyChances: [0, 35, 0] })).carWash).toBe('FAIR');
		expect(tiers(weather(15, 50, 3, 0, 2, { dailyChances: [0, 70, 0] })).carWash).toBe('POOR');
		// Raining now, or frost (the water freezes)
		expect(tiers(weather(15, 50, 3, 0.5, 2, { dailyChances: [0, 0, 0] })).carWash).toBe('POOR');
		expect(tiers(weather(-3, 50, 3, 0, 2, { dailyChances: [0, 0, 0] })).carWash).toBe('POOR');
	});

	it('gives the driving tier from ice, snow, fog and gusts', () => {
		expect(tiers(weather()).driving).toBe('EXCELLENT');
		expect(tiers(weather(15, 50, 3, 0, 2, { weatherCode: 61 })).driving).toBe('GOOD');
		expect(tiers(weather(15, 50, 3, 0, 2, { weatherCode: 45, visibility: 800 })).driving).toBe('FAIR');
		expect(tiers(weather(15, 50, 3, 0, 2, { weatherCode: 71 })).driving).toBe('FAIR');
		expect(tiers(weather(15, 50, 3, 0, 2, { weatherCode: 66 })).driving).toBe('POOR');
		expect(tiers(weather(15, 50, 3, 0, 2, { gustMs: 27 })).driving).toBe('POOR');
		// Rain at freezing point: black ice
		expect(tiers(weather(0.5, 50, 3, 0.3, 2, { weatherCode: 61 })).driving).toBe('FAIR');
		// No visibility or gust data: the weather code alone decides
		expect(tiers(weather(15, 50, 3, 0, 2, { visibility: null, gustMs: null })).driving).toBe('EXCELLENT');
	});
});
