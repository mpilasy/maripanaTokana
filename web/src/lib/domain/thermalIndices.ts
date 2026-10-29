// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

import { Temperature } from './temperature.js';
import type { WindSpeed } from './windSpeed.js';
import type { DailyForecast } from './weatherData.js';

/*
 * The NWS heat index and wind chill, from the air temperature, the relative humidity and the wind.
 * Each is null outside the range where the NWS uses it.
 */

/** The heat index (Rothfusz regression with the NWS adjustments), at 80 °F (27 °C) and above, when it is above the temperature. */
export function heatIndex(temperature: Temperature, humidity: number | null | undefined): Temperature | null {
	const t = temperature.fahrenheit;
	if (humidity == null || !Number.isFinite(t) || t < 80) return null;
	const rh = humidity;
	let hi = -42.379 + 2.04901523 * t + 10.14333127 * rh - 0.22475541 * t * rh - 0.00683783 * t * t -
		0.05481717 * rh * rh + 0.00122874 * t * t * rh + 0.00085282 * t * rh * rh - 0.00000199 * t * t * rh * rh;
	if (rh < 13 && t <= 112) hi -= ((13 - rh) / 4) * Math.sqrt((17 - Math.abs(t - 95)) / 17);
	if (rh > 85 && t <= 87) hi += ((rh - 85) / 10) * ((87 - t) / 5);
	// In very dry air the index falls below the air temperature; then the source's "feels like" shows instead
	if (hi <= t) return null;
	return Temperature.fromFahrenheit(hi);
}

/** The wind chill (NWS 2001 formula), at 50 °F (10 °C) and below, with wind over 3 mph (4.8 km/h). */
export function windChill(temperature: Temperature, wind: WindSpeed): Temperature | null {
	const t = temperature.fahrenheit;
	const v = wind.mph;
	if (!Number.isFinite(t) || !Number.isFinite(v) || t > 50 || v <= 3) return null;
	const p = Math.pow(v, 0.16);
	return Temperature.fromFahrenheit(35.74 + 0.6215 * t - 35.75 * p + 0.4275 * t * p);
}

/**
 * Environment Canada's time for exposed skin to freeze at `windChill`, as the band's [fewest, most] minutes.
 * Fewest is 0 for the "under 2 minutes" band. Null when the wind chill is above −28 °C (−18 °F).
 */
export function frostbiteMinutes(windChill: Temperature): [number, number] | null {
	const c = windChill.celsius;
	if (!Number.isFinite(c)) return null;
	const r = Math.round(c);
	if (r > -28) return null;
	if (r >= -39) return [10, 30];
	if (r >= -47) return [5, 10];
	if (r >= -54) return [2, 5];
	return [0, 2];
}

/** The relative humidity (%) of air at `temperature` with `dewPoint` (Magnus formula); null when not known. */
export function relativeHumidity(temperature: Temperature, dewPoint: Temperature | null | undefined): number | null {
	const t = temperature.celsius;
	const td = dewPoint?.celsius;
	if (td == null || !Number.isFinite(t) || !Number.isFinite(td)) return null;
	const saturation = (c: number) => Math.exp((17.625 * c) / (243.04 + c));
	return Math.min(100, Math.max(0, Math.round((100 * saturation(td)) / saturation(t))));
}

/**
 * A day's heat index: the day's high, with the humidity at that temperature from the day's mean
 * dew point (it changes little during the day).
 */
export function dailyHeatIndex(day: DailyForecast): Temperature | null {
	return heatIndex(day.tempMax, relativeHumidity(day.tempMax, day.dewPoint));
}

/** A day's wind chill: the day's low with the day's wind. */
export function dailyWindChill(day: DailyForecast): Temperature | null {
	return windChill(day.tempMin, day.windSpeed);
}
