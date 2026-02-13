import { Injectable, signal, computed, effect } from '@angular/core';
import { SUPPORTED_LOCALES, localizeDigits, type SupportedLocale } from '$lib/i18n/locales';

import en from '$lib/i18n/locales/en.json';
import mg from '$lib/i18n/locales/mg.json';
import ar from '$lib/i18n/locales/ar.json';
import es from '$lib/i18n/locales/es.json';
import fr from '$lib/i18n/locales/fr.json';
import hi from '$lib/i18n/locales/hi.json';
import ne from '$lib/i18n/locales/ne.json';
import zh from '$lib/i18n/locales/zh.json';

const allTranslations: Record<string, Record<string, any>> = { en, mg, ar, es, fr, hi, ne, zh };

@Injectable({ providedIn: 'root' })
export class I18nService {
	private _localeTag = signal('mg');
	private _translations = signal<Record<string, any>>(mg);

	readonly localeTag = this._localeTag.asReadonly();

	setLocale(tag: string) {
		this._localeTag.set(tag);
		this._translations.set(allTranslations[tag] ?? mg);
	}

	t(key: string, params?: Record<string, string>): string {
		let val = this._translations()[key];
		if (val == null) return key;
		if (typeof val === 'string' && params) {
			for (const [k, v] of Object.entries(params)) {
				val = val.replace(`{${k}}`, v);
			}
		}
		return typeof val === 'string' ? val : key;
	}

	tArray(key: string): string[] {
		const val = this._translations()[key];
		return Array.isArray(val) ? val : [];
	}

	loc(s: string, locale: SupportedLocale): string {
		return localizeDigits(s, locale);
	}

	getLocaleStrings(tag: string): Record<string, any> | null {
		return allTranslations[tag] ?? null;
	}
}
