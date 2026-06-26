import type { NwsAlertResponse, GdacsAlertResponse } from './externalAlertsTypes';
import type { WeatherAlert, AlertLevel } from '../domain/weatherData';

const GDACS_SEARCH_RADIUS_KM = 500;
const GDACS_SEARCH_DAYS = 7;
const EARTH_RADIUS_KM = 6371;
const NHC_SEARCH_RADIUS_KM = 1500;

const USER_AGENT = 'maripanaTokana (contact@orinasa.mg)';

const METEOALARM_COUNTRIES = new Set([
	'at','ba','be','bg','hr','cy','cz','dk','ee','fi','fr','de','gr','hu','ie','it',
	'lv','li','lt','lu','mt','md','me','nl','mk','no','pl','pt','ro','rs','sk','si',
	'es','se','ch','tr','ua','gb'
]);

export async function fetchNwsAlerts(lat: number, lon: number): Promise<WeatherAlert[]> {
	try {
		const res = await fetch(`https://api.weather.gov/alerts/active?point=${lat},${lon}`, {
			headers: {
				'Accept': 'application/geo+json',
				'User-Agent': USER_AGENT
			}
		});
		if (!res.ok) return [];
		const data: NwsAlertResponse = await res.json();
		return data.features.map(f => {
			const p = f.properties;
			const level: AlertLevel = (p.severity === 'Extreme' || p.severity === 'Severe') ? 'warning' : 'watch';
			return {
				level,
				title: p.event,
				description: p.description + (p.instruction ? '\n\n' + p.instruction : ''),
				source: 'official' as const,
				time: p.sent ? new Date(p.sent).getTime() : undefined,
				headline: p.headline,
				link: f.id
			};
		});
	} catch (e) {
		console.error('NWS fetch error:', e);
		return [];
	}
}

export async function fetchGdacsAlerts(lat: number, lon: number): Promise<WeatherAlert[]> {
	try {
		const toDate = new Date().toISOString().split('T')[0];
		const fromDate = new Date(Date.now() - GDACS_SEARCH_DAYS * 24 * 60 * 60 * 1000).toISOString().split('T')[0];

		const res = await fetch(`https://www.gdacs.org/gdacsapi/api/events/geteventlist/SEARCH?fromdate=${fromDate}&todate=${toDate}&alertlevel=green;orange;red`);
		if (!res.ok) return [];
		const data: GdacsAlertResponse = await res.json();

		const latDelta = GDACS_SEARCH_RADIUS_KM / 111.0;
		const lonDelta = Math.abs(lat) < 89.0 ? GDACS_SEARCH_RADIUS_KM / (111.0 * Math.cos(lat * Math.PI / 180)) : 360.0;
		const minLat = lat - latDelta;
		const maxLat = lat + latDelta;

		return data.features
			.filter(f => {
				const fLat = f.geometry.coordinates[1];
				const fLon = f.geometry.coordinates[0];
				if (fLat < minLat || fLat > maxLat) return false;

				const dLon = Math.abs(fLon - lon);
				const shortestDLon = dLon > 180.0 ? 360.0 - dLon : dLon;
				if (shortestDLon > lonDelta) return false;

				return calculateDistance(lat, lon, fLat, fLon) < GDACS_SEARCH_RADIUS_KM;
			})
			.map(f => {
				const p = f.properties;
				const level: AlertLevel = p.alertlevel === 'red' ? 'emergency' : p.alertlevel === 'orange' ? 'warning' : 'watch';
				return {
					level,
					title: `GDACS: ${p.eventtype} - ${p.name}`,
					description: p.description,
					source: 'gdacs' as const,
					time: p.fromdate ? new Date(p.fromdate).getTime() : undefined,
					link: p.url?.report
				};
			});
	} catch (e) {
		console.error('GDACS fetch error:', e);
		return [];
	}
}

export function calculateDistance(lat1: number, lon1: number, lat2: number, lon2: number): number {
	const R = EARTH_RADIUS_KM;
	const dLat = (lat2 - lat1) * Math.PI / 180;
	const dLon = (lon2 - lon1) * Math.PI / 180;
	const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
		Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
		Math.sin(dLon / 2) * Math.sin(dLon / 2);
	const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
	return R * c;
}

async function getCountryCode(lat: number, lon: number): Promise<string | null> {
	try {
		const res = await fetch(
			`https://nominatim.openstreetmap.org/reverse?lat=${lat}&lon=${lon}&format=json&zoom=3`,
			{ headers: { 'User-Agent': USER_AGENT } }
		);
		if (!res.ok) return null;
		// eslint-disable-next-line @typescript-eslint/no-explicit-any
		const data: any = await res.json();
		return data.address?.country_code?.toLowerCase() ?? null;
	} catch { return null; }
}

function isInJapan(lat: number, lon: number): boolean {
	return lat >= 24.0 && lat <= 46.0 && lon >= 122.0 && lon <= 154.0;
}

function nearestJapanPrefectureCode(lat: number, lon: number): string {
	const prefectures: [string, number, number][] = [
		['010006',43.06,141.35],['020000',40.82,140.74],['030000',39.70,141.15],
		['040000',38.27,140.87],['050000',39.72,140.10],['060000',38.24,140.36],
		['070000',37.75,140.47],['080000',36.34,140.45],['090000',36.57,139.88],
		['100000',36.39,139.06],['110000',35.86,139.65],['120000',35.61,140.12],
		['130000',35.69,139.69],['140000',35.45,139.64],['150000',37.90,139.02],
		['160000',36.70,137.21],['170000',36.59,136.63],['180000',36.07,136.22],
		['190000',35.66,138.57],['200000',36.65,138.18],['210000',35.39,136.72],
		['220000',34.98,138.38],['230000',35.18,136.91],['240000',34.73,136.51],
		['250000',35.00,135.87],['260000',35.02,135.76],['270000',34.69,135.50],
		['280000',34.69,135.19],['290000',34.69,135.83],['300000',34.23,135.17],
		['310000',35.50,134.24],['320000',35.47,133.05],['330000',34.66,133.93],
		['340000',34.40,132.45],['350000',34.19,131.47],['360000',34.07,134.56],
		['370000',34.34,134.04],['380000',33.84,132.77],['390000',33.56,133.53],
		['400000',33.61,130.42],['410000',33.25,130.30],['420000',32.74,129.87],
		['430000',32.79,130.74],['440000',33.24,131.61],['450000',31.91,131.42],
		['460000',31.56,130.56],['471000',26.21,127.68]
	];
	let best = prefectures[0];
	let bestDist = Infinity;
	for (const p of prefectures) {
		const d = (lat - p[1]) ** 2 + (lon - p[2]) ** 2;
		if (d < bestDist) { bestDist = d; best = p; }
	}
	return best[0];
}

export async function fetchMeteoAlarmAlerts(lat: number, lon: number, countryCode: string): Promise<WeatherAlert[]> {
	if (!METEOALARM_COUNTRIES.has(countryCode)) return [];
	try {
		const res = await fetch(`https://feeds.meteoalarm.org/feeds/meteoalarm-legacy-atom-${countryCode}`, {
			headers: { 'User-Agent': USER_AGENT }
		});
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

export async function fetchJmaAlerts(lat: number, lon: number): Promise<WeatherAlert[]> {
	if (!isInJapan(lat, lon)) return [];
	try {
		const areaCode = nearestJapanPrefectureCode(lat, lon);
		const res = await fetch(`https://www.jma.go.jp/bosai/warning/data/warning/${areaCode}.json`);
		if (!res.ok) return [];
		// eslint-disable-next-line @typescript-eslint/no-explicit-any
		const data: any = await res.json();
		const alerts: WeatherAlert[] = [];
		// eslint-disable-next-line @typescript-eslint/no-explicit-any
		for (const areaType of (data.areaTypes ?? [])) {
			// eslint-disable-next-line @typescript-eslint/no-explicit-any
			for (const area of (areaType.areas ?? [])) {
				// eslint-disable-next-line @typescript-eslint/no-explicit-any
				for (const w of (area.warnings ?? [])) {
					if (w.status !== '発表' && w.status !== '継続') continue;
					const level: AlertLevel = w.code === '01' ? 'emergency' : (parseInt(w.code) <= 8) ? 'warning' : 'watch';
					alerts.push({ level, title: `JMA: ${area.name}`, description: '', source: 'jma' });
				}
			}
		}
		return alerts;
	} catch { return []; }
}

export async function fetchEcccAlerts(lat: number, lon: number, countryCode: string): Promise<WeatherAlert[]> {
	if (countryCode !== 'ca') return [];
	try {
		const minLon = lon - 1; const maxLon = lon + 1;
		const minLat = lat - 1; const maxLat = lat + 1;
		const res = await fetch(
			`https://api.weather.gc.ca/collections/alerts/items?bbox=${minLon},${minLat},${maxLon},${maxLat}&f=json`,
			{ headers: { 'User-Agent': USER_AGENT } }
		);
		if (!res.ok) return [];
		// eslint-disable-next-line @typescript-eslint/no-explicit-any
		const data: any = await res.json();
		// eslint-disable-next-line @typescript-eslint/no-explicit-any
		return (data.features ?? []).map((f: any) => {
			const p = f.properties ?? {};
			const level: AlertLevel = /warning/i.test(p.type ?? '') ? 'warning' : 'watch';
			return { level, title: p.alert_type ?? 'Alert', description: p.name ?? '', source: 'eccc' as const };
		});
	} catch { return []; }
}

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

export async function fetchNhcAlerts(lat: number, lon: number): Promise<WeatherAlert[]> {
	try {
		const res = await fetch('https://www.nhc.noaa.gov/CurrentStorms.json');
		if (!res.ok) return [];
		// eslint-disable-next-line @typescript-eslint/no-explicit-any
		const data: any = await res.json();
		// eslint-disable-next-line @typescript-eslint/no-explicit-any
		return (data.activeStorms ?? [])
			.filter((s: any) => {
				const sLat = s.latitudeNumeric; const sLon = s.longitudeNumeric;
				if (sLat == null || sLon == null) return false;
				return calculateDistance(lat, lon, sLat, sLon) < NHC_SEARCH_RADIUS_KM;
			})
			// eslint-disable-next-line @typescript-eslint/no-explicit-any
			.map((s: any) => {
				const intensity = parseInt(s.intensity ?? '0');
				const level: AlertLevel = s.classification === 'HU' && intensity >= 96 ? 'emergency'
					: (s.classification === 'HU' || s.classification === 'TS') ? 'warning' : 'watch';
				return {
					level,
					title: `${s.name}: ${s.headline ?? s.classification}`,
					description: '',
					source: 'nhc' as const,
					link: s.advisory?.url
				};
			});
	} catch { return []; }
}

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

export interface AlertSettings {
	alertsEnabled: boolean;
	alertsNwsEnabled: boolean;
	alertsGdacsEnabled: boolean;
	alertsDerivedEnabled: boolean;
	alertsMeteoAlarmEnabled: boolean;
	alertsJmaEnabled: boolean;
	alertsEcccEnabled: boolean;
	alertsWmoSwicEnabled: boolean;
	alertsBomEnabled: boolean;
	alertsNhcEnabled: boolean;
}

export async function fetchAllAlerts(
	lat: number,
	lon: number,
	derivedAlerts: WeatherAlert[],
	settings: AlertSettings
): Promise<WeatherAlert[]> {
	if (!settings.alertsEnabled) return [];

	const countryCode = await getCountryCode(lat, lon);
	const cc = countryCode ?? '';

	const coveredByRegional =
		cc === 'us' || cc === 'ca' || cc === 'au' ||
		METEOALARM_COUNTRIES.has(cc) || isInJapan(lat, lon);

	const [nws, gdacs, meteoAlarm, jma, eccc, bom, nhc, wmo] = await Promise.all([
		settings.alertsNwsEnabled ? fetchNwsAlerts(lat, lon) : Promise.resolve([]),
		(settings.alertsGdacsEnabled && !coveredByRegional) ? fetchGdacsAlerts(lat, lon) : Promise.resolve([]),
		settings.alertsMeteoAlarmEnabled ? fetchMeteoAlarmAlerts(lat, lon, cc) : Promise.resolve([]),
		settings.alertsJmaEnabled ? fetchJmaAlerts(lat, lon) : Promise.resolve([]),
		settings.alertsEcccEnabled ? fetchEcccAlerts(lat, lon, cc) : Promise.resolve([]),
		(settings.alertsBomEnabled && cc === 'au') ? fetchBomAlerts() : Promise.resolve([]),
		settings.alertsNhcEnabled ? fetchNhcAlerts(lat, lon) : Promise.resolve([]),
		(settings.alertsWmoSwicEnabled && !coveredByRegional) ? fetchWmoSwicAlerts(lat, lon, cc) : Promise.resolve([]),
	]);

	const sourceAlerts = [...nws, ...gdacs, ...meteoAlarm, ...jma, ...eccc, ...bom, ...nhc, ...wmo];
	const hasSourceAlerts = sourceAlerts.length > 0;

	const all = (hasSourceAlerts || !settings.alertsDerivedEnabled)
		? sourceAlerts
		: [...sourceAlerts, ...derivedAlerts];

	return all.filter((a, i, self) =>
		i === self.findIndex(t => t.title === a.title && t.source === a.source)
	);
}
