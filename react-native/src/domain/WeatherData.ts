import {Temperature} from './Temperature';
import {WindSpeed} from './WindSpeed';
import {Pressure} from './Pressure';
import {Precipitation} from './Precipitation';

export interface HourlyForecast {
  time: number; // epoch millis
  temperature: Temperature;
  weatherCode: number;
  precipProbability: number;
}

export interface DailyForecast {
  date: number; // epoch millis
  tempMax: Temperature;
  tempMin: Temperature;
  weatherCode: number;
  precipProbability: number;
}

export interface WeatherData {
  temperature: Temperature;
  feelsLike: Temperature;
  tempMin: Temperature;
  tempMax: Temperature;
  weatherCode: number;
  iconCode: string;
  locationName: string;
  pressure: Pressure;
  humidity: number; // percentage
  dewPoint: Temperature;
  windSpeed: WindSpeed;
  windDeg: number; // degrees
  windGust: WindSpeed | null;
  rain: Precipitation | null;
  snow: Precipitation | null;
  cloudCover: number; // percentage
  uvIndex: number;
  visibility: number; // meters
  sunrise: number; // epoch seconds
  sunset: number; // epoch seconds
  dailySunrise: number[]; // epoch millis per day
  dailySunset: number[]; // epoch millis per day
  hourlyForecast: HourlyForecast[];
  dailyForecast: DailyForecast[];
  timestamp: number; // millis
}
