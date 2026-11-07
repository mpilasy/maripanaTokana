// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

import { afterEach, describe, expect, it, vi } from 'vitest';
import { mapToWeatherData } from './openMeteoMapper';
import { computeActivityIndices } from '#lib/domain/activityIndices.js';

const response: any = {
	utc_offset_seconds: 0,
	current: {
		time: '2026-10-05T12:00', temperature_2m: 20, weather_code: 1,
		relative_humidity_2m: null, uv_index: null, cloud_cover: null,
		wind_direction_10m: null, visibility: null,
	},
	hourly: { time: [] },
	daily: { time: [], temperature_2m_min: [], temperature_2m_max: [], sunrise: [], sunset: [] },
};

describe('open-meteo mapper nulls', () => {
	it('maps missing secondary metrics to null and activity indices do not throw', () => {
		const d = mapToWeatherData(response, 'X');
		expect(d.humidity).toBeNull();
		expect(d.uvIndex).toBeNull();
		expect(d.cloudCover).toBeNull();
		expect(d.windDeg).toBeNull();
		expect(d.visibility).toBeNull();
		expect(() => computeActivityIndices(d, true)).not.toThrow();
		expect(computeActivityIndices(d, true).uvSafety).toBe('FAIR');
	});
});

describe('open-meteo mapper chart hours', () => {
	// 11 days of hourly data from 2026-11-07 00:00 at UTC+3, as forecast_days=11 gives
	const pad = (n: number) => String(n).padStart(2, '0');
	const iso = (ms: number) => {
		const d = new Date(ms);
		return `${d.getUTCFullYear()}-${pad(d.getUTCMonth() + 1)}-${pad(d.getUTCDate())}T${pad(d.getUTCHours())}:00`;
	};
	const localStart = Date.UTC(2026, 10, 7);
	const hourTimes = Array.from({ length: 264 }, (_, i) => iso(localStart + i * 3_600_000));
	const dayTimes = Array.from({ length: 11 }, (_, i) => iso(localStart + i * 86_400_000).substring(0, 10));
	const eleven: any = {
		utc_offset_seconds: 3 * 3600,
		current: { time: '2026-11-07T10:30', temperature_2m: 20, weather_code: 1 },
		hourly: {
			time: hourTimes,
			temperature_2m: hourTimes.map((_, i) => 10 + (i % 24) / 2),
			weather_code: hourTimes.map(() => 3),
			precipitation_probability: hourTimes.map(() => 40),
			wind_speed_10m: hourTimes.map(() => 4),
			wind_direction_10m: hourTimes.map(() => 90),
			pressure_msl: hourTimes.map(() => 1015),
			precipitation: hourTimes.map(() => 0.2),
			dew_point_2m: hourTimes.map(() => 5),
			wind_gusts_10m: hourTimes.map((_, i) => (i === 12 ? null : 9)),
		},
		daily: {
			time: dayTimes,
			temperature_2m_min: dayTimes.map(() => 10),
			temperature_2m_max: dayTimes.map(() => 21),
			weather_code: dayTimes.map(() => 3),
			sunrise: dayTimes.map((d) => `${d}T06:00`),
			sunset: dayTimes.map((d) => `${d}T18:00`),
		},
	};

	afterEach(() => vi.useRealTimers());

	it('keeps 240 hours from the current hour, the 24-hour list and 10 days', () => {
		// 10:30 at the place (07:30 UTC)
		vi.useFakeTimers();
		vi.setSystemTime(Date.UTC(2026, 10, 7, 7, 30));
		const d = mapToWeatherData(eleven, 'X');
		expect(d.chartHourly).toHaveLength(240);
		expect(d.chartHourly![0].time).toBe(Date.UTC(2026, 10, 7, 7));
		expect(d.hourlyForecast).toHaveLength(24);
		expect(d.hourlyForecast[0].time).toBe(Date.UTC(2026, 10, 7, 8));
		expect(d.dailyForecast).toHaveLength(10);
	});

	it('fills the gusts, the dew point and the night, with null for a missing value', () => {
		vi.useFakeTimers();
		vi.setSystemTime(Date.UTC(2026, 10, 7, 7, 30));
		const hours = mapToWeatherData(eleven, 'X').chartHourly!;
		expect(hours[0].windGust?.metersPerSecond).toBe(9);
		expect(hours[0].dewPoint?.celsius).toBe(5);
		expect(hours[2].windGust).toBeNull(); // 12:00 at the place
		expect(hours[2].uvIndex).toBeNull();
		expect(hours[0].isNight).toBe(false); // 10:00
		expect(hours[9].isNight).toBe(true); // 19:00
	});

	it('has fewer hours near the end of the data', () => {
		vi.useFakeTimers();
		vi.setSystemTime(Date.UTC(2026, 10, 15, 21));
		const d = mapToWeatherData(eleven, 'X');
		expect(d.chartHourly!.length).toBeLessThan(240);
		expect(d.chartHourly![0].time).toBe(Date.UTC(2026, 10, 15, 21));
	});
});
