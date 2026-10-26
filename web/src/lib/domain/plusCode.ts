// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

/**
 * Plus Codes (Open Location Code, an open standard: https://github.com/google/open-location-code).
 * Full codes ("6GCRPR6C+24") decode offline; short codes ("PR6C+24 Nairobi") need a nearby
 * reference point, usually the place named next to them. Mirrors Android's PlusCode.kt.
 */
const ALPHABET = '23456789CFGHJMPQRVWX';

export interface PlusCodeMatch {
	code: string;
	isFull: boolean;
	/** The rest of the text: the place a short code is relative to. */
	locality: string;
}

const FULL = new RegExp(`(?<![A-Z0-9])[${ALPHABET}]{8}\\+[${ALPHABET}]{2,}(?![A-Z0-9])`, 'i');
const SHORT = new RegExp(`(?<![A-Z0-9])(?:[${ALPHABET}]{4}|[${ALPHABET}]{6})\\+[${ALPHABET}]{2,}(?![A-Z0-9])`, 'i');

// Typed without the "+": a whole word of 6-7 (short) or 10-11 (full) code characters that
// mixes letters and digits, so all-digit postcodes and plain words don't match
const NO_PLUS = new RegExp(`(?<![A-Z0-9+])(?=[${ALPHABET}]*[0-9])(?=[${ALPHABET}]*[A-Z])(?:[${ALPHABET}]{10,11}|[${ALPHABET}]{6,7})(?![A-Z0-9+])`, 'i');

export function findPlusCode(text: string): PlusCodeMatch | null {
	const m = text.match(FULL) ?? text.match(SHORT) ?? text.match(NO_PLUS);
	if (!m || m.index === undefined) return null;
	const raw = m[0].toUpperCase();
	const code = raw.includes('+') ? raw : raw.length >= 10 ? `${raw.slice(0, 8)}+${raw.slice(8)}` : `${raw.slice(0, 4)}+${raw.slice(4)}`;
	const isFull = code.indexOf('+') === 8;
	// First digit encodes latitude in 20° bands (max 9), second longitude (max 18)
	if (isFull && (ALPHABET.indexOf(code[0]) >= 9 || ALPHABET.indexOf(code[1]) >= 18)) return null;
	const locality = (text.slice(0, m.index) + text.slice(m.index + m[0].length)).replace(/^[\s,]+|[\s,]+$/g, '');
	return { code, isFull, locality };
}

/** Centre of a full code's area. */
export function decodePlusCode(fullCode: string): { latitude: number; longitude: number } {
	const digits = fullCode.toUpperCase().replace('+', '');
	let lat = -90;
	let lon = -180;
	let pairValue = 20;
	let latSize = 0;
	let lonSize = 0;
	let i = 0;
	for (; i < Math.min(digits.length, 10); i += 2) {
		lat += ALPHABET.indexOf(digits[i]) * pairValue;
		lon += ALPHABET.indexOf(digits[i + 1]) * pairValue;
		latSize = lonSize = pairValue;
		pairValue /= 20;
	}
	// Beyond 10 digits each one picks a cell in a 4-column, 5-row grid
	for (; i < digits.length; i++) {
		latSize /= 5;
		lonSize /= 4;
		const d = ALPHABET.indexOf(digits[i]);
		lat += Math.floor(d / 4) * latSize;
		lon += (d % 4) * lonSize;
	}
	return { latitude: Math.min(lat + latSize / 2, 90), longitude: lon + lonSize / 2 };
}

/** Centre of a short code's area, choosing the cell nearest to the reference point. */
export function recoverNearest(shortCode: string, refLat: number, refLon: number): { latitude: number; longitude: number } {
	const code = shortCode.toUpperCase();
	const prefixLength = 8 - code.indexOf('+');
	const resolution = Math.pow(20, 2 - prefixLength / 2);
	const { latitude: lat, longitude: lon } = decodePlusCode(prefix(refLat, refLon, prefixLength) + code);
	const half = resolution / 2;
	let nearLat = lat;
	if (refLat + half < lat && lat - resolution >= -90) nearLat = lat - resolution;
	else if (refLat - half > lat && lat + resolution <= 90) nearLat = lat + resolution;
	let nearLon = lon;
	if (refLon + half < lon) nearLon = lon - resolution;
	else if (refLon - half > lon) nearLon = lon + resolution;
	return { latitude: nearLat, longitude: ((nearLon + 540) % 360) - 180 };
}

/** Standard 10-digit code (about 14 m x 14 m) for a point, e.g. "6GCRPR6C+24". */
export function encodePlusCode(lat: number, lon: number): string {
	const p = prefix(lat, lon, 10);
	return `${p.slice(0, 8)}+${p.slice(8)}`;
}

function prefix(lat: number, lon: number, length: number): string {
	let latV = Math.min(Math.min(Math.max(lat, -90), 90) + 90, 179.999999);
	let lonV = (((lon + 180) % 360) + 360) % 360;
	let pairValue = 20;
	let out = '';
	for (let i = 0; i < length / 2; i++) {
		const a = Math.floor(latV / pairValue);
		const b = Math.floor(lonV / pairValue);
		out += ALPHABET[a] + ALPHABET[b];
		latV -= a * pairValue;
		lonV -= b * pairValue;
		pairValue /= 20;
	}
	return out;
}
