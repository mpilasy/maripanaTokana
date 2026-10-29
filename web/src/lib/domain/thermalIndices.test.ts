// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

import { describe, expect, it } from 'vitest';
import { Temperature } from './temperature.js';
import { WindSpeed } from './windSpeed.js';
import { heatIndex, relativeHumidity, windChill } from './thermalIndices.js';

// Reference values from the NWS heat index and wind chill charts
const f = (v: number) => Temperature.fromFahrenheit(v);
const mph = (v: number) => WindSpeed.fromMph(v);

describe('heatIndex', () => {
	it('matches the NWS chart', () => {
		expect(Math.abs(heatIndex(f(90), 70)!.fahrenheit - 106)).toBeLessThan(1);
		expect(Math.abs(heatIndex(f(80), 40)!.fahrenheit - 80)).toBeLessThan(1);
		expect(Math.abs(heatIndex(f(100), 40)!.fahrenheit - 109)).toBeLessThan(1);
	});

	it('is null below 80 °F or without humidity', () => {
		expect(heatIndex(f(79), 90)).toBeNull();
		expect(heatIndex(f(90), null)).toBeNull();
	});
});

describe('windChill', () => {
	it('matches the NWS chart', () => {
		expect(Math.abs(windChill(f(0), mph(15))!.fahrenheit + 19)).toBeLessThan(0.5);
		expect(Math.abs(windChill(f(40), mph(10))!.fahrenheit - 34)).toBeLessThan(0.5);
		expect(Math.abs(windChill(f(20), mph(60))!.fahrenheit + 4)).toBeLessThan(0.5);
	});

	it('is null when warm or calm', () => {
		expect(windChill(f(51), mph(20))).toBeNull();
		expect(windChill(f(30), mph(3))).toBeNull();
	});
});

describe('relativeHumidity', () => {
	it('comes from the dew point', () => {
		expect(relativeHumidity(Temperature.fromCelsius(30), Temperature.fromCelsius(20))).toBe(55);
		expect(relativeHumidity(Temperature.fromCelsius(15), Temperature.fromCelsius(15))).toBe(100);
		expect(relativeHumidity(Temperature.fromCelsius(15), null)).toBeNull();
	});
});
