import { Injectable, signal, effect, inject } from '@angular/core';
import { fontPairings } from '$lib/fonts';
import { SUPPORTED_LOCALES } from '$lib/i18n/locales';
import { I18nService } from './i18n.service';

function loadPersisted<T>(key: string, defaultValue: T): T {
	try {
		const stored = localStorage.getItem(key);
		if (stored !== null) return JSON.parse(stored);
	} catch { /* ignore */ }
	return defaultValue;
}

@Injectable({ providedIn: 'root' })
export class PreferencesService {
	private i18n = inject(I18nService);

	readonly metricPrimary = signal(loadPersisted<boolean>('metric_primary', true));
	readonly fontIndex = signal(loadPersisted<number>('font_index', 0));
	readonly localeIndex = signal(loadPersisted<number>('locale_index', 0));

	constructor() {
		// Set initial locale
		const loc = SUPPORTED_LOCALES[this.localeIndex()];
		if (loc) this.i18n.setLocale(loc.tag);

		effect(() => localStorage.setItem('metric_primary', JSON.stringify(this.metricPrimary())));
		effect(() => localStorage.setItem('font_index', JSON.stringify(this.fontIndex())));
		effect(() => {
			const idx = this.localeIndex();
			localStorage.setItem('locale_index', JSON.stringify(idx));
			const loc = SUPPORTED_LOCALES[idx];
			if (loc) {
				this.i18n.setLocale(loc.tag);
				document.documentElement.dir = loc.tag === 'ar' ? 'rtl' : 'ltr';
				document.documentElement.lang = loc.tag;
			}
		});
	}

	toggleUnits() { this.metricPrimary.update(v => !v); }
	cycleFont() { this.fontIndex.update(i => (i + 1) % fontPairings.length); }
	cycleLanguage() { this.localeIndex.update(i => (i + 1) % SUPPORTED_LOCALES.length); }
}
