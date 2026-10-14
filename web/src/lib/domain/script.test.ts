import { describe, expect, it } from 'vitest';
import { isLatinScript } from './script';

describe('isLatinScript', () => {
	it('accepts Latin names, with accents and punctuation', () => {
		expect(isLatinScript('Saint-Étienne, Île-de-France')).toBe(true);
		expect(isLatinScript("Antananarivo 101")).toBe(true);
	});

	it('rejects other scripts', () => {
		expect(isLatinScript('القاهرة')).toBe(false);
		expect(isLatinScript('東京')).toBe(false);
		expect(isLatinScript('काठमाडौं')).toBe(false);
	});
});
