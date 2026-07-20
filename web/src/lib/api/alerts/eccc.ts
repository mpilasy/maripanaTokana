import type { WeatherAlert, AlertLevel } from '$lib/domain/weatherData';

// Coarse fallback for when reverse geocoding fails — mirrors isInAustralia/isInJapan/isInUS.
export function isInCanada(lat: number, lon: number): boolean {
	return lat >= 41.0 && lat <= 84.0 && lon >= -141.0 && lon <= -52.0;
}

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
			// Android: warning → warning, else (watch/statement) → watch
			const level: AlertLevel = (p.alert_type ?? '').toLowerCase() === 'warning' ? 'warning' : 'watch';
			return {
				level,
				title: p.alert_name_en || 'ECCC Alert',
				description: p.alert_text_en ?? '',
				source: 'eccc' as const,
				time: p.publication_datetime ? new Date(p.publication_datetime).getTime() : undefined,
			};
		});
	} catch { return []; }
}
