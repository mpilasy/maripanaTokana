import type { WeatherAlert, AlertLevel } from '$lib/domain/weatherData';

export const METEOALARM_COUNTRIES = new Set([
	'at','ba','be','bg','hr','cy','cz','dk','ee','fi','fr','de','gr','hu','ie','it',
	'lv','lt','lu','mt','md','me','nl','mk','no','pl','pt','ro','rs','sk','si',
	'es','se','ch','ua','gb'
]);

export async function fetchMeteoAlarmAlerts(lat: number, lon: number, countryCode: string): Promise<WeatherAlert[]> {
	if (!METEOALARM_COUNTRIES.has(countryCode)) return [];
	try {
		const res = await fetch(`/api/alerts/meteoalarm?country=${countryCode}`);
		if (!res.ok) return [];
		const text = await res.text();
		const parser = new DOMParser();
		const doc = parser.parseFromString(text, 'application/xml');
		const CAP = 'urn:oasis:names:tc:emergency:cap:1.2';
		const alerts: WeatherAlert[] = [];
		for (const entry of Array.from(doc.getElementsByTagName('entry'))) {
			const g = (tag: string) => entry.getElementsByTagNameNS(CAP, tag)[0]?.textContent ?? '';
			const status = g('status');
			if (status && status !== 'Actual') continue;
			const event = g('event') || 'Alert';
			const severity = g('severity');
			const desc = g('description');
			const areaDesc = g('areaDesc');
			const onset = g('onset');
			const linkEl = entry.querySelector('link[rel="alternate"]');
			const link = linkEl?.getAttribute('href') || undefined;
			// Android mapping: extreme→emergency, severe→warning, else→watch
			const level: AlertLevel = severity === 'Extreme' ? 'emergency'
				: severity === 'Severe' ? 'warning' : 'watch';
			alerts.push({
				level,
				title: event,
				description: desc,
				source: 'meteoalarm',
				time: onset ? new Date(onset).getTime() : undefined,
				headline: areaDesc || undefined,
				link,
			});
		}
		return alerts;
	} catch { return []; }
}
