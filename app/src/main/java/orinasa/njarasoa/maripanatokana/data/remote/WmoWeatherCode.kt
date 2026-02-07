package orinasa.njarasoa.maripanatokana.data.remote

/**
 * Maps WMO weather interpretation codes to human-readable descriptions.
 * https://open-meteo.com/en/docs#weathervariables
 */
fun wmoDescription(code: Int): String = when (code) {
    0 -> "Clear sky"
    1 -> "Mainly clear"
    2 -> "Partly cloudy"
    3 -> "Overcast"
    45 -> "Fog"
    48 -> "Depositing rime fog"
    51 -> "Light drizzle"
    53 -> "Moderate drizzle"
    55 -> "Dense drizzle"
    56 -> "Light freezing drizzle"
    57 -> "Dense freezing drizzle"
    61 -> "Slight rain"
    63 -> "Moderate rain"
    65 -> "Heavy rain"
    66 -> "Light freezing rain"
    67 -> "Heavy freezing rain"
    71 -> "Slight snowfall"
    73 -> "Moderate snowfall"
    75 -> "Heavy snowfall"
    77 -> "Snow grains"
    80 -> "Slight rain showers"
    81 -> "Moderate rain showers"
    82 -> "Violent rain showers"
    85 -> "Slight snow showers"
    86 -> "Heavy snow showers"
    95 -> "Thunderstorm"
    96 -> "Thunderstorm with slight hail"
    99 -> "Thunderstorm with heavy hail"
    else -> "Unknown"
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
