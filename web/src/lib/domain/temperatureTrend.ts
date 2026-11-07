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

/**
 * Compares the current temperature with each forecast hour after now and up to 6 hours later, with
 * the exact values in the primary unit. Gives the hour with the largest difference (the earlier hour on
 * a tie), or null when that difference is less than 2 °C (4 °F when Fahrenheit is primary).
 */
export function temperatureTrend(
	current: Temperature,
	hourly: HourlyForecast[],
	metricPrimary: boolean,
	nowMs = Date.now()
): TemperatureTrend | null {
	// Degrees of the primary unit for each °C of difference
	const scale = metricPrimary ? 1 : 9 / 5;
	let best: HourlyForecast | null = null;
	let bestDiff = 0;
	for (const h of hourly) {
		if (h.time <= nowMs || h.time > nowMs + 6 * HOUR) continue;
		const diff = (h.temperature.celsius - current.celsius) * scale;
		if (Math.abs(diff) > Math.abs(bestDiff)) {
			best = h;
			bestDiff = diff;
		}
	}
	if (!best || Math.abs(bestDiff) < (metricPrimary ? 2 : 4)) return null;
	return { direction: bestDiff > 0 ? 'up' : 'down', time: best.time, temperature: best.temperature };
}
