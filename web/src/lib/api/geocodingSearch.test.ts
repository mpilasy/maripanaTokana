// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

import { afterEach, describe, expect, it, vi } from 'vitest';
import { dedupeResults, searchLocations, type SearchResult } from './geocodingSearch';

const r = (id: number, name: string, lat: number, lon: number): SearchResult => ({
	id, name, latitude: lat, longitude: lon, displayName: `${name}, Île-de-France, France`,
});

describe('dedupeResults', () => {
	it('drops same-name results at nearly the same coordinates', () => {
		const out = dedupeResults([r(1, 'Paris', 48.85341, 2.3488), r(2, 'Paris', 48.8566, 2.3522), r(3, 'Paris', 33.66, -95.55)]);
		expect(out.map((x) => x.id)).toEqual([1, 3]);
	});
});

describe('searchLocations fallback', () => {
	const sudbury = (admin1: string) => ({ id: admin1.length, name: 'Sudbury', latitude: 42, longitude: -71, admin1, country: 'United States' });
	const stubFetch = (nominatim: unknown) => vi.stubGlobal('fetch', vi.fn(async (url: string) => ({
		ok: true,
		json: async () => url.includes('open-meteo') ? { results: [sudbury('Massachusetts'), sudbury('Washington')] } : nominatim,
	})));
	afterEach(() => vi.unstubAllGlobals());

	it('keeps Open-Meteo results that match the region', async () => {
		stubFetch([]);
		expect((await searchLocations('Sudbury, MA')).map((r) => r.admin1)).toEqual(['Massachusetts']);
	});

	it('asks Nominatim when no result is in the region', async () => {
		stubFetch([{ place_id: 9, lat: '46.49', lon: '-80.99', name: 'Greater Sudbury', place_rank: 16,
			address: { city: 'Greater Sudbury', state: 'Ontario', country: 'Canada' } }]);
		const out = await searchLocations('Sudbury, ON');
		expect(out.map((r) => r.displayName)).toEqual(['Greater Sudbury, Ontario, Canada']);
	});

	it('keeps Open-Meteo results when Nominatim finds nothing', async () => {
		stubFetch([]);
		expect(await searchLocations('Sudbury, ON')).toHaveLength(2);
	});
});
