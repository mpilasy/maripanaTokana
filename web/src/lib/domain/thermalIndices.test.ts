// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

import { describe, expect, it } from 'vitest';
import { Temperature } from './temperature.js';
import { WindSpeed } from './windSpeed.js';
import { frostbiteMinutes, heatIndex, relativeHumidity, windChill } from './thermalIndices.js';

// Reference values in °C and km/h, calculated with the NWS heat index and wind chill formulas
const c = (v: number) => Temperature.fromCelsius(v);
const kmh = (v: number) => WindSpeed.fromMetersPerSecond(v / 3.6);

describe('heatIndex', () => {
	it('follows the NWS formula', () => {
		expect(Math.abs(heatIndex(c(32), 70)!.celsius - 40.4)).toBeLessThan(0.5);
		expect(Math.abs(heatIndex(c(30), 70)!.celsius - 35.0)).toBeLessThan(0.5);
		expect(Math.abs(heatIndex(c(38), 50)!.celsius - 48.6)).toBeLessThan(0.5);
	});

	it('is null below 27 °C or without humidity', () => {
		expect(heatIndex(c(26), 90)).toBeNull();
		expect(heatIndex(c(32), null)).toBeNull();
	});

	it('is null when less than 5 °C above the temperature', () => {
		// Very dry heat (Kidal, Mali: 38 °C at 12 %) gives about 35 °C, under the temperature
		expect(heatIndex(c(38), 12)).toBeNull();
		expect(heatIndex(c(27), 40)).toBeNull();
		// 28.1 °C at 27 °C and 60 %, 37.2 °C at 35 °C and 40 %
		expect(heatIndex(c(27), 60)).toBeNull();
		expect(heatIndex(c(35), 40)).toBeNull();
	});
});

describe('windChill', () => {
	it('follows the NWS formula', () => {
		expect(Math.abs(windChill(c(-20), kmh(30))!.celsius + 32.5)).toBeLessThan(0.3);
		expect(Math.abs(windChill(c(-5), kmh(20))!.celsius + 11.5)).toBeLessThan(0.3);
		expect(Math.abs(windChill(c(-10), kmh(80))!.celsius + 24.0)).toBeLessThan(0.3);
		// Just under freezing: -0.9 °C at 2 °C with 10 km/h
		expect(Math.abs(windChill(c(2), kmh(10))!.celsius + 0.9)).toBeLessThan(0.3);
	});

	it('is null when warm, calm or above freezing', () => {
		expect(windChill(c(11), kmh(30))).toBeNull();
		expect(windChill(c(-5), kmh(4))).toBeNull();
		// 0.5 °C at 4 °C with 15 km/h
		expect(windChill(c(4), kmh(15))).toBeNull();
	});
});

describe('frostbiteMinutes', () => {
	it('follows the Environment Canada bands', () => {
		const c = (v: number) => Temperature.fromCelsius(v);
		expect(frostbiteMinutes(c(-27))).toBeNull();
		expect(frostbiteMinutes(c(-28))).toEqual([10, 30]);
		expect(frostbiteMinutes(c(-39.4))).toEqual([10, 30]);
		expect(frostbiteMinutes(c(-40))).toEqual([5, 10]);
		expect(frostbiteMinutes(c(-54))).toEqual([2, 5]);
		expect(frostbiteMinutes(c(-55))).toEqual([0, 2]);
	});
});

describe('relativeHumidity', () => {
	it('comes from the dew point', () => {
		expect(relativeHumidity(Temperature.fromCelsius(30), Temperature.fromCelsius(20))).toBe(55);
		expect(relativeHumidity(Temperature.fromCelsius(15), Temperature.fromCelsius(15))).toBe(100);
		expect(relativeHumidity(Temperature.fromCelsius(15), null)).toBeNull();
	});
});
