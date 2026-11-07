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

/** The default, smallest and largest width of one hour, in dp. */
const val CHART_SLOT_DEFAULT = 28f
const val CHART_SLOT_MIN = 12f
const val CHART_SLOT_MAX = 64f

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

/** A new hour width and scroll after a zoom. */
data class ChartZoom(val slotPx: Float, val scrollPx: Float)

/**
 * Zooms the hour width by [factor], kept inside [min] to [max], and gives the scroll that keeps the hour at [anchorX]
 * (px from the start of the view) in the same place. The scroll is not less than 0.
 */
fun chartZoom(
    slotPx: Float, factor: Float, anchorX: Float, scrollPx: Float,
    min: Float = CHART_SLOT_MIN, max: Float = CHART_SLOT_MAX,
): ChartZoom {
    val next = (slotPx * factor).coerceIn(min, max)
    val hourAtAnchor = (scrollPx + anchorX) / slotPx
    return ChartZoom(next, maxOf(0f, hourAtAnchor * next - anchorX))
}
