// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.ui.weather.chart

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartViewMathTest {
    private val hour = 3_600_000L
    private val day = 24 * hour
    // 2030-01-01T00:00Z
    private val t0 = 1_893_456_000_000L

    private fun hourly(start: Long, count: Int) = List(count) { start + it * hour }

    // chartHours

    @Test
    fun chartHoursStartsAtTheHourThatContainsNow() {
        val times = hourly(t0, 10)
        assertEquals(2 until 10, chartHours(times, t0 + 2 * hour + 20 * 60_000L))
        // On the hour, that hour is the current one
        assertEquals(3 until 10, chartHours(times, t0 + 3 * hour))
    }

    @Test
    fun chartHoursIsCappedAt240() {
        val times = hourly(t0, 11 * 24)
        val range = chartHours(times, t0 + 5 * hour + 1)
        assertEquals(5, range.first)
        assertEquals(240, range.count())
        assertEquals(3, chartHours(times, t0, max = 3).count())
    }

    @Test
    fun chartHoursIsEmptyWhenEveryHourHasEnded() {
        assertTrue(chartHours(hourly(t0, 3), t0 + 3 * hour).isEmpty())
        assertTrue(chartHours(emptyList(), t0).isEmpty())
    }

    // chartDaySpans

    @Test
    fun chartDaySpansGroupsByLocalDay() {
        // 30 hours from 22:00 UTC; at UTC+3 the first local day starts at 01:00 and has 23 hours
        val times = hourly(t0 - 2 * hour, 30)
        val spans = chartDaySpans(times, 3 * 3600)
        assertEquals(listOf(0 to 23, 23 to 30), spans.map { it.startIndex to it.endIndex })
        // The second day starts at local midnight: 21:00 UTC
        assertEquals(t0 + day - 3 * hour, spans[1].date)
        assertEquals(times[23], spans[1].date)
    }

    @Test
    fun chartDaySpansWithANegativeOffset() {
        val times = hourly(t0, 24)
        val spans = chartDaySpans(times, -5 * 3600)
        // 00:00 UTC is 19:00 the day before at UTC-5
        assertEquals(listOf(0 to 5, 5 to 24), spans.map { it.startIndex to it.endIndex })
        assertEquals(t0 + 5 * hour, spans[1].date)
    }

    @Test
    fun chartDaySpansKeepsDaysOf23And25Hours() {
        // Local times read with one offset, as the mapper does: a change to summer time leaves out one hour,
        // a change back gives one hour two times
        val short = hourly(t0, 2) + hourly(t0 + 3 * hour, 21) + hourly(t0 + day, 2)
        assertEquals(listOf(23, 2), chartDaySpans(short, 0).map { it.endIndex - it.startIndex })
        val long = hourly(t0, 3) + hourly(t0 + 2 * hour, 22) + hourly(t0 + day, 2)
        assertEquals(listOf(25, 2), chartDaySpans(long, 0).map { it.endIndex - it.startIndex })
    }

    @Test
    fun chartDaySpansIsEmptyWithoutHours() {
        assertTrue(chartDaySpans(emptyList(), 0).isEmpty())
    }

    // chartMidnights

    @Test
    fun chartMidnightsGivesTheFirstHourOfEachLocalDayAfterTheFirst() {
        // 30 hours from 22:00 UTC; at UTC+3 local midnight is hour 23
        assertEquals(listOf(23), chartMidnights(hourly(t0 - 2 * hour, 30), 3 * 3600))
        // 50 hours from 00:00 UTC: the first hour is a midnight, but it is not a change of day
        assertEquals(listOf(24, 48), chartMidnights(hourly(t0, 50), 0))
    }

    @Test
    fun chartMidnightsIsEmptyInOneDay() {
        assertTrue(chartMidnights(hourly(t0 + hour, 10), 0).isEmpty())
        assertTrue(chartMidnights(emptyList(), 0).isEmpty())
    }

    // chartNightRuns

    @Test
    fun chartNightRunsGroupsTheNightHours() {
        val night = listOf(true, true, false, false, true, true, true, false, true)
        assertEquals(listOf(0..1, 4..6, 8..8), chartNightRuns(night))
    }

    @Test
    fun chartNightRunsIsEmptyWithoutNight() {
        assertTrue(chartNightRuns(listOf(false, false)).isEmpty())
        assertTrue(chartNightRuns(emptyList()).isEmpty())
    }

    // chartDaylight

    @Test
    fun chartDaylightUsesEachDaysSunriseAndSunset() {
        val sunrises = listOf(t0 + 6 * hour, t0 + day + 6 * hour)
        val sunsets = listOf(t0 + 18 * hour, t0 + day + 18 * hour)
        val times = listOf(t0 + 3 * hour, t0 + 12 * hour, t0 + 20 * hour, t0 + day + 7 * hour, t0 + day + 23 * hour)
        assertEquals(listOf(true, false, true, false, true), chartDaylight(times, sunrises, sunsets))
    }

    @Test
    fun chartDaylightInAPolarNightIsAlwaysNight() {
        assertEquals(listOf(true, true), chartDaylight(listOf(t0 + hour, t0 + 12 * hour), listOf(t0), listOf(t0)))
        // No sun times at all: night
        assertEquals(listOf(true), chartDaylight(listOf(t0), emptyList(), emptyList()))
    }

    // chartVisibleRange

    @Test
    fun chartVisibleRangeAddsTheOverscanInsideTheHours() {
        assertEquals(0 until 7, chartVisibleRange(0f, 100f, 20f, 240))
        assertEquals(3 until 12, chartVisibleRange(100f, 100f, 20f, 240))
        assertEquals(5 until 10, chartVisibleRange(100f, 100f, 20f, 240, overscan = 0))
        assertEquals(233 until 240, chartVisibleRange(4700f, 100f, 20f, 240))
    }

    @Test
    fun chartVisibleRangeIsEmptyWithoutHours() {
        assertTrue(chartVisibleRange(0f, 100f, 20f, 0).isEmpty())
        assertTrue(chartVisibleRange(0f, 100f, 0f, 10).isEmpty())
        // A viewport of 0 still draws the first hour
        assertEquals(0 until 1, chartVisibleRange(0f, 0f, 20f, 10, overscan = 0))
    }

    // chartIndexForX

    @Test
    fun chartIndexForXFindsTheHourUnderX() {
        assertEquals(0, chartIndexForX(0f, 28f, 240))
        assertEquals(0, chartIndexForX(27.9f, 28f, 240))
        assertEquals(1, chartIndexForX(28f, 28f, 240))
        assertEquals(0, chartIndexForX(-10f, 28f, 240))
        assertEquals(239, chartIndexForX(100_000f, 28f, 240))
        assertEquals(-1, chartIndexForX(10f, 28f, 0))
    }

    // chartDomain

    @Test
    fun chartDomainRoundsToNiceTicksAndIgnoresNulls() {
        val d = chartDomain(listOf(12.3, null, 23.1, Double.NaN, 17.0), fromZero = false, minSpan = 5.0)!!
        assertEquals(10.0, d.min, 0.0)
        assertEquals(25.0, d.max, 0.0)
        assertEquals(listOf(10.0, 15.0, 20.0, 25.0), d.ticks)
    }

    @Test
    fun chartDomainKeepsTheSmallestSpanAroundTheMiddle() {
        val d = chartDomain(listOf(1013.0, 1014.0), fromZero = false, minSpan = 4.0)!!
        assertTrue(d.min <= 1011.5 && d.max >= 1015.5)
        assertEquals(d.ticks.first(), d.min, 0.0)
        assertEquals(d.ticks.last(), d.max, 0.0)
    }

    @Test
    fun chartDomainFromZero() {
        val dry = chartDomain(listOf(0.0, 0.0), fromZero = true, minSpan = 1.0)!!
        assertEquals(0.0, dry.min, 0.0)
        assertEquals(1.0, dry.max, 0.0)
        assertEquals(listOf(0.0, 0.5, 1.0), dry.ticks)
        val wet = chartDomain(listOf(0.2, 7.4), fromZero = true, minSpan = 1.0)!!
        assertEquals(listOf(0.0, 5.0, 10.0), wet.ticks)
    }

    @Test
    fun chartDomainStepsAreOneTwoOrFiveTimesAPowerOfTen() {
        // A step of 2.5 would give 1017.5 and 1022.5
        val pressure = chartDomain(listOf(1015.2, 1021.8), fromZero = false, minSpan = 4.0)!!
        assertEquals(listOf(1015.0, 1020.0, 1025.0), pressure.ticks)
        // Temperatures with the smallest spans of the chart view keep whole ticks
        for (lo in -30..40) {
            for ((span, minSpan) in listOf(0.0 to 5.0, 3.3 to 5.0, 7.0 to 5.0, 0.0 to 9.0, 11.0 to 9.0)) {
                val d = chartDomain(listOf(lo + 0.3, lo + 0.3 + span), fromZero = false, minSpan = minSpan)!!
                d.ticks.forEach { assertEquals(Math.rint(it), it, 0.0) }
            }
        }
    }

    @Test
    fun chartDomainHasNoFloatingPointNoise() {
        val d = chartDomain(listOf(0.0, 0.3), fromZero = true, minSpan = 0.04)!!
        assertEquals(listOf(0.0, 0.1, 0.2, 0.3), d.ticks)
    }

    @Test
    fun chartDomainIsNullWithoutValues() {
        assertNull(chartDomain(listOf(null, Double.NaN), fromZero = false, minSpan = 1.0))
        assertNull(chartDomain(emptyList(), fromZero = true, minSpan = 1.0))
    }

    @Test
    fun chartDomainOfOneValueWithoutMinimumSpan() {
        val d = chartDomain(listOf(5.0), fromZero = false, minSpan = 0.0)!!
        assertTrue(d.max > d.min)
    }

    // chartZoom

    @Test
    fun chartZoomKeepsTheHourAtTheAnchor() {
        // Hour 10.5 is at x = 100 (scroll 194 + 100 = 294 = 10.5 * 28)
        val z = chartZoom(28f, 2f, 100f, 194f)
        assertEquals(56f, z.slotPx, 0f)
        assertEquals(10.5f, (z.scrollPx + 100f) / z.slotPx, 1e-4f)
    }

    @Test
    fun chartZoomStaysInsideTheLimits() {
        assertEquals(64f, chartZoom(28f, 10f, 0f, 0f).slotPx, 0f)
        assertEquals(12f, chartZoom(28f, 0.1f, 0f, 0f).slotPx, 0f)
        assertEquals(30f, chartZoom(28f, 2f, 0f, 0f, min = 10f, max = 30f).slotPx, 0f)
        // Back to the default
        assertEquals(28f, chartZoom(50f, 28f / 50f, 0f, 0f).slotPx, 1e-4f)
    }

    @Test
    fun chartZoomScrollIsNotLessThanZero() {
        assertEquals(0f, chartZoom(28f, 0.5f, 10f, 0f).scrollPx, 0f)
    }

    // The centre line

    @Test
    fun chartCenterIndexIsTheHourUnderTheCentreLine() {
        // The view opens with the middle of the first hour under the line
        assertEquals(0, chartCenterIndex(chartScrollForIndex(0, 28f), 28f, 240))
        assertEquals(0, chartCenterIndex(27.9f, 28f, 240))
        assertEquals(1, chartCenterIndex(28f, 28f, 240))
        assertEquals(10, chartCenterIndex(chartScrollForIndex(10, 28f), 28f, 240))
        // At the ends of the range: the first and the last hour
        assertEquals(0, chartCenterIndex(chartScrollRange(28f, 240).start, 28f, 240))
        assertEquals(239, chartCenterIndex(chartScrollRange(28f, 240).endInclusive, 28f, 240))
        assertEquals(-1, chartCenterIndex(0f, 28f, 0))
    }

    @Test
    fun chartScrollForIndexPutsTheMiddleOfTheHourUnderTheLine() {
        assertEquals(14f, chartScrollForIndex(0, 28f), 0f)
        assertEquals(98f, chartScrollForIndex(3, 28f), 0f)
        assertEquals(6.0f, chartScrollForIndex(0, 12f), 0f)
    }

    @Test
    fun chartScrollRangeLetsTheFirstAndLastHourReachTheLine() {
        val range = chartScrollRange(28f, 240)
        assertEquals(14f, range.start, 0f)
        assertEquals(239.5f * 28f, range.endInclusive, 0f)
        // One hour, or none: the range is one point
        assertEquals(14f, chartScrollRange(28f, 1).endInclusive, 0f)
        assertEquals(14f, chartScrollRange(28f, 0).endInclusive, 0f)
    }

    @Test
    fun chartZoomAroundTheCentreLineKeepsTheSelectedHour() {
        val scroll = chartScrollForIndex(37, 28f) + 5f
        for (factor in listOf(0.3f, 0.8f, 1.5f, 3f)) {
            val z = chartZoom(28f, factor, 0f, scroll)
            assertEquals(37, chartCenterIndex(z.scrollPx, z.slotPx, 240))
        }
    }
}
