import { writable } from 'svelte/store';
import { doFetchWeather, restoreGpsWeather } from '$lib/stores/weather';

export const expertModeActive = writable(false);
export const showGpsCoordinates = writable(false);
export const showLocationOverrideDialog = writable(false);

export interface DevOverrideLocation {
    lat: number;
    lon: number;
    name: string;
    subtext?: string;
}

export const locationOverride = writable<DevOverrideLocation | null>(null);

export function checkOverrideExpiry() {
    if (typeof localStorage === 'undefined') return;
    const setTime = localStorage.getItem('expert_override_set_time');
    if (!setTime) return;
    if (Date.now() - parseInt(setTime, 10) >= 12 * 60 * 60 * 1000) {
        clearDevModeOverride();
    }
}

export function initExpertMode() {
    if (typeof localStorage === 'undefined') return;
    const active = localStorage.getItem('expert_mode_enabled') === 'true';
    expertModeActive.set(active);
    const lat = localStorage.getItem('dev_override_lat');
    const lon = localStorage.getItem('dev_override_lon');
    const name = localStorage.getItem('dev_override_name');
    const subtext = localStorage.getItem('dev_override_subtext') || undefined;
    if (lat && lon && name) {
        locationOverride.set({ lat: parseFloat(lat), lon: parseFloat(lon), name, subtext });
    }
    checkOverrideExpiry();
}

export function onLocationClicked() {
    showGpsCoordinates.update((v) => !v);
}

export function enableExpertMode() {
    if (typeof localStorage !== 'undefined') {
        localStorage.setItem('expert_mode_enabled', 'true');
    }
    expertModeActive.set(true);
}

export function disableExpertMode() {
    if (typeof localStorage !== 'undefined') {
        localStorage.removeItem('expert_mode_enabled');
    }
    expertModeActive.set(false);
    clearDevModeOverride();
    showLocationOverrideDialog.set(false);
    restoreGpsWeather();
}

export function openLocationOverride() {
    showLocationOverrideDialog.set(true);
}

export function setLocationOverride(lat: number, lon: number, name: string, subtext?: string) {
    if (typeof localStorage !== 'undefined') {
        localStorage.setItem('dev_override_lat', lat.toString());
        localStorage.setItem('dev_override_lon', lon.toString());
        localStorage.setItem('dev_override_name', name);
        localStorage.setItem('expert_override_set_time', Date.now().toString());
        if (subtext) {
            localStorage.setItem('dev_override_subtext', subtext);
        } else {
            localStorage.removeItem('dev_override_subtext');
        }
    }
    locationOverride.set({ lat, lon, name, subtext });
    showLocationOverrideDialog.set(false);
    doFetchWeather();
}

export function clearDevModeOverride() {
    if (typeof localStorage !== 'undefined') {
        localStorage.removeItem('dev_override_lat');
        localStorage.removeItem('dev_override_lon');
        localStorage.removeItem('dev_override_name');
        localStorage.removeItem('dev_override_subtext');
        localStorage.removeItem('expert_override_set_time');
    }
    locationOverride.set(null);
}

export function resetLocationToCurrent() {
    clearDevModeOverride();
    showLocationOverrideDialog.set(false);
    doFetchWeather();
}
