// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

import type { WeatherData } from './weatherData';

// NOT_NEEDED: only for sun protection at night
export type ActivityTier = 'EXCELLENT' | 'GOOD' | 'FAIR' | 'POOR' | 'NOT_NEEDED';

export interface ActivityIndices {
	running: ActivityTier;
	laundry: ActivityTier;
	uvSafety: ActivityTier;
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
		uvSafety: uvTier(uv, isDay)
	};
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
