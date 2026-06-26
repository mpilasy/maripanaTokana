import type { WeatherAlert, AlertLevel } from '$lib/domain/weatherData';
import { USER_AGENT } from './shared';

export async function fetchBomAlerts(): Promise<WeatherAlert[]> {
	try {
		const res = await fetch('https://api.weather.bom.gov.au/v1/warnings', {
			headers: { 'User-Agent': USER_AGENT }
		});
		if (!res.ok) return [];
		// eslint-disable-next-line @typescript-eslint/no-explicit-any
		const data: any = await res.json();
		// eslint-disable-next-line @typescript-eslint/no-explicit-any
		return (data.data ?? [])
			.filter((w: any) => w.warningAction !== 'cancelled')
			.map((w: any) => {
				const level: AlertLevel = w.phase === 'warning' ? 'warning' : 'watch';
				return { level, title: w.title ?? 'Alert', description: w.shortDescription ?? '', source: 'bom' as const };
			});
	} catch { return []; }
}
