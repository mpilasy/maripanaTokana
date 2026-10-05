import type { WeatherAlert } from '$lib/domain/weatherData';
import { getLocationInfo, type LocationInfo } from './shared';
import { fetchNwsAlerts, isInUS } from './nws';
import { fetchGdacsAlerts } from './gdacs';
import { fetchMeteoAlarmAlerts, METEOALARM_COUNTRIES } from './meteoAlarm';
import { fetchJmaAlerts, isInJapan } from './jma';
import { fetchEcccAlerts, isInCanada } from './eccc';
import { fetchBomAlerts, isInAustralia } from './bom';
import { fetchNhcAlerts } from './nhc';

export { fetchNwsAlerts } from './nws';
export { fetchGdacsAlerts } from './gdacs';
export { fetchMeteoAlarmAlerts } from './meteoAlarm';
export { fetchJmaAlerts } from './jma';
export { fetchEcccAlerts } from './eccc';
export { fetchBomAlerts } from './bom';
export { fetchNhcAlerts } from './nhc';
export { calculateDistance } from './shared';

export interface AlertSettings {
	alertsEnabled: boolean;
	alertsNwsEnabled: boolean;
	alertsGdacsEnabled: boolean;
	alertsMeteoAlarmEnabled: boolean;
	alertsJmaEnabled: boolean;
	alertsEcccEnabled: boolean;
	alertsBomEnabled: boolean;
	alertsNhcEnabled: boolean;
}

export interface AlertsResult {
	alerts: WeatherAlert[];
	failedSources: string[];
}

export async function fetchAllAlerts(
	lat: number,
	lon: number,
	settings: AlertSettings,
	locationInfo?: LocationInfo
): Promise<AlertsResult> {
	if (!settings.alertsEnabled) return { alerts: [], failedSources: [] };

	// Accept a pre-fetched location lookup when the caller already needed one (e.g. for AQI
	// standard selection) to avoid firing a second, redundant reverse-geocode request.
	const { countryCode, stateCode, subdivisionName } = locationInfo ?? await getLocationInfo(lat, lon);
	const cc = countryCode ?? '';

	// Coordinate-based fallbacks so a failed/rate-limited reverse-geocode (cc === '') doesn't
	// silently suppress a country-gated source — reverse geocoding is an extra network call
	// that can fail independently of the alert fetch itself.
	const inUS = cc === 'us' || isInUS(lat, lon);
	const inCanada = cc === 'ca' || isInCanada(lat, lon);
	const inAustralia = cc === 'au' || isInAustralia(lat, lon);

	const coveredByRegional =
		inUS || inCanada || inAustralia ||
		METEOALARM_COUNTRIES.has(cc) || isInJapan(lat, lon);

	const sources: [string, Promise<WeatherAlert[]> | null][] = [
		['NWS', (settings.alertsNwsEnabled && inUS) ? fetchNwsAlerts(lat, lon) : null],
		['GDACS', (settings.alertsGdacsEnabled && !coveredByRegional) ? fetchGdacsAlerts(lat, lon) : null],
		['MeteoAlarm', settings.alertsMeteoAlarmEnabled ? fetchMeteoAlarmAlerts(lat, lon, cc, subdivisionName) : null],
		['JMA', settings.alertsJmaEnabled ? fetchJmaAlerts(lat, lon) : null],
		['ECCC', (settings.alertsEcccEnabled && inCanada) ? fetchEcccAlerts(lat, lon, 'ca') : null],
		['BOM', (settings.alertsBomEnabled && inAustralia) ? fetchBomAlerts(stateCode) : null],
		['NHC', settings.alertsNhcEnabled ? fetchNhcAlerts(lat, lon) : null],
	];
	const active = sources.filter((s): s is [string, Promise<WeatherAlert[]>] => s[1] !== null);
	const results = await Promise.allSettled(active.map(([, p]) => p));

	const sourceAlerts: WeatherAlert[] = [];
	const failedSources: string[] = [];
	results.forEach((r, i) => {
		if (r.status === 'fulfilled') {
			sourceAlerts.push(...r.value);
		} else {
			failedSources.push(active[i][0]);
			console.warn(`${active[i][0]} alerts unavailable:`, r.reason instanceof Error ? r.reason.message : 'fetch failed');
		}
	});

	const alerts = sourceAlerts.filter((a, i, self) =>
		i === self.findIndex(t => t.title === a.title && t.source === a.source)
	);
	return { alerts, failedSources };
}
