// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

/**
 * The moon's phase, place in the sky, rise and set, calculated in the browser. The formulas are
 * those of SunCalc (Vladimir Agafonkin, BSD-2-Clause), after "Astronomy Answers" by Aart Kerkhoven.
 * The Android app has the same code in `domain/model/Moon.kt`; keep the two the same.
 */

const RAD = Math.PI / 180;
const DAY_MS = 86_400_000;
const J1970 = 2440588;
const J2000 = 2451545;
const E = RAD * 23.4397; // obliquity of the Earth

/** `fraction` lit (0-1); `phase` 0 = new, 0.25 = first quarter, 0.5 = full, 0.75 = last quarter. */
export interface MoonIllumination { fraction: number; phase: number }
/** `azimuth` in degrees from north, clockwise; `altitude` in degrees above the horizon. */
export interface MoonPosition { azimuth: number; altitude: number }
/** Epoch millis; null when the moon does not rise (or set) in the 24 hours searched. */
export interface MoonTimes { rise: number | null; set: number | null }

const toDays = (ms: number) => ms / DAY_MS - 0.5 + J1970 - J2000;
const rightAscension = (l: number, b: number) => Math.atan2(Math.sin(l) * Math.cos(E) - Math.tan(b) * Math.sin(E), Math.cos(l));
const declination = (l: number, b: number) => Math.asin(Math.sin(b) * Math.cos(E) + Math.cos(b) * Math.sin(E) * Math.sin(l));
const siderealTime = (d: number, lw: number) => RAD * (280.16 + 360.9856235 * d) - lw;

function astroRefraction(h: number): number {
	const a = h < 0 ? 0 : h;
	return 0.0002967 / Math.tan(a + 0.00312536 / (a + 0.08901179));
}

function sunCoords(d: number) {
	const m = RAD * (357.5291 + 0.98560028 * d);
	const c = RAD * (1.9148 * Math.sin(m) + 0.02 * Math.sin(2 * m) + 0.0003 * Math.sin(3 * m));
	const l = m + c + RAD * 102.9372 + Math.PI;
	return { ra: rightAscension(l, 0), dec: declination(l, 0) };
}

function moonCoords(d: number) {
	const l = RAD * (218.316 + 13.176396 * d);
	const m = RAD * (134.963 + 13.064993 * d);
	const f = RAD * (93.272 + 13.22935 * d);
	const lon = l + RAD * 6.289 * Math.sin(m);
	const lat = RAD * 5.128 * Math.sin(f);
	return { ra: rightAscension(lon, lat), dec: declination(lon, lat), dist: 385001 - 20905 * Math.cos(m) };
}

/** Altitude in radians, with refraction; azimuth in radians from south, westward (SunCalc's). */
function rawPosition(ms: number, lat: number, lon: number): [number, number] {
	const lw = RAD * -lon;
	const phi = RAD * lat;
	const d = toDays(ms);
	const c = moonCoords(d);
	const h = siderealTime(d, lw) - c.ra;
	const az = Math.atan2(Math.sin(h), Math.cos(h) * Math.sin(phi) - Math.tan(c.dec) * Math.cos(phi));
	const alt = Math.asin(Math.sin(phi) * Math.sin(c.dec) + Math.cos(phi) * Math.cos(c.dec) * Math.cos(h));
	return [az, alt + astroRefraction(alt)];
}

export function moonPosition(ms: number, lat: number, lon: number): MoonPosition {
	const [az, alt] = rawPosition(ms, lat, lon);
	return { azimuth: (((az / RAD + 180) % 360) + 360) % 360, altitude: alt / RAD };
}

export function moonIllumination(ms: number): MoonIllumination {
	const d = toDays(ms);
	const s = sunCoords(d);
	const m = moonCoords(d);
	const sdist = 149598000;
	const phi = Math.acos(Math.sin(s.dec) * Math.sin(m.dec) + Math.cos(s.dec) * Math.cos(m.dec) * Math.cos(s.ra - m.ra));
	const inc = Math.atan2(sdist * Math.sin(phi), m.dist - sdist * Math.cos(phi));
	const angle = Math.atan2(Math.cos(s.dec) * Math.sin(s.ra - m.ra), Math.sin(s.dec) * Math.cos(m.dec) - Math.cos(s.dec) * Math.sin(m.dec) * Math.cos(s.ra - m.ra));
	return { fraction: (1 + Math.cos(inc)) / 2, phase: 0.5 + (0.5 * inc * (angle < 0 ? -1 : 1)) / Math.PI };
}

/** The rise and set in the 24 hours from `dayStart` (epoch millis, e.g. midnight at the place). */
export function moonTimes(dayStart: number, lat: number, lon: number): MoonTimes {
	const hc = 0.133 * RAD;
	const altAt = (hours: number) => rawPosition(dayStart + hours * 3_600_000, lat, lon)[1] - hc;
	let h0 = altAt(0);
	let rise: number | null = null;
	let set: number | null = null;
	// Fits a parabola through each two hours of altitudes and finds where it crosses the horizon
	for (let i = 1; i <= 23; i += 2) {
		const h1 = altAt(i);
		const h2 = altAt(i + 1);
		const a = (h0 + h2) / 2 - h1;
		const b = (h2 - h0) / 2;
		const xe = -b / (2 * a);
		const ye = (a * xe + b) * xe + h1;
		const disc = b * b - 4 * a * h1;
		let roots = 0;
		let x1 = 0;
		let x2 = 0;
		if (disc >= 0) {
			const dx = Math.sqrt(disc) / (Math.abs(a) * 2);
			x1 = xe - dx;
			x2 = xe + dx;
			if (Math.abs(x1) <= 1) roots++;
			if (Math.abs(x2) <= 1) roots++;
			if (x1 < -1) x1 = x2;
		}
		if (roots === 1) {
			if (h0 < 0) rise = i + x1;
			else set = i + x1;
		} else if (roots === 2) {
			rise = i + (ye < 0 ? x2 : x1);
			set = i + (ye < 0 ? x1 : x2);
		}
		if (rise != null && set != null) break;
		h0 = h2;
	}
	const toMs = (h: number | null) => (h == null ? null : dayStart + Math.trunc(h * 3_600_000));
	return { rise: toMs(rise), set: toMs(set) };
}

/** Index into moon_phases: new, waxing crescent, first quarter, waxing gibbous, full, waning gibbous, last quarter, waning crescent. */
export function moonPhaseIndex(phase: number): number {
	return Math.trunc(phase * 8 + 0.5) % 8;
}

/**
 * Where the moon is on its arc across the sky now: 0 at its rise, 1 at its set, -1 while it is down.
 * `days` are the times for yesterday, today and tomorrow.
 */
export function moonArcProgress(now: number, days: MoonTimes[]): number {
	const events = days
		.flatMap((d) => [...(d.rise != null ? [{ t: d.rise, rise: true }] : []), ...(d.set != null ? [{ t: d.set, rise: false }] : [])])
		.sort((a, b) => a.t - b.t);
	const last = events.filter((e) => e.t <= now).pop();
	const next = events.find((e) => e.t > now && !e.rise);
	return last?.rise && next ? (now - last.t) / (next.t - last.t) : -1;
}
