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
	return Math.abs(lat1 - lat2) > MOVE_THRESHOLD || Math.abs(lon1 - lon2) > MOVE_THRESHOLD;
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

export async function reverseGeocode(lat: number, lon: number, localeTag?: string): Promise<GeocodedLocation> {
	try {
		const headers: Record<string, string> = { 'User-Agent': 'maripanaTokana-PWA/1.0' };
		if (localeTag) headers['Accept-Language'] = `${localeTag},en;q=0.5`;
		const res = await fetch(
			`https://nominatim.openstreetmap.org/reverse?lat=${lat}&lon=${lon}&format=json`,
			{ headers }
		);
		if (!res.ok) throw new Error('Geocoding failed');
		const data = await res.json();
		const addr = data.address;
		
		const rawName = addr?.city || addr?.town || addr?.village || addr?.county || addr?.state || `${lat.toFixed(2)}, ${lon.toFixed(2)}`;
		const name = rawName.split(/[,;\-]/)[0].trim();
		
		const subParts = [];
		if (addr?.state && !name.includes(addr.state) && !addr.state.includes(name)) subParts.push(addr.state);
		if (addr?.country) subParts.push(addr.country);
		
		return {
			name,
			subtext: subParts.length > 0 ? subParts.join(', ') : undefined
		};
	} catch {
		return { name: `${lat.toFixed(2)}, ${lon.toFixed(2)}` };
	}
}
