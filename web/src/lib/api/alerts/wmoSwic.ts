import type { WeatherAlert, AlertLevel } from '$lib/domain/weatherData';

export async function fetchWmoSwicAlerts(lat: number, lon: number, countryCode: string): Promise<WeatherAlert[]> {
	if (!countryCode) return [];
	try {
		const res = await fetch(`/api/alerts/wmoswic?country=${countryCode.toUpperCase()}`);
		if (!res.ok) return [];
		// eslint-disable-next-line @typescript-eslint/no-explicit-any
		const data: any = await res.json();
		// eslint-disable-next-line @typescript-eslint/no-explicit-any
		return (data.Warning ?? []).map((w: any) => ({
			level: 'warning' as AlertLevel,
			title: w.Summary || 'WMO Alert',
			description: w.Detail || w.Summary || '',
			source: 'wmoswic' as const,
			time: w.Issuance ? new Date(w.Issuance).getTime() : undefined,
			headline: w.City?.trim() || undefined,
			link: w.Url?.trim() || undefined,
		}));
	} catch { return []; }
}
