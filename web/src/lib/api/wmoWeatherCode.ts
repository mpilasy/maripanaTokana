// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

const WMO_DESCRIPTION_KEYS: Record<number, string> = {
	0: 'wmo_clear_sky',
	1: 'wmo_mainly_clear',
	2: 'wmo_partly_cloudy',
	3: 'wmo_overcast',
	45: 'wmo_fog',
	48: 'wmo_rime_fog',
	51: 'wmo_light_drizzle',
	53: 'wmo_moderate_drizzle',
	55: 'wmo_dense_drizzle',
	56: 'wmo_light_freezing_drizzle',
	57: 'wmo_dense_freezing_drizzle',
	61: 'wmo_slight_rain',
	63: 'wmo_moderate_rain',
	65: 'wmo_heavy_rain',
	66: 'wmo_light_freezing_rain',
	67: 'wmo_heavy_freezing_rain',
	71: 'wmo_slight_snowfall',
	73: 'wmo_moderate_snowfall',
	75: 'wmo_heavy_snowfall',
	77: 'wmo_snow_grains',
	80: 'wmo_slight_rain_showers',
	81: 'wmo_moderate_rain_showers',
	82: 'wmo_violent_rain_showers',
	85: 'wmo_slight_snow_showers',
	86: 'wmo_heavy_snow_showers',
	95: 'wmo_thunderstorm',
	96: 'wmo_thunderstorm_slight_hail',
	99: 'wmo_thunderstorm_heavy_hail',
};

/** Returns an i18n key string for the WMO weather code description. */
export function wmoDescriptionKey(code: number): string {
	return WMO_DESCRIPTION_KEYS[code] ?? 'wmo_unknown';
}

/** Each code group's emoji by day; WMO_NIGHT_EMOJI has the night ones where they differ. */
const WMO_EMOJI: Record<number, string> = Object.fromEntries(
	(
		[
			[[0], '\u2600\uFE0F'],
			[[1], '\uD83C\uDF24\uFE0F'],
			[[2], '\u26C5'],
			[[3], '\u2601\uFE0F'],
			[[45, 48], '\uD83C\uDF2B\uFE0F'],
			[[51, 53, 55, 80, 81, 82], '\uD83C\uDF26\uFE0F'],
			[[56, 57, 66, 67, 85, 86], '\uD83C\uDF28\uFE0F'],
			[[61, 63, 65], '\uD83C\uDF27\uFE0F'],
			[[71, 73, 75, 77], '\u2744\uFE0F'],
			[[95, 96, 99], '\u26C8\uFE0F']
		] as [number[], string][]
	).flatMap(([codes, emoji]) => codes.map((c) => [c, emoji]))
);

const WMO_NIGHT_EMOJI: Record<number, string> = {
	0: '\uD83C\uDF11',
	1: '\uD83C\uDF14',
	2: '\uD83C\uDF13'
};

/** Returns a weather emoji for the WMO code, with day/night variants. */
export function wmoEmoji(code: number, isNight = false): string {
	return (isNight ? WMO_NIGHT_EMOJI[code] : undefined) ?? WMO_EMOJI[code] ?? '\uD83C\uDF10';
}
