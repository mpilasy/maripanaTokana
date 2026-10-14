import { describe, expect, it } from 'vitest';
import { decodePlusCode, encodePlusCode, findPlusCode, recoverNearest } from './plusCode';

function expectNear(actual: { latitude: number; longitude: number }, lat: number, lon: number) {
	expect(actual.latitude).toBeCloseTo(lat, 5);
	expect(actual.longitude).toBeCloseTo(lon, 5);
}

describe('plusCode', () => {
	it('decodes full codes to the centre of their area', () => {
		expectNear(decodePlusCode('8FVC9G8F+6X'), 47.3655625, 8.5249375);
		expectNear(decodePlusCode('849VCWC8+R9'), 37.4220625, -122.0840625);
	});

	it('decodes grid digits past the tenth', () => {
		// Q = row 3, column 3 of the 4x5 grid inside 8FVC9G8F+6X
		expectNear(decodePlusCode('8FVC9G8F+6XQ'), 47.3655875, 8.524984375);
	});

	it('encodes to a 10-digit code that decodes back to the same cell', () => {
		expect(encodePlusCode(47.3655625, 8.5249375)).toBe('8FVC9G8F+6X');
		expect(encodePlusCode(37.42207, -122.08405)).toBe('849VCWC8+R9');
		const { latitude, longitude } = decodePlusCode('6GCRPR6C+24');
		expect(encodePlusCode(latitude, longitude)).toBe('6GCRPR6C+24');
	});

	it('recovers short codes nearest the reference, across a cell edge', () => {
		const zurich = decodePlusCode('8FVC9G8F+6X');
		expectNear(recoverNearest('9G8F+6X', 47.4, 8.6), zurich.latitude, zurich.longitude);
		expectNear(recoverNearest('9G8F+6X', 46.99, 8.6), zurich.latitude, zurich.longitude);
		const google = decodePlusCode('849VCWC8+R9');
		expectNear(recoverNearest('CWC8+R9', 37.4, -122.1), google.latitude, google.longitude);
	});

	it('finds codes in text with their locality', () => {
		expect(findPlusCode('PR6C+24 Nairobi, Kenya')).toEqual({ code: 'PR6C+24', isFull: false, locality: 'Nairobi, Kenya' });
		expect(findPlusCode('6gcrpr6c+24')).toEqual({ code: '6GCRPR6C+24', isFull: true, locality: '' });
	});

	it('ignores text without a code', () => {
		expect(findPlusCode('Nairobi')).toBeNull();
		expect(findPlusCode('1+1')).toBeNull();
		expect(findPlusCode('C++ meetup')).toBeNull();
	});
});
