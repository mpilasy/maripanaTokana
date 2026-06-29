import type { WeatherAlert, AlertLevel } from '$lib/domain/weatherData';

export async function fetchBomAlerts(stateCode: string | null): Promise<WeatherAlert[]> {
	try {
		const res = await fetch('/api/alerts/bom');
		if (!res.ok) return [];
		// eslint-disable-next-line @typescript-eslint/no-explicit-any
		const data: any = await res.json();
		// eslint-disable-next-line @typescript-eslint/no-explicit-any
		return (data.data ?? [])
			.filter((w: any) => w.warningAction !== 'cancelled')
			.filter((w: any) => !stateCode || !w.states?.length || (w.states as string[]).includes(stateCode))
			.map((w: any) => {
				const level: AlertLevel = w.phase === 'warning' ? 'warning' : 'watch';
				const eventType: string = w.short_title || w.title || 'Alert';
				const area: string = w.state ? `${w.state}: ${w.title ?? ''}` : (w.title ?? '');
				return { level, title: eventType, description: area, source: 'bom' as const };
			});
	} catch { return []; }
}
