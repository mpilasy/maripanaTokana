// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ForecastChangesTest {
    private val hour = 60 * 60 * 1000L

    private fun day(date: String, high: Double = 20.0, rain: Double = 0.0, wind: Double = 10.0) =
        SnapshotDay(date, high, high - 10, rain, wind)

    private fun snapshot(vararg days: SnapshotDay, savedAt: Long = 0L) = ForecastSnapshot(savedAt, days.toList())

    @Test
    fun `no change gives nothing`() {
        val days = arrayOf(day("2026-11-07"), day("2026-11-08"), day("2026-11-09"))
        assertEquals(emptyList<ForecastChange>(), forecastChanges(snapshot(*days), snapshot(*days)))
    }

    @Test
    fun `the high changes from 3 degrees`() {
        val old = snapshot(day("2026-11-08", high = 20.0), day("2026-11-09", high = 20.0), day("2026-11-10", high = 20.0))
        val new = snapshot(day("2026-11-07"), day("2026-11-08", high = 23.0), day("2026-11-09", high = 17.5), day("2026-11-10", high = 22.9))
        // 2.5 and 2.9 degrees are not enough
        assertEquals(listOf(ForecastChange("2026-11-08", ForecastChange.Kind.HIGH_WARMER, 3.0)), forecastChanges(old, new))
    }

    @Test
    fun `rain now, dry now, more and less`() {
        val old = snapshot(
            day("2026-11-08", rain = 0.5), day("2026-11-09", rain = 3.0), day("2026-11-10", rain = 8.0),
            day("2026-11-11", rain = 10.0), day("2026-11-12", rain = 20.0),
        )
        val new = snapshot(
            day("2026-11-07"), day("2026-11-08", rain = 1.0), day("2026-11-09", rain = 0.9), day("2026-11-10", rain = 13.0),
            day("2026-11-11", rain = 4.0), day("2026-11-12", rain = 26.0),
        )
        assertEquals(
            listOf(
                ForecastChange("2026-11-08", ForecastChange.Kind.RAIN_NOW, 1.0),
                ForecastChange("2026-11-09", ForecastChange.Kind.DRY_NOW, 0.0),
                // 8 to 13 mm: 5 mm and 62 %
                ForecastChange("2026-11-10", ForecastChange.Kind.RAIN_MORE, 13.0),
                // 10 to 4 mm: 6 mm and 60 %
                ForecastChange("2026-11-11", ForecastChange.Kind.RAIN_LESS, 4.0),
                // 20 to 26 mm is only 30 %: no change
            ),
            forecastChanges(old, new),
        )
    }

    @Test
    fun `the wind changes from 15 km per h`() {
        val old = snapshot(day("2026-11-08", wind = 10.0), day("2026-11-09", wind = 40.0), day("2026-11-10", wind = 10.0))
        val new = snapshot(day("2026-11-07"), day("2026-11-08", wind = 25.0), day("2026-11-09", wind = 20.0), day("2026-11-10", wind = 24.0))
        assertEquals(
            listOf(
                ForecastChange("2026-11-08", ForecastChange.Kind.WIND_STRONGER, 25.0),
                ForecastChange("2026-11-09", ForecastChange.Kind.WIND_WEAKER, 20.0),
            ),
            forecastChanges(old, new),
        )
    }

    @Test
    fun `order is date, then high, rain, wind, and at most 4`() {
        val old = snapshot(day("2026-11-08"), day("2026-11-09"))
        val new = snapshot(
            day("2026-11-07"),
            day("2026-11-08", high = 25.0, rain = 5.0, wind = 30.0),
            day("2026-11-09", high = 10.0, rain = 5.0, wind = 30.0),
        )
        assertEquals(
            listOf(
                ForecastChange("2026-11-08", ForecastChange.Kind.HIGH_WARMER, 5.0),
                ForecastChange("2026-11-08", ForecastChange.Kind.RAIN_NOW, 5.0),
                ForecastChange("2026-11-08", ForecastChange.Kind.WIND_STRONGER, 30.0),
                ForecastChange("2026-11-09", ForecastChange.Kind.HIGH_COOLER, -10.0),
            ),
            forecastChanges(old, new),
        )
    }

    @Test
    fun `today, days missing from the old forecast and days after 6 days are left out`() {
        val dates = (7..20).map { "2026-11-%02d".format(it) }
        val old = snapshot(*dates.filter { it != "2026-11-09" }.map { day(it) }.toTypedArray())
        val new = snapshot(*dates.map { day(it, high = 30.0) }.toTypedArray())
        // Today (07) and 09 are not compared; 08, 10, 11, 12 are the first 4 of 08..13
        assertEquals(listOf("2026-11-08", "2026-11-10", "2026-11-11", "2026-11-12"), forecastChanges(old, new).map { it.date })
        val all = forecastChanges(old, snapshot(*dates.take(8).map { day(it, high = 30.0) }.toTypedArray()))
        assertEquals(4, all.size)
    }

    @Test
    fun `up changes are warmer, wetter and windier`() {
        val up = ForecastChange.Kind.entries.filter { isUpChange(ForecastChange("2026-11-08", it, 0.0)) }
        assertEquals(listOf(ForecastChange.Kind.HIGH_WARMER, ForecastChange.Kind.RAIN_NOW, ForecastChange.Kind.RAIN_MORE, ForecastChange.Kind.WIND_STRONGER), up)
    }

    @Test
    fun `the snapshot from yesterday is the one closest to 24 hours, from 18 to 36 hours old`() {
        val now = 100 * hour
        val snapshots = listOf(17, 19, 23, 30, 37).map { snapshot(savedAt = now - it * hour) }
        assertEquals(now - 23 * hour, snapshotToCompare(snapshots, now)!!.savedAt)
        assertNull(snapshotToCompare(listOf(snapshot(savedAt = now - 17 * hour), snapshot(savedAt = now - 37 * hour)), now))
        assertEquals(now - 36 * hour, snapshotToCompare(listOf(snapshot(savedAt = now - 36 * hour)), now)!!.savedAt)
    }

    @Test
    fun `the snapshot of a forecast uses the dates at the place and km per h`() {
        val data = WeatherData(
            temperature = Temperature.fromCelsius(20.0), feelsLike = Temperature.fromCelsius(20.0),
            tempMin = Temperature.fromCelsius(10.0), tempMax = Temperature.fromCelsius(25.0),
            weatherCode = 0, iconCode = "01d", locationName = "", pressure = Pressure.fromHPa(1013.0), humidity = 50,
            dewPoint = Temperature.fromCelsius(10.0), windSpeed = WindSpeed.fromMetersPerSecond(1.0), windDeg = 0,
            windGust = null, rain = null, snow = null, cloudCover = 0, uvIndex = 0.0, visibility = 10_000,
            sunrise = 0L, sunset = 0L,
            // Local midnight of 2026-11-07 at UTC+3 is 2026-11-06 21:00 UTC
            dailyForecast = listOf(
                DailyForecast(
                    date = 1_794_074_400_000L - 3 * hour, tempMax = Temperature.fromCelsius(25.0), tempMin = Temperature.fromCelsius(15.0),
                    weatherCode = 0, precipProbability = 0, windSpeed = WindSpeed.fromMetersPerSecond(5.0), windDirection = 0,
                    precipitation = Precipitation.fromMm(2.5), uvIndexMax = 5.0,
                ),
            ),
            utcOffsetSeconds = 3 * 3600,
        )
        assertEquals(
            ForecastSnapshot(42L, listOf(SnapshotDay("2026-11-07", 25.0, 15.0, 2.5, 18.0))),
            toSnapshot(data, 42L),
        )
    }
}
