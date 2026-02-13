import {Temperature} from '../domain/Temperature';
import {WindSpeed} from '../domain/WindSpeed';
import {Pressure} from '../domain/Pressure';
import {Precipitation} from '../domain/Precipitation';
import {WeatherData, HourlyForecast, DailyForecast} from '../domain/WeatherData';
import {wmoIconCode} from './WmoWeatherCode';

// --- API Response Types ---

interface OpenMeteoCurrent {
  temperature_2m: number;
  apparent_temperature: number;
  relative_humidity_2m: number;
  dew_point_2m: number;
  wind_speed_10m: number;
  wind_direction_10m: number;
  wind_gusts_10m: number;
  pressure_msl: number;
  precipitation: number;
  rain: number;
  snowfall: number;
  visibility: number;
  weather_code: number;
  is_day: number;
  uv_index: number;
  cloud_cover: number;
}

interface OpenMeteoDaily {
  time: string[];
  temperature_2m_max: number[];
  temperature_2m_min: number[];
  weather_code: number[];
  precipitation_probability_max: number[];
  sunrise: string[];
  sunset: string[];
}

interface OpenMeteoHourly {
  time: string[];
  temperature_2m: number[];
  weather_code: number[];
  precipitation_probability: number[];
}

interface OpenMeteoResponse {
  current: OpenMeteoCurrent;
  daily: OpenMeteoDaily;
  hourly: OpenMeteoHourly;
}

// --- API URL Builder ---

const BASE_URL = 'https://api.open-meteo.com/v1/forecast';

const CURRENT_PARAMS = [
  'temperature_2m', 'apparent_temperature', 'relative_humidity_2m', 'dew_point_2m',
  'wind_speed_10m', 'wind_direction_10m', 'wind_gusts_10m', 'pressure_msl',
  'precipitation', 'rain', 'snowfall', 'visibility', 'weather_code', 'is_day',
  'uv_index', 'cloud_cover',
].join(',');

const HOURLY_PARAMS = 'temperature_2m,weather_code,precipitation_probability';
const DAILY_PARAMS = 'temperature_2m_max,temperature_2m_min,weather_code,precipitation_probability_max,sunrise,sunset';

function buildUrl(lat: number, lon: number): string {
  return `${BASE_URL}?latitude=${lat}&longitude=${lon}`
    + `&current=${CURRENT_PARAMS}`
    + `&hourly=${HOURLY_PARAMS}`
    + `&daily=${DAILY_PARAMS}`
    + `&forecast_days=10&timezone=auto&wind_speed_unit=ms`;
}

// --- Date parsing ---

function parseIsoDateTime(iso: string): number {
  return new Date(iso).getTime();
}

function parseIsoDate(iso: string): number {
  return new Date(iso + 'T00:00:00').getTime();
}

// --- Mapper ---

function mapResponse(response: OpenMeteoResponse, locationName: string): WeatherData {
  const c = response.current;
  const isDay = c.is_day === 1;

  const sunriseEpoch = response.daily.sunrise[0]
    ? Math.floor(parseIsoDateTime(response.daily.sunrise[0]) / 1000)
    : 0;
  const sunsetEpoch = response.daily.sunset[0]
    ? Math.floor(parseIsoDateTime(response.daily.sunset[0]) / 1000)
    : 0;

  const dailySunriseMillis = response.daily.sunrise.map(s => parseIsoDateTime(s));
  const dailySunsetMillis = response.daily.sunset.map(s => parseIsoDateTime(s));

  const nowMillis = Date.now();

  const hourlyForecast: HourlyForecast[] = response.hourly.time
    .map((time, i) => ({
      time: parseIsoDateTime(time),
      temperature: Temperature.fromCelsius(response.hourly.temperature_2m[i]),
      weatherCode: response.hourly.weather_code[i],
      precipProbability: response.hourly.precipitation_probability[i],
    }))
    .filter(h => h.time >= nowMillis)
    .slice(0, 24);

  const dailyForecast: DailyForecast[] = response.daily.time.map((time, i) => ({
    date: parseIsoDate(time),
    tempMax: Temperature.fromCelsius(response.daily.temperature_2m_max[i]),
    tempMin: Temperature.fromCelsius(response.daily.temperature_2m_min[i]),
    weatherCode: response.daily.weather_code[i],
    precipProbability: response.daily.precipitation_probability_max[i],
  }));

  return {
    temperature: Temperature.fromCelsius(c.temperature_2m),
    feelsLike: Temperature.fromCelsius(c.apparent_temperature),
    tempMin: Temperature.fromCelsius(response.daily.temperature_2m_min[0] ?? c.temperature_2m),
    tempMax: Temperature.fromCelsius(response.daily.temperature_2m_max[0] ?? c.temperature_2m),
    weatherCode: c.weather_code,
    iconCode: wmoIconCode(c.weather_code, isDay),
    locationName,
    pressure: Pressure.fromHPa(c.pressure_msl),
    humidity: c.relative_humidity_2m,
    dewPoint: Temperature.fromCelsius(c.dew_point_2m),
    windSpeed: WindSpeed.fromMetersPerSecond(c.wind_speed_10m),
    windDeg: c.wind_direction_10m,
    windGust: c.wind_gusts_10m > 0 ? WindSpeed.fromMetersPerSecond(c.wind_gusts_10m) : null,
    rain: c.rain > 0 ? Precipitation.fromMm(c.rain) : null,
    snow: c.snowfall > 0 ? Precipitation.fromMm(c.snowfall) : null,
    cloudCover: c.cloud_cover,
    uvIndex: c.uv_index,
    visibility: Math.round(c.visibility),
    sunrise: sunriseEpoch,
    sunset: sunsetEpoch,
    dailySunrise: dailySunriseMillis,
    dailySunset: dailySunsetMillis,
    hourlyForecast,
    dailyForecast,
    timestamp: Date.now(),
  };
}

// --- Public API ---

export async function fetchWeather(lat: number, lon: number, locationName: string): Promise<WeatherData> {
  const url = buildUrl(lat, lon);
  const response = await fetch(url);
  if (!response.ok) {
    throw new Error(`Open-Meteo API error: ${response.status}`);
  }
  const data: OpenMeteoResponse = await response.json();
  return mapResponse(data, locationName);
}
