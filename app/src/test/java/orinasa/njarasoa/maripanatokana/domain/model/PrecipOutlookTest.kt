// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PrecipOutlookTest {
    private val h = 3_600_000L

    private val m = 60_000L

    private fun hours(vararg codes: Int, probability: Int = 80, from: Long = 0L) = codes.mapIndexed { i, code ->
        HourlyForecast(
            time = from + i * h,
            temperature = Temperature.fromCelsius(10.0),
            weatherCode = code,
            precipProbability = probability,
            windSpeed = WindSpeed.fromMetersPerSecond(1.0),
            windDirection = 0,
            pressure = Pressure.fromHPa(1013.0),
            precipitation = Precipitation.fromMm(0.0),
        )
    }

    private fun nowcast(vararg mm: Double, from: Long = 0L) =
        mm.mapIndexed { i, v -> MinutelyForecast(time = from + i * 15 * m, precipitation = Precipitation.fromMm(v)) }

    @Test
    fun `follows the weather codes, as the hour icons do`() {
        assertEquals(PrecipOutlook(PrecipOutlook.Kind.STARTS, false, 2 * h), PrecipOutlook.from(hours(3, 3, 61)))
        assertEquals(PrecipOutlook(PrecipOutlook.Kind.STOPS, false, 2 * h), PrecipOutlook.from(hours(61, 61, 3)))
        assertEquals(PrecipOutlook(PrecipOutlook.Kind.ALL_DAY, true), PrecipOutlook.from(hours(73, 73)))
    }

    @Test
    fun `ignores a high chance of rain for hours drawn as plain cloud`() {
        assertEquals(PrecipOutlook(PrecipOutlook.Kind.DRY, false), PrecipOutlook.from(hours(3, 3, 3, probability = 60)))
    }

    @Test
    fun `lets the nowcast decide the hours it covers`() {
        // Hourly codes say rain now, but the nowcast for the first two hours is dry
        assertEquals(PrecipOutlook(PrecipOutlook.Kind.STARTS, false, 2 * h),
            PrecipOutlook.from(hours(61, 61, 61), nowcast(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0), nowMs = 0L))
        // Hourly codes say dry, but the nowcast shows rain from the second quarter hour
        assertEquals(PrecipOutlook(PrecipOutlook.Kind.STARTS, false, 15 * m),
            PrecipOutlook.from(hours(3, 3), nowcast(0.0, 0.4, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0), nowMs = 0L))
    }

    @Test
    fun `takes now from the nowcast, not from the next full hour`() {
        // At 06:49 the hours start at 07:00 with drizzle until 11:00, and the nowcast has rain from 07:15:
        // the rain starts at 07:15; it does not stop at 11:00
        val now = 6 * h + 49 * m
        val outlook = PrecipOutlook.from(
            hours(51, 51, 51, 51, 3, 3, from = 7 * h),
            nowcast(0.0, 0.0, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, from = 6 * h + 45 * m),
            nowMs = now,
        )
        assertEquals(PrecipOutlook(PrecipOutlook.Kind.STARTS, false, 7 * h + 15 * m), outlook)
    }

    @Test
    fun `gives the stop time to the minute inside the nowcast`() {
        val outlook = PrecipOutlook.from(hours(61, 3, 3, from = h), nowcast(0.5, 0.5, 0.0, 0.0, from = 0L), nowMs = 5 * m)
        assertEquals(PrecipOutlook(PrecipOutlook.Kind.STOPS, false, 30 * m), outlook)
    }

    @Test
    fun `is null without hourly data`() {
        assertNull(PrecipOutlook.from(emptyList()))
    }

    @Test
    fun `the icon of the rain or snow now follows the weather code when it tells the kind`() {
        assertEquals(66, precipIconCode(66, rain = true, snow = false)) // freezing rain or sleet
        assertEquals(53, precipIconCode(53, rain = true, snow = false)) // drizzle
        assertEquals(99, precipIconCode(99, rain = true, snow = false)) // hail
        assertEquals(73, precipIconCode(73, rain = true, snow = true))
        assertEquals(71, precipIconCode(3, rain = false, snow = true)) // overcast code, snow falls
        assertEquals(61, precipIconCode(3, rain = true, snow = false))
        assertEquals(3, precipIconCode(3, rain = false, snow = false))
    }
}
