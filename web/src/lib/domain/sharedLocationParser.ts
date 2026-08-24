export interface ParsedLocation {
	latitude: number;
	longitude: number;
	name?: string;
}

export function parseLocationText(text: string | null | undefined): ParsedLocation | null {
	let result: ParsedLocation | null = null;
	if (text && text.trim().length > 0) {
		const input = text.trim();
		const geoMatch = parseGeoUri(input);
		const dmsMatch = parseDmsCoordinates(input);
		const urlCoords = parseUrlCoordinates(input);
		const rawCoords = parseRawCoordinates(input);

		const coords = geoMatch || dmsMatch || urlCoords || rawCoords;
		if (coords) {
			const label = extractLabel(input);
			result = { latitude: coords.latitude, longitude: coords.longitude, name: label };
		}
	}
	return result;
}

function parseGeoUri(input: string): { latitude: number; longitude: number } | null {
	let result: { latitude: number; longitude: number } | null = null;
	const regex = /geo:(?:0,0\?q=)?(-?\d{1,2}\.\d+)\s*,\s*(-?\d{1,3}\.\d+)/i;
	const match = input.match(regex);
	if (match) {
		const lat = parseFloat(match[1]);
		const lon = parseFloat(match[2]);
		if (!isNaN(lat) && !isNaN(lon) && isValidCoordinate(lat, lon)) {
			result = { latitude: lat, longitude: lon };
		}
	}
	return result;
}

function parseUrlCoordinates(input: string): { latitude: number; longitude: number } | null {
	let result: { latitude: number; longitude: number } | null = null;
	const atMatch = input.match(/@(-?\d{1,2}\.\d+),(-?\d{1,3}\.\d+)/);
	const qMatch = input.match(/(?:query|q|place)[=/](-?\d{1,2}\.\d+)\s*,\s*(-?\d{1,3}\.\d+)/i);
	const osmMatch = input.match(/#map=\d+\/(-?\d{1,2}\.\d+)\/(-?\d{1,3}\.\d+)/);
	const paramMatch = input.match(/(?:mlat|ll)=(-?\d{1,2}\.\d+)[&,]?(?:mlon=)?(-?\d{1,3}\.\d+)/i);

	const match = atMatch || qMatch || osmMatch || paramMatch;
	if (match) {
		const lat = parseFloat(match[1]);
		const lon = parseFloat(match[2]);
		if (!isNaN(lat) && !isNaN(lon) && isValidCoordinate(lat, lon)) {
			result = { latitude: lat, longitude: lon };
		}
	}
	return result;
}

function parseRawCoordinates(input: string): { latitude: number; longitude: number } | null {
	let result: { latitude: number; longitude: number } | null = null;
	const regex = /(-?\d{1,2}\.\d+)\s*,\s*(-?\d{1,3}\.\d+)/;
	const match = input.match(regex);
	if (match) {
		const lat = parseFloat(match[1]);
		const lon = parseFloat(match[2]);
		if (!isNaN(lat) && !isNaN(lon) && isValidCoordinate(lat, lon)) {
			result = { latitude: lat, longitude: lon };
		}
	}
	return result;
}

function parseDmsCoordinates(input: string): { latitude: number; longitude: number } | null {
	let result: { latitude: number; longitude: number } | null = null;
	const regex = /(\d+)[°\s]+(\d+)['\s]+(\d+(?:\.\d+)?)["\s]*([NSns])\s*[,]?\s*(\d+)[°\s]+(\d+)['\s]+(\d+(?:\.\d+)?)["\s]*([EWew])/;
	const match = input.match(regex);
	if (match) {
		const latDeg = parseFloat(match[1]);
		const latMin = parseFloat(match[2]);
		const latSec = parseFloat(match[3]);
		const latDir = match[4].toUpperCase();

		const lonDeg = parseFloat(match[5]);
		const lonMin = parseFloat(match[6]);
		const lonSec = parseFloat(match[7]);
		const lonDir = match[8].toUpperCase();

		let lat = latDeg + latMin / 60 + latSec / 3600;
		if (latDir === 'S') lat = -lat;

		let lon = lonDeg + lonMin / 60 + lonSec / 3600;
		if (lonDir === 'W') lon = -lon;

		if (isValidCoordinate(lat, lon)) {
			result = { latitude: lat, longitude: lon };
		}
	}
	return result;
}

function extractLabel(input: string): string | undefined {
	let result: string | undefined = undefined;
	const lines = input.split('\n');
	const nonUrlLines = lines.filter(
		(line) => !line.includes('http://') && !line.includes('https://') && !line.includes('geo:')
	);
	if (nonUrlLines.length > 0) {
		const candidate = nonUrlLines[0].trim();
		if (candidate.length > 0 && candidate !== 'Dropped pin') {
			result = candidate;
		}
	}
	return result;
}

function isValidCoordinate(lat: number, lon: number): boolean {
	let valid = false;
	if (lat >= -90 && lat <= 90 && lon >= -180 && lon <= 180) {
		valid = true;
	}
	return valid;
}
