// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

/** The temperature colour scale: [°C, colour] stops, cold to hot. Same stops as on Android. */
export const TEMP_COLOR_STOPS: [number, string][] = [
	[-20, '#B39DDB'],
	[-10, '#90CAF9'],
	[0, '#4FC3F7'],
	[10, '#4DD0E1'],
	[15, '#81C784'],
	[20, '#DCE775'],
	[25, '#FFD54F'],
	[30, '#FFB74D'],
	[35, '#FF8A65'],
	[40, '#E57373'],
];

function channels(hex: string): number[] {
	return [1, 3, 5].map((i) => parseInt(hex.slice(i, i + 2), 16));
}

/** The colour for a temperature in °C: a linear mix of the two stops around it, the end colour outside the scale. */
export function temperatureColor(celsius: number): string {
	const stops = TEMP_COLOR_STOPS;
	if (!(celsius > stops[0][0])) return stops[0][1];
	const upper = stops.findIndex(([c]) => c >= celsius);
	if (upper === -1) return stops[stops.length - 1][1];
	const [c0, hex0] = stops[upper - 1];
	const [c1, hex1] = stops[upper];
	const f = (celsius - c0) / (c1 - c0);
	const a = channels(hex0);
	const b = channels(hex1);
	return '#' + a.map((v, i) => Math.round(v + (b[i] - v) * f).toString(16).padStart(2, '0').toUpperCase()).join('');
}
