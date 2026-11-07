// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

import { HttpError, netFetch, retryOnce } from './http';
import type { OpenMeteoResponse } from './openMeteoTypes';

const BASE_URL = 'https://api.open-meteo.com/v1/forecast';

const CURRENT_PARAMS = [
	'temperature_2m', 'apparent_temperature', 'relative_humidity_2m', 'dew_point_2m',
	'wind_speed_10m', 'wind_direction_10m', 'wind_gusts_10m', 'pressure_msl',
	'precipitation', 'rain', 'snowfall', 'visibility', 'weather_code', 'is_day', 'uv_index', 'cloud_cover'
].join(',');

const HOURLY_PARAMS = 'temperature_2m,weather_code,precipitation_probability,wind_speed_10m,wind_direction_10m,pressure_msl,precipitation,uv_index,relative_humidity_2m,dew_point_2m';

// Current values that are not in HOURLY_PARAMS. With a chosen model, the request adds them to the hourly
// list, so that pickModel can fill a current null with the best_match value of this hour.
const CURRENT_ONLY_HOURLY_PARAMS = 'apparent_temperature,wind_gusts_10m,rain,snowfall,visibility,is_day,cloud_cover';

/** Second model in a two-model request: Open-Meteo's own choice for the place. */
const BEST_MATCH = 'best_match';

const DAILY_PARAMS = [
	'temperature_2m_max', 'temperature_2m_min', 'weather_code',
	'precipitation_probability_max', 'sunrise', 'sunset',
	'wind_speed_10m_max', 'wind_direction_10m_dominant', 'precipitation_sum', 'uv_index_max', 'pressure_msl_mean',
	'relative_humidity_2m_mean', 'dew_point_2m_mean'
].join(',');

/** Header the service worker sets (epoch millis) on responses it replays from cache. */
export const CACHED_AT_HEADER = 'X-SW-Cached-At';

export function parseCachedAt(value: string | null): number | null {
	if (!value) return null;
	const n = Number(value);
	return Number.isFinite(n) && n > 0 ? n : null;
}

/** Forecast request parameters. A null model (Automatic) gives the request without `models`. */
export function forecastParams(lat: number, lon: number, model: string | null): URLSearchParams {
	const params = new URLSearchParams({
		latitude: lat.toString(),
		longitude: lon.toString(),
		current: CURRENT_PARAMS,
		hourly: model ? `${HOURLY_PARAMS},${CURRENT_ONLY_HOURLY_PARAMS}` : HOURLY_PARAMS,
		daily: DAILY_PARAMS,
		minutely_15: 'precipitation',
		forecast_days: '10',
		timezone: 'auto',
		wind_speed_unit: 'ms',
	});
	// The chosen model is first: the current values come from the first model
	if (model) params.set('models', `${model},${BEST_MATCH}`);
	return params;
}

type Series = (number | string | null)[];
type Section = Record<string, unknown>;

/** One section (hourly, daily or minutely_15) with the model suffixes removed. */
function pickSection(section: Section, model: string): Section {
	const chosenSuffix = `_${model}`;
	const bestSuffix = `_${BEST_MATCH}`;
	const result: Section = {};
	for (const [key, value] of Object.entries(section)) {
		if (key.endsWith(chosenSuffix)) {
			const base = key.slice(0, -chosenSuffix.length);
			if (!(`${base}${bestSuffix}` in section)) result[base] = value;
		} else if (key.endsWith(bestSuffix)) {
			const base = key.slice(0, -bestSuffix.length);
			const best = value as Series;
			const chosen = section[`${base}${chosenSuffix}`] as Series | undefined;
			// As many values as best_match: the days after the model's horizon come from best_match
			result[base] = best.map((b, i) => chosen?.[i] ?? b);
		} else {
			result[key] = value;
		}
	}
	return result;
}

/**
 * Merges a two-model response (`models=<model>,best_match`) into the one-model shape.
 * Each value is the chosen model's value when it is not null, else the best_match value.
 * A current null takes the best_match hourly value of the same variable at the current hour.
 */
export function pickModel(raw: OpenMeteoResponse, model: string): OpenMeteoResponse {
	const rawHourly = raw.hourly as unknown as Section;
	const hourly = pickSection(rawHourly, model);
	const current = { ...raw.current } as unknown as Section;
	const currentTime = current.time;
	const hourIndex = typeof currentTime === 'string'
		? (rawHourly.time as string[]).indexOf(`${currentTime.substring(0, 13)}:00`)
		: -1;
	if (hourIndex !== -1) {
		for (const [key, value] of Object.entries(current)) {
			if (value != null) continue;
			const best = rawHourly[`${key}_${BEST_MATCH}`] as Series | undefined;
			current[key] = best?.[hourIndex] ?? null;
		}
	}
	return {
		...raw,
		current: current as unknown as OpenMeteoResponse['current'],
		hourly: hourly as unknown as OpenMeteoResponse['hourly'],
		daily: pickSection(raw.daily as unknown as Section, model) as unknown as OpenMeteoResponse['daily'],
		minutely_15: raw.minutely_15
			? pickSection(raw.minutely_15 as unknown as Section, model) as unknown as OpenMeteoResponse['minutely_15']
			: undefined,
	};
}

/** Fetches the forecast. A chosen model (not null) asks for it and best_match, then merges them with pickModel. */
export async function fetchWeather(lat: number, lon: number, model: string | null = null): Promise<OpenMeteoResponse> {
	const params = forecastParams(lat, lon, model);

	return retryOnce(async () => {
		const res = await netFetch(`${BASE_URL}?${params}`, { signal: AbortSignal.timeout(10_000) });
		if (!res.ok) throw new HttpError(res.status, `Open-Meteo API error: ${res.status}`);
		const raw: OpenMeteoResponse = await res.json();
		const body = model ? pickModel(raw, model) : raw;
		// The service worker adds this header when it served the response from its cache (network failed).
		const cachedAt = parseCachedAt(res.headers.get(CACHED_AT_HEADER));
		return cachedAt ? { ...body, cachedAt } : body;
	});
}
