// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.domain.model

import kotlin.math.abs

/** The largest change of temperature in the next 6 hours, for the chip next to the current temperature. */
data class TemperatureTrend(
    val direction: Direction,
    /** The hour of the largest change, epoch millis. */
    val time: Long,
    val temperature: Temperature,
) {
    enum class Direction { UP, DOWN }
}

private const val HOUR = 3_600_000L

/** The chip shows only for a change over this many °C (9 °F). */
const val TREND_MIN_CELSIUS = 5.0

/**
 * Compares the current temperature with each forecast hour after now and up to 6 hours later, with the
 * exact values. Gives the hour with the largest difference (the earlier hour on a tie), or null when
 * that difference is not over [TREND_MIN_CELSIUS], whatever unit shows first.
 */
fun temperatureTrend(
    current: Temperature,
    hourly: List<HourlyForecast>,
    nowMs: Long = System.currentTimeMillis(),
): TemperatureTrend? {
    var best: HourlyForecast? = null
    var bestDiff = 0.0
    for (h in hourly) {
        if (h.time <= nowMs || h.time > nowMs + 6 * HOUR) continue
        val diff = h.temperature.celsius - current.celsius
        if (abs(diff) > abs(bestDiff)) {
            best = h
            bestDiff = diff
        }
    }
    if (best == null || abs(bestDiff) <= TREND_MIN_CELSIUS) return null
    return TemperatureTrend(if (bestDiff > 0) TemperatureTrend.Direction.UP else TemperatureTrend.Direction.DOWN, best.time, best.temperature)
}
