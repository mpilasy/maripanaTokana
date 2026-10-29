// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

/**
 * A chart label "primary · secondary" (both units) as its two parts, so that the chart can draw the
 * secondary unit smaller and dimmer, without the dot. Other labels have no second part.
 */
export function splitDual(text: string): [string, string | null] {
	const i = text.indexOf(' · ');
	return i < 0 ? [text, null] : [text.slice(0, i), text.slice(i + 3)];
}

/** Points on a line, in runs with no missing value between them: a missing value leaves a gap. */
export function runsOf<T extends { i: number }>(points: T[]): T[][] {
	const runs: T[][] = [];
	points.forEach((p, k) => {
		if (k === 0 || p.i !== points[k - 1].i + 1) runs.push([]);
		runs[runs.length - 1].push(p);
	});
	return runs;
}
