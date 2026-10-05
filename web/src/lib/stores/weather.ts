import { writable, get } from 'svelte/store';
import type { WeatherData } from '$lib/domain/weatherData';
import { classifyError, HttpError } from '$lib/api/http';
import { saveSnapshot, loadSnapshot } from '$lib/stores/weatherSnapshot';
import { fetchWeather } from '$lib/api/openMeteo';
import { fetchPirateWeather } from '$lib/api/pirateWeather';
import { fetchAllAlerts, type AlertSettings } from '$lib/api/externalAlerts';
import { mapToWeatherData } from '$lib/api/openMeteoMapper';
import { fetchAirQuality, mapToAirQuality, mapToHourlyAirQuality } from '$lib/api/openMeteoAirQuality';
import { getLocationInfo, type LocationInfo } from '$lib/api/alerts/shared';
import {
	getCachedLocation, cacheLocation, movedSignificantly,
	getPosition, reverseGeocode
} from '$lib/stores/location';
import {
	localeIndex,
	weatherSource, weatherApiKey,
	alertsEnabled, alertsNwsEnabled, alertsGdacsEnabled,
	alertsMeteoAlarmEnabled, alertsJmaEnabled, alertsEcccEnabled,
	alertsBomEnabled, alertsNhcEnabled,
} from '$lib/stores/preferences';
import { SUPPORTED_LOCALES } from '$lib/i18n/locales';
import { activeLocationId, savedLocations, locationOverride, checkOverrideExpiry } from '$lib/stores/savedLocations';

export type WeatherState =
	| { kind: 'loading' }
	| { kind: 'success'; data: WeatherData }
	| { kind: 'error'; message: string };

export const weatherState = writable<WeatherState>({ kind: 'loading' });
export const isRefreshing = writable<boolean>(false);
/** True when a refresh failed while older data is still on screen. */
export const refreshFailed = writable<boolean>(false);
/** i18n key explaining why the last refresh failed (invalid key, offline, ...); null when generic or no failure. */
export const refreshError = writable<string | null>(null);

/** Only informative failures get shown next to the generic "couldn't refresh" line. */
export function refreshErrorReason(key: string): string | null {
	return key === 'error_fetch_weather' ? null : key;
}

const STALE_MS = 30 * 60 * 1000; // 30 minutes

// Incremented on every doFetchWeather; results of older fetches (including their alerts) are dropped.
let fetchGeneration = 0;

// Open-Meteo snaps response coordinates to a ~10 km grid, so compare with a tolerance.
const SAME_LOCATION_DEG = 0.1;

// Cached GPS weather data fetched in background while previewing another location
let cachedGpsWeatherData: WeatherData | null = null;

async function fetchAtLocation(lat: number, lon: number, knownName?: string, knownSubtext?: string, localeTag?: string, alertsGeneration: number | null = fetchGeneration): Promise<WeatherData> {
	const src = get(weatherSource);
	const apiKey = get(weatherApiKey);

	// One shared (cached) Nominatim lookup feeds both the display name and country info.
	const namePromise = knownName
		? Promise.resolve({ name: knownName, subtext: knownSubtext })
		: reverseGeocode(lat, lon, localeTag);

	if (src === 'PIRATE_WEATHER' && apiKey) {
		const location = await namePromise;
		const data = await fetchPirateWeather(lat, lon, apiKey, location.name, knownSubtext ?? location.subtext);
		if (alertsGeneration !== null) fetchAlertsForData(lat, lon, alertsGeneration);
		return data;
	}

	// Open-Meteo path
	const weatherPromise = fetchWeather(lat, lon);
	const airQualityResponsePromise = fetchAirQuality(lat, lon).catch(() => null);
	// Country decides which AQI standard is primary (european_aqi vs us_aqi) — same lookup
	// used for alert-source gating in fetchAllAlerts. Fetched once here and passed through to
	// avoid firing a second, redundant reverse-geocode request from fetchAllAlerts.
	const locationInfoPromise = getLocationInfo(lat, lon, localeTag);
	const [response, location, airQualityResponse, locationInfo] = await Promise.all([weatherPromise, namePromise, airQualityResponsePromise, locationInfoPromise]);
	const airQuality = airQualityResponse ? mapToAirQuality(airQualityResponse, locationInfo.countryCode) : null;
	const hourlyAirQuality = airQualityResponse ? mapToHourlyAirQuality(airQualityResponse) : [];
	const data: WeatherData = { ...mapToWeatherData(response, location.name, knownSubtext || location.subtext), airQuality, hourlyAirQuality };
	// The service worker replayed an old cached response (network failed): show its real age, not "now".
	if (response.cachedAt) {
		data.timestamp = response.cachedAt;
		data.staleFromCache = true;
	}

	if (alertsGeneration !== null) fetchAlertsForData(lat, lon, alertsGeneration, locationInfo);

	return data;
}

/** Snapshot key for the location the next fetch will show: preview, saved id, or GPS. */
function currentSnapshotKey(): string {
	if (get(locationOverride)) return 'preview';
	const id = get(activeLocationId);
	if (id && get(savedLocations).some((l) => l.id === id)) return id;
	return 'gps';
}

function setWeatherData(fetched: WeatherData) {
	const { staleFromCache, ...data } = fetched;
	refreshFailed.set(!!staleFromCache);
	refreshError.set(staleFromCache ? 'error_offline' : null);
	saveSnapshot(currentSnapshotKey(), { ...data, alerts: [], alertsLoading: true });
	weatherState.update(s => {
		const existingAlerts =
			s.kind === 'success'
			&& Math.abs(s.data.latitude - data.latitude) < SAME_LOCATION_DEG
			&& Math.abs(s.data.longitude - data.longitude) < SAME_LOCATION_DEG
				? s.data.alerts
				: [];
		return { kind: 'success', data: { ...data, alerts: existingAlerts } };
	});
}

async function fetchAlertsForData(lat: number, lon: number, generation: number, locationInfo?: LocationInfo) {
	try {
		const settings: AlertSettings = {
			alertsEnabled: get(alertsEnabled),
			alertsNwsEnabled: get(alertsNwsEnabled),
			alertsGdacsEnabled: get(alertsGdacsEnabled),
			alertsMeteoAlarmEnabled: get(alertsMeteoAlarmEnabled),
			alertsJmaEnabled: get(alertsJmaEnabled),
			alertsEcccEnabled: get(alertsEcccEnabled),
			alertsBomEnabled: get(alertsBomEnabled),
			alertsNhcEnabled: get(alertsNhcEnabled),
		};
		const { alerts, failedSources } = await fetchAllAlerts(lat, lon, settings, locationInfo);
		if (generation !== fetchGeneration) return;
		weatherState.update(s => {
			if (s.kind === 'success') {
				return { ...s, data: { ...s.data, alerts, failedAlertSources: failedSources, alertsLoading: false } };
			}
			return s;
		});
	} catch {
		if (generation !== fetchGeneration) return;
		weatherState.update(s => {
			if (s.kind === 'success') {
				return { ...s, data: { ...s.data, alertsLoading: false } };
			}
			return s;
		});
	}
}

/** Background-fetch GPS weather and cache it for when the preview is cleared */
function spawnGpsCacheRefresh() {
	getPosition()
		.then(async (fresh) => {
			const cached = getCachedLocation();
			const lat = fresh.lat;
			const lon = fresh.lon;
			const name = cached?.name;
			const subtext = cached?.subtext;
			const data = await fetchAtLocation(lat, lon, name, subtext, undefined, null);
			cachedGpsWeatherData = data;
			// Don't overwrite the location cache while previewing another location — it would
			// corrupt the cached_location key and cause updateLocationName to reverse-geocode the
			// wrong (real GPS) coordinates on language change.
			if (!get(locationOverride)) {
				cacheLocation(lat, lon, data.locationName, data.locationSubtext);
			}
		})
		.catch(() => {
			// Silently fail - this is a best-effort background refresh
		});
}

/** Called when a preview is cleared to immediately show GPS weather */
export function restoreGpsWeather() {
	if (cachedGpsWeatherData) {
		setWeatherData(cachedGpsWeatherData);
		cachedGpsWeatherData = null;
		// Also refresh in background to get truly fresh data
		doFetchWeather();
	} else {
		doFetchWeather();
	}
}

export async function doFetchWeather() {
	const gen = ++fetchGeneration;
	const current = get(weatherState);
	if (current.kind !== 'success') {
		// Show the last saved snapshot (original timestamp) while the fetch runs
		const snapshot = loadSnapshot(currentSnapshotKey());
		if (snapshot) {
			weatherState.set({ kind: 'success', data: snapshot });
			isRefreshing.set(true);
		} else {
			weatherState.set({ kind: 'loading' });
		}
	} else {
		isRefreshing.set(true);
	}

	try {
		checkOverrideExpiry();
		const override = get(locationOverride);
		if (override) {
			const data = await fetchAtLocation(override.lat, override.lon, override.name, override.subtext);
			if (gen !== fetchGeneration) return;
			setWeatherData(data);
			isRefreshing.set(false);
			spawnGpsCacheRefresh();
			return;
		}

		const activeSavedId = get(activeLocationId);
		if (activeSavedId) {
			const savedLocation = get(savedLocations).find((l) => l.id === activeSavedId);
			if (savedLocation) {
				const data = await fetchAtLocation(savedLocation.latitude, savedLocation.longitude, savedLocation.name, savedLocation.subtext);
				if (gen !== fetchGeneration) return;
				setWeatherData(data);
				isRefreshing.set(false);
				return;
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
		freshLocationPromise.catch(() => {}); // handled below; avoid unhandled rejection on early exit

		let cachedShown = false;
		let cachedError: unknown = null;
		if (cachedFetchPromise) {
			try {
				data = await cachedFetchPromise;
				if (gen !== fetchGeneration) return;
				setWeatherData(data);
				cachedShown = true;
			} catch (err) {
				if (gen !== fetchGeneration) return;
				// A 4xx (bad key, rate limit) would fail identically for any location: don't ask again.
				if (err instanceof HttpError && err.status < 500) throw err;
				// Otherwise still try the fresh position below
				cachedError = err;
			}
		}

		// Step 2: get fresh location
		let fresh: Awaited<typeof freshLocationPromise>;
		try {
			fresh = await freshLocationPromise;
		} catch (err) {
			if (gen !== fetchGeneration) return;
			// Cached-location weather is already on screen; a failed GPS fix is not a failed refresh.
			if (cachedShown) {
				console.warn('Fresh position failed; keeping cached-location weather', err);
				return;
			}
			throw err;
		}
		if (gen !== fetchGeneration) return;

		// Re-fetch if moved significantly or if we had no cached location
		// Cached-location fetch failed and we haven't moved: the same request would just fail again.
		if (cachedError && cached && !movedSignificantly(cached.lat, cached.lon, fresh.lat, fresh.lon)) throw cachedError;

		if (!cached || !cachedShown || movedSignificantly(cached.lat, cached.lon, fresh.lat, fresh.lon)) {
			data = await fetchAtLocation(fresh.lat, fresh.lon, undefined, undefined, localeTag);
			if (gen !== fetchGeneration) return;
			setWeatherData(data);
			cacheLocation(fresh.lat, fresh.lon, data.locationName, data.locationSubtext);
		} else {
			// Update cached coordinates to fresher ones, preserving name if available
			cacheLocation(fresh.lat, fresh.lon, cached.name, cached.subtext);
		}
	} catch (err) {
		if (gen !== fetchGeneration) return;
		const current = get(weatherState);
		const message = classifyError(err, get(weatherSource) === 'PIRATE_WEATHER' ? [401, 403] : []);
		if (current.kind !== 'success') {
			weatherState.set({ kind: 'error', message });
		} else {
			refreshFailed.set(true);
			refreshError.set(refreshErrorReason(message));
		}
	} finally {
		if (gen === fetchGeneration) isRefreshing.set(false);
	}
}

export async function updateLocationName(localeTag: string) {
	// Use advanced mode override coordinates if present (read from localStorage, not the store, to
	// avoid stale state). This prevents language switches from reverse-geocoding real GPS coordinates.
	const overrideLat = typeof localStorage !== 'undefined' ? localStorage.getItem('advanced_override_lat') : null;
	const overrideLon = typeof localStorage !== 'undefined' ? localStorage.getItem('advanced_override_lon') : null;
	const lat = overrideLat ? parseFloat(overrideLat) : getCachedLocation()?.lat;
	const lon = overrideLon ? parseFloat(overrideLon) : getCachedLocation()?.lon;
	if (lat == null || lon == null) return;
	const location = await reverseGeocode(lat, lon, localeTag);
	cacheLocation(lat, lon, location.name, location.subtext);
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
