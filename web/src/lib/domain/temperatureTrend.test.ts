// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

import { describe, expect, it } from 'vitest';
import { temperatureTrend } from './temperatureTrend';
import { Temperature } from './temperature';
import type { HourlyForecast } from './weatherData';

const H = 3_600_000;
const NOW = 100 * H;
// One forecast hour per value, the first one at the current hour
function hours(celsius: number[]): HourlyForecast[] {
	return celsius.map((c, i) => ({ time: NOW + i * H, temperature: Temperature.fromCelsius(c) }) as HourlyForecast);
}
const c = (v: number) => Temperature.fromCelsius(v);

describe('temperatureTrend', () => {
	it('gives the hour with the largest rise', () => {
		const t = temperatureTrend(c(20), hours([20, 21, 24, 26, 25]), true, NOW);
		expect(t?.direction).toBe('up');
		expect(t?.time).toBe(NOW + 3 * H);
		expect(t?.temperature.celsius).toBe(26);
	});

	it('gives a fall', () => {
		const t = temperatureTrend(c(10), hours([10, 9, 7, 5]), true, NOW);
		expect(t?.direction).toBe('down');
		expect(t?.time).toBe(NOW + 3 * H);
	});

	it('takes the earlier hour on a tie', () => {
		const t = temperatureTrend(c(10), hours([10, 13, 7, 13]), true, NOW);
		expect(t?.direction).toBe('up');
		expect(t?.time).toBe(NOW + H);
	});

	it('gives nothing for less than 2 °C', () => {
		expect(temperatureTrend(c(20), hours([20, 21, 21.4, 19]), true, NOW)).toBeNull();
	});

	it('compares the exact values, not the rounded ones', () => {
		// 20.4 shows as 20 and 22.3 as 22, but the change is 1.9
		expect(temperatureTrend(c(20.4), hours([20, 22.3]), true, NOW)).toBeNull();
		// 20.6 shows as 21 and 22.6 as 23; the change is 2.0
		expect(temperatureTrend(c(20.6), hours([20, 22.6]), true, NOW)?.direction).toBe('up');
	});

	it('needs 4 °F when Fahrenheit is primary', () => {
		// 2 °C is 3.6 °F: a chip in Celsius, none in Fahrenheit; 2.3 °C is 4.14 °F
		expect(temperatureTrend(c(20), hours([20, 22]), true, NOW)).not.toBeNull();
		expect(temperatureTrend(c(20), hours([20, 22]), false, NOW)).toBeNull();
		expect(temperatureTrend(c(20), hours([20, 22.3]), false, NOW)?.direction).toBe('up');
	});

	it('looks only at hours after now and up to 6 hours later', () => {
		expect(temperatureTrend(c(20), hours([30, 20, 20, 20, 20, 20, 20, 30]), true, NOW)).toBeNull();
		expect(temperatureTrend(c(20), hours([20, 20, 20, 20, 20, 20, 30]), true, NOW)?.time).toBe(NOW + 6 * H);
		expect(temperatureTrend(c(20), [], true, NOW)).toBeNull();
	});
});
