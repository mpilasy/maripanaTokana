import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import { SUPPORTED_LOCALES } from '$lib/i18n/locales';

import enRaw from '$lib/i18n/locales/en.json';
import mgRaw from '$lib/i18n/locales/mg.json';
import arRaw from '$lib/i18n/locales/ar.json';
import esRaw from '$lib/i18n/locales/es.json';
import frRaw from '$lib/i18n/locales/fr.json';
import hiRaw from '$lib/i18n/locales/hi.json';
import neRaw from '$lib/i18n/locales/ne.json';
import zhRaw from '$lib/i18n/locales/zh.json';

/** Flatten canonical JSON: merge web_only into top-level, drop android_only. */
function flattenForWeb(raw: Record<string, any>): Record<string, any> {
	const { web_only, android_only, ...shared } = raw;
	return { ...shared, ...web_only };
}

const resources = {
	en: { translation: flattenForWeb(enRaw) },
	mg: { translation: flattenForWeb(mgRaw) },
	ar: { translation: flattenForWeb(arRaw) },
	es: { translation: flattenForWeb(esRaw) },
	fr: { translation: flattenForWeb(frRaw) },
	hi: { translation: flattenForWeb(hiRaw) },
	ne: { translation: flattenForWeb(neRaw) },
	zh: { translation: flattenForWeb(zhRaw) },
};

const savedIndex = (() => {
	try {
		return JSON.parse(localStorage.getItem('locale_index') ?? '0');
	} catch {
		return 0;
	}
})();

const initialLocale = SUPPORTED_LOCALES[savedIndex]?.tag ?? 'mg';

i18n.use(initReactI18next).init({
	resources,
	lng: initialLocale,
	fallbackLng: 'mg',
	interpolation: { escapeValue: false, prefix: '{', suffix: '}' },
	returnObjects: true,
});

export default i18n;

/** Load locale strings for a given tag (for dual-language error screen) */
export function getLocaleStrings(tag: string): Record<string, string> | null {
	const res = resources[tag as keyof typeof resources];
	return res ? (res.translation as unknown as Record<string, string>) : null;
}
