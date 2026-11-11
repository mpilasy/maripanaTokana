// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

import { writable } from 'svelte/store';
import { fontPairings } from '#lib/fonts.js';
import { SUPPORTED_LOCALES } from '#lib/i18n/locales.js';
import type { WeatherSource } from '#lib/domain/weatherData.js';

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
/** The index in SUPPORTED_LOCALES of the first of `languages` (BCP 47 tags) that the app has, else English. */
export function browserLocaleIndex(languages: readonly string[]): number {
	for (const tag of languages) {
		const i = SUPPORTED_LOCALES.findIndex((l) => l.tag === tag.split('-')[0].toLowerCase());
		if (i >= 0) return i;
	}
	return SUPPORTED_LOCALES.findIndex((l) => l.tag === 'en');
}

// The saved language; on the first visit, the browser language (twin of DefaultLanguage.kt)
export const localeIndex = persistedWritable<number>(
	'locale_index',
	browserLocaleIndex(typeof navigator === 'undefined' ? [] : navigator.languages?.length ? navigator.languages : [navigator.language ?? ''])
);

export const weatherSource = persistedWritable<WeatherSource>('weather_source', 'OPEN_METEO');
export const weatherApiKey = persistedWritable<string>('weather_api_key', '');
/** Open-Meteo model key; empty = Automatic. Read it through weatherModelFor (an unknown key is Automatic). */
export const weatherModel = persistedWritable<string>('weather_model', '');

export const alertsEnabled = persistedWritable<boolean>('alerts_enabled', true);
export const alertsNwsEnabled = persistedWritable<boolean>('alerts_nws', true);
export const alertsGdacsEnabled = persistedWritable<boolean>('alerts_gdacs', true);
export const alertsMeteoAlarmEnabled = persistedWritable<boolean>('alerts_meteoalarm', true);
export const alertsJmaEnabled = persistedWritable<boolean>('alerts_jma', true);
export const alertsEcccEnabled = persistedWritable<boolean>('alerts_eccc', true);
export const alertsBomEnabled = persistedWritable<boolean>('alerts_bom', true);
export const alertsNhcEnabled = persistedWritable<boolean>('alerts_nhc', true);

export function toggleUnits() {
	metricPrimary.update((v) => !v);
}

export function cycleFont() {
	fontIndex.update((i) => (i + 1) % fontPairings.length);
}

export function selectFont(i: number) {
	fontIndex.set(i);
}

export function cycleLanguage() {
	localeIndex.update((i) => (i + 1) % SUPPORTED_LOCALES.length);
}

export function selectLanguage(i: number) {
	localeIndex.set(i);
}
