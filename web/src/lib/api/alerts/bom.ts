import type { WeatherAlert, AlertLevel } from '$lib/domain/weatherData';

export function isInAustralia(lat: number, lon: number): boolean {
	return lat >= -44.0 && lat <= -10.0 && lon >= 113.0 && lon <= 154.0;
}

export async function fetchBomAlerts(stateCode: string | null): Promise<WeatherAlert[]> {
	try {
		const res = await fetch('/api/alerts/bom');
		if (!res.ok) return [];
		// eslint-disable-next-line @typescript-eslint/no-explicit-any
		const data: any = await res.json();
		// eslint-disable-next-line @typescript-eslint/no-explicit-any
		return (data.data ?? [])
			.filter((w: any) => w.warning_action !== 'cancelled')
			.filter((w: any) => !stateCode || !w.states?.length || (w.states as string[]).includes(stateCode))
			.map((w: any) => {
				const level: AlertLevel = w.phase === 'warning' ? 'warning' : 'watch';
				// Android: shortTitle.ifBlank { shortDescription }
				const eventType: string = w.short_title || w.short_description || 'Alert';
				const area: string = w.state ? `${w.state}: ${w.title ?? ''}` : (w.title ?? '');
				return {
					level,
					title: eventType,
					description: area,
					source: 'bom' as const,
					time: w.issue_time ? new Date(w.issue_time).getTime() : undefined,
				};
			});
	} catch { return []; }
}
