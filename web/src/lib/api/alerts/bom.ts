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
				const rawDesc: string = w.short_title ?? '';
				const description = rawDesc.trim().toLowerCase() === (w.title ?? '').trim().toLowerCase() ? '' : rawDesc;
				return { level, title: w.title ?? 'Alert', description, source: 'bom' as const };
			});
	} catch { return []; }
}
