// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

import type { HourlyForecast, MinutelyForecast } from './weatherData';

/** What the next 24 hours hold for rain or snow, for the one-line summary on the first page. */
export type PrecipOutlook =
	| { kind: 'starts' | 'stops'; snow: boolean; time: number }
	| { kind: 'all_day' | 'dry'; snow: boolean };

const SNOW_CODES = new Set([71, 73, 75, 77, 85, 86]);
const HOUR = 3_600_000;

// Drizzle, rain, freezing rain, snow, showers and thunderstorms
function isPrecipCode(code: number): boolean {
	return (code >= 51 && code <= 67) || (code >= 71 && code <= 77) || (code >= 80 && code <= 86) || code >= 95;
}

/**
 * An hour is wet when the nowcast covering it shows precipitation (as the nowcast card does), or, past
 * the nowcast, when its weather code is precipitation (as its icon does). The chance of rain alone
 * doesn't count: it comes from a different model and can say 50% for an hour drawn as plain cloud.
 */
function isWet(h: HourlyForecast, minutely: MinutelyForecast[]): boolean {
	const covering = minutely.filter((m) => m.time >= h.time && m.time < h.time + HOUR);
	if (covering.length > 0) return covering.some((m) => m.precipitation.mm > 0.05);
	return isPrecipCode(h.weatherCode);
}

export function precipOutlook(hourly: HourlyForecast[], minutely: MinutelyForecast[] = []): PrecipOutlook | null {
	if (hourly.length === 0) return null;
	const wet = hourly.map((h) => isWet(h, minutely));
	const wetNow = wet[0];
	const change = wet.findIndex((w) => w !== wetNow);
	// The precipitation the summary talks about: the current one, or the one that starts
	const wetHour = wetNow ? hourly[0] : change !== -1 ? hourly[change] : null;
	const snow = wetHour != null && SNOW_CODES.has(wetHour.weatherCode);
	if (change === -1) return { kind: wetNow ? 'all_day' : 'dry', snow };
	return { kind: wetNow ? 'stops' : 'starts', snow, time: hourly[change].time };
}

/** Whether rain falls within the nowcast's two hours; a dry nowcast just repeats the main card's rain line. */
export function nowcastHasRain(minutely: MinutelyForecast[] | undefined, nowMs = Date.now()): boolean {
	return (minutely ?? []).some(
		(m) => m.time >= nowMs - 15 * 60 * 1000 && m.time <= nowMs + 2 * 3600 * 1000 && m.precipitation.mm > 0.05
	);
}
