// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

import type { Temperature } from './temperature';
import type { HourlyForecast } from './weatherData';

/** The largest change of temperature in the next 6 hours, for the chip next to the current temperature. */
export interface TemperatureTrend {
	direction: 'up' | 'down';
	/** The hour of the largest change, epoch millis. */
	time: number;
	temperature: Temperature;
}

const HOUR = 3_600_000;

/** The chip shows only for a change over this many °C (9 °F). */
export const TREND_MIN_CELSIUS = 5;

/**
 * Compares the current temperature with each forecast hour after now and up to 6 hours later, with
 * the exact values. Gives the hour with the largest difference (the earlier hour on a tie), or null
 * when that difference is not over TREND_MIN_CELSIUS, whatever unit shows first.
 */
export function temperatureTrend(
	current: Temperature,
	hourly: HourlyForecast[],
	nowMs = Date.now()
): TemperatureTrend | null {
	let best: HourlyForecast | null = null;
	let bestDiff = 0;
	for (const h of hourly) {
		if (h.time <= nowMs || h.time > nowMs + 6 * HOUR) continue;
		const diff = h.temperature.celsius - current.celsius;
		if (Math.abs(diff) > Math.abs(bestDiff)) {
			best = h;
			bestDiff = diff;
		}
	}
	if (!best || Math.abs(bestDiff) <= TREND_MIN_CELSIUS) return null;
	return { direction: bestDiff > 0 ? 'up' : 'down', time: best.time, temperature: best.temperature };
}
