import { writable, get } from 'svelte/store';
import type { WeatherData } from '$lib/domain/weatherData';
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
	alertsWmoSwicEnabled, alertsBomEnabled, alertsNhcEnabled,
} from '$lib/stores/preferences';
import { SUPPORTED_LOCALES } from '$lib/i18n/locales';
import { advancedModeActive, checkOverrideExpiry } from '$lib/stores/advancedMode';
import { activeLocationId, savedLocations } from '$lib/stores/savedLocations';

export type WeatherState =
	| { kind: 'loading' }
	| { kind: 'success'; data: WeatherData }
	| { kind: 'error'; message: string };

export const weatherState = writable<WeatherState>({ kind: 'loading' });
export const isRefreshing = writable<boolean>(false);

const STALE_MS = 30 * 60 * 1000; // 30 minutes

// Cached GPS weather data fetched in background during advanced mode
let cachedGpsWeatherData: WeatherData | null = null;

async function fetchAtLocation(lat: number, lon: number, knownName?: string, knownSubtext?: string, localeTag?: string, updateAlerts = true): Promise<WeatherData> {
	const src = get(weatherSource);
	const apiKey = get(weatherApiKey);

	const namePromise = knownName
		? Promise.resolve({ name: knownName, subtext: knownSubtext })
		: reverseGeocode(lat, lon, localeTag);

	if (src === 'PIRATE_WEATHER' && apiKey) {
		const location = await namePromise;
		const data = await fetchPirateWeather(lat, lon, apiKey, location.name, knownSubtext ?? location.subtext);
		if (updateAlerts) fetchAlertsForData(lat, lon);
		return data;
	}

	// Open-Meteo path
	const weatherPromise = fetchWeather(lat, lon);
	const airQualityResponsePromise = fetchAirQuality(lat, lon).catch(() => null);
	// Country decides which AQI standard is primary (european_aqi vs us_aqi) — same lookup
	// used for alert-source gating in fetchAllAlerts. Fetched once here and passed through to
	// avoid firing a second, redundant reverse-geocode request from fetchAllAlerts.
	const locationInfoPromise = getLocationInfo(lat, lon);
	const [response, location, airQualityResponse, locationInfo] = await Promise.all([weatherPromise, namePromise, airQualityResponsePromise, locationInfoPromise]);
	const airQuality = airQualityResponse ? mapToAirQuality(airQualityResponse, locationInfo.countryCode) : null;
	const hourlyAirQuality = airQualityResponse ? mapToHourlyAirQuality(airQualityResponse) : [];
	const data = { ...mapToWeatherData(response, location.name, knownSubtext || location.subtext), airQuality, hourlyAirQuality };

	if (updateAlerts) fetchAlertsForData(lat, lon, locationInfo);

	return data;
}

function setWeatherData(data: WeatherData) {
	weatherState.update(s => {
		const existingAlerts =
			s.kind === 'success' && s.data.locationName === data.locationName
				? s.data.alerts
				: [];
		return { kind: 'success', data: { ...data, alerts: existingAlerts } };
	});
}

async function fetchAlertsForData(lat: number, lon: number, locationInfo?: LocationInfo) {
	try {
		const settings: AlertSettings = {
			alertsEnabled: get(alertsEnabled),
			alertsNwsEnabled: get(alertsNwsEnabled),
			alertsGdacsEnabled: get(alertsGdacsEnabled),
			alertsMeteoAlarmEnabled: get(alertsMeteoAlarmEnabled),
			alertsJmaEnabled: get(alertsJmaEnabled),
			alertsEcccEnabled: get(alertsEcccEnabled),
			alertsWmoSwicEnabled: get(alertsWmoSwicEnabled),
			alertsBomEnabled: get(alertsBomEnabled),
			alertsNhcEnabled: get(alertsNhcEnabled),
		};
		const alerts = await fetchAllAlerts(lat, lon, settings, locationInfo);
		weatherState.update(s => {
			if (s.kind === 'success') {
				return { ...s, data: { ...s.data, alerts, alertsLoading: false } };
			}
			return s;
		});
	} catch {
		weatherState.update(s => {
			if (s.kind === 'success') {
				return { ...s, data: { ...s.data, alertsLoading: false } };
			}
			return s;
		});
	}
}

/** Background-fetch GPS weather and cache it for when advanced mode is disabled */
function spawnGpsCacheRefresh() {
	getPosition()
		.then(async (fresh) => {
			const cached = getCachedLocation();
			const lat = fresh.lat;
			const lon = fresh.lon;
			const name = cached?.name;
			const subtext = cached?.subtext;
			const data = await fetchAtLocation(lat, lon, name, subtext, undefined, false);
			cachedGpsWeatherData = data;
			// Don't overwrite the location cache while advanced mode is active — it would corrupt
			// the cached_location key and cause updateLocationName to reverse-geocode the
			// wrong (real GPS) coordinates on language change.
			if (!get(advancedModeActive)) {
				cacheLocation(lat, lon, data.locationName, data.locationSubtext);
			}
		})
		.catch(() => {
			// Silently fail - this is a best-effort background refresh
		});
}

/** Called when advanced mode is disabled to immediately show GPS weather */
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
	const current = get(weatherState);
	if (current.kind !== 'success') {
		weatherState.set({ kind: 'loading' });
	} else {
		isRefreshing.set(true);
	}

	try {
		if (get(advancedModeActive)) {
			checkOverrideExpiry();
			const lat = localStorage.getItem('advanced_override_lat');
			const lon = localStorage.getItem('advanced_override_lon');
			const name = localStorage.getItem('advanced_override_name');
			const subtext = localStorage.getItem('advanced_override_subtext') || undefined;
			if (lat && lon && name) {
				const lLat = parseFloat(lat);
				const lLon = parseFloat(lon);
				const data = await fetchAtLocation(lLat, lLon, name, subtext);
				setWeatherData(data);
				isRefreshing.set(false);
				spawnGpsCacheRefresh();
				return;
			}
		}

		const activeSavedId = get(activeLocationId);
		if (activeSavedId) {
			const savedLocation = get(savedLocations).find((l) => l.id === activeSavedId);
			if (savedLocation) {
				const data = await fetchAtLocation(savedLocation.latitude, savedLocation.longitude, savedLocation.name, savedLocation.subtext);
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

		if (cachedFetchPromise) {
			data = await cachedFetchPromise;
			setWeatherData(data);
		}

		// Step 2: get fresh location
		const fresh = await freshLocationPromise;

		// Re-fetch if moved significantly or if we had no cached location
		if (!cached || movedSignificantly(cached.lat, cached.lon, fresh.lat, fresh.lon)) {
			data = await fetchAtLocation(fresh.lat, fresh.lon, undefined, undefined, localeTag);
			setWeatherData(data);
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
