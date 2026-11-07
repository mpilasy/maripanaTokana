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

/**
 * Compares the current temperature with each forecast hour after now and up to 6 hours later, with the
 * exact values. Gives the hour with the largest difference (the earlier hour on a tie), or null when
 * that difference is less than 2 °C (4 °F when Fahrenheit is primary).
 */
fun temperatureTrend(
    current: Temperature,
    hourly: List<HourlyForecast>,
    metricPrimary: Boolean,
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
    // The same difference in the primary unit: °F degrees are 5/9 of °C degrees
    val primaryDiff = if (metricPrimary) abs(bestDiff) else abs(bestDiff) * 9 / 5
    if (best == null || primaryDiff < if (metricPrimary) 2 else 4) return null
    return TemperatureTrend(if (bestDiff > 0) TemperatureTrend.Direction.UP else TemperatureTrend.Direction.DOWN, best.time, best.temperature)
}
