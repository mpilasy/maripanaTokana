import { writable, get } from 'svelte/store';
import { doFetchWeather } from '$lib/stores/weather';

export interface SavedLocation {
	id: string;
	name: string;
	subtext?: string;
	latitude: number;
	longitude: number;
}

const SAVED_LOCATIONS_KEY = 'saved_locations';
const ACTIVE_LOCATION_KEY = 'active_location_id';

function loadSavedLocations(): SavedLocation[] {
	if (typeof localStorage === 'undefined') return [];
	const raw = localStorage.getItem(SAVED_LOCATIONS_KEY);
	if (!raw) return [];
	try {
		return JSON.parse(raw);
	} catch {
		return [];
	}
}

function persistSavedLocations(list: SavedLocation[]) {
	if (typeof localStorage !== 'undefined') {
		localStorage.setItem(SAVED_LOCATIONS_KEY, JSON.stringify(list));
	}
}

export const savedLocations = writable<SavedLocation[]>(loadSavedLocations());

export const activeLocationId = writable<string | null>(
	typeof localStorage !== 'undefined' ? localStorage.getItem(ACTIVE_LOCATION_KEY) : null
);

export const showSavedLocationsDialog = writable(false);

export function openSavedLocationsDialog() {
	showSavedLocationsDialog.set(true);
}

/** Preview machinery: switching to a location without saving it. A core location-switching
 * concern, not Advanced-Mode-exclusive — anyone can preview a searched location. */
export interface AdvancedOverrideLocation {
	lat: number;
	lon: number;
	name: string;
	subtext?: string;
}

export const locationOverride = writable<AdvancedOverrideLocation | null>(null);

export function checkOverrideExpiry() {
	if (typeof localStorage === 'undefined') return;
	const setTime = localStorage.getItem('advanced_override_set_time');
	if (!setTime) return;
	if (Date.now() - parseInt(setTime, 10) >= 12 * 60 * 60 * 1000) {
		clearAdvancedModeOverride();
	}
}

export function initLocationOverride() {
	if (typeof localStorage === 'undefined') return;
	const lat = localStorage.getItem('advanced_override_lat');
	const lon = localStorage.getItem('advanced_override_lon');
	const name = localStorage.getItem('advanced_override_name');
	const subtext = localStorage.getItem('advanced_override_subtext') || undefined;
	if (lat && lon && name) {
		locationOverride.set({ lat: parseFloat(lat), lon: parseFloat(lon), name, subtext });
	}
	checkOverrideExpiry();
}

/** Previews a location without saving it: switches the displayed weather to it, but does not add
 * it to the saved-locations list. Expires after 12h. Mutually exclusive with switchToLocation —
 * starting a preview clears the active saved-location id, and switching to a real location clears
 * an active preview. Use favoriteCurrentLocation to save it. */
export function setLocationOverride(lat: number, lon: number, name: string, subtext?: string) {
	if (typeof localStorage !== 'undefined') {
		localStorage.setItem('advanced_override_lat', lat.toString());
		localStorage.setItem('advanced_override_lon', lon.toString());
		localStorage.setItem('advanced_override_name', name);
		localStorage.setItem('advanced_override_set_time', Date.now().toString());
		if (subtext) {
			localStorage.setItem('advanced_override_subtext', subtext);
		} else {
			localStorage.removeItem('advanced_override_subtext');
		}
		localStorage.removeItem(ACTIVE_LOCATION_KEY);
	}
	locationOverride.set({ lat, lon, name, subtext });
	activeLocationId.set(null);
	showSavedLocationsDialog.set(false);
	doFetchWeather();
}

export function clearAdvancedModeOverride() {
	if (typeof localStorage !== 'undefined') {
		localStorage.removeItem('advanced_override_lat');
		localStorage.removeItem('advanced_override_lon');
		localStorage.removeItem('advanced_override_name');
		localStorage.removeItem('advanced_override_subtext');
		localStorage.removeItem('advanced_override_set_time');
	}
	locationOverride.set(null);
}

/** Adds a search result as a saved location (or reuses an existing one at the same
 * coordinates) and immediately switches to it. */
export function addSavedLocation(result: { name: string; latitude: number; longitude: number; admin1?: string; country?: string }) {
	const id = `${result.latitude},${result.longitude}`;
	const subtext = [result.admin1, result.country].filter(Boolean).join(', ') || undefined;
	const current = get(savedLocations);
	if (!current.some((l) => l.id === id)) {
		const updated = [...current, { id, name: result.name, subtext, latitude: result.latitude, longitude: result.longitude }];
		savedLocations.set(updated);
		persistSavedLocations(updated);
	}
	switchToLocation(id);
}

export function removeSavedLocation(id: string) {
	const updated = get(savedLocations).filter((l) => l.id !== id);
	savedLocations.set(updated);
	persistSavedLocations(updated);
	if (get(activeLocationId) === id) switchToLocation(null);
}

/** Promotes the location currently being previewed (via setLocationOverride) into a saved
 * favorite, and switches to it via its saved-location id. No-op if nothing is being previewed. */
export function favoriteCurrentLocation() {
	const override = get(locationOverride);
	if (!override) return;
	const id = `${override.lat},${override.lon}`;
	const current = get(savedLocations);
	if (!current.some((l) => l.id === id)) {
		const updated = [...current, { id, name: override.name, subtext: override.subtext, latitude: override.lat, longitude: override.lon }];
		savedLocations.set(updated);
		persistSavedLocations(updated);
	}
	switchToLocation(id);
}

/** Unfavorites the currently active saved location, if any. */
export function unfavoriteCurrentLocation() {
	const id = get(activeLocationId);
	if (id) removeSavedLocation(id);
}

/** Switches the active location. Pass null to switch back to GPS ("Current Location"). Clears
 * any active preview (see setLocationOverride) — the two are mutually exclusive. */
export function switchToLocation(id: string | null) {
	clearAdvancedModeOverride();
	activeLocationId.set(id);
	if (typeof localStorage !== 'undefined') {
		if (id == null) localStorage.removeItem(ACTIVE_LOCATION_KEY);
		else localStorage.setItem(ACTIVE_LOCATION_KEY, id);
	}
	showSavedLocationsDialog.set(false);
	doFetchWeather();
}
