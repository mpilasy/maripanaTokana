import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { lookupLocation, _clearGeocodeCache } from './location';

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
});
