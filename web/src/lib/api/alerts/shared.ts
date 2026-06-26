export const USER_AGENT = 'maripanaTokana (contact@orinasa.mg)';

const EARTH_RADIUS_KM = 6371;

export function calculateDistance(lat1: number, lon1: number, lat2: number, lon2: number): number {
	const dLat = (lat2 - lat1) * Math.PI / 180;
	const dLon = (lon2 - lon1) * Math.PI / 180;
	const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
		Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
		Math.sin(dLon / 2) * Math.sin(dLon / 2);
	return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

export async function getCountryCode(lat: number, lon: number): Promise<string | null> {
	try {
		const res = await fetch(
			`https://nominatim.openstreetmap.org/reverse?lat=${lat}&lon=${lon}&format=json&zoom=3`,
			{ headers: { 'User-Agent': USER_AGENT } }
		);
		if (!res.ok) return null;
		// eslint-disable-next-line @typescript-eslint/no-explicit-any
		const data: any = await res.json();
		return data.address?.country_code?.toLowerCase() ?? null;
	} catch { return null; }
}
