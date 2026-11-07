// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.domain.model

import kotlinx.serialization.Serializable
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * The normal high and low temperature (°C) for each day of the year. Index 0 is January 1 and index 365 is
 * December 31, numbered as in a leap year, so February 29 is index 59 every year. Null where there is no data.
 */
@Serializable
data class DailyNormals(val max: List<Double?>, val min: List<Double?>)

/** The normal high and low temperature (°C) of one day. */
data class NormalTemps(val high: Double, val low: Double)

/** The hero shows today's high against the normal high when they differ by this much or more (°C). */
const val NORMAL_DELTA_MIN_C = 2.0

/** Days on each side of a day that go into its normal. */
private const val NORMALS_HALF_WINDOW = 7

private const val DAYS_IN_LEAP_YEAR = 366

/** Days before each month in a leap year. */
private val LEAP_MONTH_STARTS = intArrayOf(0, 31, 60, 91, 121, 152, 182, 213, 244, 274, 305, 335)

/** The day of the year of an ISO date ("2024-03-01"), 1–366, numbered as in a leap year. Null for a bad date. */
fun dayOfYear(date: String): Int? {
    if (date.length < 10) return null
    val month = date.substring(5, 7).toIntOrNull() ?: return null
    val day = date.substring(8, 10).toIntOrNull() ?: return null
    if (month !in 1..12 || day !in 1..31) return null
    return LEAP_MONTH_STARTS[month - 1] + day
}

/**
 * The normals from daily highs and lows of many years (ERA5). Each day's normal is the mean of all values in a
 * window of ±7 days around it; the window wraps around the year end. February 29 has a slot of its own, filled
 * by the days near it (February 28, March 1 and the rest of the window). Missing values are skipped.
 */
fun dailyNormals(dates: List<String>, max: List<Double?>, min: List<Double?>): DailyNormals {
    val maxSum = DoubleArray(DAYS_IN_LEAP_YEAR)
    val maxCount = IntArray(DAYS_IN_LEAP_YEAR)
    val minSum = DoubleArray(DAYS_IN_LEAP_YEAR)
    val minCount = IntArray(DAYS_IN_LEAP_YEAR)
    dates.forEachIndexed { i, date ->
        val slot = (dayOfYear(date) ?: return@forEachIndexed) - 1
        max.getOrNull(i)?.takeIf { it.isFinite() }?.let { maxSum[slot] += it; maxCount[slot]++ }
        min.getOrNull(i)?.takeIf { it.isFinite() }?.let { minSum[slot] += it; minCount[slot]++ }
    }
    fun windowMeans(sum: DoubleArray, count: IntArray): List<Double?> = (0 until DAYS_IN_LEAP_YEAR).map { slot ->
        var s = 0.0
        var n = 0
        for (offset in -NORMALS_HALF_WINDOW..NORMALS_HALF_WINDOW) {
            val k = Math.floorMod(slot + offset, DAYS_IN_LEAP_YEAR)
            s += sum[k]
            n += count[k]
        }
        if (n > 0) s / n else null
    }
    return DailyNormals(windowMeans(maxSum, maxCount), windowMeans(minSum, minCount))
}

/** The normals of an ISO date ("2026-11-07"); null when the date is bad or has no data. */
fun normalFor(normals: DailyNormals, date: String): NormalTemps? {
    val slot = (dayOfYear(date) ?: return null) - 1
    val high = normals.max.getOrNull(slot) ?: return null
    val low = normals.min.getOrNull(slot) ?: return null
    return NormalTemps(high, low)
}

/** The ISO date ("2026-11-07") at the place of the instant [epochMillis], for a place [utcOffsetSeconds] from UTC. */
fun localDate(epochMillis: Long, utcOffsetSeconds: Int): String {
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    calendar.timeInMillis = epochMillis + utcOffsetSeconds * 1000L
    return "%04d-%02d-%02d".format(
        Locale.US,
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH) + 1,
        calendar.get(Calendar.DAY_OF_MONTH),
    )
}
