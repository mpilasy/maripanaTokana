// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

/**
 * The SVG path of the moon's lit part for `phase` (0 = new, 0.5 = full), centred on (cx, cy) with
 * radius r. `southern` mirrors it, as it is seen south of the equator. Same shape as drawMoon on Android.
 */
export function moonLitPath(phase: number, southern: boolean, cx: number, cy: number, r: number): string {
	// The terminator is a half ellipse; cos > 0 is a crescent, cos < 0 a gibbous moon
	const k = Math.cos(2 * Math.PI * phase);
	const litRight = phase < 0.5 !== southern;
	const rx = r * Math.abs(k);
	const limbSweep = litRight ? 1 : 0;
	const terminatorSweep = k > 0 === litRight ? 0 : 1;
	return `M ${cx} ${cy - r} A ${r} ${r} 0 0 ${limbSweep} ${cx} ${cy + r} A ${rx.toFixed(3)} ${r} 0 0 ${terminatorSweep} ${cx} ${cy - r} Z`;
}
