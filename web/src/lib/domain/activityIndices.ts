// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

import type { DailyForecast, WeatherData } from './weatherData';

// NOT_NEEDED: only for sun protection at night
export type ActivityTier = 'EXCELLENT' | 'GOOD' | 'FAIR' | 'POOR' | 'NOT_NEEDED';

export interface ActivityIndices {
	running: ActivityTier;
	laundry: ActivityTier;
	uvSafety: ActivityTier;
	/** Dry air (low dew point or humidity) dries the skin: POOR means use moisturizer */
	skinComfort: ActivityTier;
	/** Rain or snow soon, or frost, spoils a car wash */
	carWash: ActivityTier;
	/** Weather only (no traffic): ice, snow, fog, heavy rain, strong gusts */
	driving: ActivityTier;
}

/** The activity tiers for `data`; `isDay` is false between sunset and sunrise. */
export function computeActivityIndices(data: WeatherData, isDay: boolean): ActivityIndices {
	const tempC = data.temperature.celsius;
	const humidity = data.humidity ?? 70; // neutral when unavailable
	const windMs = data.windSpeed.metersPerSecond;
	const precipMm = (data.rain?.mm ?? 0) + (data.snow?.mm ?? 0);
	const uv = data.uvIndex ?? 6.0; // neutral (FAIR) when unavailable
	return {
		running: runningTier(tempC, humidity, windMs, precipMm),
		laundry: laundryTier(tempC, humidity, windMs, precipMm),
		uvSafety: uvTier(uv, isDay),
		skinComfort: skinTier(data.dewPoint?.celsius ?? 10, data.humidity),
		carWash: carWashTier(tempC, precipMm, data.dailyForecast ?? []),
		driving: drivingTier(data.weatherCode, data.visibility, data.windGust?.metersPerSecond ?? null, tempC, precipMm)
	};
}

function skinTier(dewC: number, humidity: number | null): ActivityTier {
	if (dewC < -10 || (humidity != null && humidity < 25)) return 'POOR';
	if (dewC < -2 || (humidity != null && humidity < 35)) return 'FAIR';
	if (dewC < 5) return 'GOOD';
	return 'EXCELLENT';
}

// A day is wet when rain or snow is likely (50 % or more) or 1 mm or more is forecast
function isWet(day: DailyForecast): boolean {
	return day.precipProbability >= 50 || day.precipitation.mm >= 1;
}

/** `days`: the daily forecast from today; today, tomorrow and the day after count. */
function carWashTier(tempC: number, precipMm: number, days: DailyForecast[]): ActivityTier {
	const next = days.slice(0, 3);
	if (precipMm > 0 || tempC < 0 || next.slice(0, 2).some(isWet)) return 'POOR';
	if ((next[2] && isWet(next[2])) || (next[1]?.precipProbability ?? 0) >= 30) return 'FAIR';
	if (next.some((d) => d.precipProbability >= 20) || tempC < 5) return 'GOOD';
	return 'EXCELLENT';
}

// WMO weather codes
const ICE_OR_HEAVY_SNOW = new Set([56, 57, 66, 67, 75, 86, 96, 99]); // freezing drizzle or rain, heavy snow, hail
const SNOW_FOG_HEAVY_RAIN = new Set([45, 48, 65, 71, 73, 77, 82, 85, 95]);
const LIGHT_RAIN = new Set([51, 53, 55, 61, 63, 80, 81]);

function drivingTier(code: number, visibilityM: number | null, gustMs: number | null, tempC: number, precipMm: number): ActivityTier {
	const vis = visibilityM ?? Infinity;
	const gust = gustMs ?? 0;
	if (ICE_OR_HEAVY_SNOW.has(code) || vis < 200 || gust > 25) return 'POOR';
	if (SNOW_FOG_HEAVY_RAIN.has(code) || vis < 1000 || gust > 17 || (tempC <= 1 && precipMm > 0)) return 'FAIR';
	if (LIGHT_RAIN.has(code) || vis < 5000 || gust > 12) return 'GOOD';
	return 'EXCELLENT';
}

function runningTier(tempC: number, humidity: number, windMs: number, precipMm: number): ActivityTier {
	if (precipMm > 0 || tempC < 0 || tempC > 32 || windMs > 12) return 'POOR';
	if (tempC >= 8 && tempC <= 22 && humidity < 70 && windMs < 7) return 'EXCELLENT';
	if (tempC >= 2 && tempC <= 28 && humidity < 85) return 'GOOD';
	return 'FAIR';
}

function laundryTier(tempC: number, humidity: number, windMs: number, precipMm: number): ActivityTier {
	if (precipMm > 0 || humidity > 80) return 'POOR';
	if (tempC > 18 && humidity < 50 && windMs >= 2) return 'EXCELLENT';
	if (tempC > 12 && humidity < 65) return 'GOOD';
	return 'FAIR';
}

function uvTier(uv: number, isDay: boolean): ActivityTier {
	// No sun protection is necessary at night
	if (!isDay) return 'NOT_NEEDED';
	if (uv < 3) return 'EXCELLENT';
	if (uv < 6) return 'GOOD';
	if (uv < 8) return 'FAIR';
	return 'POOR';
}
