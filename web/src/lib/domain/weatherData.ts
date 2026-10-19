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
}
