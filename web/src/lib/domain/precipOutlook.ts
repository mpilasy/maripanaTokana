import type { HourlyForecast } from './weatherData';

/** What the next 24 hours hold for rain or snow, for the one-line summary on the first page. */
export type PrecipOutlook =
	| { kind: 'starts' | 'stops'; snow: boolean; time: number }
	| { kind: 'all_day' | 'dry'; snow: boolean };

const SNOW_CODES = new Set([71, 73, 75, 77, 85, 86]);

// An hour counts as wet with an even-or-better chance of rain, or a measurable amount that's at least
// somewhat likely; a trace the model gives a 2% chance isn't rain worth announcing
function isWet(h: HourlyForecast): boolean {
	return h.precipProbability >= 50 || (h.precipitation.mm >= 0.1 && h.precipProbability >= 30);
}

export function precipOutlook(hourly: HourlyForecast[]): PrecipOutlook | null {
	if (hourly.length === 0) return null;
	const wetNow = isWet(hourly[0]);
	const change = hourly.findIndex((h) => isWet(h) !== wetNow);
	// The precipitation the summary talks about: the current one, or the one that starts
	const wetHour = wetNow ? hourly[0] : change !== -1 ? hourly[change] : null;
	const snow = wetHour != null && SNOW_CODES.has(wetHour.weatherCode);
	if (change === -1) return { kind: wetNow ? 'all_day' : 'dry', snow };
	return { kind: wetNow ? 'stops' : 'starts', snow, time: hourly[change].time };
}
