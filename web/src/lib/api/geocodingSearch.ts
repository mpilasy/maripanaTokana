export interface SearchResult {
	id: number;
	name: string;
	latitude: number;
	longitude: number;
	admin1?: string;
	admin2?: string;
	country?: string;
	displayName: string;
}

/**
 * Forward-geocoding search used by SavedLocationsDialog (including its Advanced Mode "Use once"
 * temporary-override action). Supports direct "lat,lon" input, else queries Open-Meteo's geocoding
 * API — mirrors Android's SystemGeocoderSource.searchLocations.
 */
export async function searchLocations(query: string): Promise<SearchResult[]> {
	const trimmed = query.trim();
	const match = trimmed.match(/^(-?\d+\.\d+)\s*,\s*(-?\d+\.\d+)$/);
	if (match) {
		const lat = parseFloat(match[1]);
		const lon = parseFloat(match[2]);
		if (!isNaN(lat) && !isNaN(lon)) {
			return [{ id: 0, name: `${lat}, ${lon}`, latitude: lat, longitude: lon, displayName: 'Coordinates' }];
		}
	}

	// Open-Meteo's `name` param only matches the place name itself — a combined
	// "City, State" query returns zero results. Split off the qualifier (state/country)
	// and use it to filter the results client-side instead.
	const commaIndex = trimmed.indexOf(',');
	const namePart = commaIndex >= 0 ? trimmed.slice(0, commaIndex).trim() : trimmed;
	const qualifier = commaIndex >= 0 ? trimmed.slice(commaIndex + 1).trim().toLowerCase() : '';

	try {
		const res = await fetch(`https://geocoding-api.open-meteo.com/v1/search?name=${encodeURIComponent(namePart)}&count=20&language=en&format=json`);
		const data = await res.json();
		if (!data.results) return [];
		// eslint-disable-next-line @typescript-eslint/no-explicit-any
		let mapped: SearchResult[] = data.results.map((r: any) => ({
			...r,
			displayName: [r.name, r.admin1, r.country].filter(Boolean).join(', '),
		}));
		if (qualifier) {
			const filtered = mapped.filter((r) =>
				r.admin1?.toLowerCase().includes(qualifier) ||
				r.admin2?.toLowerCase().includes(qualifier) ||
				r.country?.toLowerCase().includes(qualifier)
			);
			if (filtered.length > 0) mapped = filtered;
		}
		return mapped;
	} catch {
		return [];
	}
}
