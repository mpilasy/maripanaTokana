import { Injectable, signal, computed, effect } from '@angular/core';
import { SUPPORTED_LOCALES, localizeDigits, type SupportedLocale } from '$lib/i18n/locales';

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

const en = flattenForWeb(enRaw);
const mg = flattenForWeb(mgRaw);
const allTranslations: Record<string, Record<string, any>> = {
	en, mg,
	ar: flattenForWeb(arRaw), es: flattenForWeb(esRaw),
	fr: flattenForWeb(frRaw), hi: flattenForWeb(hiRaw),
	ne: flattenForWeb(neRaw), zh: flattenForWeb(zhRaw),
};

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
