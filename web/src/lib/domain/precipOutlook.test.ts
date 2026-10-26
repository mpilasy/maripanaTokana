// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

import { describe, expect, it } from 'vitest';
import { precipOutlook } from './precipOutlook';
import type { HourlyForecast, MinutelyForecast } from './weatherData';
import { Precipitation } from './precipitation';

const H = 3_600_000;
function hours(codes: number[], probability = 80): HourlyForecast[] {
	return codes.map((code, i) => ({
		time: i * H,
		weatherCode: code,
		precipProbability: probability,
		precipitation: Precipitation.fromMm(0),
	}) as HourlyForecast);
}
const nowcast = (mm: number[]): MinutelyForecast[] => mm.map((v, i) => ({ time: i * 15 * 60_000, precipitation: Precipitation.fromMm(v) }));

describe('precipOutlook', () => {
	it('follows the weather codes, as the hour icons do', () => {
		expect(precipOutlook(hours([3, 3, 61]))).toEqual({ kind: 'starts', snow: false, time: 2 * H });
		expect(precipOutlook(hours([61, 61, 3]))).toEqual({ kind: 'stops', snow: false, time: 2 * H });
		expect(precipOutlook(hours([73, 73]))).toEqual({ kind: 'all_day', snow: true });
	});

	it('ignores a high chance of rain for hours drawn as plain cloud', () => {
		expect(precipOutlook(hours([3, 3, 3], 60))).toEqual({ kind: 'dry', snow: false });
	});

	it('lets the nowcast decide the hours it covers', () => {
		// Hourly codes say rain now, but the nowcast for the first two hours is dry
		expect(precipOutlook(hours([61, 61, 61]), nowcast([0, 0, 0, 0, 0, 0, 0, 0]))).toEqual({ kind: 'starts', snow: false, time: 2 * H });
		// Hourly codes say dry, but the nowcast shows rain in the first hour
		expect(precipOutlook(hours([3, 3]), nowcast([0, 0.4, 0, 0, 0, 0, 0, 0]))).toEqual({ kind: 'stops', snow: false, time: H });
	});

	it('is null without hourly data', () => {
		expect(precipOutlook([])).toBeNull();
	});
});
