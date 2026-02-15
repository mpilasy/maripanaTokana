import type { OpenMeteoResponse } from './openMeteoTypes';

const BASE_URL = 'https://api.open-meteo.com/v1/forecast';

const CURRENT_PARAMS = [
	'temperature_2m', 'apparent_temperature', 'relative_humidity_2m', 'dew_point_2m',
	'wind_speed_10m', 'wind_direction_10m', 'wind_gusts_10m', 'pressure_msl',
	'precipitation', 'rain', 'snowfall', 'visibility', 'weather_code', 'is_day', 'uv_index', 'cloud_cover'
].join(',');

const HOURLY_PARAMS = 'temperature_2m,weather_code,precipitation_probability';

const DAILY_PARAMS = [
	'temperature_2m_max', 'temperature_2m_min', 'weather_code',
	'precipitation_probability_max', 'sunrise', 'sunset'
].join(',');

export async function fetchWeather(lat: number, lon: number): Promise<OpenMeteoResponse> {
	const params = new URLSearchParams({
		latitude: lat.toString(),
		longitude: lon.toString(),
		current: CURRENT_PARAMS,
		hourly: HOURLY_PARAMS,
		daily: DAILY_PARAMS,
		forecast_days: '10',
		timezone: 'auto',
		wind_speed_unit: 'ms',
	});

	const res = await fetch(`${BASE_URL}?${params}`);
	if (!res.ok) throw new Error(`Open-Meteo API error: ${res.status}`);
	return res.json();
}
