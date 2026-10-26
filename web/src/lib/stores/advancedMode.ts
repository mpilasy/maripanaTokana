// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

import { writable } from 'svelte/store';
import { restoreGpsWeather } from '#lib/stores/weather.js';
import { clearAdvancedModeOverride } from '#lib/stores/savedLocations.js';

export const advancedModeActive = writable(false);

export function initAdvancedMode() {
    if (typeof localStorage === 'undefined') return;
    const active = localStorage.getItem('advanced_mode_enabled') === 'true';
    advancedModeActive.set(active);
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
