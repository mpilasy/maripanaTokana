// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

/** The sea at a coastal place now (Open-Meteo marine). Null values are missing. Directions: where the waves come from. */
export interface SeaConditions {
	waveHeightM: number | null;
	wavePeriodS: number | null;
	waveDirection: number | null;
	swellHeightM: number | null;
	swellPeriodS: number | null;
	swellDirection: number | null;
	seaTemperatureC: number | null;
	/** The sea level now, above mean sea level (m). */
	seaLevelM: number | null;
	/** The hourly sea level (m) for the tides, with its times (epoch millis). */
	levelTimes: number[];
	levels: (number | null)[];
}

export interface Tide {
	time: number; // epoch millis
	levelM: number;
}

/**
 * The next high and the next low tide after `now`: the first local maximum and minimum of the hourly
 * sea level that come after it. Null when the data has none.
 */
export function nextTides(times: number[], levels: (number | null)[], now: number): { high: Tide | null; low: Tide | null } {
	let high: Tide | null = null;
	let low: Tide | null = null;
	for (let i = 1; i < levels.length - 1 && (!high || !low); i++) {
		const prev = levels[i - 1];
		const cur = levels[i];
		const next = levels[i + 1];
		if (prev == null || cur == null || next == null || times[i] <= now) continue;
		if (!high && cur > prev && cur >= next) high = { time: times[i], levelM: cur };
		if (!low && cur < prev && cur <= next) low = { time: times[i], levelM: cur };
	}
	return { high, low };
}

const FEET_PER_METER = 3.28084;

/** A height in metres and feet, one decimal, primary first: ["1.1 m", "3.7 ft"]. */
export function seaHeightDual(meters: number, metricPrimary: boolean): [string, string] {
	const m = `${meters.toFixed(1)} m`;
	const ft = `${(meters * FEET_PER_METER).toFixed(1)} ft`;
	return metricPrimary ? [m, ft] : [ft, m];
}
