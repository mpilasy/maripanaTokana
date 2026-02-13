import { createContext, useContext, useState, useCallback, useEffect, type ReactNode } from 'react';
import { fontPairings } from '$lib/fonts';
import { SUPPORTED_LOCALES } from '$lib/i18n/locales';
import { useTranslation } from 'react-i18next';

function loadPersisted<T>(key: string, defaultValue: T): T {
	try {
		const stored = localStorage.getItem(key);
		if (stored !== null) return JSON.parse(stored);
	} catch { /* ignore */ }
	return defaultValue;
}

interface PreferencesContextType {
	metricPrimary: boolean;
	fontIndex: number;
	localeIndex: number;
	toggleUnits: () => void;
	cycleFont: () => void;
	cycleLanguage: () => void;
}

const PreferencesContext = createContext<PreferencesContextType>(null!);

export function PreferencesProvider({ children }: { children: ReactNode }) {
	const { i18n } = useTranslation();
	const [metricPrimary, setMetricPrimary] = useState(() => loadPersisted('metric_primary', true));
	const [fontIndex, setFontIndex] = useState(() => loadPersisted('font_index', 0));
	const [localeIndex, setLocaleIndex] = useState(() => loadPersisted('locale_index', 0));

	// Persist to localStorage on change
	useEffect(() => { localStorage.setItem('metric_primary', JSON.stringify(metricPrimary)); }, [metricPrimary]);
	useEffect(() => { localStorage.setItem('font_index', JSON.stringify(fontIndex)); }, [fontIndex]);
	useEffect(() => {
		localStorage.setItem('locale_index', JSON.stringify(localeIndex));
		const loc = SUPPORTED_LOCALES[localeIndex];
		if (loc) {
			i18n.changeLanguage(loc.tag);
			document.documentElement.dir = loc.tag === 'ar' ? 'rtl' : 'ltr';
			document.documentElement.lang = loc.tag;
		}
	}, [localeIndex, i18n]);

	const toggleUnits = useCallback(() => setMetricPrimary(v => !v), []);
	const cycleFont = useCallback(() => setFontIndex(i => (i + 1) % fontPairings.length), []);
	const cycleLanguage = useCallback(() => setLocaleIndex(i => (i + 1) % SUPPORTED_LOCALES.length), []);

	return (
		<PreferencesContext.Provider value={{ metricPrimary, fontIndex, localeIndex, toggleUnits, cycleFont, cycleLanguage }}>
			{children}
		</PreferencesContext.Provider>
	);
}

export function usePreferences() {
	return useContext(PreferencesContext);
}
