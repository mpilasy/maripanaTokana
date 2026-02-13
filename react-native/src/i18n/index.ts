/**
 * i18n module for the React Native app.
 *
 * Re-exports the shared locale definitions and localizeDigits from the project's
 * single source of truth at shared/i18n/, then adds React-Native-specific helpers
 * (t, tArray) for looking up translated strings.
 */
import {
  SUPPORTED_LOCALES,
  localizeDigits as sharedLocalizeDigits,
} from '../../../shared/i18n/locales';
import type {SupportedLocale as SharedSupportedLocale} from '../../../shared/i18n/locales';

// Import canonical JSON locale files from the shared directory
import en from '../../../shared/i18n/locales/en.json';
import ar from '../../../shared/i18n/locales/ar.json';
import es from '../../../shared/i18n/locales/es.json';
import fr from '../../../shared/i18n/locales/fr.json';
import hi from '../../../shared/i18n/locales/hi.json';
import mg from '../../../shared/i18n/locales/mg.json';
import ne from '../../../shared/i18n/locales/ne.json';
import zh from '../../../shared/i18n/locales/zh.json';

// Re-export the shared types and locale list
export type {SharedSupportedLocale as SupportedLocale};
export const supportedLocales = SUPPORTED_LOCALES;

// Re-export localizeDigits directly from shared
export const localizeDigits = sharedLocalizeDigits;

// Flatten each JSON: merge top-level keys + android_only keys (skip web_only)
function flattenStrings(
  json: Record<string, unknown>,
): Record<string, string | string[]> {
  const result: Record<string, string | string[]> = {};
  for (const [key, value] of Object.entries(json)) {
    if (key === 'web_only') continue;
    if (key === 'android_only' && typeof value === 'object' && value !== null) {
      // Merge android_only keys into top level (these are shared with RN)
      for (const [k, v] of Object.entries(value as Record<string, unknown>)) {
        result[k] = v as string | string[];
      }
    } else {
      result[key] = value as string | string[];
    }
  }
  return result;
}

type Strings = Record<string, string | string[]>;

const localeMap: Record<string, Strings> = {
  en: flattenStrings(en),
  ar: flattenStrings(ar),
  es: flattenStrings(es),
  fr: flattenStrings(fr),
  hi: flattenStrings(hi),
  mg: flattenStrings(mg),
  ne: flattenStrings(ne),
  zh: flattenStrings(zh),
};

/** Get a translated string for the given key and locale tag. */
export function t(
  key: string,
  localeTag: string,
  params?: Record<string, string>,
): string {
  const strings = localeMap[localeTag] || localeMap.en;
  let value =
    (strings[key] as string) ?? (localeMap.en[key] as string) ?? key;
  if (params) {
    for (const [k, v] of Object.entries(params)) {
      value = value.replace(`{${k}}`, v);
    }
  }
  return value;
}

/** Get a translated string array for the given key and locale tag. */
export function tArray(key: string, localeTag: string): string[] {
  const strings = localeMap[localeTag] || localeMap.en;
  return (
    (strings[key] as string[]) ?? (localeMap.en[key] as string[]) ?? []
  );
}
