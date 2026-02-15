import { writable, get } from 'svelte/store';
import type { WeatherData } from '$shared/domain/weatherData';
import { fetchWeather } from '$shared/api/openMeteo';
import { mapToWeatherData } from '$shared/api/openMeteoMapper';
import {
	getCachedLocation, cacheLocation, movedSignificantly,
	getPosition, reverseGeocode
} from '$shared/stores/location';

export type WeatherState =
	| { kind: 'loading' }
	| { kind: 'success'; data: WeatherData }
	| { kind: 'error'; message: string };

export const weatherState = writable<WeatherState>({ kind: 'loading' });
export const isRefreshing = writable<boolean>(false);

const STALE_MS = 30 * 60 * 1000; // 30 minutes

async function fetchAtLocation(lat: number, lon: number): Promise<WeatherData> {
	const [response, locationName] = await Promise.all([
		fetchWeather(lat, lon),
		reverseGeocode(lat, lon),
	]);
	return mapToWeatherData(response, locationName);
}

export async function doFetchWeather() {
	const current = get(weatherState);
	if (current.kind !== 'success') {
		weatherState.set({ kind: 'loading' });
	} else {
		isRefreshing.set(true);
	}

	try {
		// Step 1: try cached location for instant result
		const cached = getCachedLocation();
		let data: WeatherData | null = null;

		if (cached) {
			data = await fetchAtLocation(cached.lat, cached.lon);
			weatherState.set({ kind: 'success', data });
		}

		// Step 2: get fresh location
		const fresh = await getPosition();
		cacheLocation(fresh.lat, fresh.lon);

		// Re-fetch if moved significantly or if we had no cached location
		if (!cached || movedSignificantly(cached.lat, cached.lon, fresh.lat, fresh.lon)) {
			data = await fetchAtLocation(fresh.lat, fresh.lon);
			weatherState.set({ kind: 'success', data });
		}
	} catch (err) {
		const current = get(weatherState);
		// Only show error if we don't already have data
		if (current.kind !== 'success') {
			weatherState.set({
				kind: 'error',
				message: err instanceof GeolocationPositionError
					? 'error_get_location'
					: 'error_fetch_weather',
			});
		}
	} finally {
		isRefreshing.set(false);
	}
}

export function refreshIfStale() {
	const current = get(weatherState);
	if (current.kind === 'success' && Date.now() - current.data.timestamp > STALE_MS) {
		doFetchWeather();
	}
}
