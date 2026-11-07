// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

import { afterEach, describe, expect, it, vi } from 'vitest';
import { fetchPirateWeather } from './pirateWeather';

const NOW = Date.UTC(2026, 10, 7, 10, 30);
const START = Date.UTC(2026, 10, 7, 9) / 1000;

// 169 hours from 09:00 UTC, as extend=hourly gives; the first hour has no gust or dew point
const body = {
	offset: 0,
	currently: { temperature: 20, apparentTemperature: 19, icon: 'cloudy' },
	hourly: {
		data: Array.from({ length: 169 }, (_, i) => ({
			time: START + i * 3600,
			icon: 'rain',
			temperature: 15,
			precipIntensity: 0.4,
			precipProbability: 0.35,
			windSpeed: 3,
			windBearing: 180,
			pressure: 1010,
			humidity: 0.8,
			uvIndex: 2,
			...(i === 0 ? {} : { windGust: 7, dewPoint: 11 }),
		})),
	},
	daily: {
		data: Array.from({ length: 8 }, (_, i) => ({
			time: Date.UTC(2026, 10, 7 + i) / 1000,
			icon: 'rain',
			temperatureMin: 10,
			temperatureMax: 20,
			sunriseTime: Date.UTC(2026, 10, 7 + i, 6) / 1000,
			sunsetTime: Date.UTC(2026, 10, 7 + i, 18) / 1000,
		})),
	},
};

afterEach(() => {
	vi.useRealTimers();
	vi.unstubAllGlobals();
});

describe('pirate weather chart hours', () => {
	it('asks for the extended hours and fills the chart hours from the current hour', async () => {
		vi.useFakeTimers({ toFake: ['Date'] });
		vi.setSystemTime(NOW);
		const fetchMock = vi.fn(async () => new Response(JSON.stringify(body)));
		vi.stubGlobal('fetch', fetchMock);

		const d = await fetchPirateWeather(1, 2, 'key', 'X');

		expect(String((fetchMock.mock.calls[0] as unknown[])[0])).toContain('extend=hourly');
		expect(d.hourlyForecast).toHaveLength(24);
		// The 24 hours take the gust from the same hours
		expect(d.hourlyForecast[0].windGust?.metersPerSecond).toBe(7);
		const hours = d.chartHourly!;
		expect(hours).toHaveLength(168);
		expect(hours[0].time).toBe(Date.UTC(2026, 10, 7, 10));
		expect(hours[0].windGust?.metersPerSecond).toBe(7);
		expect(hours[0].dewPoint?.celsius).toBe(11);
		expect(hours[0].precipProbability).toBe(35);
		expect(hours[0].humidity).toBe(80);
		expect(hours[0].isNight).toBe(false);
		expect(hours[9].isNight).toBe(true); // 19:00
	});

	it('gives null for a missing gust and dew point', async () => {
		vi.useFakeTimers({ toFake: ['Date'] });
		vi.setSystemTime(Date.UTC(2026, 10, 7, 9, 10));
		vi.stubGlobal('fetch', vi.fn(async () => new Response(JSON.stringify(body))));

		const hours = (await fetchPirateWeather(1, 2, 'key', 'X')).chartHourly!;

		expect(hours[0].windGust).toBeNull();
		expect(hours[0].dewPoint).toBeNull();
	});
});
