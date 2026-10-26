// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

import type { WeatherAlert, AlertLevel } from '$lib/domain/weatherData';

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

export function areaMatches(areaDesc: string, subdivision: string): boolean {
	const a = normalizeArea(areaDesc);
	const b = normalizeArea(subdivision);
	// Whole words only: a plain substring match let "Ain" match "Maine et Loire"
	const tokensA = areaWords(a);
	const tokensB = areaWords(b);
	if (a === b || containsWords(tokensA, tokensB) || containsWords(tokensB, tokensA)) return true;
	// Word intersection: handles cases like "Grad Zagreb" vs "Zagreb region"
	const wordsA = tokensA.filter(w => w.length >= 4);
	const wordsB = tokensB.filter(w => w.length >= 4);
	return wordsA.some(w => wordsB.includes(w));
}

export async function fetchMeteoAlarmAlerts(lat: number, lon: number, countryCode: string, subdivisionName: string | null = null): Promise<WeatherAlert[]> {
	if (!METEOALARM_COUNTRIES.has(countryCode)) return [];
	const res = await fetch(`/api/alerts/meteoalarm?country=${countryCode}`, { signal: AbortSignal.timeout(10_000) });
	if (!res.ok) throw new Error(`HTTP ${res.status}`);
	const text = await res.text();
	const parser = new DOMParser();
	const doc = parser.parseFromString(text, 'application/xml');
	const CAP = 'urn:oasis:names:tc:emergency:cap:1.2';
	const alerts: WeatherAlert[] = [];
	for (const entry of Array.from(doc.getElementsByTagName('entry'))) {
		const g = (tag: string) => entry.getElementsByTagNameNS(CAP, tag)[0]?.textContent ?? '';
		const status = g('status');
		if (status && status !== 'Actual') continue;
		const event = g('event') || 'Alert';
		const severity = g('severity');
		const desc = g('description');
		const areaDesc = g('areaDesc');
		const onset = g('onset');
		const linkEl = entry.querySelector('link[rel="alternate"]');
		const link = linkEl?.getAttribute('href') || undefined;
		// Filter to user's subdivision when known; skip alerts for other areas
		if (subdivisionName && areaDesc && !areaMatches(areaDesc, subdivisionName)) continue;
		// Android mapping: extreme→emergency, severe→warning, else→watch
		const level: AlertLevel = severity === 'Extreme' ? 'emergency'
			: severity === 'Severe' ? 'warning' : 'watch';
		alerts.push({
			level,
			title: event,
			description: desc,
			source: 'meteoalarm',
			time: onset ? new Date(onset).getTime() : undefined,
			headline: areaDesc || undefined,
			link,
		});
	}
	return alerts;
}
