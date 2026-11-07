// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

import { describe, expect, it } from 'vitest';
import { forecastParams, parseCachedAt, pickModel } from './openMeteo';
import type { OpenMeteoResponse } from './openMeteoTypes';

describe('parseCachedAt', () => {
	it('reads the service-worker cache time header', () => {
		expect(parseCachedAt('1700000000000')).toBe(1700000000000);
	});
	it('ignores missing or invalid values', () => {
		expect(parseCachedAt(null)).toBeNull();
		expect(parseCachedAt('nope')).toBeNull();
		expect(parseCachedAt('0')).toBeNull();
	});
});

// A saved two-model response (models=meteofrance_seamless,best_match), cut down by hand.
// The model has a 2-day horizon here: its third day and its last hour are null.
const twoModelResponse = {
	latitude: 48.86,
	longitude: 2.34,
	utc_offset_seconds: 3600,
	current: {
		time: '2026-11-07T15:45', interval: 900,
		temperature_2m: 10.0, apparent_temperature: 7.5, uv_index: null, visibility: null, weather_code: 3, is_day: 1,
	},
	hourly: {
		time: ['2026-11-07T14:00', '2026-11-07T15:00', '2026-11-07T16:00'],
		temperature_2m_meteofrance_seamless: [10.4, null, null],
		temperature_2m_best_match: [9.5, 9.8, 9.1],
		uv_index_meteofrance_seamless: [null, null, null],
		uv_index_best_match: [1.2, 0.8, 0.3],
		visibility_meteofrance_seamless: [null, null, null],
		visibility_best_match: [24000, 23000, 22000],
	},
	daily: {
		time: ['2026-11-07', '2026-11-08', '2026-11-09'],
		temperature_2m_max_meteofrance_seamless: [10.4, 11.7, null],
		temperature_2m_max_best_match: [9.5, 9.4, 14.3],
		sunrise_meteofrance_seamless: ['2026-11-07T07:46', '2026-11-08T07:48', '2026-11-09T07:49'],
		sunrise_best_match: ['2026-11-07T07:46', '2026-11-08T07:48', '2026-11-09T07:49'],
	},
	minutely_15: {
		time: ['2026-11-07T15:45', '2026-11-07T16:00'],
		precipitation_meteofrance_seamless: [0.2, null],
		precipitation_best_match: [0.0, 0.1],
	},
} as unknown as OpenMeteoResponse;

describe('pickModel', () => {
	const merged = pickModel(twoModelResponse, 'meteofrance_seamless') as unknown as {
		current: Record<string, unknown>;
		hourly: Record<string, unknown>;
		daily: Record<string, unknown>;
		minutely_15: Record<string, unknown>;
	};

	it('removes the model suffixes', () => {
		expect(Object.keys(merged.hourly).sort()).toEqual(['temperature_2m', 'time', 'uv_index', 'visibility']);
		expect(Object.keys(merged.daily).sort()).toEqual(['sunrise', 'temperature_2m_max', 'time']);
		expect(merged.minutely_15.precipitation).toEqual([0.2, 0.1]);
	});
	it('uses the chosen model when it has a value', () => {
		expect((merged.hourly.temperature_2m as number[])[0]).toBe(10.4);
		expect((merged.daily.temperature_2m_max as number[]).slice(0, 2)).toEqual([10.4, 11.7]);
		expect(merged.current.temperature_2m).toBe(10.0);
	});
	it('falls back to best_match for nulls', () => {
		expect(merged.hourly.temperature_2m).toEqual([10.4, 9.8, 9.1]);
		expect(merged.hourly.uv_index).toEqual([1.2, 0.8, 0.3]);
	});
	it('takes the days after the model horizon from best_match', () => {
		expect(merged.daily.temperature_2m_max).toEqual([10.4, 11.7, 14.3]);
		expect((merged.daily.time as string[]).length).toBe(3);
	});
	it('fills current nulls with the best_match hourly value at the current hour', () => {
		expect(merged.current.uv_index).toBe(0.8);
		expect(merged.current.visibility).toBe(23000);
		expect(merged.current.apparent_temperature).toBe(7.5);
	});
});

describe('forecastParams', () => {
	it('builds the same request as before for Automatic (no models)', () => {
		const params = forecastParams(-18.9, 47.5, null);
		expect(params.has('models')).toBe(false);
		expect(params.get('hourly')).toBe('temperature_2m,weather_code,precipitation_probability,wind_speed_10m,wind_direction_10m,pressure_msl,precipitation,uv_index,relative_humidity_2m,dew_point_2m,wind_gusts_10m');
		expect(params.get('forecast_days')).toBe('11');
		expect([...params.keys()]).toEqual(['latitude', 'longitude', 'current', 'hourly', 'daily', 'minutely_15', 'forecast_days', 'timezone', 'wind_speed_unit']);
	});
	it('asks for the chosen model first, then best_match, with the current-only variables hourly', () => {
		const params = forecastParams(-18.9, 47.5, 'ecmwf_ifs025');
		expect(params.get('models')).toBe('ecmwf_ifs025,best_match');
		const hourly = params.get('hourly')!.split(',');
		expect(hourly).toEqual(expect.arrayContaining(['apparent_temperature', 'wind_gusts_10m', 'rain', 'snowfall', 'visibility', 'is_day', 'cloud_cover']));
		// The gusts are in the default list, so they are not asked for two times
		expect(hourly.filter((p) => p === 'wind_gusts_10m')).toHaveLength(1);
	});
});
