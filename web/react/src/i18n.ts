import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import { SUPPORTED_LOCALES } from '$lib/i18n/locales';

import en from '$lib/i18n/locales/en.json';
import mg from '$lib/i18n/locales/mg.json';
import ar from '$lib/i18n/locales/ar.json';
import es from '$lib/i18n/locales/es.json';
import fr from '$lib/i18n/locales/fr.json';
import hi from '$lib/i18n/locales/hi.json';
import ne from '$lib/i18n/locales/ne.json';
import zh from '$lib/i18n/locales/zh.json';

const resources = {
	en: { translation: en },
	mg: { translation: mg },
	ar: { translation: ar },
	es: { translation: es },
	fr: { translation: fr },
	hi: { translation: hi },
	ne: { translation: ne },
	zh: { translation: zh },
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
