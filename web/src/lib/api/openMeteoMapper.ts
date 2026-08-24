import type { OpenMeteoResponse } from './openMeteoTypes';
import type { WeatherData, HourlyForecast, DailyForecast } from '../domain/weatherData';
import { Temperature } from '../domain/temperature';
import { Pressure } from '../domain/pressure';
import { WindSpeed } from '../domain/windSpeed';
import { Precipitation } from '../domain/precipitation';

export function parseIsoDateTime(iso: string, utcOffsetSeconds: number): number {
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
			uvIndexMax: d.uv_index_max?.[i] ?? 0,
		};
	});

	const m15 = response.minutely_15;
	const parsedM15Times = m15 ? m15.time.map((t) => parseIsoDateTime(t, response.utc_offset_seconds)) : [];
	const startM15Index = parsedM15Times.findIndex((t) => t >= nowMillis - 15 * 60 * 1000);
	const minutelyForecast = (m15 && startM15Index !== -1)
		? m15.time.slice(startM15Index, startM15Index + 12).map((_, i) => {
				const actualIndex = startM15Index + i;
				return {
					time: parsedM15Times[actualIndex],
					precipitation: Precipitation.fromMm(m15.precipitation[actualIndex] ?? 0),
				};
			})
		: [];

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
		minutelyForecast,
		dailyForecast,
		alerts: [],
		alertsLoading: true,
		timestamp: Date.now(),
	};
}

