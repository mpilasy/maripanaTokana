import type { NwsAlertResponse, GdacsAlertResponse } from './externalAlertsTypes';
import type { WeatherAlert, AlertLevel } from '../domain/weatherData';

const GDACS_SEARCH_RADIUS_KM = 500;
const GDACS_SEARCH_DAYS = 7;
const EARTH_RADIUS_KM = 6371;

const USER_AGENT = 'maripanaTokana (contact@orinasa.mg)';

export async function fetchNwsAlerts(lat: number, lon: number): Promise<WeatherAlert[]> {
	try {
		const res = await fetch(`https://api.weather.gov/alerts/active?point=${lat},${lon}`, {
			headers: {
				'Accept': 'application/geo+json',
				'User-Agent': USER_AGENT
			}
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
				source: 'official',
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

export async function fetchGdacsAlerts(lat: number, lon: number): Promise<WeatherAlert[]> {
	try {
		// Fetch alerts from the last 7 days
		const toDate = new Date().toISOString().split('T')[0];
		const fromDate = new Date(Date.now() - GDACS_SEARCH_DAYS * 24 * 60 * 60 * 1000).toISOString().split('T')[0];
		
		const res = await fetch(`https://www.gdacs.org/gdacsapi/api/events/geteventlist/SEARCH?fromdate=${fromDate}&todate=${toDate}&alertlevel=green;orange;red`);
		if (!res.ok) return [];
		const data: GdacsAlertResponse = await res.json();
		
		// Filter by distance
		const latDelta = GDACS_SEARCH_RADIUS_KM / 111.0;
		const lonDelta = Math.abs(lat) < 89.0 ? GDACS_SEARCH_RADIUS_KM / (111.0 * Math.cos(lat * Math.PI / 180)) : 360.0;
		const minLat = lat - latDelta;
		const maxLat = lat + latDelta;

		return data.features
			.filter(f => {
				const fLat = f.geometry.coordinates[1];
				const fLon = f.geometry.coordinates[0];
				if (fLat < minLat || fLat > maxLat) return false;

				const dLon = Math.abs(fLon - lon);
				const shortestDLon = dLon > 180.0 ? 360.0 - dLon : dLon;
				if (shortestDLon > lonDelta) return false;

				return calculateDistance(lat, lon, fLat, fLon) < GDACS_SEARCH_RADIUS_KM;
			})
			.map(f => {
				const p = f.properties;
				const level: AlertLevel = p.alertlevel === 'red' ? 'emergency' : p.alertlevel === 'orange' ? 'warning' : 'watch';
				return {
					level,
					title: `GDACS: ${p.eventtype} - ${p.name}`,
					description: p.description,
					source: 'gdacs',
					time: p.fromdate ? new Date(p.fromdate).getTime() : undefined,
					link: p.url?.report
				};
			});
	} catch (e) {
		console.error('GDACS fetch error:', e);
		return [];
	}
}

function calculateDistance(lat1: number, lon1: number, lat2: number, lon2: number): number {
	const R = EARTH_RADIUS_KM;
	const dLat = (lat2 - lat1) * Math.PI / 180;
	const dLon = (lon2 - lon1) * Math.PI / 180;
	const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
		Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
		Math.sin(dLon / 2) * Math.sin(dLon / 2);
	const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
	return R * c;
}
