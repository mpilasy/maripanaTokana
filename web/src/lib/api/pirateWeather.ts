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

// eslint-disable-next-line @typescript-eslint/no-explicit-any
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

	const c = d.currently;
	const offsetSec = Math.round((d.offset ?? 0) * 3600);

	// eslint-disable-next-line @typescript-eslint/no-explicit-any
	const hourly: HourlyForecast[] = (d.hourly?.data ?? []).slice(0, 48).map((h: any) => ({
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

	// eslint-disable-next-line @typescript-eslint/no-explicit-any
	const days: any[] = (d.daily?.data ?? []).slice(0, 10);
	const nightCodes = nightWeatherCodes(
		hourly.map((h) => h.time),
		hourly.map((h) => h.weatherCode),
		days.map((day) => (day.sunsetTime ?? 0) * 1000),
		days.map((day) => (day.sunriseTime ?? 0) * 1000),
	);
	const daily: DailyForecast[] = days.map((day, i) => ({
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

	// eslint-disable-next-line @typescript-eslint/no-explicit-any
	const todayRaw: any = d.daily?.data?.[0];

	// eslint-disable-next-line @typescript-eslint/no-explicit-any
	const minutelyForecast: MinutelyForecast[] = (d.minutely?.data ?? []).map((m: any) => ({
		time: m.time * 1000,
		precipitation: Precipitation.fromMm(m.precipIntensity ?? 0),
	}));

	return {
		temperature: Temperature.fromCelsius(c.temperature),
		feelsLike: Temperature.fromCelsius(c.apparentTemperature),
		tempMin: todayRaw
			? Temperature.fromCelsius(todayRaw.temperatureMin)
			: Temperature.fromCelsius(c.temperature - 3),
		tempMax: todayRaw
			? Temperature.fromCelsius(todayRaw.temperatureMax)
			: Temperature.fromCelsius(c.temperature + 3),
		weatherCode: iconToWmoCode(c.icon),
		locationName,
		locationSubtext,
		latitude: lat,
		longitude: lon,
		pressure: Pressure.fromHPa(c.pressure ?? 1013),
		humidity: c.humidity != null ? Math.round(c.humidity * 100) : null,
		dewPoint: Temperature.fromCelsius(c.dewPoint ?? 0),
		windSpeed: WindSpeed.fromMetersPerSecond(c.windSpeed ?? 0),
		windDeg: c.windBearing ?? null,
		windGust: c.windGust != null ? WindSpeed.fromMetersPerSecond(c.windGust) : null,
		rain: c.precipType === 'rain' ? Precipitation.fromMm(c.precipIntensity ?? 0) : null,
		snow: c.precipType === 'snow' ? Precipitation.fromMm(c.precipIntensity ?? 0) : null,
		uvIndex: c.uvIndex ?? null,
		cloudCover: c.cloudCover != null ? Math.round(c.cloudCover * 100) : null,
		visibility: c.visibility != null ? c.visibility * 1000 : null,
		sunrise: todayRaw ? todayRaw.sunriseTime : 0,
		sunset: todayRaw ? todayRaw.sunsetTime : 0,
		// eslint-disable-next-line @typescript-eslint/no-explicit-any
		dailySunrise: (d.daily?.data ?? []).slice(0, 10).map((day: any) => (day.sunriseTime ?? 0) * 1000),
		// eslint-disable-next-line @typescript-eslint/no-explicit-any
		dailySunset: (d.daily?.data ?? []).slice(0, 10).map((day: any) => (day.sunsetTime ?? 0) * 1000),
		hourlyForecast: hourly,
		minutelyForecast,
		dailyForecast: daily,
		alerts: [],
		alertsLoading: true,
		timestamp: Date.now(),
		utcOffsetSeconds: offsetSec,
	};
}
