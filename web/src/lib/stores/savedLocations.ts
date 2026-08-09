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

/** Switches the active location. Pass null to switch back to GPS ("Current Location"). */
export function switchToLocation(id: string | null) {
	activeLocationId.set(id);
	if (typeof localStorage !== 'undefined') {
		if (id == null) localStorage.removeItem(ACTIVE_LOCATION_KEY);
		else localStorage.setItem(ACTIVE_LOCATION_KEY, id);
	}
	showSavedLocationsDialog.set(false);
	doFetchWeather();
}
