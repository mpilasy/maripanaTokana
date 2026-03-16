import { writable, get } from 'svelte/store';
import type { WeatherData } from '$lib/domain/weatherData';
import { fetchWeather } from '$lib/api/openMeteo';
import { fetchNwsAlerts, fetchGdacsAlerts } from '$lib/api/externalAlerts';
import { mapToWeatherData } from '$lib/api/openMeteoMapper';
import {
	getCachedLocation, cacheLocation, movedSignificantly,
	getPosition, reverseGeocode
} from '$lib/stores/location';
import { devModeActive, checkDevModeExpiration } from '$lib/stores/devMode';

export type WeatherState =
	| { kind: 'loading' }
	| { kind: 'success'; data: WeatherData }
	| { kind: 'error'; message: string };

export const weatherState = writable<WeatherState>({ kind: 'loading' });
export const isRefreshing = writable<boolean>(false);

const STALE_MS = 30 * 60 * 1000; // 30 minutes

async function fetchAtLocation(lat: number, lon: number, knownName?: string, knownSubtext?: string): Promise<WeatherData> {
	const weatherPromise = fetchWeather(lat, lon);
	const namePromise = knownName ? Promise.resolve({ name: knownName, subtext: knownSubtext }) : reverseGeocode(lat, lon);
	const nwsPromise = fetchNwsAlerts(lat, lon);
	const gdacsPromise = fetchGdacsAlerts(lat, lon);

	const [response, location, nwsAlerts, gdacsAlerts] = await Promise.all([
		weatherPromise,
		namePromise,
		nwsPromise,
		gdacsPromise
	]);
	const data = mapToWeatherData(response, location.name, knownName ? location.subtext : undefined);
	data.alerts = [...nwsAlerts, ...gdacsAlerts, ...data.alerts];
	// Filter duplicate alerts (e.g. same title)
	data.alerts = data.alerts.filter((a, i, self) => 
		i === self.findIndex(t => t.title === a.title)
	);
	return data;
}

export async function doFetchWeather() {
	const current = get(weatherState);
	if (current.kind !== 'success') {
		weatherState.set({ kind: 'loading' });
	} else {
		isRefreshing.set(true);
	}

	try {
		if (get(devModeActive)) {
			if (!checkDevModeExpiration()) {
				devModeActive.set(false);
			} else {
				const lat = localStorage.getItem('dev_override_lat');
				const lon = localStorage.getItem('dev_override_lon');
				const name = localStorage.getItem('dev_override_name');
				const subtext = localStorage.getItem('dev_override_subtext') || undefined;
				if (lat && lon && name) {
					const lLat = parseFloat(lat);
					const lLon = parseFloat(lon);
					const data = await fetchAtLocation(lLat, lLon, name, subtext);
					weatherState.set({ kind: 'success', data });
					isRefreshing.set(false);
					return;
				}
			}
		}

		// Step 1: try cached location for instant result
		const cached = getCachedLocation();
		let data: WeatherData | null = null;

		// Start fetching weather for cached location immediately if available
		const cachedFetchPromise = cached ? fetchAtLocation(cached.lat, cached.lon, cached.name, cached.subtext) : null;

		// Start getting fresh location concurrently
		const freshLocationPromise = getPosition();

		if (cachedFetchPromise) {
			data = await cachedFetchPromise;
			weatherState.set({ kind: 'success', data });
		}

		// Step 2: get fresh location
		const fresh = await freshLocationPromise;

		// Re-fetch if moved significantly or if we had no cached location
		if (!cached || movedSignificantly(cached.lat, cached.lon, fresh.lat, fresh.lon)) {
			data = await fetchAtLocation(fresh.lat, fresh.lon);
			weatherState.set({ kind: 'success', data });
			cacheLocation(fresh.lat, fresh.lon, data.locationName, data.locationSubtext);
		} else {
			// Update cached coordinates to fresher ones, preserving name if available
			cacheLocation(fresh.lat, fresh.lon, cached.name, cached.subtext);
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
