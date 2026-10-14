import { get } from 'svelte/store';
import { parseLocationText } from '$lib/domain/sharedLocationParser';
import { decodePlusCode, findPlusCode, recoverNearest, type PlusCodeMatch } from '$lib/domain/plusCode';
import { weatherState } from '$lib/stores/weather';

export interface SearchResult {
	id: number;
	name: string;
	latitude: number;
	longitude: number;
	admin1?: string;
	admin2?: string;
	country?: string;
	displayName: string;
	/** Region or country rather than a town or city (GeoNames ADM or PCL feature code) */
	isArea?: boolean;
}

/** Drop results with the same name at (nearly) the same place; the API often lists a city twice. */
export function dedupeResults(items: SearchResult[]): SearchResult[] {
	const kept: SearchResult[] = [];
	for (const r of items) {
		const dup = kept.some((k) =>
			k.displayName.toLowerCase() === r.displayName.toLowerCase()
			&& Math.abs(k.latitude - r.latitude) < 0.1 && Math.abs(k.longitude - r.longitude) < 0.1);
		if (!dup) kept.push(r);
	}
	return kept;
}

/**
 * Forward-geocoding search used by SavedLocationsDialog (including its Advanced Mode "Use once"
 * temporary-override action). Supports direct "lat,lon" input, URLs, DMS, or queries Open-Meteo's geocoding
 * API — mirrors Android's SystemGeocoderSource.searchLocations.
 */
export async function searchLocations(query: string): Promise<SearchResult[]> {
	let results: SearchResult[] = [];
	const trimmed = query.trim();
	const plusCode = findPlusCode(trimmed);
	if (plusCode) return searchPlusCode(plusCode);
	const parsed = parseLocationText(trimmed);

	if (parsed) {
		const nameStr = parsed.name || `${parsed.latitude.toFixed(4)}, ${parsed.longitude.toFixed(4)}`;
		results = [{
			id: 0,
			name: nameStr,
			latitude: parsed.latitude,
			longitude: parsed.longitude,
			displayName: parsed.name ? `${parsed.name} (${parsed.latitude.toFixed(2)}, ${parsed.longitude.toFixed(2)})` : 'Coordinates'
		}];
	} else {
		const commaIndex = trimmed.indexOf(',');
		const namePart = commaIndex >= 0 ? trimmed.slice(0, commaIndex).trim() : trimmed;
		const qualifier = commaIndex >= 0 ? trimmed.slice(commaIndex + 1).trim().toLowerCase() : '';

		try {
			const res = await fetch(`https://geocoding-api.open-meteo.com/v1/search?name=${encodeURIComponent(namePart)}&count=20&language=en&format=json`, { signal: AbortSignal.timeout(10_000) });
			const data = await res.json();
			if (data.results) {
				// eslint-disable-next-line @typescript-eslint/no-explicit-any
				let mapped: SearchResult[] = data.results.map((r: any) => ({
					...r,
					displayName: [r.name, r.admin1, r.country].filter(Boolean).join(', '),
					isArea: /^(ADM|PCL)/.test(r.feature_code ?? ''),
				}));
				if (qualifier) {
					const filtered = mapped.filter((r) =>
						r.admin1?.toLowerCase().includes(qualifier) ||
						r.admin2?.toLowerCase().includes(qualifier) ||
						r.country?.toLowerCase().includes(qualifier)
					);
					if (filtered.length > 0) mapped = filtered;
				}
				results = dedupeResults(mapped);
			}
		} catch {
			results = [];
		}
	}
	return results;
}

/**
 * A full code is its own location. A short one is resolved near the place named with it, or,
 * with no place given, near the location on screen (as Google Maps does with its map view).
 */
async function searchPlusCode(match: PlusCodeMatch): Promise<SearchResult[]> {
	if (match.isFull) {
		const { latitude, longitude } = decodePlusCode(match.code);
		return [{ id: 0, name: match.code, latitude, longitude, displayName: match.code }];
	}
	if (match.locality) {
		const place = (await searchLocations(match.locality))[0];
		if (!place) return [];
		return [{ ...place, id: 0, isArea: false, ...recoverNearest(match.code, place.latitude, place.longitude) }];
	}
	const shown = get(weatherState);
	if (shown.kind !== 'success') return [];
	const { latitude, longitude } = recoverNearest(match.code, shown.data.latitude, shown.data.longitude);
	return [{ id: 0, name: match.code, latitude, longitude, admin1: shown.data.locationName, displayName: `${match.code}, ${shown.data.locationName}` }];
}
