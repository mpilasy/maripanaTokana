import type { GdacsAlertResponse } from '../externalAlertsTypes';
import type { WeatherAlert, AlertLevel } from '$lib/domain/weatherData';
import { calculateDistance } from './shared';

const SEARCH_RADIUS_KM = 500;
const SEARCH_DAYS = 7;

export async function fetchGdacsAlerts(lat: number, lon: number): Promise<WeatherAlert[]> {
	try {
		const toDate = new Date().toISOString().split('T')[0];
		const fromDate = new Date(Date.now() - SEARCH_DAYS * 24 * 60 * 60 * 1000).toISOString().split('T')[0];

		const res = await fetch(
			`https://www.gdacs.org/gdacsapi/api/events/geteventlist/SEARCH?fromdate=${fromDate}&todate=${toDate}&alertlevel=green;orange;red`
		);
		if (!res.ok) return [];
		const data: GdacsAlertResponse = await res.json();

		const latDelta = SEARCH_RADIUS_KM / 111.0;
		const lonDelta = Math.abs(lat) < 89.0
			? SEARCH_RADIUS_KM / (111.0 * Math.cos(lat * Math.PI / 180))
			: 360.0;

		return data.features
			.filter(f => {
				const fLat = f.geometry.coordinates[1];
				const fLon = f.geometry.coordinates[0];
				if (Math.abs(fLat - lat) > latDelta) return false;
				const dLon = Math.abs(fLon - lon);
				if (Math.min(dLon, 360 - dLon) > lonDelta) return false;
				return calculateDistance(lat, lon, fLat, fLon) < SEARCH_RADIUS_KM;
			})
			.map(f => {
				const p = f.properties;
				const level: AlertLevel = p.alertlevel === 'red' ? 'emergency'
					: p.alertlevel === 'orange' ? 'warning' : 'watch';
				return {
					level,
					title: `GDACS: ${p.eventtype} - ${p.name}`,
					description: p.description,
					source: 'gdacs' as const,
					time: p.fromdate ? new Date(p.fromdate).getTime() : undefined,
					link: p.url?.report
				};
			});
	} catch (e) {
		console.error('GDACS fetch error:', e);
		return [];
	}
}
