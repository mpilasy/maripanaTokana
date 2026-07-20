export const USER_AGENT = 'maripanaTokana (mpilasy@duck.com)';

const EARTH_RADIUS_KM = 6371;

export function calculateDistance(lat1: number, lon1: number, lat2: number, lon2: number): number {
	const dLat = (lat2 - lat1) * Math.PI / 180;
	const dLon = (lon2 - lon1) * Math.PI / 180;
	const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
		Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
		Math.sin(dLon / 2) * Math.sin(dLon / 2);
	return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

export interface LocationInfo {
	countryCode: string | null;
	stateCode: string | null;
	subdivisionName: string | null;
}

export async function getLocationInfo(lat: number, lon: number): Promise<LocationInfo> {
	try {
		const res = await fetch(
			`https://nominatim.openstreetmap.org/reverse?lat=${lat}&lon=${lon}&format=json&zoom=8&addressdetails=1`,
			{ headers: { 'User-Agent': USER_AGENT } }
		);
		if (!res.ok) return { countryCode: null, stateCode: null, subdivisionName: null };
		// eslint-disable-next-line @typescript-eslint/no-explicit-any
		const data: any = await res.json();
		const countryCode = data.address?.country_code?.toLowerCase() ?? null;
		const iso: string | undefined = data.address?.['ISO3166-2-lvl4'];
		const stateCode = iso?.split('-')[1] ?? null;
		// county matches département-level (NUTS3) in most MeteoAlarm countries
		const subdivisionName: string | null =
			data.address?.county ?? data.address?.city ?? data.address?.state ?? null;
		return { countryCode, stateCode, subdivisionName };
	} catch { return { countryCode: null, stateCode: null, subdivisionName: null }; }
}
