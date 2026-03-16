import type { Temperature } from './temperature';
import type { Pressure } from './pressure';
import type { WindSpeed } from './windSpeed';
import type { Precipitation } from './precipitation';

export type AlertLevel = 'watch' | 'warning' | 'emergency';

export interface WeatherAlert {
	level: AlertLevel;
	title: string;
	description: string;
	source: 'official' | 'derived' | 'gdacs';
	time?: number; // epoch millis
	headline?: string;
	link?: string;
}

export interface HourlyForecast {
	time: number; // epoch millis
	temperature: Temperature;
	weatherCode: number;
	precipProbability: number;
	windSpeed: WindSpeed;
	windDeg: number;
	pressure: Pressure;
	precipitation: Precipitation;
}

export interface DailyForecast {
	date: number; // epoch millis
	tempMax: Temperature;
	tempMin: Temperature;
	weatherCode: number;
	precipProbability: number;
	windSpeed: WindSpeed;
	windDeg: number;
	precipitation: Precipitation;
}

export interface WeatherData {
	temperature: Temperature;
	feelsLike: Temperature;
	tempMin: Temperature;
	tempMax: Temperature;
	weatherCode: number;
	locationName: string;
	locationSubtext?: string;
	latitude: number;
	longitude: number;
	pressure: Pressure;
	humidity: number;
	dewPoint: Temperature;
	windSpeed: WindSpeed;
	windDeg: number;
	windGust: WindSpeed | null;
	rain: Precipitation | null;
	snow: Precipitation | null;
	uvIndex: number;
	cloudCover: number; // percent 0-100
	visibility: number; // meters
	sunrise: number; // epoch seconds
	sunset: number; // epoch seconds
	dailySunrise: number[]; // epoch millis per day
	dailySunset: number[]; // epoch millis per day
	hourlyForecast: HourlyForecast[];
	dailyForecast: DailyForecast[];
	alerts: WeatherAlert[];
	timestamp: number; // epoch millis
}
