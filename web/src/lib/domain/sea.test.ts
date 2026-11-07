// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

import { describe, expect, it } from 'vitest';
import { nextTides, seaHeightDual } from './sea';
import { mapToSea } from '../api/openMeteoMarine';

const HOUR = 3_600_000;
const times = Array.from({ length: 13 }, (_, i) => i * HOUR);
// Low at 1 h, high at 4 h, low at 8 h (two equal values: the first), high at 11 h
const levels = [0.1, -0.2, 0.0, 0.3, 0.5, 0.4, 0.1, -0.1, -0.3, -0.3, 0.2, 0.4, 0.3];

describe('nextTides', () => {
	it('finds the next high and low after now', () => {
		expect(nextTides(times, levels, 0)).toEqual({ high: { time: 4 * HOUR, levelM: 0.5 }, low: { time: HOUR, levelM: -0.2 } });
		expect(nextTides(times, levels, 4 * HOUR)).toEqual({ high: { time: 11 * HOUR, levelM: 0.4 }, low: { time: 8 * HOUR, levelM: -0.3 } });
	});
	it('gives null when no extremum comes', () => {
		expect(nextTides(times, levels, 11 * HOUR)).toEqual({ high: null, low: null });
		expect(nextTides([0, HOUR, 2 * HOUR], [0.1, 0.2, 0.3], 0)).toEqual({ high: null, low: null });
	});
	it('skips missing values', () => {
		expect(nextTides([0, HOUR, 2 * HOUR], [0.1, 0.5, null], 0)).toEqual({ high: null, low: null });
	});
});

describe('seaHeightDual', () => {
	it('gives metres and feet, primary first', () => {
		expect(seaHeightDual(1.14, true)).toEqual(['1.1 m', '3.7 ft']);
		expect(seaHeightDual(-0.3, false)).toEqual(['-1.0 ft', '-0.3 m']);
	});
});

describe('mapToSea', () => {
	it('gives null inland, where the wave height and the sea temperature are missing', () => {
		expect(mapToSea({ utc_offset_seconds: 0, current: { wave_height: null, sea_surface_temperature: null } })).toBeNull();
		expect(mapToSea({ utc_offset_seconds: 0 })).toBeNull();
	});
	it('maps a coastal place', () => {
		const sea = mapToSea({
			utc_offset_seconds: 10800,
			current: { wave_height: 1.14, wave_period: 7.1, wave_direction: 106, sea_surface_temperature: 26.8, sea_level_height_msl: 0.34 },
			hourly: { time: ['2026-11-07T00:00'], sea_level_height_msl: [0.2] },
		});
		expect(sea?.waveHeightM).toBe(1.14);
		expect(sea?.swellHeightM).toBeNull();
		expect(sea?.levelTimes).toEqual([Date.UTC(2026, 10, 6, 21)]);
		expect(sea?.levels).toEqual([0.2]);
	});
});
