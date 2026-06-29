import type { WeatherAlert } from '$lib/domain/weatherData';
import { getLocationInfo } from './shared';
import { fetchNwsAlerts } from './nws';
import { fetchGdacsAlerts } from './gdacs';
import { fetchMeteoAlarmAlerts, METEOALARM_COUNTRIES } from './meteoAlarm';
import { fetchJmaAlerts, isInJapan } from './jma';
import { fetchEcccAlerts } from './eccc';
import { fetchBomAlerts } from './bom';
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

	const { countryCode, stateCode } = await getLocationInfo(lat, lon);
	const cc = countryCode ?? '';

	const coveredByRegional =
		cc === 'us' || cc === 'ca' || cc === 'au' ||
		METEOALARM_COUNTRIES.has(cc) || isInJapan(lat, lon);

	const [nws, gdacs, meteoAlarm, jma, eccc, bom, nhc, wmo] = await Promise.all([
		(settings.alertsNwsEnabled && cc === 'us') ? fetchNwsAlerts(lat, lon) : Promise.resolve([]),
		(settings.alertsGdacsEnabled && !coveredByRegional) ? fetchGdacsAlerts(lat, lon) : Promise.resolve([]),
		settings.alertsMeteoAlarmEnabled ? fetchMeteoAlarmAlerts(lat, lon, cc) : Promise.resolve([]),
		settings.alertsJmaEnabled ? fetchJmaAlerts(lat, lon) : Promise.resolve([]),
		(settings.alertsEcccEnabled && cc === 'ca') ? fetchEcccAlerts(lat, lon, cc) : Promise.resolve([]),
		(settings.alertsBomEnabled && cc === 'au') ? fetchBomAlerts(stateCode) : Promise.resolve([]),
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
