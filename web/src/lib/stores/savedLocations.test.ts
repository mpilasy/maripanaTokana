// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

import { describe, expect, it, vi } from 'vitest';

const here = vi.hoisted(() => ({ value: null as { lat: number; lon: number } | null }));
vi.mock('#lib/stores/location.js', () => ({ getCachedLocation: () => here.value }));
vi.mock('#lib/stores/weather.js', () => ({ doFetchWeather: vi.fn(), keepShownWeather: vi.fn(), weatherState: { subscribe: vi.fn() } }));

import { sortedByDistance } from './savedLocations.js';

const place = (name: string, latitude: number, longitude: number) => ({ id: `${latitude},${longitude}`, name, latitude, longitude });
const paris = place('Paris', 48.8566, 2.3522);
const toamasina = place('Toamasina', -18.1492, 49.4023);
const antsirabe = place('Antsirabe', -19.8659, 47.0333);

describe('sortedByDistance', () => {
	it('puts the nearest place first', () => {
		here.value = { lat: -18.8792, lon: 47.5079 }; // Antananarivo
		expect(sortedByDistance([paris, toamasina, antsirabe]).map((l) => l.name)).toEqual(['Antsirabe', 'Toamasina', 'Paris']);
	});

	it('keeps the order when the device location is not known', () => {
		here.value = null;
		expect(sortedByDistance([paris, toamasina, antsirabe]).map((l) => l.name)).toEqual(['Paris', 'Toamasina', 'Antsirabe']);
	});
});
