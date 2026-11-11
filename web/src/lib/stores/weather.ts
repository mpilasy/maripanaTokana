// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

import { writable, get } from 'svelte/store';
import type { WeatherData } from '#lib/domain/weatherData.js';
import { classifyError, HttpError } from '#lib/api/http.js';
import { saveSnapshot, loadSnapshot } from '#lib/stores/weatherSnapshot.js';
import { fetchWeather } from '#lib/api/openMeteo.js';
import { fetchPirateWeather } from '#lib/api/pirateWeather.js';
import { fetchAllAlerts, type AlertSettings } from '#lib/api/externalAlerts.js';
import { mapToWeatherData } from '#lib/api/openMeteoMapper.js';
import { fetchMarine, mapToSea } from '#lib/api/openMeteoMarine.js';
import { cachedNormals, fetchNormals } from '#lib/api/openMeteoArchive.js';
import { normalFor, placeIsoDate, type DayNormal } from '#lib/domain/normals.js';
import { changesSinceYesterday, forecastHistoryKey, recordForecast, toSnapshot } from '#lib/stores/forecastHistory.js';
import { fetchAirQuality, mapToAirQuality, mapToHourlyAirQuality } from '#lib/api/openMeteoAirQuality.js';
import { getLocationInfo, type LocationInfo } from '#lib/api/alerts/shared.js';
import {
	getCachedLocation, cacheLocation, movedSignificantly,
	getPosition, reverseGeocode, lookupLocation
} from '#lib/stores/location.js';
import { isLatinScript } from '#lib/domain/script.js';
import { weatherModelFor } from '#lib/domain/weatherModel.js';
import {
	localeIndex,
	weatherSource, weatherApiKey, weatherModel,
	alertsEnabled, alertsNwsEnabled, alertsGdacsEnabled,
	alertsMeteoAlarmEnabled, alertsJmaEnabled, alertsEcccEnabled,
	alertsBomEnabled, alertsNhcEnabled,
} from '#lib/stores/preferences.js';
import { SUPPORTED_LOCALES } from '#lib/i18n/locales.js';
import {
	activeLocationId, savedLocations, locationOverride, checkOverrideExpiry, displayName, setSavedLocationSubtext,
	type PreviewLocation, type SavedLocation
} from '#lib/stores/savedLocations.js';

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
/**
 * Set when the device-location weather uses the cached position because the fresh position failed.
 * `time` is the time of the cached fix (absent for a location saved before 1.7). Null otherwise.
 */
export const lastKnownLocation = writable<{ time?: number } | null>(null);

/** Only informative failures get shown next to the generic "couldn't refresh" line. */
export function refreshErrorReason(key: string): string | null {
	return key === 'error_fetch_weather' ? null : key;
}

const STALE_MS = 30 * 60 * 1000;

// Incremented on every doFetchWeather; results of older fetches (including their alerts) are dropped.
let fetchGeneration = 0;

// Open-Meteo snaps response coordinates to a ~10 km grid, so compare with a tolerance.
const SAME_LOCATION_DEG = 0.1;

// The GPS weather, fetched in the background while a preview shows another place
let cachedGpsWeatherData: WeatherData | null = null;

async function fetchAtLocation(lat: number, lon: number, knownName?: string, knownSubtext?: string, localeTag?: string, alertsGeneration: number | null = fetchGeneration): Promise<WeatherData> {
	const src = get(weatherSource);
	const apiKey = get(weatherApiKey);

	// One shared (cached) Nominatim lookup feeds both the display name and country info.
	const namePromise = knownName
		? Promise.resolve({ name: knownName, subtext: knownSubtext })
		: reverseGeocode(lat, lon, localeTag);

	// The sea comes from Open-Meteo for both sources; a failure only leaves the sea section out
	const seaPromise = fetchMarine(lat, lon).then(mapToSea).catch(() => null);

	if (src === 'PIRATE_WEATHER' && apiKey) {
		const location = await namePromise;
		const data = await fetchPirateWeather(lat, lon, apiKey, location.name, knownSubtext ?? location.subtext);
		if (alertsGeneration !== null) fetchAlertsForData(lat, lon, alertsGeneration);
		return { ...data, latitude: lat, longitude: lon, sea: await seaPromise };
	}

	const weatherPromise = fetchWeather(lat, lon, weatherModelFor(get(weatherModel))?.key ?? null);
	const airQualityResponsePromise = fetchAirQuality(lat, lon).catch(() => null);
	// The country picks the primary AQI standard (european_aqi or us_aqi). fetchAllAlerts gets the same
	// lookup, so it does not reverse geocode again.
	const locationInfoPromise = getLocationInfo(lat, lon, localeTag);
	const [response, location, airQualityResponse, locationInfo, sea] = await Promise.all([weatherPromise, namePromise, airQualityResponsePromise, locationInfoPromise, seaPromise]);
	const airQuality = airQualityResponse ? mapToAirQuality(airQualityResponse, locationInfo.countryCode) : null;
	const hourlyAirQuality = airQualityResponse ? mapToHourlyAirQuality(airQualityResponse) : [];
	// The requested point, not the forecast grid cell Open-Meteo snaps it to (as on Android)
	const data: WeatherData = { ...mapToWeatherData(response, location.name, knownSubtext || location.subtext), latitude: lat, longitude: lon, airQuality, hourlyAirQuality, sea };
	// The service worker replayed an old cached response (network failed): show its real age, not "now".
	if (response.cachedAt) {
		data.timestamp = response.cachedAt;
		data.staleFromCache = true;
	}

	if (alertsGeneration !== null) fetchAlertsForData(lat, lon, alertsGeneration, locationInfo);

	return data;
}

/** The favorite on screen, if one is (not a preview or GPS). */
function activeSavedLocation() {
	if (get(locationOverride)) return undefined;
	const id = get(activeLocationId);
	return id ? get(savedLocations).find((l) => l.id === id) : undefined;
}

/** Snapshot key for the location the next fetch will show: preview, saved id, or GPS. */
function currentSnapshotKey(): string {
	if (get(locationOverride)) return 'preview';
	const id = get(activeLocationId);
	if (id && get(savedLocations).some((l) => l.id === id)) return id;
	return 'gps';
}

/** Files the weather on screen under the now-current location (a preview just favorited) without refetching. */
export function keepShownWeather() {
	const s = get(weatherState);
	if (s.kind === 'success') saveSnapshot(currentSnapshotKey(), { ...s.data, alerts: [], alertsLoading: true });
}

/** The source and model that made the forecast: Pirate Weather needs a key, else Open-Meteo answers. */
function forecastOrigin(): [string, string] {
	if (get(weatherSource) === 'PIRATE_WEATHER' && get(weatherApiKey)) return ['PIRATE_WEATHER', ''];
	return ['OPEN_METEO', weatherModelFor(get(weatherModel))?.key ?? 'auto'];
}

/** The forecast with what changed since about 24 hours ago. A fresh forecast is also saved for later comparisons. */
function withForecastChanges(data: WeatherData, fresh: boolean): WeatherData {
	if (data.dailyForecast.length === 0) return data;
	const key = forecastHistoryKey(data.latitude, data.longitude, ...forecastOrigin());
	const snapshot = toSnapshot(data.dailyForecast, data.utcOffsetSeconds, data.timestamp);
	const forecastChanges = changesSinceYesterday(key, snapshot);
	if (fresh) recordForecast(key, snapshot);
	return { ...data, forecastChanges };
}

/** The normal of each day of the forecast. */
function alignNormals(normals: (DayNormal | null)[], data: WeatherData): (DayNormal | null)[] {
	return data.dailyForecast.map((d) => normalFor(normals, placeIsoDate(d.date, data.utcOffsetSeconds)));
}

/** Loads the normals of the place in the background; a failure shows nothing. */
async function loadNormals(lat: number, lon: number) {
	const normals = await fetchNormals(lat, lon);
	if (!normals) return;
	weatherState.update((s) =>
		s.kind === 'success' && s.data.latitude === lat && s.data.longitude === lon
			? { kind: 'success', data: { ...s.data, normals: alignNormals(normals, s.data) } }
			: s
	);
}

function setWeatherData(fetched: WeatherData) {
	const { staleFromCache, ...rest } = fetched;
	const data = withForecastChanges(rest, !staleFromCache);
	const normals = cachedNormals(data.latitude, data.longitude);
	data.normals = normals ? alignNormals(normals, data) : undefined;
	refreshFailed.set(!!staleFromCache);
	refreshError.set(staleFromCache ? 'error_offline' : null);
	lastKnownLocation.set(null);
	saveSnapshot(currentSnapshotKey(), { ...data, alerts: [], alertsLoading: true });
	weatherState.update(s => {
		const existingAlerts =
			s.kind === 'success'
			&& Math.abs(s.data.latitude - data.latitude) < SAME_LOCATION_DEG
			&& Math.abs(s.data.longitude - data.longitude) < SAME_LOCATION_DEG
				? s.data.alerts
				: [];
		// A refresh of the same place keeps the Latin-letter name already found for it
		const roman = s.kind === 'success' && s.data.romanName && s.data.locationName === data.locationName && s.data.latitude === data.latitude
			? { romanName: s.data.romanName, romanSubtext: s.data.romanSubtext }
			: {};
		return { kind: 'success', data: { ...data, ...roman, alerts: existingAlerts } };
	});
	lookUpRomanName();
	if (!normals) loadNormals(data.latitude, data.longitude);
}

/** For a name in a non-Latin script, finds its Latin-letter version in the background. */
async function lookUpRomanName() {
	const s = get(weatherState);
	if (s.kind !== 'success' || s.data.romanName || isLatinScript(s.data.locationName)) return;
	// A favorite the user renamed keeps their name; there is no other script to offer
	if (activeSavedLocation()?.label) return;
	const { latitude, longitude, locationName } = s.data;
	const r = await lookupLocation(latitude, longitude, 'en');
	if (!r || !isLatinScript(r.name) || r.name === locationName) return;
	weatherState.update((cur) =>
		cur.kind === 'success' && cur.data.latitude === latitude && cur.data.locationName === locationName
			? { kind: 'success', data: { ...cur.data, romanName: r.name, romanSubtext: r.subtext } }
			: cur
	);
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

/** Fetches the GPS weather in the background and keeps it for when the preview is cleared. */
function spawnGpsCacheRefresh() {
	getPosition()
		.then(async (fresh) => {
			const cached = getCachedLocation();
			const lat = fresh.lat;
			const lon = fresh.lon;
			// The saved name only if it is in the current language; otherwise look it up again
			const localeTag = SUPPORTED_LOCALES[get(localeIndex)]?.tag;
			const fits = cached?.lang === localeTag;
			const data = await fetchAtLocation(lat, lon, fits ? cached?.name : undefined, fits ? cached?.subtext : undefined, localeTag, null);
			cachedGpsWeatherData = data;
			// Not during a preview: updateLocationName would then look up the GPS place on a language change
			if (!get(locationOverride)) {
				cacheLocation(lat, lon, data.locationName, data.locationSubtext, localeTag);
			}
		})
		.catch(() => {
			// Best effort: a failure shows nothing
		});
}

/** Shows the GPS weather at once when a preview is cleared. */
export function restoreGpsWeather() {
	if (cachedGpsWeatherData) {
		setWeatherData(cachedGpsWeatherData);
		cachedGpsWeatherData = null;
		doFetchWeather();
	} else {
		doFetchWeather();
	}
}

export async function doFetchWeather() {
	const gen = ++fetchGeneration;
	showSnapshotWhileLoading();
	try {
		checkOverrideExpiry();
		const override = get(locationOverride);
		const favorite = activeSavedLocation();
		if (override) await fetchPreview(gen, override);
		else if (favorite) await fetchFavorite(gen, favorite);
		else await fetchDeviceLocation(gen);
	} catch (err) {
		if (gen === fetchGeneration) showFetchError(err);
	} finally {
		if (gen === fetchGeneration) isRefreshing.set(false);
	}
}

/** Keeps the weather on screen, or shows the last saved snapshot (original timestamp), while the fetch runs. */
function showSnapshotWhileLoading() {
	if (get(weatherState).kind === 'success') {
		isRefreshing.set(true);
		return;
	}
	const snapshot = loadSnapshot(currentSnapshotKey());
	if (snapshot) {
		weatherState.set({ kind: 'success', data: snapshot });
		isRefreshing.set(true);
	} else {
		weatherState.set({ kind: 'loading' });
	}
}

async function fetchPreview(gen: number, override: PreviewLocation) {
	const data = await fetchAtLocation(override.lat, override.lon, override.name, override.subtext);
	if (gen !== fetchGeneration) return;
	setWeatherData(data);
	isRefreshing.set(false);
	spawnGpsCacheRefresh();
}

async function fetchFavorite(gen: number, savedLocation: SavedLocation) {
	// The region and country are looked up again in the current language; the favorite keeps its
	// name (or the user's own) and falls back to the saved line when the lookup fails
	const looked = await fetchAtLocation(savedLocation.latitude, savedLocation.longitude, undefined, undefined, SUPPORTED_LOCALES[get(localeIndex)]?.tag);
	if (gen !== fetchGeneration) return;
	if (looked.locationSubtext) setSavedLocationSubtext(savedLocation.id, looked.locationSubtext);
	setWeatherData({ ...looked, locationName: displayName(savedLocation), locationSubtext: looked.locationSubtext ?? savedLocation.subtext });
	isRefreshing.set(false);
}

/** Shows the weather at the cached position at once, then at a fresh position when the device moved. */
async function fetchDeviceLocation(gen: number) {
	const cached = getCachedLocation();
	const localeTag = SUPPORTED_LOCALES[get(localeIndex)]?.tag;
	// The saved name only if it is in the current language; otherwise look it up again
	const nameFits = cached?.lang === localeTag;

	// The weather at the cached position and the fresh position are fetched at the same time
	const cachedFetchPromise = cached
		? fetchAtLocation(cached.lat, cached.lon, nameFits ? cached.name : undefined, nameFits ? cached.subtext : undefined, localeTag)
		: null;

	const freshLocationPromise = getPosition();
	freshLocationPromise.catch(() => {}); // handled below; avoid unhandled rejection on early exit

	const shown = cachedFetchPromise ? await showCachedLocationWeather(gen, cachedFetchPromise) : { data: null, error: null };
	if (!shown) return;
	const { data, error: cachedError } = shown;
	const cachedShown = data !== null;

	const fresh = await freshPosition(gen, freshLocationPromise, cachedShown);
	if (!fresh) {
		// The fresh position failed: the weather on screen is for the cached position
		if (cachedShown && gen === fetchGeneration) lastKnownLocation.set({ time: cached?.time });
		return;
	}

	const moved = !cached || movedSignificantly(cached.lat, cached.lon, fresh.lat, fresh.lon);
	// The cached-location fetch failed and the device did not move: the same request would fail again.
	if (cachedError && !moved) throw cachedError;

	if (moved || !cachedShown) {
		const freshData = await fetchAtLocation(fresh.lat, fresh.lon, undefined, undefined, localeTag);
		if (gen !== fetchGeneration) return;
		setWeatherData(freshData);
		cacheLocation(fresh.lat, fresh.lon, freshData.locationName, freshData.locationSubtext, localeTag);
	} else {
		// Update cached coordinates to fresher ones, keeping the name shown (looked up again when its language changed)
		cacheLocation(fresh.lat, fresh.lon, data.locationName, data.locationSubtext, localeTag);
	}
}

/** The fresh position; null when a newer fetch started, or when it failed while the cached-location weather is shown. */
async function freshPosition(gen: number, locating: ReturnType<typeof getPosition>, cachedShown: boolean): Promise<Awaited<ReturnType<typeof getPosition>> | null> {
	try {
		const fresh = await locating;
		return gen === fetchGeneration ? fresh : null;
	} catch (err) {
		if (gen !== fetchGeneration) return null;
		// Cached-location weather is already on screen; a failed GPS fix is not a failed refresh.
		if (cachedShown) {
			console.warn('Fresh position failed; keeping cached-location weather', err);
			return null;
		}
		throw err;
	}
}

/**
 * Shows the weather fetched for the cached position. Returns it, or the error when the fresh position may
 * still work; null when a newer fetch started. Throws a 4xx (bad key, rate limit): it would fail identically
 * for any location, so it is not worth asking again.
 */
async function showCachedLocationWeather(gen: number, fetching: Promise<WeatherData>): Promise<{ data: WeatherData | null; error: unknown } | null> {
	try {
		const data = await fetching;
		if (gen !== fetchGeneration) return null;
		setWeatherData(data);
		return { data, error: null };
	} catch (err) {
		if (gen !== fetchGeneration) return null;
		if (err instanceof HttpError && err.status < 500) throw err;
		return { data: null, error: err };
	}
}

/** An error screen when nothing is shown yet; otherwise keeps the old weather and marks the refresh as failed. */
function showFetchError(err: unknown) {
	const message = classifyError(err, get(weatherSource) === 'PIRATE_WEATHER' ? [401, 403] : []);
	if (get(weatherState).kind !== 'success') {
		weatherState.set({ kind: 'error', message });
	} else {
		refreshFailed.set(true);
		refreshError.set(refreshErrorReason(message));
	}
}

export async function updateLocationName(localeTag: string) {
	// A favorite keeps the name it was saved with (or the user's own); only its region and country follow the language
	const favorite = activeSavedLocation();
	if (favorite) {
		const location = await reverseGeocode(favorite.latitude, favorite.longitude, localeTag);
		if (!location.subtext || activeSavedLocation()?.id !== favorite.id) return;
		setSavedLocationSubtext(favorite.id, location.subtext);
		weatherState.update((s) => (s.kind === 'success' ? { ...s, data: { ...s.data, locationSubtext: location.subtext } } : s));
		return;
	}
	// The preview coordinates when there is a preview (from localStorage, not the store, which can be stale),
	// so that a language change does not look up the GPS place
	const overrideLat = typeof localStorage !== 'undefined' ? localStorage.getItem('advanced_override_lat') : null;
	const overrideLon = typeof localStorage !== 'undefined' ? localStorage.getItem('advanced_override_lon') : null;
	const lat = overrideLat ? parseFloat(overrideLat) : getCachedLocation()?.lat;
	const lon = overrideLon ? parseFloat(overrideLon) : getCachedLocation()?.lon;
	if (lat == null || lon == null) return;
	const location = await reverseGeocode(lat, lon, localeTag);
	// Only the name changes: keep the time of the fix
	cacheLocation(lat, lon, location.name, location.subtext, localeTag, getCachedLocation()?.time ?? null);
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
