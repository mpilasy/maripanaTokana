import { writable } from 'svelte/store';
import { doFetchWeather } from '$lib/stores/weather';

export const devModeActive = writable(false);
export const showGpsCoordinates = writable(false);
export const showLocationOverrideDialog = writable(false);

export interface DevOverrideLocation {
    lat: number;
    lon: number;
    name: string;
}

export const locationOverride = writable<DevOverrideLocation | null>(null);

let locationClicks = 0;
let lastLocationClickTime = 0;

export function checkDevModeExpiration(): boolean {
    if (typeof localStorage === 'undefined') return false;
    const expiration = localStorage.getItem('dev_mode_expiration');
    if (!expiration) return false;
    const isExpired = Date.now() >= parseInt(expiration, 10);
    if (isExpired) {
        clearDevModeOverride();
        return false;
    }
    return true;
}

export function initDevMode() {
    if (typeof localStorage === 'undefined') return;
    const active = checkDevModeExpiration();
    devModeActive.set(active);
    if (active) {
        const lat = localStorage.getItem('dev_override_lat');
        const lon = localStorage.getItem('dev_override_lon');
        const name = localStorage.getItem('dev_override_name');
        if (lat && lon && name) {
            locationOverride.set({ lat: parseFloat(lat), lon: parseFloat(lon), name });
        }
    } else {
        clearDevModeOverride();
    }
}

export function onLocationClicked() {
    const now = Date.now();
    if (now - lastLocationClickTime > 500) {
        locationClicks = 0;
    }
    lastLocationClickTime = now;
    locationClicks++;

    if (locationClicks >= 5) {
        locationClicks = 0;
        const expiration = now + 4 * 60 * 60 * 1000; // 4 hours
        localStorage.setItem('dev_mode_expiration', expiration.toString());
        devModeActive.set(true);
        showLocationOverrideDialog.set(true);
    } else {
        showGpsCoordinates.update((v) => !v);
    }
}

export function setLocationOverride(lat: number, lon: number, name: string) {
    if (typeof localStorage !== 'undefined') {
        localStorage.setItem('dev_override_lat', lat.toString());
        localStorage.setItem('dev_override_lon', lon.toString());
        localStorage.setItem('dev_override_name', name);
    }
    locationOverride.set({ lat, lon, name });
    showLocationOverrideDialog.set(false);
    doFetchWeather();
}

export function clearDevModeOverride() {
    if (typeof localStorage !== 'undefined') {
        localStorage.removeItem('dev_override_lat');
        localStorage.removeItem('dev_override_lon');
        localStorage.removeItem('dev_override_name');
    }
    locationOverride.set(null);
}

export function resetLocationToCurrent() {
    clearDevModeOverride();
    showLocationOverrideDialog.set(false);
    doFetchWeather();
}
