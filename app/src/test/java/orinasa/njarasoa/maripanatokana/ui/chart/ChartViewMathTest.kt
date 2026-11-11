// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.ui.chart

import orinasa.njarasoa.maripanatokana.domain.model.ChartHour
import orinasa.njarasoa.maripanatokana.domain.model.DailyForecast
import orinasa.njarasoa.maripanatokana.domain.model.Precipitation
import orinasa.njarasoa.maripanatokana.domain.model.Temperature
import orinasa.njarasoa.maripanatokana.domain.model.WindSpeed
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

    // The selection line (28 px an hour, 240 hours, a plot of 308 px: the line moves 140 px to the middle)

    @Test
    fun chartLineOffsetMovesFromTheStartToTheMiddleToTheEnd() {
        val max = chartMaxScroll(28f, 240, 308f)
        assertEquals(240 * 28f - 308f, max, 0f)
        // On the first hour at the start, in the middle after 140 px of scroll, on the last hour at the end
        assertEquals(14f, chartLineOffset(0f, 28f, 240, 308f), 0f)
        assertEquals(84f, chartLineOffset(70f, 28f, 240, 308f), 0f)
        assertEquals(154f, chartLineOffset(140f, 28f, 240, 308f), 0f)
        assertEquals(154f, chartLineOffset(3000f, 28f, 240, 308f), 0f)
        assertEquals(154f, chartLineOffset(max - 140f, 28f, 240, 308f), 0f)
        assertEquals(294f, chartLineOffset(max, 28f, 240, 308f), 0f)
    }

    @Test
    fun chartLineIndexIsTheFirstAndLastHourAtTheEnds() {
        assertEquals(0, chartLineIndex(0f, 28f, 240, 308f))
        assertEquals(239, chartLineIndex(chartMaxScroll(28f, 240, 308f), 28f, 240, 308f))
        assertEquals(-1, chartLineIndex(0f, 28f, 0, 308f))
    }

    @Test
    fun chartLineOffsetWithAShortScroll() {
        // 20 hours of 28 px in a plot of 308 px: 252 px of scroll, so the line gets to the middle at 126 px
        assertEquals(14f, chartLineOffset(0f, 28f, 20, 308f), 0f)
        assertEquals(154f, chartLineOffset(126f, 28f, 20, 308f), 0f)
        assertEquals(294f, chartLineOffset(252f, 28f, 20, 308f), 0f)
        // All the hours fit: no scroll, and the line stays on the first hour
        assertEquals(0f, chartMaxScroll(28f, 10, 308f), 0f)
        assertEquals(14f, chartLineOffset(0f, 28f, 10, 308f), 0f)
        assertEquals(0f, chartScrollForIndex(5, 28f, 10, 308f), 0f)
    }

    @Test
    fun chartScrollForIndexPutsTheMiddleOfTheHourUnderTheLine() {
        for (count in listOf(20, 240)) {
            for (i in listOf(0, 1, 3, 6, 10, count / 2, count - 7, count - 2, count - 1)) {
                val scroll = chartScrollForIndex(i, 28f, count, 308f)
                assertEquals((i + 0.5f) * 28f, scroll + chartLineOffset(scroll, 28f, count, 308f), 0.01f)
                assertEquals(i, chartLineIndex(scroll, 28f, count, 308f))
            }
        }
        assertEquals(0f, chartScrollForIndex(0, 28f, 240, 308f), 0f)
        assertEquals(chartMaxScroll(28f, 240, 308f), chartScrollForIndex(239, 28f, 240, 308f), 0.01f)
    }

    // chartDayExtremes

    @Test
    fun chartDayExtremesFindsTheHighAndLowOfEachLongDay() {
        val spans = listOf(ChartDaySpan(0, 24, 0L), ChartDaySpan(24, 48, day))
        val values = List(48) { 10.0 }.toMutableList<Double?>()
        values[5] = 20.0
        values[3] = 2.0
        values[30] = 25.0
        values[40] = -1.0
        assertEquals(listOf(ChartDayExtremes(5, 3), ChartDayExtremes(30, 40)), chartDayExtremes(values, spans))
    }

    @Test
    fun chartDayExtremesTakesTheMiddleOfTiedHours() {
        val spans = listOf(ChartDaySpan(0, 24, 0L))
        val values = MutableList<Double?>(24) { 10.0 }
        for (i in listOf(4, 5, 6, 7)) values[i] = 20.0
        for (i in listOf(10, 11, 12)) values[i] = 1.0
        // Tied 4..7: the index at size / 2 is 6; tied 10..12: 11
        assertEquals(listOf(ChartDayExtremes(6, 11)), chartDayExtremes(values, spans))
    }

    @Test
    fun chartDayExtremesSkipsShortDaysAndDaysWithoutValues() {
        val spans = listOf(ChartDaySpan(0, 11, 0L), ChartDaySpan(11, 12, day), ChartDaySpan(12, 36, 2 * day), ChartDaySpan(36, 60, 3 * day))
        val values = MutableList<Double?>(60) { it.toDouble() }
        // The day with 24 hours at 12..35 keeps only nulls and NaN; the next day has a value
        for (i in 12 until 36) values[i] = if (i % 2 == 0) null else Double.NaN
        assertEquals(listOf(ChartDayExtremes(59, 36)), chartDayExtremes(values, spans))
        // A day of exactly minHours counts; a lower minHours lets a short day in
        assertEquals(listOf(ChartDayExtremes(10, 0)), chartDayExtremes(values.take(11), listOf(ChartDaySpan(0, 11, 0L)), minHours = 11))
        assertTrue(chartDayExtremes(emptyList(), emptyList()).isEmpty())
    }

    // chartAirQuality

    @Test
    fun chartAirQualityMatchesEntriesAtMost30MinutesAway() {
        val hours = hourly(t0, 4)
        // An entry 29 minutes after the second hour, one exactly 30 minutes before the fourth hour, one on the first hour
        val aq = listOf(t0 + hour + 29 * 60_000L, t0 + 3 * hour - 30 * 60_000L, t0)
        assertEquals(listOf(2, 0, 1, 1), chartAirQuality(hours, aq))
    }

    @Test
    fun chartAirQualityGivesNullAtMoreThan30MinutesAway() {
        assertEquals(listOf<Int?>(null), chartAirQuality(listOf(t0), listOf(t0 + 31 * 60_000L, t0 - 31 * 60_000L)))
    }

    @Test
    fun chartAirQualityTakesTheFirstEntryAndGivesNullsWithoutData() {
        val hours = hourly(t0, 2)
        assertEquals(listOf(0, null), chartAirQuality(hours, listOf(t0 - 10 * 60_000L, t0 + 10 * 60_000L)))
        assertEquals(listOf(null, null), chartAirQuality(hours, emptyList()))
        assertTrue(chartAirQuality(emptyList(), listOf(t0)).isEmpty())
    }

    // chartDayMaxIndex

    @Test
    fun chartDayMaxIndexFindsTheHighestValueOfEachLocalDay() {
        // 48 hours from 00:00 UTC; at UTC+3 the first local day ends at 21:00 UTC (index 21)
        val times = hourly(t0, 48)
        val values = List<Double?>(48) { 1.0 }.toMutableList()
        values[20] = 9.0 // 23:00 local, day 1
        values[21] = 8.0 // 00:00 local, day 2
        values[30] = 8.0 // a tie in day 2: the first one wins
        val dates = listOf(t0 - 3 * hour, t0 + day - 3 * hour)
        assertEquals(listOf(20, 21), chartDayMaxIndex(dates, times, values, 3 * 3600))
    }

    @Test
    fun chartDayMaxIndexGivesNullForDaysWithoutValues() {
        val times = hourly(t0, 24)
        val values = List<Double?>(24) { if (it == 5) Double.NaN else null }
        assertEquals(listOf<Int?>(null, null), chartDayMaxIndex(listOf(t0, t0 + day), times, values, 0))
        assertEquals(listOf<Int?>(null), chartDayMaxIndex(listOf(t0), emptyList(), emptyList(), 0))
    }

    // chartDays

    @Test
    fun chartDaysTakesTheDayValuesAndTheHighestHourlyGust() {
        val day1 = DailyForecast(
            date = t0, tempMax = Temperature.fromCelsius(25.0), tempMin = Temperature.fromCelsius(14.0), weatherCode = 3,
            precipProbability = 60, windSpeed = WindSpeed.fromMetersPerSecond(7.0), windDirection = 90,
            precipitation = Precipitation.fromMm(4.0), uvIndexMax = 6.5, humidity = 70,
        )
        val day2 = day1.copy(date = t0 + day)
        val hours = listOf(
            chartHour(t0 + hour).copy(windGust = WindSpeed.fromMetersPerSecond(12.0)),
            chartHour(t0 + 5 * hour).copy(windGust = WindSpeed.fromMetersPerSecond(15.0)),
        )
        val days = chartDays(listOf(day1, day2), hours, 0)
        assertEquals(2, days.size)
        val d = days[0]
        assertEquals(t0, d.time)
        assertEquals(25.0, d.temperature!!.celsius, 1e-9)
        assertEquals(4.0, d.precipitation!!.mm, 1e-9)
        assertEquals(60, d.precipProbability)
        assertEquals(90, d.windDirection)
        assertEquals(15.0, d.windGust!!.metersPerSecond, 1e-9)
        assertEquals(6.5, d.uvIndex!!, 1e-9)
        assertEquals(70, d.humidity)
        assertNull(d.pressure)
        assertEquals(false, d.isNight)
        // The hours do not reach the second day: no gust
        assertNull(days[1].windGust)
    }

    // chartTiles

    private fun chartHour(time: Long) = ChartHour(
        time = time, temperature = Temperature.fromCelsius(10.0), dewPoint = null, weatherCode = 0, isNight = false,
        precipitation = null, precipProbability = null, windSpeed = null, windDirection = null, windGust = null,
        pressure = null, humidity = 50, uvIndex = null,
    )

    @Test
    fun chartTilesKeepsEachValueThatOneHourHas() {
        val hours = listOf(chartHour(0), chartHour(hour).copy(windGust = WindSpeed.fromMetersPerSecond(5.0), uvIndex = Double.NaN))
        assertEquals(listOf(ChartTile.TEMPERATURE, ChartTile.WIND_GUST, ChartTile.HUMIDITY), chartTiles(hours, hasAirQuality = false))
        assertEquals(
            listOf(ChartTile.TEMPERATURE, ChartTile.WIND_GUST, ChartTile.HUMIDITY, ChartTile.AIR_QUALITY),
            chartTiles(hours, hasAirQuality = true),
        )
        assertTrue(chartTiles(emptyList(), hasAirQuality = false).isEmpty())
    }

    // chartLabelFit

    // Each character is half the size wide
    private val measure = { text: String, size: Float -> text.length * size / 2 }

    @Test
    fun chartLabelFitKeepsTheLargestSizeThatFits() {
        assertEquals(ChartLabelFit(11f, listOf(1, 2)), chartLabelFit(listOf("Wind", "Tahan'ny tara-masoandro"), listOf(60f, 78f), measure))
    }

    @Test
    fun chartLabelFitMakesAllLabelsSmallerWhenAWordIsTooWide() {
        // "Precipitation" is 71.5 wide at 11 and 58.5 at 9
        assertEquals(ChartLabelFit(9f, listOf(1, 1)), chartLabelFit(listOf("Wind", "Precipitation"), listOf(60f, 60f), measure))
    }

    @Test
    fun chartLabelFitGivesTheSmallestSizeWhenNoSizeFits() {
        // 3 lines at every size
        assertEquals(ChartLabelFit(8f, listOf(2)), chartLabelFit(listOf("Air quality index"), listOf(40f), measure))
        assertEquals(ChartLabelFit(11f, emptyList()), chartLabelFit(emptyList(), emptyList(), measure))
    }

    // chartTileRows

    @Test
    fun chartTileRowsSpreadsTheTilesEvenly() {
        assertEquals(listOf(5, 4), chartTileRows(9))
        assertEquals(listOf(4, 4), chartTileRows(8))
        assertEquals(listOf(4, 3), chartTileRows(7))
        assertEquals(listOf(3, 3), chartTileRows(6))
        assertEquals(listOf(5), chartTileRows(5))
        assertEquals(listOf(1), chartTileRows(1))
        assertEquals(listOf(5, 5), chartTileRows(10))
        assertEquals(listOf(4, 4, 3), chartTileRows(11))
        assertTrue(chartTileRows(0).isEmpty())
    }
}
