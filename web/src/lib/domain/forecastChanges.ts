// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

/** One day of a saved forecast. Null values are missing in the forecast. */
export interface SnapshotDay {
	/** The place's date, "2026-11-07". */
	date: string;
	highC: number | null;
	lowC: number | null;
	rainMm: number | null;
	windMaxKmh: number | null;
}

/** A forecast saved for a place. The first day is the day of the fetch. */
export interface ForecastSnapshot {
	savedAt: number; // epoch millis
	days: SnapshotDay[];
}

/** The i18n key of a change is `change_${kind}`. */
export type ForecastChangeKind =
	| 'high_warmer' | 'high_cooler'
	| 'rain_now' | 'dry_now' | 'rain_more' | 'rain_less'
	| 'wind_stronger' | 'wind_weaker';

export interface ForecastChange {
	date: string;
	kind: ForecastChangeKind;
	/** high: the difference in °C (positive); rain: the new sum in mm; wind: the new maximum in km/h. */
	value: number;
}

const HIGH_DELTA_C = 3;
const RAIN_MM = 1;
const RAIN_DELTA_MM = 5;
const RAIN_DELTA_SHARE = 0.5;
const WIND_DELTA_KMH = 15;
/** The days compared: tomorrow (1) up to 6 days ahead. */
const FIRST_DAY = 1;
const LAST_DAY = 6;
export const MAX_CHANGES = 4;

const HOUR_MS = 60 * 60 * 1000;
/** A saved forecast is used only at this age; the one closest to 24 hours wins. */
const COMPARE_MIN_AGE_MS = 18 * HOUR_MS;
const COMPARE_MAX_AGE_MS = 36 * HOUR_MS;
const COMPARE_TARGET_AGE_MS = 24 * HOUR_MS;

function known(v: number | null | undefined): v is number {
	return v != null && Number.isFinite(v);
}

/**
 * What changed between an old and the current forecast, for the days from tomorrow up to 6 days ahead
 * that are in both: a high 3 °C warmer or cooler; rain that comes or goes (1 mm), or 5 mm and half
 * more or less; a maximum wind 15 km/h stronger or weaker. By date, then high, rain, wind; 4 at most.
 */
export function forecastChanges(old: ForecastSnapshot, current: ForecastSnapshot): ForecastChange[] {
	const changes: ForecastChange[] = [];
	for (const day of current.days.slice(FIRST_DAY, LAST_DAY + 1)) {
		const before = old.days.find((d) => d.date === day.date);
		if (!before) continue;
		if (known(before.highC) && known(day.highC)) {
			const delta = day.highC - before.highC;
			if (Math.abs(delta) >= HIGH_DELTA_C) changes.push({ date: day.date, kind: delta > 0 ? 'high_warmer' : 'high_cooler', value: Math.abs(delta) });
		}
		if (known(before.rainMm) && known(day.rainMm)) {
			const was = before.rainMm;
			const now = day.rainMm;
			if (was < RAIN_MM && now >= RAIN_MM) changes.push({ date: day.date, kind: 'rain_now', value: now });
			else if (was >= RAIN_MM && now < RAIN_MM) changes.push({ date: day.date, kind: 'dry_now', value: now });
			else if (was >= RAIN_MM && now >= RAIN_MM && Math.abs(now - was) >= RAIN_DELTA_MM && Math.abs(now - was) >= RAIN_DELTA_SHARE * was) {
				changes.push({ date: day.date, kind: now > was ? 'rain_more' : 'rain_less', value: now });
			}
		}
		if (known(before.windMaxKmh) && known(day.windMaxKmh)) {
			const delta = day.windMaxKmh - before.windMaxKmh;
			if (Math.abs(delta) >= WIND_DELTA_KMH) changes.push({ date: day.date, kind: delta > 0 ? 'wind_stronger' : 'wind_weaker', value: day.windMaxKmh });
		}
	}
	return changes.slice(0, MAX_CHANGES);
}

/** The saved forecast whose age is closest to 24 hours, only at 18 to 36 hours old; else null. */
export function snapshotToCompare(snapshots: ForecastSnapshot[], now: number): ForecastSnapshot | null {
	let best: ForecastSnapshot | null = null;
	for (const s of snapshots) {
		const age = now - s.savedAt;
		if (age < COMPARE_MIN_AGE_MS || age > COMPARE_MAX_AGE_MS) continue;
		if (!best || Math.abs(age - COMPARE_TARGET_AGE_MS) < Math.abs(now - best.savedAt - COMPARE_TARGET_AGE_MS)) best = s;
	}
	return best;
}

/** Whether a change goes up (warmer, rain, stronger), for the warm accent; else the cool accent. */
export function isUpChange(kind: ForecastChangeKind): boolean {
	return kind === 'high_warmer' || kind === 'rain_now' || kind === 'rain_more' || kind === 'wind_stronger';
}
