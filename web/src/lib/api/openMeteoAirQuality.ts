import { AirQualityIndex } from '../domain/airQuality';

export interface OpenMeteoAirQualityResponse {
	current: OpenMeteoAirQualityCurrent;
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
}

const BASE_URL = 'https://air-quality-api.open-meteo.com/v1/air-quality';

export async function fetchAirQuality(lat: number, lon: number): Promise<OpenMeteoAirQualityResponse> {
	const params = new URLSearchParams({
		latitude: lat.toString(),
		longitude: lon.toString(),
		current: 'us_aqi,european_aqi,pm2_5,pm10,carbon_monoxide,nitrogen_dioxide,sulphur_dioxide,ozone,ammonia,dust,' +
			'us_aqi_pm2_5,us_aqi_pm10,us_aqi_carbon_monoxide,us_aqi_nitrogen_dioxide,us_aqi_sulphur_dioxide,us_aqi_ozone,' +
			'european_aqi_pm2_5,european_aqi_pm10,european_aqi_nitrogen_dioxide,european_aqi_sulphur_dioxide,european_aqi_ozone',
	});

	const res = await fetch(`${BASE_URL}?${params}`);
	if (!res.ok) throw new Error(`Open-Meteo Air Quality API error: ${res.status}`);
	return res.json();
}

export function mapToAirQuality(response: OpenMeteoAirQualityResponse, countryCode: string | null): AirQualityIndex | null {
	const c = response.current;
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
	});
}
