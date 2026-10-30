// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

import { HttpError, netFetch, retryOnce } from './http';
import type { WeatherData, HourlyForecast, DailyForecast, MinutelyForecast } from '#lib/domain/weatherData.js';
import { Temperature } from '#lib/domain/temperature.js';
import { WindSpeed } from '#lib/domain/windSpeed.js';
import { Pressure } from '#lib/domain/pressure.js';
import { Precipitation } from '#lib/domain/precipitation.js';
import { nightWeatherCodes } from './nightWeather';

// The same on Android (PirateWeatherMapper.kt). Sleet is freezing rain (66), so it gets the sleet icon.
// A Map, not an object: an icon name such as "constructor" must not find an inherited property
const ICON_WMO_CODES = new Map<string, number>([
	['clear-day', 0], ['clear-night', 0],
	['partly-cloudy-day', 2], ['partly-cloudy-night', 2],
	['cloudy', 3],
	['fog', 45],
	['drizzle', 53],
	['rain', 63],
	['sleet', 66],
	['snow', 73],
	['thunderstorm', 95],
	['wind', 3]
]);

function iconToWmoCode(icon: string): number {
	return ICON_WMO_CODES.get(icon) ?? 0;
}

export async function testPirateWeatherKey(apiKey: string): Promise<void> {
	const res = await netFetch(
		`https://api.pirateweather.net/forecast/${encodeURIComponent(apiKey)}/0,0?units=si&exclude=hourly,daily,minutely,alerts`,
		{ signal: AbortSignal.timeout(10_000) }
	);
	if (!res.ok) throw new HttpError(res.status);
}

export async function fetchPirateWeather(lat: number, lon: number, apiKey: string, locationName: string, locationSubtext?: string): Promise<WeatherData> {
	// eslint-disable-next-line @typescript-eslint/no-explicit-any
	const d: any = await retryOnce(async () => {
		const res = await netFetch(
			`https://api.pirateweather.net/forecast/${encodeURIComponent(apiKey)}/${lat},${lon}?units=si&exclude=alerts`,
			{ signal: AbortSignal.timeout(10_000) }
		);
		if (!res.ok) throw new HttpError(res.status, `Pirate Weather ${res.status}`);
		return res.json();
	});
	return mapPirateWeather(d, lat, lon, locationName, locationSubtext);
}

/** The forecast in the app's model. Android has the same split in PirateWeatherMapper.kt. */
// eslint-disable-next-line @typescript-eslint/no-explicit-any
function mapPirateWeather(d: any, lat: number, lon: number, locationName: string, locationSubtext?: string): WeatherData {
	const c = d.currently;
	// eslint-disable-next-line @typescript-eslint/no-explicit-any
	const days: any[] = (d.daily?.data ?? []).slice(0, 10);
	// eslint-disable-next-line @typescript-eslint/no-explicit-any
	const todayRaw: any = d.daily?.data?.[0];
	// eslint-disable-next-line @typescript-eslint/no-explicit-any
	const hourlyRaw: any[] = d.hourly?.data ?? [];

	return {
		temperature: Temperature.fromCelsius(c.temperature),
		feelsLike: Temperature.fromCelsius(c.apparentTemperature),
		tempMin: Temperature.fromCelsius(todayRaw ? todayRaw.temperatureMin : c.temperature),
		tempMax: Temperature.fromCelsius(todayRaw ? todayRaw.temperatureMax : c.temperature),
		weatherCode: iconToWmoCode(c.icon),
		locationName,
		locationSubtext,
		latitude: lat,
		longitude: lon,
		...currentConditions(c),
		sunrise: todayRaw ? todayRaw.sunriseTime : 0,
		sunset: todayRaw ? todayRaw.sunsetTime : 0,
		dailySunrise: days.map((day) => (day.sunriseTime ?? 0) * 1000),
		dailySunset: days.map((day) => (day.sunsetTime ?? 0) * 1000),
		hourlyForecast: hourlyForecast(hourlyRaw),
		minutelyForecast: minutelyForecast(d.minutely?.data ?? []),
		dailyForecast: dailyForecast(days, hourlyRaw),
		alerts: [],
		alertsLoading: true,
		timestamp: Date.now(),
		utcOffsetSeconds: Math.round((d.offset ?? 0) * 3600),
	};
}

/** The current conditions that have a default or may be missing. */
// eslint-disable-next-line @typescript-eslint/no-explicit-any
function currentConditions(c: any) {
	return {
		pressure: Pressure.fromHPa(c.pressure ?? 1013),
		humidity: c.humidity != null ? Math.round(c.humidity * 100) : null,
		dewPoint: Temperature.fromCelsius(c.dewPoint ?? 0),
		windSpeed: WindSpeed.fromMetersPerSecond(c.windSpeed ?? 0),
		windDeg: c.windBearing ?? null,
		windGust: c.windGust != null ? WindSpeed.fromMetersPerSecond(c.windGust) : null,
		rain: c.precipType === 'rain' && c.precipIntensity > 0 ? Precipitation.fromMm(c.precipIntensity) : null,
		snow: c.precipType === 'snow' && c.precipIntensity > 0 ? Precipitation.fromMm(c.precipIntensity) : null,
		uvIndex: c.uvIndex ?? null,
		cloudCover: c.cloudCover != null ? Math.round(c.cloudCover * 100) : null,
		visibility: c.visibility != null ? Math.round(c.visibility * 1000) : null,
	};
}

/** The next 24 hours, from the first hour that has not passed. */
// eslint-disable-next-line @typescript-eslint/no-explicit-any
function hourlyForecast(data: any[]): HourlyForecast[] {
	const nowMillis = Date.now();
	const startIndex = Math.max(0, data.findIndex((h) => h.time * 1000 >= nowMillis));
	return data.slice(startIndex, startIndex + 24).map((h) => ({
		time: h.time * 1000,
		temperature: Temperature.fromCelsius(h.temperature),
		weatherCode: iconToWmoCode(h.icon),
		precipProbability: Math.round((h.precipProbability ?? 0) * 100),
		windSpeed: WindSpeed.fromMetersPerSecond(h.windSpeed ?? 0),
		windDeg: h.windBearing ?? 0,
		pressure: Pressure.fromHPa(h.pressure ?? 1013),
		precipitation: Precipitation.fromMm(h.precipIntensity ?? 0),
		uvIndex: h.uvIndex ?? null,
		humidity: h.humidity != null ? Math.round(h.humidity * 100) : null,
		dewPoint: h.dewPoint != null ? Temperature.fromCelsius(h.dewPoint) : null,
	}));
}

/** The days, with the night weather taken from the hourly forecast. */
// eslint-disable-next-line @typescript-eslint/no-explicit-any
function dailyForecast(days: any[], hourly: any[]): DailyForecast[] {
	const nightCodes = nightWeatherCodes(
		hourly.map((h) => h.time * 1000),
		hourly.map((h) => iconToWmoCode(h.icon)),
		days.map((day) => (day.sunsetTime ?? 0) * 1000),
		days.map((day) => (day.sunriseTime ?? 0) * 1000),
	);
	return days.map((day, i) => ({
		date: day.time * 1000,
		tempMax: Temperature.fromCelsius(day.temperatureMax),
		tempMin: Temperature.fromCelsius(day.temperatureMin),
		weatherCode: iconToWmoCode(day.icon),
		nightWeatherCode: nightCodes[i],
		precipProbability: Math.round((day.precipProbability ?? 0) * 100),
		windSpeed: WindSpeed.fromMetersPerSecond(day.windSpeed ?? 0),
		windDeg: day.windBearing ?? 0,
		precipitation: Precipitation.fromMm(day.precipIntensity ?? 0),
		uvIndexMax: day.uvIndex ?? null,
		pressure: Pressure.fromHPa(day.pressure ?? NaN),
		humidity: day.humidity != null ? Math.round(day.humidity * 100) : null,
		dewPoint: day.dewPoint != null ? Temperature.fromCelsius(day.dewPoint) : null,
	}));
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function minutelyForecast(data: any[]): MinutelyForecast[] {
	return data.map((m) => ({
		time: m.time * 1000,
		precipitation: Precipitation.fromMm(m.precipIntensity ?? 0),
	}));
}
