// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

import type { HourlyAirQuality } from './weatherData';
import type { AqiStandard } from './airQuality';

const DAY = 24 * 3_600_000;

/**
 * Each day's worst air quality as [primary, secondary] index, from the hourly forecast. Null for a day
 * the forecast covers for less than 6 hours (air-quality forecasts only reach about 5 days ahead);
 * today counts with whatever hours are left of it.
 * `dates` are each day's local midnight, epoch millis.
 */
export function dailyAirQuality(dates: number[], hourly: HourlyAirQuality[], standard: AqiStandard): ([number, number] | null)[] {
	return dates.map((start, i) => {
		const day = hourly.filter((h) => h.time >= start && h.time < start + DAY);
		if (day.length < (i === 0 ? 1 : 6)) return null;
		const us = Math.max(...day.map((h) => h.usValue));
		const eu = Math.max(...day.map((h) => h.europeanValue));
		return standard === 'EUROPEAN' ? [eu, us] : [us, eu];
	});
}
