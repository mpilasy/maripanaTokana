const LOCATION_KEY = 'cached_location';
const MOVE_THRESHOLD = 0.045; // ~5 km in degrees

interface CachedLocation {
	lat: number;
	lon: number;
	name?: string;
	subtext?: string;
}

export function getCachedLocation(): CachedLocation | null {
	if (typeof localStorage === 'undefined') return null;
	const stored = localStorage.getItem(LOCATION_KEY);
	if (!stored) return null;
	try {
		return JSON.parse(stored);
	} catch {
		return null;
	}
}

export function cacheLocation(lat: number, lon: number, name?: string, subtext?: string) {
	if (typeof localStorage !== 'undefined') {
		const data: CachedLocation = { lat, lon };
		if (name) data.name = name;
		if (subtext) data.subtext = subtext;
		localStorage.setItem(LOCATION_KEY, JSON.stringify(data));
	}
}

export function movedSignificantly(
	lat1: number, lon1: number,
	lat2: number, lon2: number
): boolean {
	const dlat = lat1 - lat2;
	const cosLat = Math.cos(((lat1 + lat2) / 2) * (Math.PI / 180));
	const dlon = (lon1 - lon2) * cosLat;
	const result = (dlat * dlat + dlon * dlon) > (MOVE_THRESHOLD * MOVE_THRESHOLD);
	return result;
}

export function getPosition(): Promise<{ lat: number; lon: number }> {
	return new Promise((resolve, reject) => {
		if (!navigator.geolocation) {
			reject(new Error('Geolocation not supported'));
			return;
		}
		navigator.geolocation.getCurrentPosition(
			(pos) => resolve({ lat: pos.coords.latitude, lon: pos.coords.longitude }),
			(err) => reject(err),
			{ enableHighAccuracy: true, timeout: 15000, maximumAge: 0 }
		);
	});
}

export interface GeocodedLocation {
	name: string;
	subtext?: string;
}

/** First segment of a place name, split on ',', ';' or ' - ' (not bare hyphens: "Saint-Denis"). */
export function shortPlaceName(raw: string): string {
	return raw.split(/[,;]|\s-\s/)[0].trim();
}

export interface LocationLookup {
	name: string;
	subtext?: string;
	countryCode: string | null;
	stateCode: string | null;
	subdivisionName: string | null;
}

const GEOCODE_TTL_MS = 24 * 60 * 60 * 1000;
const GEOCODE_STORAGE_PREFIX = 'geocode_';
const geocodeMemory = new Map<string, { at: number; value: LocationLookup }>();
const geocodeInflight = new Map<string, Promise<LocationLookup | null>>();

function geocodeKey(lat: number, lon: number, localeTag?: string): string {
	return `${lat.toFixed(2)},${lon.toFixed(2)},${localeTag ?? ''}`;
}

function readGeocodeCache(key: string): LocationLookup | null {
	const now = Date.now();
	const mem = geocodeMemory.get(key);
	if (mem && now - mem.at < GEOCODE_TTL_MS) return mem.value;
	try {
		const raw = localStorage.getItem(GEOCODE_STORAGE_PREFIX + key);
		if (!raw) return null;
		const entry = JSON.parse(raw) as { at: number; value: LocationLookup };
		if (now - entry.at >= GEOCODE_TTL_MS) return null;
		geocodeMemory.set(key, entry);
		return entry.value;
	} catch {
		return null;
	}
}

function writeGeocodeCache(key: string, value: LocationLookup) {
	const entry = { at: Date.now(), value };
	geocodeMemory.set(key, entry);
	try { localStorage.setItem(GEOCODE_STORAGE_PREFIX + key, JSON.stringify(entry)); } catch { /* ignore */ }
}

/**
 * Single Nominatim reverse call yielding display name/subtext and country/subdivision info.
 * Cached 24h (memory + localStorage) by coords rounded to 2 decimals + locale. Returns null on failure (not cached).
 */
export function lookupLocation(lat: number, lon: number, localeTag?: string): Promise<LocationLookup | null> {
	const key = geocodeKey(lat, lon, localeTag);
	const hit = readGeocodeCache(key);
	if (hit) return Promise.resolve(hit);
	const pending = geocodeInflight.get(key);
	if (pending) return pending;
	const p = (async () => {
		try {
			// Browsers forbid setting User-Agent; Nominatim identifies browser apps by Referer.
			const headers: Record<string, string> = {};
			if (localeTag) headers['Accept-Language'] = `${localeTag},en;q=0.5`;
			const res = await fetch(
				`https://nominatim.openstreetmap.org/reverse?lat=${lat}&lon=${lon}&format=json&zoom=10&addressdetails=1`,
				{ headers, signal: AbortSignal.timeout(10_000) }
			);
			if (!res.ok) return null;
			const data = await res.json();
			const addr = data.address;

			const rawName = addr?.city || addr?.town || addr?.village || addr?.county || addr?.state || `${lat.toFixed(2)}, ${lon.toFixed(2)}`;
			const name = shortPlaceName(rawName);

			const subParts = [];
			if (addr?.state && !name.includes(addr.state) && !addr.state.includes(name)) subParts.push(addr.state);
			if (addr?.country) subParts.push(addr.country);

			const iso: string | undefined = addr?.['ISO3166-2-lvl4'];
			const value: LocationLookup = {
				name,
				subtext: subParts.length > 0 ? subParts.join(', ') : undefined,
				countryCode: addr?.country_code?.toLowerCase() ?? null,
				stateCode: iso?.split('-')[1] ?? null,
				// county matches département-level (NUTS3) in most MeteoAlarm countries
				subdivisionName: addr?.county ?? addr?.city ?? addr?.state ?? null
			};
			writeGeocodeCache(key, value);
			return value;
		} catch {
			return null;
		} finally {
			geocodeInflight.delete(key);
		}
	})();
	geocodeInflight.set(key, p);
	return p;
}

export async function reverseGeocode(lat: number, lon: number, localeTag?: string): Promise<GeocodedLocation> {
	const r = await lookupLocation(lat, lon, localeTag);
	return r ? { name: r.name, subtext: r.subtext } : { name: `${lat.toFixed(2)}, ${lon.toFixed(2)}` };
}

/** Test helper. */
export function _clearGeocodeCache() {
	geocodeMemory.clear();
	geocodeInflight.clear();
}
