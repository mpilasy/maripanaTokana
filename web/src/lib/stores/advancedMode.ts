import { writable } from 'svelte/store';
import { restoreGpsWeather } from '$lib/stores/weather';
import { clearAdvancedModeOverride } from '$lib/stores/savedLocations';

export const advancedModeActive = writable(false);
export const showGpsCoordinates = writable(false);

export function initAdvancedMode() {
    if (typeof localStorage === 'undefined') return;
    const active = localStorage.getItem('advanced_mode_enabled') === 'true';
    advancedModeActive.set(active);
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
    restoreGpsWeather();
}
