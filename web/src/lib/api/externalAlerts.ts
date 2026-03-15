import type { NwsAlertResponse, GdacsAlertResponse } from './externalAlertsTypes';
import type { WeatherAlert, AlertLevel } from '../domain/weatherData';

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
		const fromDate = new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString().split('T')[0];
		
		const res = await fetch(`https://www.gdacs.org/gdacsapi/api/events/geteventlist/SEARCH?fromdate=${fromDate}&todate=${toDate}&alertlevel=green;orange;red`);
		if (!res.ok) return [];
		const data: GdacsAlertResponse = await res.json();
		
		// Filter by distance (radius 500km)
		return data.features
			.filter(f => calculateDistance(lat, lon, f.geometry.coordinates[1], f.geometry.coordinates[0]) < 500)
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
	const R = 6371; // km
	const dLat = (lat2 - lat1) * Math.PI / 180;
	const dLon = (lon2 - lon1) * Math.PI / 180;
	const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
		Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
		Math.sin(dLon / 2) * Math.sin(dLon / 2);
	const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
	return R * c;
}
