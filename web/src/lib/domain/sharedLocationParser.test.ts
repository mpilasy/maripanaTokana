// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

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

describe('parseLocationText with encoded links', () => {
	it('reads coordinates when the comma is encoded as %2C', () => {
		for (const link of [
			'geo:0,0?q=-18.97%2C49.14',
			'https://www.google.com/maps/search/?api=1&query=-18.97%2C49.14',
			'https://maps.google.com/?q=-18.97%2c49.14',
		]) {
			expect(parseLocationText(link), link).toMatchObject({ latitude: -18.97, longitude: 49.14 });
		}
	});

	it('still parses text with a percent sign', () => {
		expect(parseLocationText('100% here\n-18.8792, 47.5079')).toMatchObject({ latitude: -18.8792 });
	});
});

describe('parseLocationText with Google Maps place links', () => {
	it('reads the place pin when the link has no "@"', () => {
		// The link that a maps.app.goo.gl short link from Google Photos resolves to
		expect(parseLocationText('https://www.google.com/maps/place/5553%2BMCG+Gare+Train,+Ambila+Lemaitso,+Madagascar/data=!4m6!3m5!1s0x21f38ffc892ea313:0x7acf3fb7e2ee5ef2!7e2!8m2!3d-18.8407999!4d49.1535083!18m1!1e1?entry=gps')).toMatchObject({ latitude: -18.8407999, longitude: 49.1535083 });
	});

	it('uses the place pin, not the centre of the map view', () => {
		expect(parseLocationText('https://www.google.com/maps/place/Ambila/@-18.80,49.10,12z/data=!3m1!4b1!4m6!3m5!1s0x0:0x0!8m2!3d-18.8407999!4d49.1535083')).toMatchObject({ latitude: -18.8407999, longitude: 49.1535083 });
	});
});
