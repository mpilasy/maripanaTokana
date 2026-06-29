import { writable } from 'svelte/store';
import { fontPairings } from '$lib/fonts';
import { SUPPORTED_LOCALES } from '$lib/i18n/locales';
import type { WeatherSource } from '$lib/domain/weatherData';

function persistedWritable<T>(key: string, defaultValue: T) {
	let initial = defaultValue;
	if (typeof localStorage !== 'undefined') {
		const stored = localStorage.getItem(key);
		if (stored !== null) {
			try {
				initial = JSON.parse(stored);
			} catch {
				// ignore invalid stored value
			}
		}
	}

	const store = writable<T>(initial);
	store.subscribe((value) => {
		if (typeof localStorage !== 'undefined') {
			localStorage.setItem(key, JSON.stringify(value));
		}
	});
	return store;
}

export const metricPrimary = persistedWritable<boolean>('metric_primary', true);
export const fontIndex = persistedWritable<number>('font_index', 0);
export const localeIndex = persistedWritable<number>('locale_index', 0);

export const weatherSource = persistedWritable<WeatherSource>('weather_source', 'OPEN_METEO');
export const weatherApiKey = persistedWritable<string>('weather_api_key', '');

export const alertsEnabled = persistedWritable<boolean>('alerts_enabled', true);
export const alertsNwsEnabled = persistedWritable<boolean>('alerts_nws', true);
export const alertsGdacsEnabled = persistedWritable<boolean>('alerts_gdacs', true);
export const alertsMeteoAlarmEnabled = persistedWritable<boolean>('alerts_meteoalarm', true);
export const alertsJmaEnabled = persistedWritable<boolean>('alerts_jma', true);
export const alertsEcccEnabled = persistedWritable<boolean>('alerts_eccc', true);
export const alertsWmoSwicEnabled = persistedWritable<boolean>('alerts_wmoswic', true);
export const alertsBomEnabled = persistedWritable<boolean>('alerts_bom', true);
export const alertsNhcEnabled = persistedWritable<boolean>('alerts_nhc', true);

export function toggleUnits() {
	metricPrimary.update((v) => !v);
}

export function cycleFont() {
	fontIndex.update((i) => (i + 1) % fontPairings.length);
}

export function cycleLanguage() {
	localeIndex.update((i) => (i + 1) % SUPPORTED_LOCALES.length);
}
