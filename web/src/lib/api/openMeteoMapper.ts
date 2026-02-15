import type { OpenMeteoResponse } from './openMeteoTypes';
import type { WeatherData, HourlyForecast, DailyForecast } from '../domain/weatherData';
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

export function mapToWeatherData(response: OpenMeteoResponse, locationName: string): WeatherData {
	const c = response.current;
	const d = response.daily;
	const h = response.hourly;

	const sunriseEpochSec = d.sunrise[0] ? Math.floor(parseIsoDateTime(d.sunrise[0]) / 1000) : 0;
	const sunsetEpochSec = d.sunset[0] ? Math.floor(parseIsoDateTime(d.sunset[0]) / 1000) : 0;

	const dailySunriseMillis = d.sunrise.map(parseIsoDateTime);
	const dailySunsetMillis = d.sunset.map(parseIsoDateTime);

	const nowMillis = Date.now();

	const hourlyForecast: HourlyForecast[] = h.time
		.map((time, i) => ({
			time: parseIsoDateTime(time),
			temperature: Temperature.fromCelsius(h.temperature_2m[i]),
			weatherCode: h.weather_code[i],
			precipProbability: h.precipitation_probability[i],
		}))
		.filter((f) => f.time >= nowMillis)
		.slice(0, 24);

	const dailyForecast: DailyForecast[] = d.time.map((time, i) => ({
		date: parseIsoDate(time),
		tempMax: Temperature.fromCelsius(d.temperature_2m_max[i]),
		tempMin: Temperature.fromCelsius(d.temperature_2m_min[i]),
		weatherCode: d.weather_code[i],
		precipProbability: d.precipitation_probability_max[i],
	}));

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
		timestamp: Date.now(),
	};
}
