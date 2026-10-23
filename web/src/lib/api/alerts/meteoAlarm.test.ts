import { describe, it, expect } from 'vitest';
import { areaMatches } from './meteoAlarm';

describe('areaMatches', () => {
	it('matches the same area with other case, accents or hyphens', () => {
		expect(areaMatches('Bouches-du-Rhône', 'bouches du rhone')).toBe(true);
	});

	it('matches an area name that is part of the other as whole words', () => {
		expect(areaMatches('Grad Zagreb', 'Zagreb')).toBe(true);
		expect(areaMatches('Attica', 'Αττική Attica')).toBe(true);
	});

	it('does not match a short name inside a longer word', () => {
		expect(areaMatches('Maine-et-Loire', 'Ain')).toBe(false);
		expect(areaMatches('Var', 'Vaucluse')).toBe(false);
	});

	it('matches on a shared word of 4 or more letters', () => {
		expect(areaMatches('Zagreb region', 'Grad Zagreb')).toBe(true);
	});

	it('matches names in non-Latin scripts as whole words', () => {
		expect(areaMatches('Περιφέρεια Αττικής', 'Αττικής')).toBe(true);
	});
});
