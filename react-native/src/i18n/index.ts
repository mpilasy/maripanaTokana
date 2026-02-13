import en from './locales/en.json';
import ar from './locales/ar.json';
import es from './locales/es.json';
import fr from './locales/fr.json';
import hi from './locales/hi.json';
import mg from './locales/mg.json';
import ne from './locales/ne.json';
import zh from './locales/zh.json';

export interface SupportedLocale {
  tag: string;
  flag: string;
  /** Unicode code point of native zero digit, or null for ASCII digits. */
  nativeZero: number | null;
  /** Decimal separator character for this locale. */
  decimalSep: string;
  /** Whether this locale uses RTL layout. */
  rtl: boolean;
}

export const supportedLocales: SupportedLocale[] = [
  {tag: 'mg', flag: '\uD83C\uDDF2\uD83C\uDDEC', nativeZero: null, decimalSep: ',', rtl: false},
  {tag: 'ar', flag: '\uD83C\uDDF8\uD83C\uDDE6', nativeZero: 0x0660, decimalSep: '\u066B', rtl: true},
  {tag: 'en', flag: '\uD83C\uDDEC\uD83C\uDDE7', nativeZero: null, decimalSep: '.', rtl: false},
  {tag: 'es', flag: '\uD83C\uDDEA\uD83C\uDDF8', nativeZero: null, decimalSep: ',', rtl: false},
  {tag: 'fr', flag: '\uD83C\uDDEB\uD83C\uDDF7', nativeZero: null, decimalSep: ',', rtl: false},
  {tag: 'hi', flag: '\uD83C\uDDEE\uD83C\uDDF3', nativeZero: 0x0966, decimalSep: '.', rtl: false},
  {tag: 'ne', flag: '\uD83C\uDDF3\uD83C\uDDF5', nativeZero: 0x0966, decimalSep: '.', rtl: false},
  {tag: 'zh', flag: '\uD83C\uDDE8\uD83C\uDDF3', nativeZero: null, decimalSep: '.', rtl: false},
];

type Strings = Record<string, string | string[]>;

const localeMap: Record<string, Strings> = {
  mg, ar, en, es, fr, hi, ne, zh,
};

/** Replace ASCII digits 0-9 with native script digits and '.' with locale decimal separator. */
export function localizeDigits(s: string, locale: SupportedLocale): string {
  const {nativeZero, decimalSep} = locale;
  if (nativeZero == null && decimalSep === '.') return s;
  let result = '';
  for (const c of s) {
    const code = c.charCodeAt(0);
    if (nativeZero != null && code >= 48 && code <= 57) {
      result += String.fromCharCode(nativeZero + (code - 48));
    } else if (c === '.' && decimalSep !== '.') {
      result += decimalSep;
    } else {
      result += c;
    }
  }
  return result;
}

/** Get a translated string for the given key and locale tag. */
export function t(key: string, localeTag: string, params?: Record<string, string>): string {
  const strings = localeMap[localeTag] || localeMap.en;
  let value = (strings[key] as string) ?? (localeMap.en[key] as string) ?? key;
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
  return (strings[key] as string[]) ?? (localeMap.en[key] as string[]) ?? [];
}
