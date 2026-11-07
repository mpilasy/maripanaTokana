// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.domain.model

import kotlinx.serialization.Serializable

/**
 * The sea near a coastal place, from the Open-Meteo marine model. Heights and sea levels are in metres, periods
 * in seconds, directions in degrees the waves come from (as for the wind). [tideTimes] (epoch millis) and
 * [tideLevels] are the hourly sea level, for the next high and low tide.
 */
@Serializable
data class SeaConditions(
    val waveHeight: Double? = null,
    val wavePeriod: Double? = null,
    val waveDirection: Int? = null,
    val swellHeight: Double? = null,
    val swellPeriod: Double? = null,
    val swellDirection: Int? = null,
    val seaTemperature: Temperature? = null,
    val seaLevel: Double? = null,
    val tideTimes: List<Long> = emptyList(),
    val tideLevels: List<Double?> = emptyList(),
)

/** A high or low tide: its time (epoch millis) and its sea level (m). */
data class TidePoint(val time: Long, val level: Double)

/** The next high and the next low tide; null when the data does not have one. */
data class Tides(val high: TidePoint?, val low: TidePoint?)

/**
 * The first high and the first low tide after [now], found as local extremes of the hourly sea level.
 * A high is higher than the hour before it and not lower than the hour after it; a low is the reverse.
 */
fun nextTides(times: List<Long>, levels: List<Double?>, now: Long): Tides {
    var high: TidePoint? = null
    var low: TidePoint? = null
    for (i in 1 until minOf(times.size, levels.size) - 1) {
        if (times[i] <= now) continue
        val before = levels[i - 1] ?: continue
        val level = levels[i] ?: continue
        val after = levels[i + 1] ?: continue
        if (high == null && level > before && level >= after) high = TidePoint(times[i], level)
        if (low == null && level < before && level <= after) low = TidePoint(times[i], level)
        if (high != null && low != null) break
    }
    return Tides(high, low)
}

/** Metres to feet. */
fun metersToFeet(meters: Double): Double = meters * 3.28084
