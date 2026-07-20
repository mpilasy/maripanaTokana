import type { NwsAlertResponse } from '../externalAlertsTypes';
import type { WeatherAlert, AlertLevel } from '$lib/domain/weatherData';
import { USER_AGENT } from './shared';

// Coarse fallback for when reverse geocoding fails (e.g. Nominatim rate-limiting) so NWS
// alerts don't silently disappear — mirrors isInAustralia/isInJapan. Three sub-regions (rather
// than one wide box) to avoid a false positive over Mexico/Cuba/the Caribbean.
export function isInUS(lat: number, lon: number): boolean {
	const conus = lat >= 24.0 && lat <= 49.5 && lon >= -125.0 && lon <= -66.0;
	const alaska = lat >= 51.0 && lat <= 72.0 && lon >= -180.0 && lon <= -129.0;
	const hawaii = lat >= 18.5 && lat <= 22.5 && lon >= -160.5 && lon <= -154.5;
	return conus || alaska || hawaii;
}

export async function fetchNwsAlerts(lat: number, lon: number): Promise<WeatherAlert[]> {
	try {
		const res = await fetch(`https://api.weather.gov/alerts/active?point=${lat},${lon}`, {
			headers: { 'Accept': 'application/geo+json', 'User-Agent': USER_AGENT }
		});
		if (!res.ok) return [];
		const data: NwsAlertResponse = await res.json();
		return data.features.map(f => {
			const p = f.properties;
			const level: AlertLevel = (p.severity === 'Extreme' || p.severity === 'Severe') ? 'warning' : 'watch';
			return {
				level,
				title: p.event,
				description: p.description + (p.instruction ? '\n\n' + p.instruction : ''),
				source: 'nws' as const,
				time: p.sent ? new Date(p.sent).getTime() : undefined,
				headline: p.headline,
				link: f.id
			};
		});
	} catch (e) {
		console.error('NWS fetch error:', e);
		return [];
	}
}
