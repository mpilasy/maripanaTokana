import type { WeatherAlert, AlertLevel } from '$lib/domain/weatherData';

export async function fetchWmoSwicAlerts(lat: number, lon: number, countryCode: string): Promise<WeatherAlert[]> {
	if (!countryCode) return [];
	try {
		const res = await fetch(`https://severe.worldweather.wmo.int/json/${countryCode.toUpperCase()}.json`);
		if (!res.ok) return [];
		// eslint-disable-next-line @typescript-eslint/no-explicit-any
		const data: any = await res.json();
		// eslint-disable-next-line @typescript-eslint/no-explicit-any
		return (data.Warning ?? []).map((w: any) => ({
			level: 'warning' as AlertLevel,
			title: w.Summary ?? 'WMO Alert',
			description: w.Detail ?? '',
			source: 'wmo_swic' as const,
			time: w.Issuance ? new Date(w.Issuance).getTime() : undefined,
		}));
	} catch { return []; }
}
