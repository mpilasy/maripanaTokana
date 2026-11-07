// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

/** An Open-Meteo forecast model the user can choose. Names and centres are proper names: not translated. */
export interface WeatherModel {
	key: string;
	name: string;
	centre: string;
	days: number;
}

// Twin: WEATHER_MODELS on Android. Keep the same order.
export const WEATHER_MODELS: WeatherModel[] = [
	{ key: 'ecmwf_ifs025', name: 'ECMWF IFS', centre: 'ECMWF', days: 10 },
	{ key: 'ecmwf_aifs025_single', name: 'ECMWF AIFS', centre: 'ECMWF', days: 10 },
	{ key: 'gfs_seamless', name: 'GFS', centre: 'NOAA', days: 10 },
	{ key: 'icon_seamless', name: 'ICON', centre: 'DWD', days: 7 },
	{ key: 'meteofrance_seamless', name: 'Météo-France', centre: 'Météo-France', days: 4 },
	{ key: 'ukmo_seamless', name: 'UK Met Office', centre: 'Met Office', days: 7 },
	{ key: 'gem_seamless', name: 'GEM', centre: 'ECCC', days: 10 },
	{ key: 'jma_seamless', name: 'JMA', centre: 'JMA', days: 10 },
];

/** The model for a stored key; null for Automatic (empty or unknown key). */
export function weatherModelFor(key: string | null | undefined): WeatherModel | null {
	return WEATHER_MODELS.find((m) => m.key === key) ?? null;
}
