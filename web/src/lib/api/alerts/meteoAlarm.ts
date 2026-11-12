// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

import type { WeatherAlert, AlertLevel } from '#lib/domain/weatherData.js';
import type { LocationInfo } from './shared';

export const METEOALARM_COUNTRIES = new Set([
	'at','ba','be','bg','hr','cy','cz','dk','ee','fi','fr','de','gr','hu','ie','it',
	'lv','lt','lu','mt','md','me','nl','mk','no','pl','pt','ro','rs','sk','si',
	'es','se','ch','ua','gb'
]);

function normalizeArea(s: string): string {
	return s.toLowerCase()
		.normalize('NFD')
		.replace(/[̀-ͯ]/g, '')
		.replace(/-/g, ' ')
		.trim();
}

function areaWords(s: string): string[] {
	return s.split(/[^\p{L}\p{N}]+/u).filter(w => w.length > 0);
}

/** True when `needle` occurs in `hay` as a run of whole words. */
function containsWords(hay: string[], needle: string[]): boolean {
	if (needle.length === 0) return false;
	for (let i = 0; i + needle.length <= hay.length; i++) {
		if (needle.every((w, j) => hay[i + j] === w)) return true;
	}
	return false;
}

const STRICT_AREA_COUNTRIES = new Set(['fr']);
const REGION_AREA_COUNTRIES = new Set(['it']);
// English region names (lowercase, without accents) with the Italian names of the MeteoAlarm feed
const ITALIAN_REGIONS: Record<string, string> = {
	'lombardy': 'Lombardia', 'piedmont': 'Piemonte', 'tuscany': 'Toscana', 'apulia': 'Puglia',
	'sicily': 'Sicilia', 'sardinia': 'Sardegna', 'trentino south tyrol': 'Trentino Alto Adige',
	'aosta valley': "Valle d'Aosta"
};

/**
 * True when the MeteoAlarm `areaDesc` covers the place, or when the area of the place is not known.
 * `subdivision` is the department or county of the place, `region` its region or state.
 */
export function meteoAlarmAreaMatches(countryCode: string, areaDesc: string, subdivision: string | null, region: string | null): boolean {
	if (!areaDesc.trim()) return true;
	// Italy sends warnings for regions, with their Italian names; the geocoder can give English names
	const regionName = REGION_AREA_COUNTRIES.has(countryCode) && region?.trim()
		? ITALIAN_REGIONS[normalizeArea(region)] ?? region
		: null;
	const name = regionName ?? (subdivision?.trim() ? subdivision : null);
	if (!name) return true;
	// In France a department name can be part of another: "Loire" is not "Haute-Loire"
	return STRICT_AREA_COUNTRIES.has(countryCode) ? sameArea(areaDesc, name) : areaMatches(areaDesc, name);
}

/** True when `areaDesc` and `name` have the same words, without the words for the type of area. */
export function sameArea(areaDesc: string, name: string): boolean {
	const words = nameWords(areaWords(normalizeArea(areaDesc)));
	const other = nameWords(areaWords(normalizeArea(name)));
	return words.size > 0 && words.size === other.size && [...words].every((w) => other.has(w));
}

/** True when the MeteoAlarm `areaDesc` is the place's `subdivision`. */
export function areaMatches(areaDesc: string, subdivision: string): boolean {
	const a = normalizeArea(areaDesc);
	const b = normalizeArea(subdivision);
	// Whole words only: a plain substring match let "Ain" match "Maine et Loire"
	const tokensA = areaWords(a);
	const tokensB = areaWords(b);
	if (a === b || containsWords(tokensA, tokensB) || containsWords(tokensB, tokensA)) return true;
	// Without the words for the type of area, all the words of one name are in the other:
	// "Grad Zagreb" is "Zagreb region", but "Haute-Garonne" is not "Haute-Savoie" or "Tarn-et-Garonne"
	const wordsA = nameWords(tokensA);
	const wordsB = nameWords(tokensB);
	const containsAll = (hay: Set<string>, needle: Set<string>) => [...needle].every((w) => hay.has(w));
	return wordsA.size > 0 && wordsB.size > 0 && (containsAll(wordsB, wordsA) || containsAll(wordsA, wordsB));
}

// Words for the type of area and joining words, lowercase without accents: they do not name a place
const AREA_TYPE_WORDS = new Set([
	'region', 'regione', 'regiao', 'regionen', 'county', 'district', 'province', 'provincia', 'provincie',
	'provinz', 'departement', 'community', 'comunidad', 'comunitat', 'autonomous', 'autonoma', 'city',
	'citta', 'ciudad', 'metropolitan', 'metropolitana', 'municipality', 'kreis', 'landkreis', 'stadt',
	'bezirk', 'grad', 'zupanija', 'kommune', 'kommun', 'maakunta', 'wojewodztwo', 'powiat', 'okres',
	'kraj', 'megye', 'judetul', 'oblast', 'periferia', 'περιφερεια',
	'and', 'the', 'und', 'der', 'die', 'das', 'des', 'del', 'les', 'los', 'las', 'von', 'van', 'sur'
]);

function nameWords(tokens: string[]): Set<string> {
	return new Set(tokens.filter((w) => w.length >= 3 && !AREA_TYPE_WORDS.has(w)));
}

export async function fetchMeteoAlarmAlerts(lat: number, lon: number, countryCode: string, place: LocationInfo | null = null): Promise<WeatherAlert[]> {
	if (!METEOALARM_COUNTRIES.has(countryCode)) return [];
	const res = await fetch(`/api/alerts/meteoalarm?country=${countryCode}`, { signal: AbortSignal.timeout(10_000) });
	if (!res.ok) throw new Error(`HTTP ${res.status}`);
	return readMeteoAlarmEntries(await res.text())
		.map((e) => meteoAlarmAlert(e, place))
		.filter((a): a is WeatherAlert => a !== null);
}

/** The CAP fields of one Atom entry in a MeteoAlarm feed. */
export interface MeteoAlarmEntry {
	event: string;
	severity: string;
	onset: string;
	status: string;
	description: string;
	areaDesc: string;
	link?: string;
}

const CAP_NS = 'urn:oasis:names:tc:emergency:cap:1.2';

function readMeteoAlarmEntries(xml: string): MeteoAlarmEntry[] {
	const doc = new DOMParser().parseFromString(xml, 'application/xml');
	return Array.from(doc.getElementsByTagName('entry')).map((entry) => {
		const g = (tag: string) => entry.getElementsByTagNameNS(CAP_NS, tag)[0]?.textContent ?? '';
		return {
			event: g('event'),
			severity: g('severity'),
			onset: g('onset'),
			status: g('status'),
			description: g('description'),
			areaDesc: g('areaDesc'),
			link: entry.querySelector('link[rel="alternate"]')?.getAttribute('href') || undefined
		};
	});
}

/**
 * The alert for `entry`, or null when it is not an actual alert or is for an area other than
 * `subdivisionName`. The same rules as meteoAlarmAlert in WeatherRepositoryImpl.kt.
 */
/** The alert for `entry`, or null when it is not an actual alert or is for an area other than that of `place`. */
export function meteoAlarmAlert(entry: MeteoAlarmEntry, place: LocationInfo | null): WeatherAlert | null {
	if (!entry.event.trim() || (entry.status.trim() && entry.status !== 'Actual')) return null;
	if (place && !meteoAlarmAreaMatches(place.countryCode ?? '', entry.areaDesc, place.subdivisionName, place.regionName)) return null;
	const severity = entry.severity.toLowerCase();
	const level: AlertLevel = severity === 'extreme' ? 'emergency' : severity === 'severe' ? 'warning' : 'watch';
	const time = entry.onset ? new Date(entry.onset).getTime() : NaN;
	return {
		level,
		title: entry.event,
		description: entry.description,
		source: 'meteoalarm',
		time: Number.isNaN(time) ? undefined : time,
		headline: entry.areaDesc.trim() ? entry.areaDesc : undefined,
		link: entry.link
	};
}
