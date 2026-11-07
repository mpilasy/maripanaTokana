// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

import type { Temperature } from './temperature';
import type { Pressure } from './pressure';
import type { WindSpeed } from './windSpeed';
import type { Precipitation } from './precipitation';
import type { AirQualityIndex } from './airQuality';
import type { DayNormal } from './normals';
import type { ForecastChange } from './forecastChanges';
import type { SeaConditions } from './sea';

export type WeatherSource = 'OPEN_METEO' | 'PIRATE_WEATHER';

export type AlertLevel = 'watch' | 'warning' | 'emergency';

export interface WeatherAlert {
	level: AlertLevel;
	title: string;
	description: string;
	source: 'nws' | 'gdacs' | 'meteoalarm' | 'jma' | 'eccc' | 'bom' | 'nhc';
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
	uvIndex?: number | null;
	humidity?: number | null; // percentage
	dewPoint?: Temperature | null;
	/** The strongest gust of the hour; missing when the source does not give it or in data saved before it was kept. */
	windGust?: WindSpeed | null;
}

/** One hour of the chart view. A value that the source does not give is null. Android has the same type (ChartHour.kt). */
export interface ChartHour {
	time: number; // epoch millis
	temperature: Temperature | null;
	dewPoint: Temperature | null;
	weatherCode: number;
	isNight: boolean;
	precipitation: Precipitation | null;
	precipProbability: number | null; // percentage
	windSpeed: WindSpeed | null;
	windDeg: number | null; // degrees
	windGust: WindSpeed | null;
	pressure: Pressure | null;
	humidity: number | null; // percentage
	uvIndex: number | null;
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
	/** The night's weather (sunset to next sunrise); null when the hourly data doesn't reach it. */
	nightWeatherCode?: number | null;
	precipProbability: number;
	windSpeed: WindSpeed;
	windDeg: number;
	precipitation: Precipitation;
	uvIndexMax: number | null;
	/** The day's mean sea-level pressure; missing in data saved before it was fetched. */
	pressure?: Pressure;
	/** The day's mean relative humidity (percentage) and dew point; missing in data saved before they were fetched. */
	humidity?: number | null;
	dewPoint?: Temperature | null;
}

export interface MinutelyForecast {
	time: number; // epoch millis
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
	/** Latin-letter name and region when locationName is in another script (e.g. 東京 -> Tokyo). */
	romanName?: string;
	romanSubtext?: string;
	latitude: number;
	longitude: number;
	pressure: Pressure;
	humidity: number | null;
	dewPoint: Temperature;
	windSpeed: WindSpeed;
	windDeg: number | null;
	windGust: WindSpeed | null;
	rain: Precipitation | null;
	snow: Precipitation | null;
	uvIndex: number | null;
	cloudCover: number | null; // percent 0-100
	visibility: number | null; // meters
	sunrise: number; // epoch seconds
	sunset: number; // epoch seconds
	dailySunrise: number[]; // epoch millis per day
	dailySunset: number[]; // epoch millis per day
	hourlyForecast: HourlyForecast[];
	/** Every hour from the current hour, at most 240, for the chart view; missing in data saved before it was added. */
	chartHourly?: ChartHour[];
	dailyForecast: DailyForecast[];
	alerts: WeatherAlert[];
	alertsLoading?: boolean;
	failedAlertSources?: string[]; // display names of alert sources whose fetch failed; absent in old snapshots
	timestamp: number; // epoch millis
	/** True when the data was replayed from the offline cache rather than freshly fetched. Not persisted. */
	staleFromCache?: boolean;
	utcOffsetSeconds: number; // location's UTC offset in seconds
	airQuality?: AirQualityIndex | null;
	hourlyAirQuality?: HourlyAirQuality[];
	minutelyForecast?: MinutelyForecast[];
	/** The normal high and low (°C) of each day of dailyForecast; missing until loaded or when the request failed. */
	normals?: (DayNormal | null)[];
	/** What changed in the forecast since about 24 hours ago; missing or empty when nothing did. */
	forecastChanges?: ForecastChange[];
	/** The sea at a coastal place; null inland or when the request failed. */
	sea?: SeaConditions | null;
}

/**
 * Whether `time` is between `sunrise` and `sunset` (same unit). In a polar night the two are equal
 * (00:00 on both), so no time is day; in a midnight sun they are 24 hours apart, so all of it is.
 */
export function isDaytime(time: number, sunrise: number, sunset: number): boolean {
	return sunset > sunrise && time >= sunrise && time <= sunset;
}
