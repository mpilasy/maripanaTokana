// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

import { beforeEach, describe, expect, it } from 'vitest';
import { changesSinceYesterday, forecastHistoryKey, recordForecast } from './forecastHistory';
import type { ForecastSnapshot } from '#lib/domain/forecastChanges.js';

const store = new Map<string, string>();
(globalThis as any).localStorage = {
	getItem: (k: string) => store.get(k) ?? null,
	setItem: (k: string, v: string) => void store.set(k, v),
	removeItem: (k: string) => void store.delete(k),
};

const HOUR = 3_600_000;
const KEY = forecastHistoryKey(-18.1534, 49.4021, 'OPEN_METEO', 'auto');

function snapshot(savedAt: number, high: number): ForecastSnapshot {
	return {
		savedAt,
		days: ['2026-11-07', '2026-11-08'].map((date) => ({ date, highC: high, lowC: 10, rainMm: 0, windMaxKmh: 10 })),
	};
}

function saved(key = KEY): ForecastSnapshot[] {
	return JSON.parse(store.get('forecast_history_v1') ?? '{}')[key] ?? [];
}

describe('forecastHistory', () => {
	beforeEach(() => store.clear());

	it('keys by the rounded place, the source and the model', () => {
		expect(KEY).toBe('-18.15,49.40,OPEN_METEO,auto');
	});

	it('saves at most one forecast per 3 hours and keeps 5 days', () => {
		const t = 1000 * HOUR;
		recordForecast(KEY, snapshot(t, 20));
		recordForecast(KEY, snapshot(t + 2 * HOUR, 21));
		expect(saved().map((s) => s.savedAt)).toEqual([t]);
		recordForecast(KEY, snapshot(t + 3 * HOUR, 22));
		expect(saved().map((s) => s.savedAt)).toEqual([t, t + 3 * HOUR]);
		recordForecast(KEY, snapshot(t + 5 * 24 * HOUR + HOUR, 23));
		// The first one is now older than 5 days
		expect(saved().map((s) => s.savedAt)).toEqual([t + 3 * HOUR, t + 5 * 24 * HOUR + HOUR]);
	});

	it('keeps 10 places', () => {
		for (let i = 0; i < 12; i++) recordForecast(forecastHistoryKey(i, 0, 'OPEN_METEO', 'auto'), snapshot(1000 * HOUR + i, 20));
		const history = JSON.parse(store.get('forecast_history_v1')!);
		expect(Object.keys(history)).toHaveLength(10);
		expect(history[forecastHistoryKey(0, 0, 'OPEN_METEO', 'auto')]).toBeUndefined();
	});

	it('compares with the forecast saved about a day before, for the same key only', () => {
		const t = 1000 * HOUR;
		recordForecast(KEY, snapshot(t, 20));
		expect(changesSinceYesterday(KEY, snapshot(t + 24 * HOUR, 25))).toEqual([{ date: '2026-11-08', kind: 'high_warmer', value: 5 }]);
		expect(changesSinceYesterday(KEY, snapshot(t + 12 * HOUR, 25))).toEqual([]);
		expect(changesSinceYesterday(forecastHistoryKey(-18.15, 49.4, 'OPEN_METEO', 'ecmwf_ifs025'), snapshot(t + 24 * HOUR, 25))).toEqual([]);
	});
});
