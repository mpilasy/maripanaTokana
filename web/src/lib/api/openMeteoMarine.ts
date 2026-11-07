// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

import type { SeaConditions } from '../domain/sea';
import { FETCH_TIMEOUT_MS, HttpError, netFetch } from './http';
import { parseIsoDateTime } from './openMeteoMapper';

const BASE_URL = 'https://marine-api.open-meteo.com/v1/marine';

export interface OpenMeteoMarineResponse {
	utc_offset_seconds: number;
	current?: {
		wave_height?: number | null;
		wave_period?: number | null;
		wave_direction?: number | null;
		swell_wave_height?: number | null;
		swell_wave_period?: number | null;
		swell_wave_direction?: number | null;
		sea_surface_temperature?: number | null;
		sea_level_height_msl?: number | null;
	};
	hourly?: {
		time: string[];
		sea_level_height_msl?: (number | null)[];
	};
}

/** The sea now and the sea level for two days. The marine API sends CORS headers, so the browser calls it directly. */
export async function fetchMarine(lat: number, lon: number): Promise<OpenMeteoMarineResponse> {
	const params = new URLSearchParams({
		latitude: lat.toString(),
		longitude: lon.toString(),
		current: 'wave_height,wave_period,wave_direction,swell_wave_height,swell_wave_period,swell_wave_direction,sea_surface_temperature,sea_level_height_msl',
		hourly: 'sea_level_height_msl',
		forecast_days: '2',
		timezone: 'auto',
	});
	const res = await netFetch(`${BASE_URL}?${params}`, { signal: AbortSignal.timeout(FETCH_TIMEOUT_MS) });
	if (!res.ok) throw new HttpError(res.status, `Open-Meteo marine error: ${res.status}`);
	return res.json();
}

/** The sea at the place; null inland, where the wave height and the sea temperature are both missing. */
export function mapToSea(response: OpenMeteoMarineResponse): SeaConditions | null {
	const c = response.current;
	if (!c || (c.wave_height == null && c.sea_surface_temperature == null)) return null;
	const h = response.hourly;
	return {
		waveHeightM: c.wave_height ?? null,
		wavePeriodS: c.wave_period ?? null,
		waveDirection: c.wave_direction ?? null,
		swellHeightM: c.swell_wave_height ?? null,
		swellPeriodS: c.swell_wave_period ?? null,
		swellDirection: c.swell_wave_direction ?? null,
		seaTemperatureC: c.sea_surface_temperature ?? null,
		seaLevelM: c.sea_level_height_msl ?? null,
		levelTimes: h?.time.map((t) => parseIsoDateTime(t, response.utc_offset_seconds)) ?? [],
		levels: h?.sea_level_height_msl ?? [],
	};
}
