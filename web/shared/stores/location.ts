const LOCATION_KEY = 'cached_location';
const MOVE_THRESHOLD = 0.045; // ~5 km in degrees

interface CachedLocation {
	lat: number;
	lon: number;
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

export function cacheLocation(lat: number, lon: number) {
	if (typeof localStorage !== 'undefined') {
		localStorage.setItem(LOCATION_KEY, JSON.stringify({ lat, lon }));
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

export async function reverseGeocode(lat: number, lon: number): Promise<string> {
	try {
		const res = await fetch(
			`https://nominatim.openstreetmap.org/reverse?lat=${lat}&lon=${lon}&format=json`,
			{ headers: { 'User-Agent': 'maripanaTokana-PWA/1.0' } }
		);
		if (!res.ok) throw new Error('Geocoding failed');
		const data = await res.json();
		const addr = data.address;
		return addr?.city || addr?.town || addr?.village || addr?.county || addr?.state || `${lat.toFixed(2)}, ${lon.toFixed(2)}`;
	} catch {
		return `${lat.toFixed(2)}, ${lon.toFixed(2)}`;
	}
}
