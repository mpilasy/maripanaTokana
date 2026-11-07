// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.domain.model

/**
 * One Open-Meteo forecast model the user can choose. [key] is the value of the `models` parameter.
 * [name] and [centre] are proper names: they are not translated. [days] is the forecast horizon.
 */
data class WeatherModel(val key: String, val name: String, val centre: String, val days: Int)

/** The models in Settings, in this order. Keep in sync with WEATHER_MODELS on the web. */
val WEATHER_MODELS = listOf(
    WeatherModel("ecmwf_ifs025", "ECMWF IFS", "ECMWF", 10),
    WeatherModel("ecmwf_aifs025_single", "ECMWF AIFS", "ECMWF", 10),
    WeatherModel("gfs_seamless", "GFS", "NOAA", 10),
    WeatherModel("icon_seamless", "ICON", "DWD", 7),
    WeatherModel("meteofrance_seamless", "Météo-France", "Météo-France", 4),
    WeatherModel("ukmo_seamless", "UK Met Office", "Met Office", 7),
    WeatherModel("gem_seamless", "GEM", "ECCC", 10),
    WeatherModel("jma_seamless", "JMA", "JMA", 10),
)

/** The model with [key], or null for Automatic or an unknown key. */
fun weatherModelFor(key: String?): WeatherModel? = WEATHER_MODELS.find { it.key == key }
