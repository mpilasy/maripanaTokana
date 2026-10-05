import { describe, expect, it } from 'vitest';
import { movedSignificantly } from './location';

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
