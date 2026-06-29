import type { NwsAlertResponse } from '../externalAlertsTypes';
import type { WeatherAlert, AlertLevel } from '$lib/domain/weatherData';
import { USER_AGENT } from './shared';

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
