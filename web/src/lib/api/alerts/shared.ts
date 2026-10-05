import { lookupLocation } from '$lib/stores/location';

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

export async function getLocationInfo(lat: number, lon: number, localeTag?: string): Promise<LocationInfo> {
	const r = await lookupLocation(lat, lon, localeTag);
	return r
		? { countryCode: r.countryCode, stateCode: r.stateCode, subdivisionName: r.subdivisionName }
		: { countryCode: null, stateCode: null, subdivisionName: null };
}
