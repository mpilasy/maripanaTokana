import { register, init, getLocaleFromNavigator } from 'svelte-i18n';
export { SUPPORTED_LOCALES, localizeDigits } from '$shared/i18n/locales';
export type { SupportedLocale } from '$shared/i18n/locales';
import { SUPPORTED_LOCALES } from '$shared/i18n/locales';

register('en', () => import('$shared/i18n/locales/en.json'));
register('mg', () => import('$shared/i18n/locales/mg.json'));
register('ar', () => import('$shared/i18n/locales/ar.json'));
register('es', () => import('$shared/i18n/locales/es.json'));
register('fr', () => import('$shared/i18n/locales/fr.json'));
register('hi', () => import('$shared/i18n/locales/hi.json'));
register('ne', () => import('$shared/i18n/locales/ne.json'));
register('zh', () => import('$shared/i18n/locales/zh.json'));

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
