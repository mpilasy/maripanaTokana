// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

import type { WeatherAlert } from '#lib/domain/weatherData.js';
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

const ALERT_SOURCE_ORDER = ['NWS', 'GDACS', 'MeteoAlarm', 'JMA', 'ECCC', 'BOM', 'NHC'];

export interface AlertsResult {
	alerts: WeatherAlert[];
	failedSources: string[];
}

/** The regions with their own alert source that hold the place. Android has the same in `WeatherRepositoryImpl.kt`. */
interface AlertRegions {
	inUS: boolean;
	inCanada: boolean;
	inAustralia: boolean;
	inJapan: boolean;
	inMeteoAlarm: boolean;
}

// Coordinate-based fallbacks so a failed/rate-limited reverse-geocode (cc === '') doesn't
// silently suppress a country-gated source — reverse geocoding is an extra network call
// that can fail independently of the alert fetch itself.
function alertRegions(lat: number, lon: number, cc: string): AlertRegions {
	return {
		inUS: cc === 'us' || isInUS(lat, lon),
		inCanada: cc === 'ca' || isInCanada(lat, lon),
		inAustralia: cc === 'au' || isInAustralia(lat, lon),
		inJapan: isInJapan(lat, lon),
		inMeteoAlarm: METEOALARM_COUNTRIES.has(cc)
	};
}

/** A regional source covers the place, so GDACS is not needed. */
function coveredByRegional(r: AlertRegions): boolean {
	return r.inUS || r.inCanada || r.inAustralia || r.inJapan || r.inMeteoAlarm;
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

	const region = alertRegions(lat, lon, cc);

	const failed = new Set<string>();

	// Each source runs at the same time; a failure gives no alerts from that source and adds its name to `failed`
	async function source(name: string, enabled: boolean, fetch: () => Promise<WeatherAlert[]>): Promise<WeatherAlert[]> {
		if (!enabled) return [];
		try {
			return await fetch();
		} catch (e) {
			failed.add(name);
			console.warn(`${name} alerts unavailable:`, e instanceof Error ? e.message : 'fetch failed');
			return [];
		}
	}

	const sources = await Promise.all([
		source('NWS', settings.alertsNwsEnabled && region.inUS, () => fetchNwsAlerts(lat, lon)),
		// Skipped when a regional source covers the area
		source('GDACS', settings.alertsGdacsEnabled && !coveredByRegional(region), () => fetchGdacsAlerts(lat, lon)),
		source('MeteoAlarm', settings.alertsMeteoAlarmEnabled && region.inMeteoAlarm, () => fetchMeteoAlarmAlerts(lat, lon, cc, subdivisionName)),
		source('JMA', settings.alertsJmaEnabled && region.inJapan, () => fetchJmaAlerts(lat, lon)),
		source('ECCC', settings.alertsEcccEnabled && region.inCanada, () => fetchEcccAlerts(lat, lon, 'ca')),
		source('BOM', settings.alertsBomEnabled && region.inAustralia, () => fetchBomAlerts(stateCode)),
		source('NHC', settings.alertsNhcEnabled, () => fetchNhcAlerts(lat, lon)),
	]);

	const keys = new Set<string>();
	const alerts = sources.flat().filter((a) => {
		const key = a.title + a.source;
		if (keys.has(key)) return false;
		keys.add(key);
		return true;
	});
	return { alerts, failedSources: ALERT_SOURCE_ORDER.filter((s) => failed.has(s)) };
}
