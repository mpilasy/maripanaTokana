import type { WeatherAlert, AlertLevel } from '$lib/domain/weatherData';
import { calculateDistance } from './shared';

const SEARCH_RADIUS_KM = 1500;
const KNOTS_TO_MPH = 1.15078;
const KM_TO_MI = 0.621371;

// NHC's CurrentStorms.json has no "headline" field despite what older code assumed — it's
// always empty, so alerts must be built from the raw classification/intensity/movement fields.
const CLASSIFICATION_NAMES: Record<string, string> = {
	TD: 'Tropical Depression',
	SD: 'Subtropical Depression',
	TS: 'Tropical Storm',
	SS: 'Subtropical Storm',
	HU: 'Hurricane',
	EX: 'Post-Tropical Cyclone',
	PTC: 'Potential Tropical Cyclone',
	LO: 'Remnant Low',
	DB: 'Disturbance',
	WV: 'Tropical Wave',
};

const COMPASS_DIRECTIONS = ['N', 'NNE', 'NE', 'ENE', 'E', 'ESE', 'SE', 'SSE', 'S', 'SSW', 'SW', 'WSW', 'W', 'WNW', 'NW', 'NNW'];

function compassFromDegrees(deg: number): string {
	const idx = Math.round((((deg % 360) + 360) % 360) / 22.5) % 16;
	return COMPASS_DIRECTIONS[idx];
}

export async function fetchNhcAlerts(lat: number, lon: number): Promise<WeatherAlert[]> {
	try {
		const res = await fetch('/api/alerts/nhc');
		if (!res.ok) return [];
		// eslint-disable-next-line @typescript-eslint/no-explicit-any
		const data: any = await res.json();
		// eslint-disable-next-line @typescript-eslint/no-explicit-any
		return (data.activeStorms ?? [])
			.filter((s: any) => {
				const sLat = s.latitudeNumeric;
				const sLon = s.longitudeNumeric;
				if (sLat == null || sLon == null) return false;
				return calculateDistance(lat, lon, sLat, sLon) < SEARCH_RADIUS_KM;
			})
			// eslint-disable-next-line @typescript-eslint/no-explicit-any
			.map((s: any) => {
				const knots = parseInt(s.intensity ?? '', 10) || 0;
				const level: AlertLevel = s.classification === 'HU' && knots >= 96 ? 'emergency'
					: (s.classification === 'HU' || s.classification === 'TS') ? 'warning' : 'watch';

				const classificationName = CLASSIFICATION_NAMES[s.classification] ?? s.classification;
				const title = classificationName ? `${classificationName} ${s.name}` : `NHC: ${s.name}`;

				const statsParts: string[] = [];
				if (knots > 0) statsParts.push(`winds ~${Math.round(knots * KNOTS_TO_MPH)} mph`);
				const pressure = parseInt(s.pressure ?? '', 10);
				if (!isNaN(pressure)) statsParts.push(`central pressure ${pressure} mb`);
				if (typeof s.movementSpeed === 'number') {
					statsParts.push(s.movementSpeed > 0
						? `moving ${compassFromDegrees(s.movementDir ?? 0)} at ${s.movementSpeed} mph`
						: 'stationary');
				}
				const joined = statsParts.join(', ');
				const statsSentence = joined ? joined.charAt(0).toUpperCase() + joined.slice(1) + '.' : '';

				const distanceKm = Math.round(calculateDistance(lat, lon, s.latitudeNumeric, s.longitudeNumeric));
				const distanceMi = Math.round(distanceKm * KM_TO_MI);
				const distanceSentence = `~${distanceKm} km (${distanceMi} mi) from your location.`;

				const description = [statsSentence, distanceSentence].filter(Boolean).join(' ') || s.name || '';

				return {
					level,
					title,
					description,
					source: 'nhc' as const,
					time: s.advisory?.issuance ? new Date(s.advisory.issuance).getTime() : undefined,
					link: s.advisory?.url,
				};
			});
	} catch { return []; }
}
