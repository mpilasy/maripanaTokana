const HOUR = 3_600_000;

/**
 * The weather code for each day's night (sunset to next sunrise), taken from hourly data as the most
 * severe code, the way Open-Meteo picks a day's code. Null when the hourly data doesn't cover that night.
 * Times are epoch millis; the last night, with no next sunrise, runs 12 hours past sunset.
 */
export function nightWeatherCodes(times: number[], codes: number[], sunsets: number[], sunrises: number[]): (number | null)[] {
	return sunsets.map((sunset, i) => {
		const end = sunrises[i + 1] ?? sunset + 12 * HOUR;
		const night = times.flatMap((t, j) => (t > sunset && t < end && Number.isFinite(codes[j]) ? [codes[j]] : []));
		// Every hour but one must be there, so a night cut off by the end of the data shows nothing
		return night.length > 0 && night.length >= Math.floor((end - sunset) / HOUR) - 1 ? Math.max(...night) : null;
	});
}
