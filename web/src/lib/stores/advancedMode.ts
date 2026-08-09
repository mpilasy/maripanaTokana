import { writable } from 'svelte/store';
import { doFetchWeather, restoreGpsWeather } from '$lib/stores/weather';

export const advancedModeActive = writable(false);
export const showGpsCoordinates = writable(false);
export const showLocationOverrideDialog = writable(false);

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

export function initAdvancedMode() {
    if (typeof localStorage === 'undefined') return;
    const active = localStorage.getItem('advanced_mode_enabled') === 'true';
    advancedModeActive.set(active);
    const lat = localStorage.getItem('advanced_override_lat');
    const lon = localStorage.getItem('advanced_override_lon');
    const name = localStorage.getItem('advanced_override_name');
    const subtext = localStorage.getItem('advanced_override_subtext') || undefined;
    if (lat && lon && name) {
        locationOverride.set({ lat: parseFloat(lat), lon: parseFloat(lon), name, subtext });
    }
    checkOverrideExpiry();
}

export function onLocationClicked() {
    showGpsCoordinates.update((v) => !v);
}

export function enableAdvancedMode() {
    if (typeof localStorage !== 'undefined') {
        localStorage.setItem('advanced_mode_enabled', 'true');
    }
    advancedModeActive.set(true);
}

export function disableAdvancedMode() {
    if (typeof localStorage !== 'undefined') {
        localStorage.removeItem('advanced_mode_enabled');
    }
    advancedModeActive.set(false);
    clearAdvancedModeOverride();
    showLocationOverrideDialog.set(false);
    restoreGpsWeather();
}

export function openLocationOverride() {
    showLocationOverrideDialog.set(true);
}

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
    }
    locationOverride.set({ lat, lon, name, subtext });
    showLocationOverrideDialog.set(false);
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

export function resetLocationToCurrent() {
    clearAdvancedModeOverride();
    showLocationOverrideDialog.set(false);
    doFetchWeather();
}
