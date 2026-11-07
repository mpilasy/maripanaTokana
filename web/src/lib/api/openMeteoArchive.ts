// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

import { dailyNormals, type DayNormal } from '../domain/normals';
import { FETCH_TIMEOUT_MS, HttpError, netFetch } from './http';

const BASE_URL = 'https://archive-api.open-meteo.com/v1/archive';
/** Years of ERA5 data that go into the normals. */
const NORMAL_YEARS = 10;
/** The normals of a place are kept this long before they are asked for again. */
const NORMALS_TTL_MS = 30 * 24 * 60 * 60 * 1000;
const NORMALS_STORAGE_PREFIX = 'normals_';

interface ArchiveResponse {
	daily?: {
		time: string[];
		temperature_2m_max: (number | null)[];
		temperature_2m_min: (number | null)[];
	};
}

interface NormalsEntry {
	at: number;
	normals: (DayNormal | null)[];
}

const normalsMemory = new Map<string, NormalsEntry>();
const normalsInflight = new Map<string, Promise<(DayNormal | null)[] | null>>();

function normalsKey(lat: number, lon: number): string {
	return `${lat.toFixed(2)},${lon.toFixed(2)}`;
}

/** The archive request: Jan 1 ten years ago to Dec 31 last year, daily highs and lows. */
export function archiveParams(lat: number, lon: number, now = new Date()): URLSearchParams {
	const year = now.getUTCFullYear();
	return new URLSearchParams({
		latitude: lat.toString(),
		longitude: lon.toString(),
		start_date: `${year - NORMAL_YEARS}-01-01`,
		end_date: `${year - 1}-12-31`,
		daily: 'temperature_2m_max,temperature_2m_min',
		timezone: 'auto',
	});
}

/** The normals of a place when they are kept and not older than 30 days; else null. */
export function cachedNormals(lat: number, lon: number): (DayNormal | null)[] | null {
	const key = normalsKey(lat, lon);
	const now = Date.now();
	const mem = normalsMemory.get(key);
	if (mem && now - mem.at < NORMALS_TTL_MS) return mem.normals;
	try {
		const raw = localStorage.getItem(NORMALS_STORAGE_PREFIX + key);
		if (!raw) return null;
		const entry = JSON.parse(raw) as NormalsEntry;
		if (now - entry.at >= NORMALS_TTL_MS || !Array.isArray(entry.normals)) return null;
		normalsMemory.set(key, entry);
		return entry.normals;
	} catch {
		return null;
	}
}

function keepNormals(key: string, normals: (DayNormal | null)[]) {
	// One decimal is enough and keeps the stored text small
	const rounded = normals.map((n) => (n ? { max: Math.round(n.max * 10) / 10, min: Math.round(n.min * 10) / 10 } : null));
	const entry = { at: Date.now(), normals: rounded };
	normalsMemory.set(key, entry);
	try { localStorage.setItem(NORMALS_STORAGE_PREFIX + key, JSON.stringify(entry)); } catch { /* ignore */ }
}

/**
 * The normal high and low per day of the year of a place (see dailyNormals), from 10 years of ERA5 data.
 * Kept 30 days (memory and localStorage) by the place rounded to 2 decimals. Null when the request fails (not kept).
 */
export function fetchNormals(lat: number, lon: number): Promise<(DayNormal | null)[] | null> {
	const hit = cachedNormals(lat, lon);
	if (hit) return Promise.resolve(hit);
	const key = normalsKey(lat, lon);
	const pending = normalsInflight.get(key);
	if (pending) return pending;
	const p = (async () => {
		try {
			const res = await netFetch(`${BASE_URL}?${archiveParams(lat, lon)}`, { signal: AbortSignal.timeout(FETCH_TIMEOUT_MS) });
			if (!res.ok) throw new HttpError(res.status, `Open-Meteo archive error: ${res.status}`);
			const body: ArchiveResponse = await res.json();
			const d = body.daily;
			if (!d?.time?.length) return null;
			const normals = dailyNormals(d.time, d.temperature_2m_max ?? [], d.temperature_2m_min ?? []);
			if (normals.every((n) => n == null)) return null;
			keepNormals(key, normals);
			return normalsMemory.get(key)!.normals;
		} catch {
			return null;
		} finally {
			normalsInflight.delete(key);
		}
	})();
	normalsInflight.set(key, p);
	return p;
}
