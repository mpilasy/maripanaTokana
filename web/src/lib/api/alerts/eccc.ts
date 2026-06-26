import type { WeatherAlert, AlertLevel } from '$lib/domain/weatherData';
import { USER_AGENT } from './shared';

export async function fetchEcccAlerts(lat: number, lon: number, countryCode: string): Promise<WeatherAlert[]> {
	if (countryCode !== 'ca') return [];
	try {
		const res = await fetch(
			`https://api.weather.gc.ca/collections/alerts/items?bbox=${lon - 1},${lat - 1},${lon + 1},${lat + 1}&f=json`,
			{ headers: { 'User-Agent': USER_AGENT } }
		);
		if (!res.ok) return [];
		// eslint-disable-next-line @typescript-eslint/no-explicit-any
		const data: any = await res.json();
		// eslint-disable-next-line @typescript-eslint/no-explicit-any
		return (data.features ?? []).map((f: any) => {
			const p = f.properties ?? {};
			const level: AlertLevel = /warning/i.test(p.type ?? '') ? 'warning' : 'watch';
			return { level, title: p.alert_type ?? 'Alert', description: p.name ?? '', source: 'eccc' as const };
		});
	} catch { return []; }
}
