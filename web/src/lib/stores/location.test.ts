import { describe, expect, it } from 'vitest';
import { movedSignificantly, shortPlaceName } from './location';

describe('movedSignificantly', () => {
	it('returns false for the same point', () => {
		expect(movedSignificantly(48.85, 2.35, 48.85, 2.35)).toBe(false);
	});

	it('returns false for ~1 km apart', () => {
		expect(movedSignificantly(48.85, 2.35, 48.859, 2.35)).toBe(false);
	});

	it('returns true for ~10 km apart', () => {
		expect(movedSignificantly(48.85, 2.35, 48.94, 2.35)).toBe(true);
	});
});

describe('shortPlaceName', () => {
	it('keeps hyphenated names intact', () => {
		expect(shortPlaceName('Saint-Denis')).toBe('Saint-Denis');
		expect(shortPlaceName('Aix-en-Provence')).toBe('Aix-en-Provence');
	});

	it('splits on comma, semicolon and spaced hyphen', () => {
		expect(shortPlaceName('Paris, France')).toBe('Paris');
		expect(shortPlaceName('Foo - Bar')).toBe('Foo');
		expect(shortPlaceName('A;B')).toBe('A');
	});
});
