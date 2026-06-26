import type { WeatherAlert, AlertLevel } from '$lib/domain/weatherData';
import { calculateDistance } from './shared';

const SEARCH_RADIUS_KM = 1500;

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
				const intensity = parseInt(s.intensity ?? '0');
				const level: AlertLevel = s.classification === 'HU' && intensity >= 96 ? 'emergency'
					: (s.classification === 'HU' || s.classification === 'TS') ? 'warning' : 'watch';
				return {
					level,
					title: `${s.name}: ${s.headline ?? s.classification}`,
					description: '',
					source: 'nhc' as const,
					link: s.advisory?.url
				};
			});
	} catch { return []; }
}
