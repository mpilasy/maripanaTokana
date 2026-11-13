// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

import type { HourlyForecast, MinutelyForecast } from './weatherData';

/** What the next 24 hours hold for rain or snow, for the one-line summary in the hero card. */
export type PrecipOutlook =
	| { kind: 'starts' | 'stops'; snow: boolean; time: number }
	| { kind: 'all_day' | 'dry'; snow: boolean };

const SNOW_CODES = new Set([71, 73, 75, 77, 85, 86]);

/** Drizzle, rain, freezing rain, snow, showers and thunderstorms. */
export function isPrecipCode(code: number): boolean {
	return (code >= 51 && code <= 67) || (code >= 71 && code <= 77) || (code >= 80 && code <= 86) || code >= 95;
}

/**
 * The weather icon of the rain or snow now: the weather code when it tells the kind (drizzle, sleet, freezing rain,
 * snow grains, hail), else snow (71) or rain (61). With no rain or snow now, the weather code.
 */
export function precipIconCode(weatherCode: number, rain: boolean, snow: boolean): number {
	if ((rain || snow) && isPrecipCode(weatherCode)) return weatherCode;
	if (snow) return 71;
	if (rain) return 61;
	return weatherCode;
}

/** A nowcast value covers this long: 15 minutes (Open-Meteo) or 1 minute (Pirate Weather). */
function stepOf(minutely: MinutelyForecast[]): number {
	return minutely.length >= 2 ? minutely[1].time - minutely[0].time : 15 * 60_000;
}

/**
 * Rain or snow now comes from the nowcast value that covers `nowMs`, as on the nowcast card. The first
 * hourly value is the next full hour, so it cannot tell. A change inside the nowcast gets its time to the
 * minute. Past the nowcast, an hour is wet when its weather code is precipitation (as its icon is). The
 * chance of rain alone doesn't count: it comes from a different model and can say 50% for an hour drawn
 * as plain cloud. Without a nowcast, the first hour stands for now.
 */
export function precipOutlook(
	hourly: HourlyForecast[],
	minutely: MinutelyForecast[] = [],
	nowMs = Date.now()
): PrecipOutlook | null {
	if (hourly.length === 0) return null;
	const step = stepOf(minutely);
	const nowcast = minutely.filter((m) => m.time + step > nowMs);
	const nowcastEnd = nowcast.length > 0 ? nowcast[nowcast.length - 1].time + step : -Infinity;
	// [start time, wet] from now to the end of the hourly forecast
	const steps: [number, boolean][] = [
		...nowcast.map((m): [number, boolean] => [m.time, m.precipitation.mm > 0.05]),
		...hourly.filter((h) => h.time >= nowcastEnd).map((h): [number, boolean] => [h.time, isPrecipCode(h.weatherCode)])
	];
	if (steps.length === 0) return null;
	const wetNow = steps[0][1];
	const change = steps.findIndex((s) => s[1] !== wetNow);
	// The precipitation the summary talks about: the current one, or the one that starts
	const wetTime = wetNow ? steps[0][0] : change !== -1 ? steps[change][0] : null;
	const wetHour = wetTime == null ? null : ([...hourly].reverse().find((h) => h.time <= wetTime) ?? hourly[0]);
	const snow = wetHour != null && SNOW_CODES.has(wetHour.weatherCode);
	if (change === -1) return { kind: wetNow ? 'all_day' : 'dry', snow };
	return { kind: wetNow ? 'stops' : 'starts', snow, time: steps[change][0] };
}

/** Whether rain falls within the nowcast's two hours; a dry nowcast repeats the hero card's rain line. */
export function nowcastHasRain(minutely: MinutelyForecast[] | undefined, nowMs = Date.now()): boolean {
	return (minutely ?? []).some(
		(m) => m.time >= nowMs - 15 * 60 * 1000 && m.time <= nowMs + 2 * 3600 * 1000 && m.precipitation.mm > 0.05
	);
}
