// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { lookupLocation, _clearGeocodeCache, LOCAL_NAMES } from './location';

describe('lookupLocation cache', () => {
	beforeEach(() => _clearGeocodeCache());
	afterEach(() => vi.unstubAllGlobals());

	it('serves nearby coords from one fetch', async () => {
		const fetchMock = vi.fn(async () => new Response(JSON.stringify({
			address: { city: 'Paris', state: 'Île-de-France', country: 'France', country_code: 'FR', 'ISO3166-2-lvl4': 'FR-IDF' }
		})));
		vi.stubGlobal('fetch', fetchMock);
		const a = await lookupLocation(48.8566, 2.3522, 'fr');
		const b = await lookupLocation(48.8571, 2.3519, 'fr');
		expect(fetchMock).toHaveBeenCalledTimes(1);
		expect(a).toEqual(b);
		expect(a).toMatchObject({ name: 'Paris', countryCode: 'fr', stateCode: 'IDF' });
	});

	it('does not cache failures', async () => {
		const fetchMock = vi.fn(async () => new Response('', { status: 500 }));
		vi.stubGlobal('fetch', fetchMock);
		expect(await lookupLocation(1, 2)).toBeNull();
		await lookupLocation(1, 2);
		expect(fetchMock).toHaveBeenCalledTimes(2);
	});

	it('asks for local names with "*" in the URL, and for the user language with English as the second choice', async () => {
		const fetchMock = vi.fn(async (_url: string, _init?: RequestInit) => new Response(JSON.stringify({
			address: { county: 'Haute-Savoie', state: 'Auvergne-Rhône-Alpes', country_code: 'fr' }
		})));
		vi.stubGlobal('fetch', fetchMock);
		const local = await lookupLocation(45.9, 6.13, LOCAL_NAMES);
		await lookupLocation(45.9, 6.13, 'mg');
		const header = (i: number) => (fetchMock.mock.calls[i][1]?.headers as Record<string, string>)['Accept-Language'];
		expect(fetchMock.mock.calls[0][0]).toContain('&accept-language=*');
		expect(header(0)).toBeUndefined();
		expect(fetchMock.mock.calls[1][0]).not.toContain('accept-language');
		expect(header(1)).toBe('mg,en;q=0.5');
		expect(local).toMatchObject({ subdivisionName: 'Haute-Savoie', regionName: 'Auvergne-Rhône-Alpes' });
	});
});

describe('geocode cache expiry', () => {
	const DAY = 24 * 60 * 60 * 1000;
	const store = new Map<string, string>();
	const fresh = (at: number) => JSON.stringify({ at, value: { name: 'X', countryCode: null, stateCode: null, subdivisionName: null } });

	beforeEach(() => {
		store.clear();
		_clearGeocodeCache();
		vi.stubGlobal('localStorage', {
			get length() { return store.size; },
			key: (i: number) => [...store.keys()][i] ?? null,
			getItem: (k: string) => store.get(k) ?? null,
			setItem: (k: string, v: string) => void store.set(k, v),
			removeItem: (k: string) => void store.delete(k),
		});
		vi.stubGlobal('fetch', vi.fn(async () => new Response('', { status: 500 })));
	});
	afterEach(() => vi.unstubAllGlobals());

	it('removes an expired entry when it reads it', async () => {
		store.set('geocode_1.00,2.00,', fresh(Date.now() - 2 * DAY));
		expect(await lookupLocation(1, 2)).toBeNull();
		expect(store.has('geocode_1.00,2.00,')).toBe(false);
	});

	it('removes an unparseable entry when it reads it', async () => {
		store.set('geocode_1.00,2.00,', 'not json');
		expect(await lookupLocation(1, 2)).toBeNull();
		expect(store.has('geocode_1.00,2.00,')).toBe(false);
	});

	it('sweeps all expired and unparseable entries once, and keeps others', async () => {
		store.set('geocode_a', fresh(Date.now() - 2 * DAY));
		store.set('geocode_b', '{bad');
		store.set('geocode_c', fresh(Date.now()));
		store.set('other_key', 'x');
		await lookupLocation(5, 6);
		expect([...store.keys()].sort()).toEqual(['geocode_c', 'other_key']);
		// No second sweep in the same page load
		store.set('geocode_d', fresh(Date.now() - 2 * DAY));
		await lookupLocation(7, 8);
		expect(store.has('geocode_d')).toBe(true);
	});
});
