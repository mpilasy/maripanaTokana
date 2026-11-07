// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

import type { DailyForecast } from '#lib/domain/weatherData.js';
import { placeIsoDate } from '#lib/domain/normals.js';
import { forecastChanges, snapshotToCompare, type ForecastChange, type ForecastSnapshot } from '#lib/domain/forecastChanges.js';

const STORAGE_KEY = 'forecast_history_v1';
const HOUR_MS = 60 * 60 * 1000;
/** At most one saved forecast per place in this time. */
const MIN_INTERVAL_MS = 3 * HOUR_MS;
/** Saved forecasts older than this are removed. */
const KEEP_MS = 5 * 24 * HOUR_MS;
/** Places kept; the place not saved for the longest time goes first. */
const MAX_PLACES = 10;

type History = Record<string, ForecastSnapshot[]>;

/** The place (rounded to 2 decimals), the source and the model: a change of one never compares two different forecasts. */
export function forecastHistoryKey(lat: number, lon: number, source: string, model: string): string {
	return `${lat.toFixed(2)},${lon.toFixed(2)},${source},${model}`;
}

function known(v: number): number | null {
	return Number.isFinite(v) ? v : null;
}

/** The days of a forecast as they are saved. */
export function toSnapshot(days: DailyForecast[], utcOffsetSeconds: number, savedAt: number): ForecastSnapshot {
	return {
		savedAt,
		days: days.map((d) => ({
			date: placeIsoDate(d.date, utcOffsetSeconds),
			highC: known(d.tempMax.celsius),
			lowC: known(d.tempMin.celsius),
			rainMm: known(d.precipitation.mm),
			windMaxKmh: known(d.windSpeed.metersPerSecond * 3.6),
		})),
	};
}

function readHistory(): History {
	try {
		const raw = localStorage.getItem(STORAGE_KEY);
		const parsed = raw ? JSON.parse(raw) : null;
		return parsed && typeof parsed === 'object' ? parsed : {};
	} catch {
		return {};
	}
}

/**
 * Saves the forecast of a place on the device, at most once per 3 hours. Keeps 5 days of
 * forecasts per place and 10 places.
 */
export function recordForecast(key: string, snapshot: ForecastSnapshot): void {
	const history = readHistory();
	const now = snapshot.savedAt;
	const kept = Array.isArray(history[key]) ? history[key] : [];
	const last = kept[kept.length - 1];
	if (!last || now - last.savedAt >= MIN_INTERVAL_MS) history[key] = [...kept, snapshot];
	for (const k of Object.keys(history)) {
		history[k] = Array.isArray(history[k]) ? history[k].filter((s) => now - s.savedAt < KEEP_MS) : [];
		if (history[k].length === 0) delete history[k];
	}
	const places = Object.keys(history);
	if (places.length > MAX_PLACES) {
		const latest = (k: string) => history[k][history[k].length - 1].savedAt;
		places.sort((a, b) => latest(a) - latest(b)).slice(0, places.length - MAX_PLACES).forEach((k) => delete history[k]);
	}
	try { localStorage.setItem(STORAGE_KEY, JSON.stringify(history)); } catch { /* storage unavailable or full */ }
}

/** What changed since the forecast saved about 24 hours ago (18 to 36 hours); empty when there is none. */
export function changesSinceYesterday(key: string, current: ForecastSnapshot): ForecastChange[] {
	const saved = readHistory()[key];
	const old = snapshotToCompare(Array.isArray(saved) ? saved : [], current.savedAt);
	return old ? forecastChanges(old, current) : [];
}
