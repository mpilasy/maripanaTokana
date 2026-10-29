// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.data.remote

import androidx.annotation.StringRes
import orinasa.njarasoa.maripanatokana.R

/**
 * Maps WMO weather interpretation codes to string resource IDs.
 * https://open-meteo.com/en/docs#weathervariables
 */
private val WMO_DESCRIPTIONS = mapOf(
    0 to R.string.wmo_clear_sky,
    1 to R.string.wmo_mainly_clear,
    2 to R.string.wmo_partly_cloudy,
    3 to R.string.wmo_overcast,
    45 to R.string.wmo_fog,
    48 to R.string.wmo_rime_fog,
    51 to R.string.wmo_light_drizzle,
    53 to R.string.wmo_moderate_drizzle,
    55 to R.string.wmo_dense_drizzle,
    56 to R.string.wmo_light_freezing_drizzle,
    57 to R.string.wmo_dense_freezing_drizzle,
    61 to R.string.wmo_slight_rain,
    63 to R.string.wmo_moderate_rain,
    65 to R.string.wmo_heavy_rain,
    66 to R.string.wmo_light_freezing_rain,
    67 to R.string.wmo_heavy_freezing_rain,
    71 to R.string.wmo_slight_snowfall,
    73 to R.string.wmo_moderate_snowfall,
    75 to R.string.wmo_heavy_snowfall,
    77 to R.string.wmo_snow_grains,
    80 to R.string.wmo_slight_rain_showers,
    81 to R.string.wmo_moderate_rain_showers,
    82 to R.string.wmo_violent_rain_showers,
    85 to R.string.wmo_slight_snow_showers,
    86 to R.string.wmo_heavy_snow_showers,
    95 to R.string.wmo_thunderstorm,
    96 to R.string.wmo_thunderstorm_slight_hail,
    99 to R.string.wmo_thunderstorm_heavy_hail,
)

@StringRes
fun wmoDescriptionRes(code: Int): Int = WMO_DESCRIPTIONS[code] ?: R.string.wmo_unknown

/** Each code group's emoji by day; [WMO_NIGHT_EMOJI] has the night ones where they differ. */
private val WMO_EMOJI = mapOf(
    listOf(0) to "\u2600\uFE0F", // ☀️
    listOf(1) to "\uD83C\uDF24\uFE0F", // 🌤️
    listOf(2) to "\u26C5", // ⛅
    listOf(3) to "\u2601\uFE0F", // ☁️
    listOf(45, 48) to "\uD83C\uDF2B\uFE0F", // 🌫️
    listOf(51, 53, 55, 80, 81, 82) to "\uD83C\uDF26\uFE0F", // 🌦️
    listOf(56, 57, 66, 67, 85, 86) to "\uD83C\uDF28\uFE0F", // 🌨️
    listOf(61, 63, 65) to "\uD83C\uDF27\uFE0F", // 🌧️
    listOf(71, 73, 75, 77) to "\u2744\uFE0F", // ❄️
    listOf(95, 96, 99) to "\u26C8\uFE0F", // ⛈️
).flatMap { (codes, emoji) -> codes.map { it to emoji } }.toMap()

private val WMO_NIGHT_EMOJI = mapOf(
    0 to "\uD83C\uDF11", // 🌑
    1 to "\uD83C\uDF14", // 🌔
    2 to "\uD83C\uDF13", // 🌓
)

fun wmoEmoji(code: Int, isNight: Boolean = false): String =
    (if (isNight) WMO_NIGHT_EMOJI[code] else null) ?: WMO_EMOJI[code] ?: "\uD83C\uDF10" // 🌐

/**
 * Maps WMO weather code + is_day to an OWM-style icon code for backward compatibility.
 */
fun wmoIconCode(code: Int, isDay: Boolean): String {
    val base = WMO_ICON_CODES[code] ?: "01"
    val suffix = if (isDay) "d" else "n"
    return "$base$suffix"
}

private val WMO_ICON_CODES = mapOf(
    listOf(0) to "01",
    listOf(1) to "02",
    listOf(2) to "03",
    listOf(3) to "04",
    listOf(45, 48) to "50",
    listOf(51, 53, 55, 56, 57, 80, 81, 82) to "09",
    listOf(61, 63, 65, 66, 67) to "10",
    listOf(71, 73, 75, 77, 85, 86) to "13",
    listOf(95, 96, 99) to "11",
).flatMap { (codes, icon) -> codes.map { it to icon } }.toMap()
