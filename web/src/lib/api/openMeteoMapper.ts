import type { OpenMeteoResponse, OpenMeteoCurrent, OpenMeteoHourly, OpenMeteoDaily } from './openMeteoTypes';
import type { WeatherData, HourlyForecast, DailyForecast, WeatherAlert } from '../domain/weatherData';
import { Temperature } from '../domain/temperature';
import { Pressure } from '../domain/pressure';
import { WindSpeed } from '../domain/windSpeed';
import { Precipitation } from '../domain/precipitation';

function parseIsoDateTime(iso: string): number {
	return new Date(iso).getTime();
}

function parseIsoDate(iso: string): number {
	return new Date(iso + 'T00:00:00').getTime();
}

function deriveAlerts(c: OpenMeteoCurrent, h: OpenMeteoHourly, d: OpenMeteoDaily): WeatherAlert[] {
	const alerts: WeatherAlert[] = [];
	const nowMillis = Date.now();
	
	// Scan next 24 hours of hourly forecast
	const startIndex = h.time.findIndex(t => new Date(t).getTime() >= nowMillis);
	const forecastWindow = h.time.slice(startIndex, startIndex + 24).map((_: string, i: number) => {
		const idx = startIndex + i;
		return {
			code: h.weather_code[idx],
			temp: h.temperature_2m[idx],
			wind: h.wind_speed_10m[idx]
		};
	});

	const hasCode = (codes: number[]) => [c.weather_code, ...forecastWindow.map((f: any) => f.code)].some(code => codes.includes(code));
	const maxWind = Math.max(c.wind_speed_10m, c.wind_gusts_10m ?? 0, ...forecastWindow.map((f: any) => f.wind));
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

export function mapToWeatherData(response: OpenMeteoResponse, locationName: string): WeatherData {
	const c = response.current;
	const d = response.daily;
	const h = response.hourly;

	const sunriseEpochSec = d.sunrise[0] ? Math.floor(parseIsoDateTime(d.sunrise[0]) / 1000) : 0;
	const sunsetEpochSec = d.sunset[0] ? Math.floor(parseIsoDateTime(d.sunset[0]) / 1000) : 0;

	const dailySunriseMillis = d.sunrise.map(parseIsoDateTime);
	const dailySunsetMillis = d.sunset.map(parseIsoDateTime);

	const nowMillis = Date.now();

	const startIndex = h.time.findIndex((time) => parseIsoDateTime(time) >= nowMillis);
	const hourlyForecast: HourlyForecast[] =
		startIndex === -1
			? []
			: h.time.slice(startIndex, startIndex + 24).map((time, i) => {
					const actualIndex = startIndex + i;
					return {
						time: parseIsoDateTime(time),
						temperature: Temperature.fromCelsius(h.temperature_2m[actualIndex]),
						weatherCode: h.weather_code[actualIndex],
						precipProbability: h.precipitation_probability[actualIndex],
						windSpeed: WindSpeed.fromMetersPerSecond(h.wind_speed_10m[actualIndex]),
						windDeg: h.wind_direction_10m[actualIndex],
						pressure: Pressure.fromHPa(h.pressure_msl[actualIndex]),
						precipitation: Precipitation.fromMm(h.precipitation[actualIndex]),
					};
				});

	const dailyForecast: DailyForecast[] = d.time.map((time, i) => {
		return {
			date: parseIsoDate(time),
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
		temperature: Temperature.fromCelsius(c.temperature_2m),
		feelsLike: Temperature.fromCelsius(c.apparent_temperature),
		tempMin: Temperature.fromCelsius(d.temperature_2m_min[0] ?? c.temperature_2m),
		tempMax: Temperature.fromCelsius(d.temperature_2m_max[0] ?? c.temperature_2m),
		weatherCode: c.weather_code,
		locationName,
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
		alerts: deriveAlerts(c, h, d),
		timestamp: Date.now(),
	};
}
