import { describe, it, expect, vi, beforeEach } from 'vitest';
import { get } from 'svelte/store';

const data = { latitude: 1, longitude: 2, timestamp: 1, alerts: [], locationName: 'X' };
const getPosition = vi.fn();
const fetchWeatherMock = vi.hoisted(() => vi.fn());

vi.mock('$lib/stores/location', () => ({
	getCachedLocation: () => ({ lat: 1, lon: 2, name: 'X' }),
	cacheLocation: vi.fn(),
	movedSignificantly: () => false,
	getPosition: () => getPosition(),
	reverseGeocode: vi.fn(),
}));
vi.mock('$lib/api/openMeteo', () => ({ fetchWeather: fetchWeatherMock }));
vi.mock('$lib/api/openMeteoMapper', () => ({ mapToWeatherData: () => ({ ...data }) }));
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

import { doFetchWeather, weatherState, refreshFailed, refreshError } from './weather';
import { HttpError } from '$lib/api/http';

describe('doFetchWeather GPS failure after cached weather shown', () => {
	beforeEach(() => {
		weatherState.set({ kind: 'loading' });
		refreshFailed.set(false);
		refreshError.set(null);
		fetchWeatherMock.mockReset();
		fetchWeatherMock.mockResolvedValue({});
		vi.spyOn(console, 'warn').mockImplementation(() => {});
	});

	it('keeps the data and does not flag a failed refresh', async () => {
		getPosition.mockRejectedValue(new Error('timeout'));
		await doFetchWeather();
		expect(get(weatherState).kind).toBe('success');
		expect(get(refreshFailed)).toBe(false);
	});
});

describe('doFetchWeather failure handling', () => {
	beforeEach(() => {
		weatherState.set({ kind: 'loading' });
		refreshFailed.set(false);
		refreshError.set(null);
		fetchWeatherMock.mockReset();
		getPosition.mockResolvedValue({ lat: 1, lon: 2 });
	});

	it('uses the service-worker cache time as timestamp and flags the refresh as failed', async () => {
		fetchWeatherMock.mockResolvedValue({ cachedAt: 123456 });
		await doFetchWeather();
		const s = get(weatherState);
		expect(s.kind === 'success' && s.data.timestamp).toBe(123456);
		expect(get(refreshFailed)).toBe(true);
		expect(get(refreshError)).toBe('error_offline');
	});

	it('does not refetch the same location after a 4xx', async () => {
		fetchWeatherMock.mockRejectedValue(new HttpError(401));
		await doFetchWeather();
		expect(fetchWeatherMock).toHaveBeenCalledTimes(1);
		expect(get(weatherState)).toEqual({ kind: 'error', message: 'error_fetch_weather' });
	});

	it('does not refetch an unmoved location after a 5xx either', async () => {
		fetchWeatherMock.mockRejectedValue(new HttpError(503));
		await doFetchWeather();
		expect(fetchWeatherMock).toHaveBeenCalledTimes(1);
		expect(get(weatherState)).toEqual({ kind: 'error', message: 'error_server' });
	});

	it('stores the failure reason when data is already on screen', async () => {
		fetchWeatherMock.mockResolvedValue({});
		await doFetchWeather();
		fetchWeatherMock.mockRejectedValue(new HttpError(429));
		await doFetchWeather();
		expect(get(weatherState).kind).toBe('success');
		expect(get(refreshFailed)).toBe(true);
		expect(get(refreshError)).toBe('error_rate_limited');
	});

	it('keeps the reason null for generic failures', async () => {
		fetchWeatherMock.mockResolvedValue({});
		await doFetchWeather();
		fetchWeatherMock.mockRejectedValue(new Error('boom'));
		await doFetchWeather();
		expect(get(refreshFailed)).toBe(true);
		expect(get(refreshError)).toBeNull();
	});
});
