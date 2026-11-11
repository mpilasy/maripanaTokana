// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

import { register, init, getLocaleFromNavigator } from 'svelte-i18n';
export { SUPPORTED_LOCALES, localizeDigits } from '#lib/i18n/locales.js';
export type { SupportedLocale } from '#lib/i18n/locales.js';
import { SUPPORTED_LOCALES } from '#lib/i18n/locales.js';

/** Flatten canonical JSON: merge web_only into top-level, drop android_only. */
// eslint-disable-next-line @typescript-eslint/no-explicit-any
function flattenForWeb(mod: { default: any }) {
	const { web_only, android_only, ...shared } = mod.default;
	return { ...shared, ...web_only };
}

register('en', () => import('#lib/i18n/locales/en.json').then(flattenForWeb));
register('mg', () => import('#lib/i18n/locales/mg.json').then(flattenForWeb));
register('ar', () => import('#lib/i18n/locales/ar.json').then(flattenForWeb));
register('es', () => import('#lib/i18n/locales/es.json').then(flattenForWeb));
register('fr', () => import('#lib/i18n/locales/fr.json').then(flattenForWeb));
register('hi', () => import('#lib/i18n/locales/hi.json').then(flattenForWeb));
register('ne', () => import('#lib/i18n/locales/ne.json').then(flattenForWeb));
register('zh', () => import('#lib/i18n/locales/zh.json').then(flattenForWeb));

export function initI18n(savedLocaleTag?: string) {
	// The saved language, else the first browser language that the app has, else English
	const fallback = 'en';
	const browser = typeof navigator === 'undefined' ? [] : [...(navigator.languages ?? []), getLocaleFromNavigator() ?? ''];
	const candidates = [savedLocaleTag ?? '', ...browser].map((tag) => tag.split('-')[0]);
	const supported = candidates.find((tag) => SUPPORTED_LOCALES.some((l) => l.tag === tag));

	init({
		fallbackLocale: fallback,
		initialLocale: supported ?? fallback,
	});
}
