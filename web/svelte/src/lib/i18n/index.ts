import { register, init, getLocaleFromNavigator } from 'svelte-i18n';
export { SUPPORTED_LOCALES, localizeDigits } from '$shared/i18n/locales';
export type { SupportedLocale } from '$shared/i18n/locales';
import { SUPPORTED_LOCALES } from '$shared/i18n/locales';

/** Flatten canonical JSON: merge web_only into top-level, drop android_only. */
// eslint-disable-next-line @typescript-eslint/no-explicit-any
function flattenForWeb(mod: { default: any }) {
	const { web_only, android_only, ...shared } = mod.default;
	return { ...shared, ...web_only };
}

register('en', () => import('$shared/i18n/locales/en.json').then(flattenForWeb));
register('mg', () => import('$shared/i18n/locales/mg.json').then(flattenForWeb));
register('ar', () => import('$shared/i18n/locales/ar.json').then(flattenForWeb));
register('es', () => import('$shared/i18n/locales/es.json').then(flattenForWeb));
register('fr', () => import('$shared/i18n/locales/fr.json').then(flattenForWeb));
register('hi', () => import('$shared/i18n/locales/hi.json').then(flattenForWeb));
register('ne', () => import('$shared/i18n/locales/ne.json').then(flattenForWeb));
register('zh', () => import('$shared/i18n/locales/zh.json').then(flattenForWeb));

export function initI18n(savedLocaleTag?: string) {
	const fallback = 'mg';
	const initialLocale = savedLocaleTag || getLocaleFromNavigator()?.split('-')[0] || fallback;
	// Only use a supported locale
	const supported = SUPPORTED_LOCALES.find((l) => l.tag === initialLocale);

	init({
		fallbackLocale: fallback,
		initialLocale: supported ? supported.tag : fallback,
	});
}
