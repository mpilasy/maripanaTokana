// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.domain.model

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * The NWS heat index and wind chill, from the air temperature, the relative humidity and the wind.
 * Each is null outside the range where the NWS uses it.
 */
object ThermalIndices {

    /** The heat index (Rothfusz regression with the NWS adjustments), at 80 °F (27 °C) and above, when it is above the temperature. */
    fun heatIndex(temperature: Temperature, humidity: Int?): Temperature? {
        val t = temperature.fahrenheit
        val rh = humidity?.toDouble() ?: return null
        if (!t.isFinite() || t < 80) return null
        var hi = -42.379 + 2.04901523 * t + 10.14333127 * rh - 0.22475541 * t * rh - 0.00683783 * t * t -
            0.05481717 * rh * rh + 0.00122874 * t * t * rh + 0.00085282 * t * rh * rh - 0.00000199 * t * t * rh * rh
        if (rh < 13 && t <= 112) hi -= (13 - rh) / 4 * sqrt((17 - abs(t - 95)) / 17)
        if (rh > 85 && t <= 87) hi += (rh - 85) / 10 * ((87 - t) / 5)
        // In very dry air the index falls below the air temperature; then the source's "feels like" shows instead
        if (hi <= t) return null
        return Temperature.fromFahrenheit(hi)
    }

    /** The wind chill (NWS 2001 formula), at 50 °F (10 °C) and below, with wind over 3 mph (4.8 km/h). */
    fun windChill(temperature: Temperature, wind: WindSpeed): Temperature? {
        val t = temperature.fahrenheit
        val v = wind.mph
        if (!t.isFinite() || !v.isFinite() || t > 50 || v <= 3) return null
        val p = v.pow(0.16)
        return Temperature.fromFahrenheit(35.74 + 0.6215 * t - 35.75 * p + 0.4275 * t * p)
    }

    /**
     * Environment Canada's time for exposed skin to freeze at [windChill], as the band's (fewest, most) minutes.
     * Fewest is 0 for the "under 2 minutes" band. Null when the wind chill is above −28 °C (−18 °F).
     */
    fun frostbiteMinutes(windChill: Temperature): Pair<Int, Int>? {
        val c = windChill.celsius
        if (!c.isFinite()) return null
        return when (c.roundToInt()) {
            in -39..-28 -> 10 to 30
            in -47..-40 -> 5 to 10
            in -54..-48 -> 2 to 5
            in Int.MIN_VALUE..-55 -> 0 to 2
            else -> null
        }
    }

    /** The relative humidity (%) of air at [temperature] with [dewPoint] (Magnus formula); null when not known. */
    fun relativeHumidity(temperature: Temperature, dewPoint: Temperature?): Int? {
        val t = temperature.celsius
        val td = dewPoint?.celsius ?: return null
        if (!t.isFinite() || !td.isFinite()) return null
        fun saturation(c: Double) = exp(17.625 * c / (243.04 + c))
        return (100 * saturation(td) / saturation(t)).roundToInt().coerceIn(0, 100)
    }

    /**
     * A day's heat index and wind chill. The heat index uses the day's high, with the humidity at that
     * temperature from the day's mean dew point (it changes little during the day). The wind chill
     * uses the day's low with the day's wind.
     */
    fun heatIndex(day: DailyForecast): Temperature? = heatIndex(day.tempMax, relativeHumidity(day.tempMax, day.dewPoint))

    fun windChill(day: DailyForecast): Temperature? = windChill(day.tempMin, day.windSpeed)
}
