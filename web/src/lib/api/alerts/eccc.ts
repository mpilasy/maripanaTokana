import type { WeatherAlert, AlertLevel } from '$lib/domain/weatherData';

export async function fetchEcccAlerts(lat: number, lon: number, countryCode: string): Promise<WeatherAlert[]> {
	if (countryCode !== 'ca') return [];
	try {
		const bbox = `${lon - 1},${lat - 1},${lon + 1},${lat + 1}`;
		const res = await fetch(`/api/alerts/eccc?bbox=${bbox}`);
		if (!res.ok) return [];
		// eslint-disable-next-line @typescript-eslint/no-explicit-any
		const data: any = await res.json();
		// eslint-disable-next-line @typescript-eslint/no-explicit-any
		return (data.features ?? []).map((f: any) => {
			const p = f.properties ?? {};
			// Android: extreme/severe → warning, else → watch
			const sev = (p.severity ?? '').toLowerCase();
			const level: AlertLevel = (sev === 'extreme' || sev === 'severe') ? 'warning' : 'watch';
			return {
				level,
				title: p.headline || 'ECCC Alert',
				description: p.description ?? '',
				source: 'eccc' as const,
				time: p.onset ? new Date(p.onset).getTime() : undefined,
			};
		});
	} catch { return []; }
}
