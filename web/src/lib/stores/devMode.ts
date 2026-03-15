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
}

export function onLocationClicked() {
    showGpsCoordinates.update((v) => !v);
}

export function onLocationLongPressed() {
    const now = Date.now();
    const expiration = now + 4 * 60 * 60 * 1000; // 4 hours
    if (typeof localStorage !== 'undefined') {
        localStorage.setItem('dev_mode_expiration', expiration.toString());
    }
    devModeActive.set(true);
}

export function onLocationDoubleClicked() {
    let active = false;
    devModeActive.subscribe(v => active = v)();

    if (active) {
        showLocationOverrideDialog.set(true);
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
