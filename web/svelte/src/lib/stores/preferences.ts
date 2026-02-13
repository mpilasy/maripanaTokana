import { writable } from 'svelte/store';
import { fontPairings } from '$shared/fonts';
import { SUPPORTED_LOCALES } from '$shared/i18n/locales';

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

export function toggleUnits() {
	metricPrimary.update((v) => !v);
}

export function cycleFont() {
	fontIndex.update((i) => (i + 1) % fontPairings.length);
}

export function cycleLanguage() {
	localeIndex.update((i) => (i + 1) % SUPPORTED_LOCALES.length);
}
