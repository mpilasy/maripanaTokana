// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

import { describe, it, expect } from 'vitest';
import { areaMatches, meteoAlarmAlert, meteoAlarmAreaMatches, type MeteoAlarmEntry } from './meteoAlarm';

const fr = (subdivisionName: string) => ({ countryCode: 'fr', stateCode: null, subdivisionName, regionName: null });

describe('areaMatches', () => {
	it('matches the same area with other case, accents or hyphens', () => {
		expect(areaMatches('Bouches-du-Rhône', 'bouches du rhone')).toBe(true);
	});

	it('matches an area name that is part of the other as whole words', () => {
		expect(areaMatches('Grad Zagreb', 'Zagreb')).toBe(true);
		expect(areaMatches('Attica', 'Αττική Attica')).toBe(true);
	});

	it('does not match a short name inside a longer word', () => {
		expect(areaMatches('Maine-et-Loire', 'Ain')).toBe(false);
		expect(areaMatches('Var', 'Vaucluse')).toBe(false);
	});

	it('matches when the names differ only in words for the type of area', () => {
		expect(areaMatches('Zagreb region', 'Grad Zagreb')).toBe(true);
		expect(areaMatches('Kreis Heilbronn', 'Landkreis Heilbronn')).toBe(true);
		expect(areaMatches('Città metropolitana di Torino', 'Provincia di Torino')).toBe(true);
	});

	it('does not match other areas that share one word', () => {
		expect(areaMatches('Haute-Garonne', 'Haute-Savoie')).toBe(false);
		expect(areaMatches('Haute-Garonne', 'Tarn-et-Garonne')).toBe(false);
		expect(areaMatches('Autonomous Community of Madrid', 'Autonomous Community of Valencia')).toBe(false);
		expect(areaMatches('North Yorkshire', 'South Yorkshire')).toBe(false);
		expect(areaMatches('Greater London', 'Greater Manchester')).toBe(false);
		expect(areaMatches('Kreis Heilbronn', 'Kreis Esslingen')).toBe(false);
	});

	it('matches names in non-Latin scripts as whole words', () => {
		expect(areaMatches('Περιφέρεια Αττικής', 'Αττικής')).toBe(true);
	});
});

describe('meteoAlarmAlert', () => {
	const entry: MeteoAlarmEntry = {
		event: 'Yellow wind warning',
		severity: 'Moderate',
		onset: '2026-10-29T06:00:00+01:00',
		status: 'Actual',
		description: 'Strong gusts',
		areaDesc: 'Bouches-du-Rhône',
		link: 'https://example.org/a'
	};

	it('maps an actual entry to an alert', () => {
		expect(meteoAlarmAlert(entry, null)).toEqual({
			level: 'watch',
			title: 'Yellow wind warning',
			description: 'Strong gusts',
			source: 'meteoalarm',
			time: Date.parse('2026-10-29T05:00:00Z'),
			headline: 'Bouches-du-Rhône',
			link: 'https://example.org/a'
		});
	});

	it('maps the severity in any case, as on Android', () => {
		expect(meteoAlarmAlert({ ...entry, severity: 'EXTREME' }, null)?.level).toBe('emergency');
		expect(meteoAlarmAlert({ ...entry, severity: 'severe' }, null)?.level).toBe('warning');
	});

	it('skips entries without an event or that are not actual', () => {
		expect(meteoAlarmAlert({ ...entry, event: ' ' }, null)).toBeNull();
		expect(meteoAlarmAlert({ ...entry, status: 'Test' }, null)).toBeNull();
		expect(meteoAlarmAlert({ ...entry, status: '' }, null)).not.toBeNull();
	});

	it('keeps only alerts for the subdivision when it is known', () => {
		expect(meteoAlarmAlert(entry, fr('Bouches du Rhone'))).not.toBeNull();
		expect(meteoAlarmAlert(entry, fr('Vaucluse'))).toBeNull();
		expect(meteoAlarmAlert({ ...entry, areaDesc: '' }, fr('Vaucluse'))).not.toBeNull();
		// An empty or blank subdivision is not known: keep the alert (the same on Android)
		expect(meteoAlarmAlert(entry, fr(''))).not.toBeNull();
		expect(meteoAlarmAlert(entry, fr(' '))).not.toBeNull();
	});

	it('gives no time for an onset it cannot read', () => {
		expect(meteoAlarmAlert({ ...entry, onset: 'soon' }, null)?.time).toBeUndefined();
	});
});

describe('meteoAlarmAreaMatches', () => {
	it('in France a department does not match a department that contains its name', () => {
		expect(meteoAlarmAreaMatches('fr', 'Haute-Loire', 'Haute-Loire', 'Auvergne-Rhône-Alpes')).toBe(true);
		expect(meteoAlarmAreaMatches('fr', "Côtes-d'Armor", "Cotes d'Armor", null)).toBe(true);
		expect(meteoAlarmAreaMatches('fr', 'Haute-Loire', 'Loire', 'Auvergne-Rhône-Alpes')).toBe(false);
		expect(meteoAlarmAreaMatches('fr', 'Loire', 'Haute-Loire', 'Auvergne-Rhône-Alpes')).toBe(false);
		expect(meteoAlarmAreaMatches('fr', 'Charente-Maritime', 'Charente', null)).toBe(false);
		expect(meteoAlarmAreaMatches('fr', 'Haute-Savoie', 'Savoie', null)).toBe(false);
	});

	it('in Italy the region of the place is used, also with its English name', () => {
		expect(meteoAlarmAreaMatches('it', 'Umbria', 'Province of Perugia', 'Umbria')).toBe(true);
		expect(meteoAlarmAreaMatches('it', 'Toscana', 'Metropolitan City of Florence', 'Tuscany')).toBe(true);
		expect(meteoAlarmAreaMatches('it', 'Emilia e Romagna', 'Bologna', 'Emilia-Romagna')).toBe(true);
		expect(meteoAlarmAreaMatches('it', 'Lombardia', 'Perugia', 'Umbria')).toBe(false);
		// Without a region, the subdivision is used
		expect(meteoAlarmAreaMatches('it', 'Umbria', 'Umbria', null)).toBe(true);
	});

	it('other countries keep the rule with containment', () => {
		expect(meteoAlarmAreaMatches('es', 'Litoral norte de Tarragona', 'Tarragona', 'Catalonia')).toBe(true);
		expect(meteoAlarmAreaMatches('de', 'Kreis Breisgau-Hochschwarzwald und Stadt Freiburg', 'Freiburg im Breisgau', null)).toBe(true);
	});

	it('keeps an alert when the area of the place or of the alert is not known', () => {
		expect(meteoAlarmAreaMatches('fr', 'Haute-Loire', null, null)).toBe(true);
		expect(meteoAlarmAreaMatches('fr', '', 'Loire', null)).toBe(true);
		expect(meteoAlarmAreaMatches('it', 'Umbria', ' ', ' ')).toBe(true);
	});
});
