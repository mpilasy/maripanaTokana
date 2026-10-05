import { describe, it, expect, vi, beforeEach } from 'vitest';
import { get } from 'svelte/store';

const data = { latitude: 1, longitude: 2, timestamp: 1, alerts: [], locationName: 'X' };
const getPosition = vi.fn();

vi.mock('$lib/stores/location', () => ({
	getCachedLocation: () => ({ lat: 1, lon: 2, name: 'X' }),
	cacheLocation: vi.fn(),
	movedSignificantly: () => false,
	getPosition: () => getPosition(),
	reverseGeocode: vi.fn(),
}));
vi.mock('$lib/api/openMeteo', () => ({ fetchWeather: vi.fn(async () => ({})) }));
vi.mock('$lib/api/openMeteoMapper', () => ({ mapToWeatherData: () => data }));
vi.mock('$lib/api/openMeteoAirQuality', () => ({
	fetchAirQuality: vi.fn(async () => null), mapToAirQuality: vi.fn(), mapToHourlyAirQuality: vi.fn(),
}));
vi.mock('$lib/api/alerts/shared', () => ({ getLocationInfo: vi.fn(async () => ({ countryCode: 'FR' })) }));
vi.mock('$lib/api/externalAlerts', () => ({ fetchAllAlerts: vi.fn(async () => ({ alerts: [], failedSources: [] })) }));
vi.mock('$lib/stores/weatherSnapshot', () => ({ saveSnapshot: vi.fn(), loadSnapshot: () => null }));
vi.mock('$lib/stores/preferences', async () => {
	const { writable } = await import('svelte/store');
	return {
		localeIndex: writable(0), weatherSource: writable('OPEN_METEO'), weatherApiKey: writable(''),
		alertsEnabled: writable(false), alertsNwsEnabled: writable(false), alertsGdacsEnabled: writable(false),
		alertsMeteoAlarmEnabled: writable(false), alertsJmaEnabled: writable(false), alertsEcccEnabled: writable(false),
		alertsBomEnabled: writable(false), alertsNhcEnabled: writable(false),
	};
});
vi.mock('$lib/stores/savedLocations', async () => {
	const { writable } = await import('svelte/store');
	return {
		activeLocationId: writable(null), savedLocations: writable([]),
		locationOverride: writable(null), checkOverrideExpiry: vi.fn(),
	};
});

import { doFetchWeather, weatherState, refreshFailed } from './weather';

describe('doFetchWeather GPS failure after cached weather shown', () => {
	beforeEach(() => {
		weatherState.set({ kind: 'loading' });
		refreshFailed.set(false);
		vi.spyOn(console, 'warn').mockImplementation(() => {});
	});

	it('keeps the data and does not flag a failed refresh', async () => {
		getPosition.mockRejectedValue(new Error('timeout'));
		await doFetchWeather();
		expect(get(weatherState).kind).toBe('success');
		expect(get(refreshFailed)).toBe(false);
	});
});
