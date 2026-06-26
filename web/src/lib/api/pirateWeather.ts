import type { WeatherData, HourlyForecast, DailyForecast } from '$lib/domain/weatherData';
import { Temperature } from '$lib/domain/temperature';
import { WindSpeed } from '$lib/domain/windSpeed';
import { Pressure } from '$lib/domain/pressure';
import { Precipitation } from '$lib/domain/precipitation';

function iconToWmoCode(icon: string): number {
	switch (icon) {
		case 'clear-day': case 'clear-night': return 0;
		case 'partly-cloudy-day': case 'partly-cloudy-night': return 2;
		case 'cloudy': return 3;
		case 'fog': return 45;
		case 'drizzle': return 51;
		case 'rain': return 61;
		case 'sleet': return 85;
		case 'snow': return 71;
		case 'thunderstorm': return 95;
		case 'wind': return 3;
		default: return 0;
	}
}

export async function testPirateWeatherKey(apiKey: string): Promise<void> {
	const res = await fetch(
		`https://api.pirateweather.net/forecast/${encodeURIComponent(apiKey)}/0,0?units=si&exclude=hourly,daily,minutely,alerts`
	);
	if (!res.ok) throw new Error(String(res.status));
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export async function fetchPirateWeather(lat: number, lon: number, apiKey: string, locationName: string, locationSubtext?: string): Promise<WeatherData> {
	const res = await fetch(
		`https://api.pirateweather.net/forecast/${encodeURIComponent(apiKey)}/${lat},${lon}?units=si&exclude=minutely,alerts`
	);
	if (!res.ok) throw new Error(`Pirate Weather ${res.status}`);
	// eslint-disable-next-line @typescript-eslint/no-explicit-any
	const d: any = await res.json();

	const c = d.currently;
	const offsetSec = Math.round((d.offset ?? 0) * 3600);

	// eslint-disable-next-line @typescript-eslint/no-explicit-any
	const hourly: HourlyForecast[] = (d.hourly?.data ?? []).slice(0, 48).map((h: any) => ({
		time: h.time * 1000,
		temperature: Temperature.fromCelsius(h.temperature),
		weatherCode: iconToWmoCode(h.icon),
		precipProbability: Math.round((h.precipProbability ?? 0) * 100),
		windSpeed: WindSpeed.fromMetersPerSecond(h.windSpeed ?? 0),
		windDeg: h.windBearing ?? 0,
		pressure: Pressure.fromHPa(h.pressure ?? 1013),
		precipitation: Precipitation.fromMm(h.precipIntensity ?? 0),
	}));

	// eslint-disable-next-line @typescript-eslint/no-explicit-any
	const daily: DailyForecast[] = (d.daily?.data ?? []).slice(0, 10).map((day: any) => ({
		date: day.time * 1000,
		tempMax: Temperature.fromCelsius(day.temperatureMax),
		tempMin: Temperature.fromCelsius(day.temperatureMin),
		weatherCode: iconToWmoCode(day.icon),
		precipProbability: Math.round((day.precipProbability ?? 0) * 100),
		windSpeed: WindSpeed.fromMetersPerSecond(day.windSpeed ?? 0),
		windDeg: day.windBearing ?? 0,
		precipitation: Precipitation.fromMm(day.precipIntensity ?? 0),
	}));

	// eslint-disable-next-line @typescript-eslint/no-explicit-any
	const todayRaw: any = d.daily?.data?.[0];

	return {
		temperature: Temperature.fromCelsius(c.temperature),
		feelsLike: Temperature.fromCelsius(c.apparentTemperature),
		tempMin: todayRaw
			? Temperature.fromCelsius(todayRaw.temperatureMin)
			: Temperature.fromCelsius(c.temperature - 3),
		tempMax: todayRaw
			? Temperature.fromCelsius(todayRaw.temperatureMax)
			: Temperature.fromCelsius(c.temperature + 3),
		weatherCode: iconToWmoCode(c.icon),
		locationName,
		locationSubtext,
		latitude: lat,
		longitude: lon,
		pressure: Pressure.fromHPa(c.pressure ?? 1013),
		humidity: Math.round((c.humidity ?? 0) * 100),
		dewPoint: Temperature.fromCelsius(c.dewPoint ?? 0),
		windSpeed: WindSpeed.fromMetersPerSecond(c.windSpeed ?? 0),
		windDeg: c.windBearing ?? 0,
		windGust: c.windGust != null ? WindSpeed.fromMetersPerSecond(c.windGust) : null,
		rain: c.precipType === 'rain' ? Precipitation.fromMm(c.precipIntensity ?? 0) : null,
		snow: c.precipType === 'snow' ? Precipitation.fromMm(c.precipIntensity ?? 0) : null,
		uvIndex: c.uvIndex ?? 0,
		cloudCover: Math.round((c.cloudCover ?? 0) * 100),
		visibility: (c.visibility ?? 16) * 1000,
		sunrise: todayRaw ? todayRaw.sunriseTime : 0,
		sunset: todayRaw ? todayRaw.sunsetTime : 0,
		// eslint-disable-next-line @typescript-eslint/no-explicit-any
		dailySunrise: (d.daily?.data ?? []).slice(0, 10).map((day: any) => (day.sunriseTime ?? 0) * 1000),
		// eslint-disable-next-line @typescript-eslint/no-explicit-any
		dailySunset: (d.daily?.data ?? []).slice(0, 10).map((day: any) => (day.sunsetTime ?? 0) * 1000),
		hourlyForecast: hourly,
		dailyForecast: daily,
		alerts: [],
		alertsLoading: true,
		timestamp: Date.now(),
		utcOffsetSeconds: offsetSec,
	};
}
