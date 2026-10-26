import { describe, expect, it } from 'vitest';
import { parseCoordinateInput, parseLocationText } from './sharedLocationParser';

describe('parseCoordinateInput', () => {
	it('accepts integers and decimals', () => {
		expect(parseCoordinateInput('-18, 47')).toEqual({ latitude: -18, longitude: 47 });
		expect(parseCoordinateInput('-18.9,47.5')).toEqual({ latitude: -18.9, longitude: 47.5 });
	});

	it('rejects out-of-range values', () => {
		expect(parseCoordinateInput('95.0, 10.0')).toBeNull();
		expect(parseCoordinateInput('10, 190')).toBeNull();
	});

	it('rejects non-coordinate text', () => {
		expect(parseCoordinateInput('Paris, 12')).toBeNull();
		expect(parseCoordinateInput('Paris')).toBeNull();
	});

	it('is used by parseLocationText for integer input', () => {
		expect(parseLocationText('-18, 47')).toMatchObject({ latitude: -18, longitude: 47 });
	});
});

describe('parseLocationText name', () => {
	it('gives coordinates alone no name', () => {
		expect(parseLocationText('-18.91, 47.52')?.name).toBeUndefined();
		expect(parseLocationText('-18, 47')?.name).toBeUndefined();
	});

	it('keeps a place name given with the coordinates', () => {
		expect(parseLocationText('Antananarivo\ngeo:-18.8792,47.5079')?.name).toBe('Antananarivo');
	});
});
