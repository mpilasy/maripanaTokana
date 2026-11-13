// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

import { describe, expect, it } from 'vitest';
import { precipOutlook } from './precipOutlook';
import type { HourlyForecast, MinutelyForecast } from './weatherData';
import { Precipitation } from './precipitation';

const H = 3_600_000;
const M = 60_000;
function hours(codes: number[], probability = 80, from = 0): HourlyForecast[] {
	return codes.map((code, i) => ({
		time: from + i * H,
		weatherCode: code,
		precipProbability: probability,
		precipitation: Precipitation.fromMm(0),
	}) as HourlyForecast);
}
const nowcast = (mm: number[], from = 0): MinutelyForecast[] =>
	mm.map((v, i) => ({ time: from + i * 15 * M, precipitation: Precipitation.fromMm(v) }));

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
		expect(precipOutlook(hours([61, 61, 61]), nowcast([0, 0, 0, 0, 0, 0, 0, 0]), 0)).toEqual({ kind: 'starts', snow: false, time: 2 * H });
		// Hourly codes say dry, but the nowcast shows rain from the second quarter hour
		expect(precipOutlook(hours([3, 3]), nowcast([0, 0.4, 0, 0, 0, 0, 0, 0]), 0)).toEqual({ kind: 'starts', snow: false, time: 15 * M });
	});

	it('takes now from the nowcast, not from the next full hour', () => {
		// At 06:49 the hours start at 07:00 with drizzle until 11:00, and the nowcast has rain from 07:15:
		// the rain starts at 07:15; it does not stop at 11:00
		const outlook = precipOutlook(
			hours([51, 51, 51, 51, 3, 3], 80, 7 * H),
			nowcast([0, 0, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1], 6 * H + 45 * M),
			6 * H + 49 * M
		);
		expect(outlook).toEqual({ kind: 'starts', snow: false, time: 7 * H + 15 * M });
	});

	it('gives the stop time to the minute inside the nowcast', () => {
		expect(precipOutlook(hours([61, 3, 3], 80, H), nowcast([0.5, 0.5, 0, 0]), 5 * M)).toEqual({ kind: 'stops', snow: false, time: 30 * M });
	});

	it('is null without hourly data', () => {
		expect(precipOutlook([])).toBeNull();
	});
});
