import type { OpenMeteoResponse, OpenMeteoCurrent, OpenMeteoDaily, OpenMeteoHourly } from './openMeteoTypes';
import type { WeatherData, HourlyForecast, DailyForecast, WeatherAlert } from '../domain/weatherData';
import { Temperature } from '../domain/temperature';
import { Pressure } from '../domain/pressure';
import { WindSpeed } from '../domain/windSpeed';
import { Precipitation } from '../domain/precipitation';

function parseIsoDateTime(iso: string, utcOffsetSeconds: number): number {
	const year = parseInt(iso.substring(0, 4), 10);
	const month = parseInt(iso.substring(5, 7), 10) - 1;
	const day = parseInt(iso.substring(8, 10), 10);
	const hour = parseInt(iso.substring(11, 13), 10);
	const minute = parseInt(iso.substring(14, 16), 10);
	return Date.UTC(year, month, day, hour, minute) - utcOffsetSeconds * 1000;
}

function parseIsoDate(iso: string, utcOffsetSeconds: number): number {
	const year = parseInt(iso.substring(0, 4), 10);
	const month = parseInt(iso.substring(5, 7), 10) - 1;
	const day = parseInt(iso.substring(8, 10), 10);
	return Date.UTC(year, month, day) - utcOffsetSeconds * 1000;
}

function deriveAlerts(c: OpenMeteoCurrent, h: OpenMeteoHourly, d: OpenMeteoDaily, utcOffsetSeconds: number): WeatherAlert[] {
	const alerts: WeatherAlert[] = [];
	const nowMillis = Date.now();
	
	const parsedTimes = h.time.map(t => parseIsoDateTime(t, utcOffsetSeconds));

	// Scan next 24 hours of hourly forecast
	const startIndex = Math.max(0, parsedTimes.findIndex(t => t >= nowMillis));

	// Bolt: Optimize loop to avoid multiple array creations
	let maxWind = Math.max(c.wind_speed_10m, c.wind_gusts_10m ?? 0);
	const windowCodes = new Set<number>([c.weather_code]);

	const maxI = Math.min(24, h.time.length - startIndex);
	for (let i = 0; i < maxI; i++) {
		const idx = startIndex + i;
		windowCodes.add(h.weather_code[idx]);
		if (h.wind_speed_10m[idx] > maxWind) {
			maxWind = h.wind_speed_10m[idx];
		}
	}

	const hasCode = (codes: number[]) => codes.some(code => windowCodes.has(code));

	const maxTemp = Math.max(
		c.temperature_2m,
		d.temperature_2m_max.length > 0 ? d.temperature_2m_max[0] : c.temperature_2m,
		d.temperature_2m_max.length > 1 ? d.temperature_2m_max[1] : c.temperature_2m
	);

	const minTemp = Math.min(
		c.temperature_2m,
		d.temperature_2m_min.length > 0 ? d.temperature_2m_min[0] : c.temperature_2m,
		d.temperature_2m_min.length > 1 ? d.temperature_2m_min[1] : c.temperature_2m
	);

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
	const hourlyForecast: HourlyForecast[] = [];
	if (startIndex !== -1) {
		const endIndex = Math.min(startIndex + 24, h.time.length);
		for (let i = startIndex; i < endIndex; i++) {
			const epoch = parsedHourlyTimes[i];
			hourlyForecast.push({
				time: epoch,
				temperature: Temperature.fromCelsius(h.temperature_2m[i]),
				weatherCode: h.weather_code[i],
				precipProbability: h.precipitation_probability[i],
				windSpeed: WindSpeed.fromMetersPerSecond(h.wind_speed_10m[i]),
				windDeg: h.wind_direction_10m[i],
				pressure: Pressure.fromHPa(h.pressure_msl[i]),
				precipitation: Precipitation.fromMm(h.precipitation[i]),
			});
		}
	}

	const parsedDailyTimes = d.time.map(t => parseIsoDate(t, response.utc_offset_seconds));

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
