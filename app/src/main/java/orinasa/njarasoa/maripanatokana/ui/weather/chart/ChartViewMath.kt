// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.ui.weather.chart

import orinasa.njarasoa.maripanatokana.domain.model.isDaytime
import java.math.BigDecimal
import java.math.MathContext
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

// Pure functions of the chart view and the charts on the main page. The web has the same functions with the same
// rules in utils/chartView.ts.

private const val HOUR = 3_600_000L
private const val DAY = 24 * HOUR

/** The most hours that the chart view shows (10 days). */
const val CHART_MAX_HOURS = 240

/** The width of one hour, in dp. */
const val CHART_SLOT = 28f

/**
 * The hours to show: from the hour that contains [nowMs] (the first hour that has not ended), at most [max].
 * [times] are the start of each hour in epoch millis, in order. Empty when every hour has ended.
 */
fun chartHours(times: List<Long>, nowMs: Long, max: Int = CHART_MAX_HOURS): IntRange {
    val found = times.indexOfFirst { it + HOUR > nowMs }
    val start = if (found == -1) times.size else found
    return start until minOf(times.size, start + maxOf(0, max))
}

/** One local day of the hours: [startIndex] is in it, [endIndex] is not; [date] is its local midnight (epoch millis). */
data class ChartDaySpan(val startIndex: Int, val endIndex: Int, val date: Long)

/**
 * The hours in groups of one local day each, in order. A day is the place's calendar day (UTC + [utcOffsetSec]).
 * A day can have fewer or more than 24 hours (the first and last day, a change to or from summer time).
 */
fun chartDaySpans(times: List<Long>, utcOffsetSec: Int): List<ChartDaySpan> {
    val offset = utcOffsetSec * 1000L
    val spans = mutableListOf<ChartDaySpan>()
    times.forEachIndexed { i, t ->
        val date = Math.floorDiv(t + offset, DAY) * DAY - offset
        val last = spans.lastOrNull()
        if (last != null && last.date == date) spans[spans.size - 1] = last.copy(endIndex = i + 1)
        else spans += ChartDaySpan(i, i + 1, date)
    }
    return spans
}

/** The first hour of each local day after the first one: where a chart draws a midnight line. */
fun chartMidnights(times: List<Long>, utcOffsetSec: Int): List<Int> =
    chartDaySpans(times, utcOffsetSec).drop(1).map { it.startIndex }

/** The hour of the highest ([highIndex]) and of the lowest ([lowIndex]) value of one day. */
data class ChartDayExtremes(val highIndex: Int, val lowIndex: Int)

/**
 * The highest and lowest value of each day in [daySpans] that has at least [minHours] hours, in order. Nulls and values
 * that are not numbers are ignored. With ties, the middle one of the tied hours, as in the temperature chart of the
 * main page. A day with fewer hours, or with no value, gives nothing.
 */
fun chartDayExtremes(values: List<Double?>, daySpans: List<ChartDaySpan>, minHours: Int = 12): List<ChartDayExtremes> =
    daySpans.mapNotNull { span ->
        if (span.endIndex - span.startIndex < minHours) return@mapNotNull null
        val valid = (span.startIndex until span.endIndex).filter { values.getOrNull(it)?.isFinite() == true }
        if (valid.isEmpty()) return@mapNotNull null
        val hi = valid.maxOf { values[it]!! }
        val lo = valid.minOf { values[it]!! }
        val highs = valid.filter { values[it] == hi }
        val lows = valid.filter { values[it] == lo }
        ChartDayExtremes(highs[highs.size / 2], lows[lows.size / 2])
    }

/** The runs of night hours in [night] (true at night), each from its first to its last hour, in order. */
fun chartNightRuns(night: List<Boolean>): List<IntRange> {
    val runs = mutableListOf<IntRange>()
    var start = -1
    for (i in 0..night.size) {
        val isNight = i < night.size && night[i]
        if (isNight && start < 0) start = i
        if (!isNight && start >= 0) {
            runs += start until i
            start = -1
        }
    }
    return runs
}

/**
 * Whether each hour is at night, with the same rule as the hourly forecast: the hour takes the last sunrise
 * at or before it, and it is night when it is not between that sunrise and the sunset of the same day.
 * An hour before the first sunrise uses the first day.
 */
fun chartDaylight(times: List<Long>, sunrises: List<Long>, sunsets: List<Long>): List<Boolean> =
    times.map { t ->
        val day = sunrises.indexOfLast { it <= t }.coerceAtLeast(0)
        !isDaytime(t, sunrises.getOrElse(day) { 0L }, sunsets.getOrElse(day) { 0L })
    }

/**
 * The hours to draw: the hours in view (scrolled [scrollPx], [viewportPx] wide, [slotPx] an hour) and [overscan]
 * more on each side, kept inside 0 to [count].
 */
fun chartVisibleRange(scrollPx: Float, viewportPx: Float, slotPx: Float, count: Int, overscan: Int = 2): IntRange {
    if (count <= 0 || slotPx <= 0f) return IntRange.EMPTY
    val first = floor(max(0f, scrollPx) / slotPx).toInt()
    val last = ceil((max(0f, scrollPx) + max(0f, viewportPx)) / slotPx).toInt()
    return max(0, first - overscan) until min(count, max(first + 1, last) + overscan)
}

/** The hour at [x] px from the start of the hours ([slotPx] an hour), kept inside 0 to [count] - 1; -1 when there is no hour. */
fun chartIndexForX(x: Float, slotPx: Float, count: Int): Int {
    if (count <= 0 || slotPx <= 0f) return -1
    return floor(x / slotPx).toInt().coerceIn(0, count - 1)
}

/** A vertical scale: the bottom and top values and the values that get a label (from bottom to top). */
data class ChartDomain(val min: Double, val max: Double, val ticks: List<Double>)

/** A step of 1, 2 or 5 times a power of 10, at least [rough]. No 2.5: a scale in whole units keeps whole ticks. */
private fun niceStep(rough: Double): Double {
    val power = 10.0.pow(floor(log10(rough)))
    val step = listOf(1.0, 2.0, 5.0, 10.0).firstOrNull { it * power >= rough * (1 - 1e-9) } ?: 10.0
    return step * power
}

/** A value with the floating-point noise of a sum of steps removed. */
private fun clean(v: Double): Double = BigDecimal(v).round(MathContext(12)).toDouble()

/**
 * The vertical scale of [values] (nulls and values that are not numbers are ignored). The scale is at least [minSpan]
 * high (around the middle of the values; up from 0 with [fromZero]), and its ends are on round ticks, about 3 steps
 * apart. With [fromZero], the scale starts at 0 or lower. Null when there is no value.
 */
fun chartDomain(values: List<Double?>, fromZero: Boolean, minSpan: Double): ChartDomain? {
    val valid = values.filterNotNull().filter { it.isFinite() }
    if (valid.isEmpty()) return null
    var lo = valid.min()
    var hi = valid.max()
    if (fromZero) lo = min(lo, 0.0)
    val span = max(minSpan, 0.0)
    if (hi - lo < span) {
        if (fromZero && lo == 0.0) {
            hi = span
        } else {
            // Around the middle; also with fromZero when a value is under 0
            val mid = (lo + hi) / 2
            lo = mid - span / 2
            hi = mid + span / 2
        }
    }
    if (hi == lo) hi = lo + 1
    val step = niceStep((hi - lo) / 3)
    val bottom = clean(floor(lo / step + 1e-9) * step)
    val top = clean(ceil(hi / step - 1e-9) * step)
    val ticks = mutableListOf<Double>()
    var k = 0
    while (bottom + k * step <= top + step * 1e-9) ticks += clean(bottom + k++ * step)
    return ChartDomain(bottom, top, ticks)
}

// The selection line starts on the first hour at the start of the plot. While the hours scroll the first half plot
// width, it moves to the middle of the plot and stays there; at the end it moves on to the last hour at the end of the
// plot. So the plot never has empty space. The scroll is the start of the plot, in px from the start of the first hour.

/** The largest scroll, with [viewPx] the plot width: 0 when all the hours fit in the plot. */
fun chartMaxScroll(slotPx: Float, count: Int, viewPx: Float): Float = maxOf(0f, count * slotPx - viewPx)

/** The position of the selection line, in px from the start of the plot, at [scrollPx]. */
fun chartLineOffset(scrollPx: Float, slotPx: Float, count: Int, viewPx: Float): Float {
    val max = chartMaxScroll(slotPx, count, viewPx)
    // The distance from the middle of the first hour to the middle of the plot, and the scroll that moves the line on it
    val travel = maxOf(0f, (viewPx - slotPx) / 2)
    val ramp = minOf(travel, max / 2)
    if (ramp <= 0f) return slotPx / 2
    val s = scrollPx.coerceIn(0f, max)
    return slotPx / 2 + travel * (minOf(1f, s / ramp) + 1f - minOf(1f, (max - s) / ramp))
}

/** The hour under the selection line, at [scrollPx]; -1 when there is no hour. */
fun chartLineIndex(scrollPx: Float, slotPx: Float, count: Int, viewPx: Float): Int =
    chartIndexForX(scrollPx + chartLineOffset(scrollPx, slotPx, count, viewPx), slotPx, count)

/** The scroll that puts the middle of hour [index] under the selection line, kept inside the scroll range. */
fun chartScrollForIndex(index: Int, slotPx: Float, count: Int, viewPx: Float): Float {
    val max = chartMaxScroll(slotPx, count, viewPx)
    val travel = maxOf(0f, (viewPx - slotPx) / 2)
    val ramp = minOf(travel, max / 2)
    if (ramp <= 0f) return 0f
    // Three straight parts: the line moves to the middle, stays there, and moves to the end
    val x = index * slotPx
    val speed = 1f + travel / ramp
    val s = when {
        x <= ramp * speed -> x / speed
        x <= max - ramp + travel -> x - travel
        else -> (x - 2 * travel + travel * max / ramp) / speed
    }
    return s.coerceIn(0f, max)
}

/**
 * For each hour of [hourTimes], the index in [aqTimes] of the first air quality entry less than 30 minutes away, as the
 * hourly forecast matches them; null when no entry is near.
 */
fun chartAirQuality(hourTimes: List<Long>, aqTimes: List<Long>): List<Int?> =
    hourTimes.map { t -> aqTimes.indexOfFirst { kotlin.math.abs(it - t) < HOUR / 2 }.takeIf { it >= 0 } }

/**
 * The number of tiles in each row of the selection card, for [n] tiles: as few rows of at most 4 as possible, with the
 * tiles spread evenly and filled in order (9 gives 3, 3, 3; 7 gives 4, 3).
 */
fun chartTileRows(n: Int): List<Int> {
    if (n <= 0) return emptyList()
    val rows = (n + 3) / 4
    val perRow = (n + rows - 1) / rows
    return List(rows) { r -> minOf(perRow, n - r * perRow) }.filter { it > 0 }
}
