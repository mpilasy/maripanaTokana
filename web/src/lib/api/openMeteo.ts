import { HttpError, netFetch, retryOnce } from './http';
import type { OpenMeteoResponse } from './openMeteoTypes';

const BASE_URL = 'https://api.open-meteo.com/v1/forecast';

const CURRENT_PARAMS = [
	'temperature_2m', 'apparent_temperature', 'relative_humidity_2m', 'dew_point_2m',
	'wind_speed_10m', 'wind_direction_10m', 'wind_gusts_10m', 'pressure_msl',
	'precipitation', 'rain', 'snowfall', 'visibility', 'weather_code', 'is_day', 'uv_index', 'cloud_cover'
].join(',');

const HOURLY_PARAMS = 'temperature_2m,weather_code,precipitation_probability,wind_speed_10m,wind_direction_10m,pressure_msl,precipitation';

const DAILY_PARAMS = [
	'temperature_2m_max', 'temperature_2m_min', 'weather_code',
	'precipitation_probability_max', 'sunrise', 'sunset',
	'wind_speed_10m_max', 'wind_direction_10m_dominant', 'precipitation_sum', 'uv_index_max'
].join(',');

/** Header the service worker sets (epoch millis) on responses it replays from cache. */
export const CACHED_AT_HEADER = 'X-SW-Cached-At';

export function parseCachedAt(value: string | null): number | null {
	if (!value) return null;
	const n = Number(value);
	return Number.isFinite(n) && n > 0 ? n : null;
}

export async function fetchWeather(lat: number, lon: number): Promise<OpenMeteoResponse> {
	const params = new URLSearchParams({
		latitude: lat.toString(),
		longitude: lon.toString(),
		current: CURRENT_PARAMS,
		hourly: HOURLY_PARAMS,
		daily: DAILY_PARAMS,
		minutely_15: 'precipitation',
		forecast_days: '10',
		timezone: 'auto',
		wind_speed_unit: 'ms',
	});

	return retryOnce(async () => {
		const res = await netFetch(`${BASE_URL}?${params}`, { signal: AbortSignal.timeout(10_000) });
		if (!res.ok) throw new HttpError(res.status, `Open-Meteo API error: ${res.status}`);
		const body: OpenMeteoResponse = await res.json();
		// The service worker adds this header when it served the response from its cache (network failed).
		const cachedAt = parseCachedAt(res.headers.get(CACHED_AT_HEADER));
		return cachedAt ? { ...body, cachedAt } : body;
	});
}
