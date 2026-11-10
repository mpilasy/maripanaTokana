// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

/**
 * The sun's declination, and the solstices and equinoxes, calculated in the browser. The formulas are those of
 * Jean Meeus, "Astronomical Algorithms" (2nd edition): chapter 25 (low accuracy, about 0.01°) for the
 * declination, chapter 27 (about a minute, for the years 1000 to 3000) for the solstices and equinoxes.
 * The Android app has the same code in `domain/model/Seasons.kt`; keep the two the same.
 */

const RAD = Math.PI / 180;
const DAY_MS = 86_400_000;
const UNIX_EPOCH_JD = 2440587.5;

/** The largest declination of the sun, north or south: the tilt of the Earth's axis, in degrees. */
export const MAX_DECLINATION = 23.44;

/** A solstice or equinox: `kind` 0 = March equinox, 1 = June solstice, 2 = September equinox, 3 = December solstice. */
export interface SeasonEvent { time: number; kind: number }

/** The solstice or equinox before a time and the one after it. */
export interface SeasonsAround { previous: SeasonEvent; next: SeasonEvent }

/** The latitude where the sun is straight overhead at `ms` (epoch millis), in degrees: north positive, south negative. */
export function declination(ms: number): number {
	const t = (ms / DAY_MS + UNIX_EPOCH_JD - 2451545) / 36525;
	const l0 = 280.46646 + t * (36000.76983 + t * 0.0003032);
	const m = RAD * (357.52911 + t * (35999.05029 - t * 0.0001537));
	const c = (1.914602 - t * (0.004817 + t * 0.000014)) * Math.sin(m) + (0.019993 - t * 0.000101) * Math.sin(2 * m) + 0.000289 * Math.sin(3 * m);
	const omega = RAD * (125.04 - 1934.136 * t);
	// The apparent longitude of the sun, with the nutation and the aberration
	const lambda = RAD * (l0 + c - 0.00569 - 0.00478 * Math.sin(omega));
	const epsilon = RAD * (23.439291 - t * 0.0130042 + 0.00256 * Math.cos(omega));
	return Math.asin(Math.sin(epsilon) * Math.sin(lambda)) / RAD;
}

// The mean time of each event in Julian Ephemeris Days, as a polynomial in (year - 2000) / 1000
const MEAN = [
	[2451623.80984, 365242.37404, 0.05169, -0.00411, -0.00057],
	[2451716.56767, 365241.62603, 0.00325, 0.00888, -0.00030],
	[2451810.21715, 365242.01767, -0.11575, 0.00337, 0.00078],
	[2451900.05952, 365242.74049, -0.06223, -0.00823, 0.00032],
];

// The periodic terms (A, B, C) of Meeus table 27.C
const TERMS = [
	[485, 324.96, 1934.136], [203, 337.23, 32964.467], [199, 342.08, 20.186],
	[182, 27.85, 445267.112], [156, 73.14, 45036.886], [136, 171.52, 22518.443],
	[77, 222.54, 65928.934], [74, 296.72, 3034.906], [70, 243.58, 9037.513],
	[58, 119.81, 33718.147], [52, 297.17, 150.678], [50, 21.02, 2281.226],
	[45, 247.54, 29929.562], [44, 325.15, 31555.956], [29, 60.93, 4443.417],
	[18, 155.12, 67555.328], [17, 288.79, 4562.452], [16, 198.04, 62894.029],
	[14, 199.76, 31436.921], [12, 95.39, 14577.848], [12, 287.11, 31931.756],
	[12, 320.81, 34777.259], [9, 227.73, 1222.114], [8, 15.45, 16859.074],
];

/** The time (epoch millis) of event `kind` (see `SeasonEvent`) in `year`. */
export function eventTime(year: number, kind: number): number {
	const y = (year - 2000) / 1000;
	const c = MEAN[kind];
	const jde0 = c[0] + y * (c[1] + y * (c[2] + y * (c[3] + y * c[4])));
	const t = (jde0 - 2451545) / 36525;
	const w = RAD * (35999.373 * t - 2.47);
	const dl = 1 + 0.0334 * Math.cos(w) + 0.0007 * Math.cos(2 * w);
	const s = TERMS.reduce((sum, [a, b, cc]) => sum + a * Math.cos(RAD * (b + cc * t)), 0);
	const jde = jde0 + (0.00001 * s) / dl;
	// Dynamical time is about 69 seconds ahead of universal time in the 2020s
	return Math.round((jde - UNIX_EPOCH_JD) * DAY_MS) - 69_000;
}

/** The solstice or equinox at or before `ms` and the first one after it. */
export function seasonsAround(ms: number): SeasonsAround {
	const year = 1970 + Math.floor(ms / DAY_MS / 365.2425);
	const events = [year - 1, year, year + 1].flatMap((y) => [0, 1, 2, 3].map((kind) => ({ time: eventTime(y, kind), kind })));
	const next = events.findIndex((e) => e.time > ms);
	return { previous: events[next - 1], next: events[next] };
}

/**
 * How far the noon sun is from straight overhead at latitude `lat` on the day of `ms`, in degrees: positive when it
 * is to the south, negative when it is to the north. Above 90°, the sun stays below the horizon all day.
 */
export function noonOffset(lat: number, ms: number): number {
	return lat - declination(ms);
}

/** The noon offsets (see `noonOffset`) at the solstices at latitude `lat`: the one nearer overhead first. */
export function noonRange(lat: number): [number, number] {
	const a = lat - MAX_DECLINATION;
	const b = lat + MAX_DECLINATION;
	return Math.abs(a) <= Math.abs(b) ? [a, b] : [b, a];
}

/**
 * The index of a solstice or equinox of `kind` in season_events (spring equinox, summer solstice, autumn equinox,
 * winter solstice) at latitude `lat`: south of the equator the seasons are the other way round.
 */
export function seasonNameIndex(kind: number, lat: number): number {
	return lat < 0 ? (kind + 2) % 4 : kind;
}

/** The number of days at the place (UTC offset `utcOffsetSeconds`) from the day of `fromMs` to the day of `toMs`. */
export function daysUntil(fromMs: number, toMs: number, utcOffsetSeconds: number): number {
	const day = (ms: number) => Math.floor((ms + utcOffsetSeconds * 1000) / DAY_MS);
	return day(toMs) - day(fromMs);
}
