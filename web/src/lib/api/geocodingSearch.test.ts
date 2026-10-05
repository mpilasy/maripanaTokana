import { describe, expect, it } from 'vitest';
import { dedupeResults, type SearchResult } from './geocodingSearch';

const r = (id: number, name: string, lat: number, lon: number): SearchResult => ({
	id, name, latitude: lat, longitude: lon, displayName: `${name}, Île-de-France, France`,
});

describe('dedupeResults', () => {
	it('drops same-name results at nearly the same coordinates', () => {
		const out = dedupeResults([r(1, 'Paris', 48.85341, 2.3488), r(2, 'Paris', 48.8566, 2.3522), r(3, 'Paris', 33.66, -95.55)]);
		expect(out.map((x) => x.id)).toEqual([1, 3]);
	});
});
