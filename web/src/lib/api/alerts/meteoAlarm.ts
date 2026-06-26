import type { WeatherAlert, AlertLevel } from '$lib/domain/weatherData';
import { USER_AGENT } from './shared';

export const METEOALARM_COUNTRIES = new Set([
	'at','ba','be','bg','hr','cy','cz','dk','ee','fi','fr','de','gr','hu','ie','it',
	'lv','li','lt','lu','mt','md','me','nl','mk','no','pl','pt','ro','rs','sk','si',
	'es','se','ch','tr','ua','gb'
]);

export async function fetchMeteoAlarmAlerts(lat: number, lon: number, countryCode: string): Promise<WeatherAlert[]> {
	if (!METEOALARM_COUNTRIES.has(countryCode)) return [];
	try {
		const res = await fetch(
			`https://feeds.meteoalarm.org/feeds/meteoalarm-legacy-atom-${countryCode}`,
			{ headers: { 'User-Agent': USER_AGENT } }
		);
		if (!res.ok) return [];
		const text = await res.text();
		const parser = new DOMParser();
		const doc = parser.parseFromString(text, 'application/xml');
		const CAP = 'urn:oasis:names:tc:emergency:cap:1.2';
		const alerts: WeatherAlert[] = [];
		for (const entry of Array.from(doc.getElementsByTagName('entry'))) {
			const info = entry.getElementsByTagNameNS(CAP, 'info')[0];
			if (!info) continue;
			const event = info.getElementsByTagNameNS(CAP, 'event')[0]?.textContent ?? 'Alert';
			const severity = info.getElementsByTagNameNS(CAP, 'severity')[0]?.textContent ?? '';
			const desc = info.getElementsByTagNameNS(CAP, 'description')[0]?.textContent ?? '';
			const areaDesc = info.getElementsByTagNameNS(CAP, 'areaDesc')[0]?.textContent ?? '';
			const onset = info.getElementsByTagNameNS(CAP, 'onset')[0]?.textContent ?? null;
			const level: AlertLevel = severity === 'Extreme' ? 'emergency'
				: (severity === 'Severe' || severity === 'Moderate') ? 'warning' : 'watch';
			alerts.push({
				level,
				title: event,
				description: areaDesc ? `${areaDesc}: ${desc}` : desc,
				source: 'meteoalarm',
				time: onset ? new Date(onset).getTime() : undefined,
			});
		}
		return alerts;
	} catch { return []; }
}
