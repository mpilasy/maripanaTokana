package orinasa.njarasoa.maripanatokana.data.remote

import androidx.annotation.StringRes
import orinasa.njarasoa.maripanatokana.R

/**
 * Maps WMO weather interpretation codes to string resource IDs.
 * https://open-meteo.com/en/docs#weathervariables
 */
@StringRes
fun wmoDescriptionRes(code: Int): Int = when (code) {
    0 -> R.string.wmo_clear_sky
    1 -> R.string.wmo_mainly_clear
    2 -> R.string.wmo_partly_cloudy
    3 -> R.string.wmo_overcast
    45 -> R.string.wmo_fog
    48 -> R.string.wmo_rime_fog
    51 -> R.string.wmo_light_drizzle
    53 -> R.string.wmo_moderate_drizzle
    55 -> R.string.wmo_dense_drizzle
    56 -> R.string.wmo_light_freezing_drizzle
    57 -> R.string.wmo_dense_freezing_drizzle
    61 -> R.string.wmo_slight_rain
    63 -> R.string.wmo_moderate_rain
    65 -> R.string.wmo_heavy_rain
    66 -> R.string.wmo_light_freezing_rain
    67 -> R.string.wmo_heavy_freezing_rain
    71 -> R.string.wmo_slight_snowfall
    73 -> R.string.wmo_moderate_snowfall
    75 -> R.string.wmo_heavy_snowfall
    77 -> R.string.wmo_snow_grains
    80 -> R.string.wmo_slight_rain_showers
    81 -> R.string.wmo_moderate_rain_showers
    82 -> R.string.wmo_violent_rain_showers
    85 -> R.string.wmo_slight_snow_showers
    86 -> R.string.wmo_heavy_snow_showers
    95 -> R.string.wmo_thunderstorm
    96 -> R.string.wmo_thunderstorm_slight_hail
    99 -> R.string.wmo_thunderstorm_heavy_hail
    else -> R.string.wmo_unknown
}

fun wmoEmoji(code: Int, isNight: Boolean = false): String = when (code) {
    0 -> if (isNight) "\uD83C\uDF11"  // 🌑
         else "\u2600\uFE0F"           // ☀️
    1 -> if (isNight) "\uD83C\uDF14"  // 🌔
         else "\uD83C\uDF24\uFE0F"    // 🌤️
    2 -> if (isNight) "\uD83C\uDF13"  // 🌓
         else "\u26C5"                 // ⛅
    3 -> "\u2601\uFE0F"       // ☁️
    45, 48 -> "\uD83C\uDF2B\uFE0F" // 🌫️
    51, 53, 55 -> "\uD83C\uDF26\uFE0F" // 🌦️
    56, 57 -> "\uD83C\uDF28\uFE0F" // 🌨️
    61, 63 -> "\uD83C\uDF27\uFE0F" // 🌧️
    65 -> "\uD83C\uDF27\uFE0F" // 🌧️
    66, 67 -> "\uD83C\uDF28\uFE0F" // 🌨️
    71, 73, 75, 77 -> "\u2744\uFE0F" // ❄️
    80, 81, 82 -> "\uD83C\uDF26\uFE0F" // 🌦️
    85, 86 -> "\uD83C\uDF28\uFE0F" // 🌨️
    95 -> "\u26C8\uFE0F"      // ⛈️
    96, 99 -> "\u26C8\uFE0F"  // ⛈️
    else -> "\uD83C\uDF10"    // 🌐
}

/**
 * Maps WMO weather code + is_day to an OWM-style icon code for backward compatibility.
 */
fun wmoIconCode(code: Int, isDay: Boolean): String {
    val base = when (code) {
        0 -> "01"
        1 -> "02"
        2 -> "03"
        3 -> "04"
        45, 48 -> "50"
        51, 53, 55, 56, 57 -> "09"
        61, 63, 65, 66, 67 -> "10"
        71, 73, 75, 77, 85, 86 -> "13"
        80, 81, 82 -> "09"
        95, 96, 99 -> "11"
        else -> "01"
    }
    val suffix = if (isDay) "d" else "n"
    return "$base$suffix"
}
