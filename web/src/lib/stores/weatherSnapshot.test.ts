import { beforeEach, describe, expect, it } from 'vitest';
import { saveSnapshot, loadSnapshot } from './weatherSnapshot';
import { Temperature } from '$lib/domain/temperature';
import { WindSpeed } from '$lib/domain/windSpeed';
import { Pressure } from '$lib/domain/pressure';
import { Precipitation } from '$lib/domain/precipitation';
import type { WeatherData } from '$lib/domain/weatherData';

const store = new Map<string, string>();
(globalThis as any).localStorage = {
	getItem: (k: string) => store.get(k) ?? null,
	setItem: (k: string, v: string) => void store.set(k, v),
	removeItem: (k: string) => void store.delete(k),
};

const sample = {
	temperature: Temperature.fromCelsius(21.5),
	windSpeed: WindSpeed.fromMetersPerSecond(3),
	pressure: Pressure.fromHPa(1012),
	rain: Precipitation.fromMm(1.2),
	snow: null,
	locationName: 'Paris',
	alerts: [],
	timestamp: 1_700_000_000_000,
	hourlyForecast: [{ time: 1, temperature: Temperature.fromCelsius(5) }],
} as unknown as WeatherData;

describe('weather snapshot', () => {
	beforeEach(() => store.clear());

	it('round-trips value objects and timestamp', () => {
		saveSnapshot('gps', sample);
		const out = loadSnapshot('gps')!;
		expect(out.timestamp).toBe(1_700_000_000_000);
		expect(out.temperature).toBeInstanceOf(Temperature);
		expect(out.temperature.fahrenheit).toBeCloseTo(70.7);
		expect(out.windSpeed.mph).toBeGreaterThan(6);
		expect(out.rain!.mm).toBe(1.2);
		expect(out.snow).toBeNull();
		expect(out.hourlyForecast[0].temperature).toBeInstanceOf(Temperature);
	});

	it('returns null for a different key or corrupt data', () => {
		saveSnapshot('gps', sample);
		expect(loadSnapshot('preview')).toBeNull();
		store.set('weather_snapshot', '{bad');
		expect(loadSnapshot('gps')).toBeNull();
	});
});
