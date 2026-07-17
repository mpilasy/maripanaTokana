import { AirQualityIndex } from '../domain/airQuality';

export interface OpenMeteoAirQualityResponse {
	current: OpenMeteoAirQualityCurrent;
}

export interface OpenMeteoAirQualityCurrent {
	us_aqi: number | null;
	european_aqi: number | null;
}

const BASE_URL = 'https://air-quality-api.open-meteo.com/v1/air-quality';

export async function fetchAirQuality(lat: number, lon: number): Promise<OpenMeteoAirQualityResponse> {
	const params = new URLSearchParams({
		latitude: lat.toString(),
		longitude: lon.toString(),
		current: 'us_aqi,european_aqi',
	});

	const res = await fetch(`${BASE_URL}?${params}`);
	if (!res.ok) throw new Error(`Open-Meteo Air Quality API error: ${res.status}`);
	return res.json();
}

export function mapToAirQuality(response: OpenMeteoAirQualityResponse, countryCode: string | null): AirQualityIndex | null {
	return AirQualityIndex.from(response.current.us_aqi, response.current.european_aqi, countryCode);
}
