// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

import { describe, it, expect, vi, afterEach } from 'vitest';
import { fetchNhcAlerts, nhcLevel, nhcStats } from './nhc';
import { fetchGdacsAlerts } from './gdacs';

describe('nhcLevel', () => {
	it('makes a major hurricane an emergency', () => {
		expect(nhcLevel('HU', 96)).toBe('emergency');
		expect(nhcLevel('HU', 95)).toBe('warning');
	});

	it('makes a tropical storm a warning and other storms a watch', () => {
		expect(nhcLevel('TS', 50)).toBe('warning');
		expect(nhcLevel('TD', 30)).toBe('watch');
	});
});

describe('nhcStats', () => {
	it('joins the winds, pressure and movement in one sentence', () => {
		expect(nhcStats({ pressure: '980', movementSpeed: 12, movementDir: 315 }, 65)).toBe(
			'Winds ~75 mph, central pressure 980 mb, moving NW at 12 mph.'
		);
	});

	it('says stationary when the storm does not move', () => {
		expect(nhcStats({ movementSpeed: 0 }, 0)).toBe('Stationary.');
	});

	it('is empty when nothing is known', () => {
		expect(nhcStats({}, 0)).toBe('');
	});
});

// Twin: `GDACS and NHC titles have no source prefix` in AlertSourceFailureTest.kt
describe('alert titles', () => {
	afterEach(() => vi.unstubAllGlobals());

	function serve(body: unknown) {
		vi.stubGlobal('fetch', vi.fn(async () => new Response(JSON.stringify(body))));
	}

	it('gives a GDACS title with no source prefix', async () => {
		serve({ features: [{ properties: { name: 'Freddy', eventtype: 'TC', alertlevel: 'orange', description: 'Tropical cyclone', severity: '' }, geometry: { coordinates: [47.6, -18.9] } }] });
		const alerts = await fetchGdacsAlerts(-18.88, 47.51);
		expect(alerts.map((a) => a.title)).toEqual(['TC - Freddy']);
	});

	it('gives an NHC storm of an unknown type its name only', async () => {
		serve({ activeStorms: [{ name: 'Alpha', classification: 'XX', latitudeNumeric: -18.9, longitudeNumeric: 47.6 }] });
		const alerts = await fetchNhcAlerts(-18.88, 47.51);
		expect(alerts.map((a) => a.title)).toEqual(['Alpha']);
	});
});
