// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

export class WindSpeed {
	private constructor(readonly metersPerSecond: number) {}

	get mph(): number {
		return this.metersPerSecond * 2.23694;
	}

	displayMetric(): string {
		return Number.isFinite(this.metersPerSecond) ? `${this.metersPerSecond.toFixed(1)} m/s` : '-- m/s';
	}

	displayImperial(): string {
		return Number.isFinite(this.mph) ? `${this.mph.toFixed(1)} mph` : '-- mph';
	}

	displayDual(metricPrimary: boolean): [string, string] {
		return metricPrimary
			? [this.displayMetric(), this.displayImperial()]
			: [this.displayImperial(), this.displayMetric()];
	}

	static fromMetersPerSecond(ms: number): WindSpeed {
		return new WindSpeed(ms);
	}

	static fromMph(mph: number): WindSpeed {
		return new WindSpeed(mph / 2.23694);
	}
}

export const DIRECTIONS = ['N', 'NNE', 'NE', 'ENE', 'E', 'ESE', 'SE', 'SSE', 'S', 'SSW', 'SW', 'WSW', 'W', 'WNW', 'NW', 'NNW'];

export function getCardinalDirection(deg: number, directions: any = DIRECTIONS): string {
	if (!Array.isArray(directions) || directions.length === 0) return '';
	const idx = Math.round(((deg % 360 + 360) % 360) / (360 / directions.length)) % directions.length;
	return directions[idx] ?? '';
}

/** Below this speed (km/h) the forecast rows show no wind direction arrow: the direction of a calm wind means little. */
export const WIND_ARROW_MIN_KMH = 5;

/** Whether a forecast row shows the wind direction arrow (5 km/h or more). */
export function showWindArrow(speed: WindSpeed): boolean {
	return speed.metersPerSecond * 3.6 >= WIND_ARROW_MIN_KMH;
}
