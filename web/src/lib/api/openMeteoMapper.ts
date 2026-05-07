import type { OpenMeteoResponse, OpenMeteoCurrent, OpenMeteoDaily, OpenMeteoHourly } from './openMeteoTypes';
import type { WeatherData, HourlyForecast, DailyForecast, WeatherAlert } from '../domain/weatherData';
import { Temperature } from '../domain/temperature';
import { Pressure } from '../domain/pressure';
import { WindSpeed } from '../domain/windSpeed';
import { Precipitation } from '../domain/precipitation';

function parseIsoDateTime(iso: string, utcOffsetSeconds: number): number {
	// Open-Meteo format: YYYY-MM-DDTHH:MM
	const year = parseInt(iso.substring(0, 4), 10);
	const month = parseInt(iso.substring(5, 7), 10) - 1; // 0-indexed
	const day = parseInt(iso.substring(8, 10), 10);
	const hour = parseInt(iso.substring(11, 13), 10);
	const minute = parseInt(iso.substring(14, 16), 10);
	return Date.UTC(year, month, day, hour, minute) - (utcOffsetSeconds * 1000);
}

function parseIsoDate(iso: string, utcOffsetSeconds: number): number {
	// Open-Meteo format: YYYY-MM-DD
	const year = parseInt(iso.substring(0, 4), 10);
	const month = parseInt(iso.substring(5, 7), 10) - 1; // 0-indexed
	const day = parseInt(iso.substring(8, 10), 10);
	// We want midnight UTC
	return Date.UTC(year, month, day, 0, 0) - (utcOffsetSeconds * 1000);
}

function deriveAlerts(c: OpenMeteoCurrent, h: OpenMeteoHourly, d: OpenMeteoDaily, utcOffsetSeconds: number): WeatherAlert[] {
	const alerts: WeatherAlert[] = [];
	const nowMillis = Date.now();
	
	const parsedTimes = h.time.map(t => parseIsoDateTime(t, utcOffsetSeconds));

	// Scan next 24 hours of hourly forecast
	const startIndex = Math.max(0, parsedTimes.findIndex(t => t >= nowMillis));
	const forecastWindow = h.time.slice(startIndex, startIndex + 24).map((_: string, i: number) => {
		const idx = startIndex + i;
		return {
			code: h.weather_code[idx],
			temp: h.temperature_2m[idx],
			wind: h.wind_speed_10m[idx]
		};
	});

	const hasCode = (codes: number[]) => [c.weather_code, ...forecastWindow.map((f: { code: number }) => f.code)].some(code => codes.includes(code));
	const maxWind = Math.max(c.wind_speed_10m, c.wind_gusts_10m ?? 0, ...forecastWindow.map((f: { wind: number }) => f.wind));
	const maxTemp = Math.max(c.temperature_2m, ...d.temperature_2m_max.slice(0, 2));
	const minTemp = Math.min(c.temperature_2m, ...d.temperature_2m_min.slice(0, 2));

	// Thunderstorm: 95, 96, 99
	if (hasCode([95, 96, 99])) {
		alerts.push({ level: 'warning', title: 'alert_title_thunderstorm', description: 'alert_desc_thunderstorm', source: 'derived' });
	}

	// Heavy Rain: 65, 82
	if (hasCode([65, 82])) {
		alerts.push({ level: 'warning', title: 'alert_title_heavy_rain', description: 'alert_desc_heavy_rain', source: 'derived' });
	}

	// Heavy Snow: 75, 86
	if (hasCode([75, 86])) {
		alerts.push({ level: 'warning', title: 'alert_title_heavy_snow', description: 'alert_desc_heavy_snow', source: 'derived' });
	}

	// High Wind
	if (maxWind > 15) {
		alerts.push({ level: 'warning', title: 'alert_title_high_wind', description: 'alert_desc_high_wind', source: 'derived' });
	}

	// Extreme Heat
	if (maxTemp > 35) {
		alerts.push({ level: 'warning', title: 'alert_title_extreme_heat', description: 'alert_desc_extreme_heat', source: 'derived' });
	}

	// Extreme Cold
	if (minTemp < -15) {
		alerts.push({ level: 'warning', title: 'alert_title_extreme_cold', description: 'alert_desc_extreme_cold', source: 'derived' });
	}

	// High UV
	if (c.uv_index > 8) {
		alerts.push({ level: 'watch', title: 'alert_title_high_uv', description: 'alert_desc_high_uv', source: 'derived' });
	}

	return alerts;
}

export function mapToWeatherData(response: OpenMeteoResponse, locationName: string, locationSubtext?: string): WeatherData {
	const c = response.current;
	const d = response.daily;
	const h = response.hourly;

	const latitude = response.latitude;
	const longitude = response.longitude;

	const sunriseEpochSec = d.sunrise[0] ? Math.floor(parseIsoDateTime(d.sunrise[0], response.utc_offset_seconds) / 1000) : 0;
	const sunsetEpochSec = d.sunset[0] ? Math.floor(parseIsoDateTime(d.sunset[0], response.utc_offset_seconds) / 1000) : 0;

	const dailySunriseMillis = d.sunrise.map(t => parseIsoDateTime(t, response.utc_offset_seconds));
	const dailySunsetMillis = d.sunset.map(t => parseIsoDateTime(t, response.utc_offset_seconds));

	const nowMillis = Date.now();

	const parsedHourlyTimes = h.time.map(t => parseIsoDateTime(t, response.utc_offset_seconds));

	const startIndex = parsedHourlyTimes.findIndex((t) => t >= nowMillis);
	const hourlyForecast: HourlyForecast[] =
		startIndex === -1
			? []
			: h.time.slice(startIndex, startIndex + 24).map((time, i) => {
					const actualIndex = startIndex + i;
					const epoch = parsedHourlyTimes[actualIndex];
					return {
						time: epoch,
						temperature: Temperature.fromCelsius(h.temperature_2m[actualIndex]),
						weatherCode: h.weather_code[actualIndex],
						precipProbability: h.precipitation_probability[actualIndex],
						windSpeed: WindSpeed.fromMetersPerSecond(h.wind_speed_10m[actualIndex]),
						windDeg: h.wind_direction_10m[actualIndex],
						pressure: Pressure.fromHPa(h.pressure_msl[actualIndex]),
						precipitation: Precipitation.fromMm(h.precipitation[actualIndex]),
					};
				});

	const parsedDailyTimes = d.time.map((t) => parseIsoDate(t, response.utc_offset_seconds));

	const dailyForecast: DailyForecast[] = d.time.map((time, i) => {
		return {
			date: parsedDailyTimes[i],
			tempMax: Temperature.fromCelsius(d.temperature_2m_max[i]),
			tempMin: Temperature.fromCelsius(d.temperature_2m_min[i]),
			weatherCode: i === 0 ? c.weather_code : d.weather_code[i],
			precipProbability: d.precipitation_probability_max[i],
			windSpeed: WindSpeed.fromMetersPerSecond(d.wind_speed_10m_max[i]),
			windDeg: d.wind_direction_10m_dominant[i],
			precipitation: Precipitation.fromMm(d.precipitation_sum[i]),
		};
	});

	return {
		utcOffsetSeconds: response.utc_offset_seconds,
		temperature: Temperature.fromCelsius(c.temperature_2m),
		feelsLike: Temperature.fromCelsius(c.apparent_temperature),
		tempMin: Temperature.fromCelsius(d.temperature_2m_min[0] ?? c.temperature_2m),
		tempMax: Temperature.fromCelsius(d.temperature_2m_max[0] ?? c.temperature_2m),
		weatherCode: c.weather_code,
		locationName,
		locationSubtext,
		latitude,
		longitude,
		pressure: Pressure.fromHPa(c.pressure_msl),
		humidity: c.relative_humidity_2m,
		dewPoint: Temperature.fromCelsius(c.dew_point_2m),
		windSpeed: WindSpeed.fromMetersPerSecond(c.wind_speed_10m),
		windDeg: c.wind_direction_10m,
		windGust: c.wind_gusts_10m > 0 ? WindSpeed.fromMetersPerSecond(c.wind_gusts_10m) : null,
		rain: c.rain > 0 ? Precipitation.fromMm(c.rain) : null,
		snow: c.snowfall > 0 ? Precipitation.fromMm(c.snowfall) : null,
		uvIndex: c.uv_index,
		cloudCover: c.cloud_cover,
		visibility: Math.round(c.visibility),
		sunrise: sunriseEpochSec,
		sunset: sunsetEpochSec,
		dailySunrise: dailySunriseMillis,
		dailySunset: dailySunsetMillis,
		hourlyForecast,
		dailyForecast,
		alerts: [],
		alertsLoading: true,
		timestamp: Date.now(),
	};
}

export { deriveAlerts };
