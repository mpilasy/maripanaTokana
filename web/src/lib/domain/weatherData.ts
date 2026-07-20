import type { Temperature } from './temperature';
import type { Pressure } from './pressure';
import type { WindSpeed } from './windSpeed';
import type { Precipitation } from './precipitation';
import type { AirQualityIndex } from './airQuality';

export type WeatherSource = 'OPEN_METEO' | 'PIRATE_WEATHER';

export type AlertLevel = 'watch' | 'warning' | 'emergency';

export interface WeatherAlert {
	level: AlertLevel;
	title: string;
	description: string;
	source: 'nws' | 'gdacs' | 'meteoalarm' | 'jma' | 'eccc' | 'bom' | 'nhc' | 'wmoswic';
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

export interface HourlyAirQuality {
	time: number; // epoch millis
	usValue: number;
	europeanValue: number;
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
	uvIndexMax: number;
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
	alertsLoading?: boolean;
	timestamp: number; // epoch millis
	utcOffsetSeconds: number; // location's UTC offset in seconds
	airQuality?: AirQualityIndex | null;
	hourlyAirQuality?: HourlyAirQuality[];
}
