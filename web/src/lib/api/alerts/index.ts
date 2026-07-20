import type { WeatherAlert } from '$lib/domain/weatherData';
import { getLocationInfo, type LocationInfo } from './shared';
import { fetchNwsAlerts, isInUS } from './nws';
import { fetchGdacsAlerts } from './gdacs';
import { fetchMeteoAlarmAlerts, METEOALARM_COUNTRIES } from './meteoAlarm';
import { fetchJmaAlerts, isInJapan } from './jma';
import { fetchEcccAlerts, isInCanada } from './eccc';
import { fetchBomAlerts, isInAustralia } from './bom';
import { fetchNhcAlerts } from './nhc';
import { fetchWmoSwicAlerts } from './wmoSwic';

export { fetchNwsAlerts } from './nws';
export { fetchGdacsAlerts } from './gdacs';
export { fetchMeteoAlarmAlerts } from './meteoAlarm';
export { fetchJmaAlerts } from './jma';
export { fetchEcccAlerts } from './eccc';
export { fetchBomAlerts } from './bom';
export { fetchNhcAlerts } from './nhc';
export { fetchWmoSwicAlerts } from './wmoSwic';
export { calculateDistance } from './shared';

export interface AlertSettings {
	alertsEnabled: boolean;
	alertsNwsEnabled: boolean;
	alertsGdacsEnabled: boolean;
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
	settings: AlertSettings,
	locationInfo?: LocationInfo
): Promise<WeatherAlert[]> {
	if (!settings.alertsEnabled) return [];

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

	const [nws, gdacs, meteoAlarm, jma, eccc, bom, nhc, wmo] = await Promise.all([
		(settings.alertsNwsEnabled && inUS) ? fetchNwsAlerts(lat, lon) : Promise.resolve([]),
		(settings.alertsGdacsEnabled && !coveredByRegional) ? fetchGdacsAlerts(lat, lon) : Promise.resolve([]),
		settings.alertsMeteoAlarmEnabled ? fetchMeteoAlarmAlerts(lat, lon, cc, subdivisionName) : Promise.resolve([]),
		settings.alertsJmaEnabled ? fetchJmaAlerts(lat, lon) : Promise.resolve([]),
		(settings.alertsEcccEnabled && inCanada) ? fetchEcccAlerts(lat, lon, inCanada ? 'ca' : cc) : Promise.resolve([]),
		(settings.alertsBomEnabled && inAustralia) ? fetchBomAlerts(stateCode) : Promise.resolve([]),
		settings.alertsNhcEnabled ? fetchNhcAlerts(lat, lon) : Promise.resolve([]),
		(settings.alertsWmoSwicEnabled && !coveredByRegional) ? fetchWmoSwicAlerts(lat, lon, cc) : Promise.resolve([]),
	]);

	const sourceAlerts = [...nws, ...gdacs, ...meteoAlarm, ...jma, ...eccc, ...bom, ...nhc, ...wmo];

	return sourceAlerts.filter((a, i, self) =>
		i === self.findIndex(t => t.title === a.title && t.source === a.source)
	);
}
