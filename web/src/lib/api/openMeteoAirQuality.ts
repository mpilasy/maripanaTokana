import { AirQualityIndex } from '../domain/airQuality';
import type { PollenReadings } from '../domain/airQuality';
import type { HourlyAirQuality } from '../domain/weatherData';
import { parseIsoDateTime } from './openMeteoMapper';

export interface OpenMeteoAirQualityResponse {
	current: OpenMeteoAirQualityCurrent;
	hourly?: OpenMeteoAirQualityHourly;
}

export interface OpenMeteoAirQualityHourly {
	time: string[];
	us_aqi: (number | null)[];
	european_aqi: (number | null)[];
}

export interface OpenMeteoAirQualityCurrent {
	us_aqi: number | null;
	european_aqi: number | null;
	pm2_5: number | null;
	pm10: number | null;
	carbon_monoxide: number | null;
	nitrogen_dioxide: number | null;
	sulphur_dioxide: number | null;
	ozone: number | null;
	ammonia: number | null;
	dust: number | null;
	us_aqi_pm2_5: number | null;
	us_aqi_pm10: number | null;
	us_aqi_carbon_monoxide: number | null;
	us_aqi_nitrogen_dioxide: number | null;
	us_aqi_sulphur_dioxide: number | null;
	us_aqi_ozone: number | null;
	european_aqi_pm2_5: number | null;
	european_aqi_pm10: number | null;
	european_aqi_nitrogen_dioxide: number | null;
	european_aqi_sulphur_dioxide: number | null;
	european_aqi_ozone: number | null;
	alder_pollen: number | null;
	birch_pollen: number | null;
	grass_pollen: number | null;
	mugwort_pollen: number | null;
	olive_pollen: number | null;
	ragweed_pollen: number | null;
}

const BASE_URL = 'https://air-quality-api.open-meteo.com/v1/air-quality';

export async function fetchAirQuality(lat: number, lon: number): Promise<OpenMeteoAirQualityResponse> {
	const params = new URLSearchParams({
		latitude: lat.toString(),
		longitude: lon.toString(),
		current: 'us_aqi,european_aqi,pm2_5,pm10,carbon_monoxide,nitrogen_dioxide,sulphur_dioxide,ozone,ammonia,dust,' +
			'us_aqi_pm2_5,us_aqi_pm10,us_aqi_carbon_monoxide,us_aqi_nitrogen_dioxide,us_aqi_sulphur_dioxide,us_aqi_ozone,' +
			'european_aqi_pm2_5,european_aqi_pm10,european_aqi_nitrogen_dioxide,european_aqi_sulphur_dioxide,european_aqi_ozone,' +
			'alder_pollen,birch_pollen,grass_pollen,mugwort_pollen,olive_pollen,ragweed_pollen',
		hourly: 'us_aqi,european_aqi',
		forecast_days: '3',
	});

	const res = await fetch(`${BASE_URL}?${params}`);
	if (!res.ok) throw new Error(`Open-Meteo Air Quality API error: ${res.status}`);
	return res.json();
}

export function mapToAirQuality(response: OpenMeteoAirQualityResponse, countryCode: string | null): AirQualityIndex | null {
	const c = response.current;
	const pollen: PollenReadings = {
		alder: c.alder_pollen,
		birch: c.birch_pollen,
		grass: c.grass_pollen,
		mugwort: c.mugwort_pollen,
		olive: c.olive_pollen,
		ragweed: c.ragweed_pollen,
	};
	return AirQualityIndex.from(c.us_aqi, c.european_aqi, countryCode, {
		pm25: c.pm2_5,
		pm10: c.pm10,
		carbonMonoxide: c.carbon_monoxide,
		nitrogenDioxide: c.nitrogen_dioxide,
		sulphurDioxide: c.sulphur_dioxide,
		ozone: c.ozone,
		ammonia: c.ammonia,
		dust: c.dust,
	}, {
		usAqiPm25: c.us_aqi_pm2_5,
		usAqiPm10: c.us_aqi_pm10,
		usAqiCarbonMonoxide: c.us_aqi_carbon_monoxide,
		usAqiNitrogenDioxide: c.us_aqi_nitrogen_dioxide,
		usAqiSulphurDioxide: c.us_aqi_sulphur_dioxide,
		usAqiOzone: c.us_aqi_ozone,
		europeanAqiPm25: c.european_aqi_pm2_5,
		europeanAqiPm10: c.european_aqi_pm10,
		europeanAqiNitrogenDioxide: c.european_aqi_nitrogen_dioxide,
		europeanAqiSulphurDioxide: c.european_aqi_sulphur_dioxide,
		europeanAqiOzone: c.european_aqi_ozone,
	}, pollen);
}

export function mapToHourlyAirQuality(response: OpenMeteoAirQualityResponse, nowMillis: number = Date.now()): HourlyAirQuality[] {
	const h = response.hourly;
	if (!h) return [];

	const parsedTimes = h.time.map(t => parseIsoDateTime(t, 0));
	const startIndex = parsedTimes.findIndex(t => t >= nowMillis);
	if (startIndex === -1) return [];

	const endIndex = Math.min(startIndex + 48, h.time.length);
	const result: HourlyAirQuality[] = [];
	const seen = new Set<number>();
	for (let i = startIndex; i < endIndex; i++) {
		const us = h.us_aqi[i];
		const eu = h.european_aqi[i];
		const time = parsedTimes[i];
		if (us == null || eu == null || seen.has(time)) continue;
		seen.add(time);
		result.push({ time, usValue: us, europeanValue: eu });
	}
	return result;
}
