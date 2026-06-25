import { writable, get } from 'svelte/store';
import type { WeatherData } from '$lib/domain/weatherData';
import { fetchWeather } from '$lib/api/openMeteo';
import { fetchNwsAlerts, fetchGdacsAlerts } from '$lib/api/externalAlerts';
import { mapToWeatherData, deriveAlerts } from '$lib/api/openMeteoMapper';
import {
	getCachedLocation, cacheLocation, movedSignificantly,
	getPosition, reverseGeocode
} from '$lib/stores/location';
import { localeIndex } from '$lib/stores/preferences';
import { SUPPORTED_LOCALES } from '$lib/i18n/locales';
import { devModeActive, checkDevModeExpiration } from '$lib/stores/devMode';

export type WeatherState =
	| { kind: 'loading' }
	| { kind: 'success'; data: WeatherData }
	| { kind: 'error'; message: string };

export const weatherState = writable<WeatherState>({ kind: 'loading' });
export const isRefreshing = writable<boolean>(false);

const STALE_MS = 30 * 60 * 1000; // 30 minutes

// Cached GPS weather data fetched in background during dev mode
let cachedGpsWeatherData: WeatherData | null = null;

async function fetchAtLocation(lat: number, lon: number, knownName?: string, knownSubtext?: string, localeTag?: string): Promise<WeatherData> {
	const weatherPromise = fetchWeather(lat, lon);
	const namePromise = knownName ? Promise.resolve({ name: knownName, subtext: knownSubtext }) : reverseGeocode(lat, lon, localeTag);

	const [response, location] = await Promise.all([
		weatherPromise,
		namePromise,
	]);
	const data = mapToWeatherData(response, location.name, knownSubtext || location.subtext);
	
	// Start alert fetching in background
	fetchAlertsForData(lat, lon, response);
	
	return data;
}

async function fetchAlertsForData(lat: number, lon: number, rawResponse: any) {
	const timestamp = Date.now();
	try {
		const nwsPromise = fetchNwsAlerts(lat, lon);
		const gdacsPromise = fetchGdacsAlerts(lat, lon);
		const derivedAlerts = deriveAlerts(
			rawResponse.current,
			rawResponse.hourly,
			rawResponse.daily,
			rawResponse.utc_offset_seconds
		);

		const [nwsAlerts, gdacsAlerts] = await Promise.all([nwsPromise, gdacsPromise]);
		
		const combined = [...nwsAlerts, ...gdacsAlerts, ...derivedAlerts];
		const filtered = combined.filter((a, i, self) =>
			i === self.findIndex(t => t.title === a.title && t.source === a.source)
		);

		weatherState.update(s => {
			if (s.kind === 'success') {
				// Only update if it's the same request (simple check: name is same or it's within 10s)
				return { ...s, data: { ...s.data, alerts: filtered, alertsLoading: false } };
			}
			return s;
		});
	} catch (err) {
		weatherState.update(s => {
			if (s.kind === 'success') {
				return { ...s, data: { ...s.data, alertsLoading: false } };
			}
			return s;
		});
	}
}

/** Background-fetch GPS weather and cache it for when dev mode is disabled */
function spawnGpsCacheRefresh() {
	getPosition()
		.then(async (fresh) => {
			const cached = getCachedLocation();
			const lat = fresh.lat;
			const lon = fresh.lon;
			const name = cached?.name;
			const subtext = cached?.subtext;
			const data = await fetchAtLocation(lat, lon, name, subtext);
			cachedGpsWeatherData = data;
			// Don't overwrite the location cache while dev mode is active — it would corrupt
			// the cached_location key and cause updateLocationName to reverse-geocode the
			// wrong (real GPS) coordinates on language change.
			if (!get(devModeActive)) {
				cacheLocation(lat, lon, data.locationName, data.locationSubtext);
			}
		})
		.catch(() => {
			// Silently fail - this is a best-effort background refresh
		});
}

/** Called when dev mode is disabled to immediately show GPS weather */
export function restoreGpsWeather() {
	if (cachedGpsWeatherData) {
		weatherState.set({ kind: 'success', data: cachedGpsWeatherData });
		cachedGpsWeatherData = null;
		// Also refresh in background to get truly fresh data
		doFetchWeather();
	} else {
		doFetchWeather();
	}
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
					// Spawn background GPS cache refresh
					spawnGpsCacheRefresh();
					return;
				}
			}
		}

		// Step 1: try cached location for instant result
		const cached = getCachedLocation();
		let data: WeatherData | null = null;
		const localeTag = SUPPORTED_LOCALES[get(localeIndex)]?.tag;

		// Start fetching weather for cached location immediately if available
		const cachedFetchPromise = cached ? fetchAtLocation(cached.lat, cached.lon, cached.name, cached.subtext, localeTag) : null;

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
			data = await fetchAtLocation(fresh.lat, fresh.lon, undefined, undefined, localeTag);
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

export async function updateLocationName(localeTag: string) {
	// If a dev override is set, leave the city name alone — don't replace it with real GPS name
	if (typeof localStorage !== 'undefined' && localStorage.getItem('dev_override_lat')) return;
	const cached = getCachedLocation();
	if (!cached) return;
	const location = await reverseGeocode(cached.lat, cached.lon, localeTag);
	cacheLocation(cached.lat, cached.lon, location.name, location.subtext);
	weatherState.update(s => {
		if (s.kind !== 'success') return s;
		return { ...s, data: { ...s.data, locationName: location.name, locationSubtext: location.subtext } };
	});
}

export function refreshIfStale() {
	const current = get(weatherState);
	if (current.kind === 'success' && Date.now() - current.data.timestamp > STALE_MS) {
		doFetchWeather();
	}
}
