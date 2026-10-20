import { describe, expect, it } from 'vitest';
import { matchesRegionQualifier } from './regionQualifier';

describe('matchesRegionQualifier', () => {
	it('postal abbreviation names its province or state', () => {
		expect(matchesRegionQualifier('ON', 'Ontario', undefined, 'Canada')).toBe(true);
		expect(matchesRegionQualifier('ma', 'Massachusetts', undefined, 'United States')).toBe(true);
	});
	it('text inside a word does not match', () => {
		expect(matchesRegionQualifier('on', 'Washington', undefined, 'United States')).toBe(false);
		expect(matchesRegionQualifier('land', 'England', undefined, 'United Kingdom')).toBe(false);
	});
	it('start of any word matches', () => {
		expect(matchesRegionQualifier('kingdom', 'England', undefined, 'United Kingdom')).toBe(true);
		expect(matchesRegionQualifier('fra', 'Île-de-France', undefined, 'France')).toBe(true);
	});
	it('blank qualifier matches everything', () => {
		expect(matchesRegionQualifier('', undefined)).toBe(true);
	});
});
