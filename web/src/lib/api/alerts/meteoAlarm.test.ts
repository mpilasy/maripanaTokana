// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

import { describe, it, expect } from 'vitest';
import { areaMatches, meteoAlarmAlert, type MeteoAlarmEntry } from './meteoAlarm';

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

	it('matches on a shared word of 4 or more letters', () => {
		expect(areaMatches('Zagreb region', 'Grad Zagreb')).toBe(true);
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
		expect(meteoAlarmAlert(entry, 'Bouches du Rhone')).not.toBeNull();
		expect(meteoAlarmAlert(entry, 'Vaucluse')).toBeNull();
		expect(meteoAlarmAlert({ ...entry, areaDesc: '' }, 'Vaucluse')).not.toBeNull();
		// An empty or blank subdivision is not known: keep the alert (the same on Android)
		expect(meteoAlarmAlert(entry, '')).not.toBeNull();
		expect(meteoAlarmAlert(entry, ' ')).not.toBeNull();
	});

	it('gives no time for an onset it cannot read', () => {
		expect(meteoAlarmAlert({ ...entry, onset: 'soon' }, null)?.time).toBeUndefined();
	});
});
