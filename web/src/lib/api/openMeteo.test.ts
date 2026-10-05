import { describe, expect, it } from 'vitest';
import { parseCachedAt } from './openMeteo';

describe('parseCachedAt', () => {
	it('reads the service-worker cache time header', () => {
		expect(parseCachedAt('1700000000000')).toBe(1700000000000);
	});
	it('ignores missing or invalid values', () => {
		expect(parseCachedAt(null)).toBeNull();
		expect(parseCachedAt('nope')).toBeNull();
		expect(parseCachedAt('0')).toBeNull();
	});
});
