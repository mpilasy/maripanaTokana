// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TemperatureTrendTest {
    private val now = 1_800_000_000_000L
    private val hour = 3_600_000L

    private fun hours(vararg celsius: Double) = celsius.mapIndexed { i, c ->
        HourlyForecast(
            time = now + (i + 1) * hour,
            temperature = Temperature.fromCelsius(c),
            weatherCode = 0,
            precipProbability = 0,
            windSpeed = WindSpeed.fromMetersPerSecond(0.0),
            windDirection = 0,
            pressure = Pressure.fromHPa(1013.0),
            precipitation = Precipitation.fromMm(0.0),
        )
    }

    private val current = Temperature.fromCelsius(12.0)

    @Test
    fun `takes the hour with the largest rise`() {
        val trend = temperatureTrend(current, hours(13.0, 15.0, 18.0, 17.0), nowMs = now)!!
        assertEquals(TemperatureTrend.Direction.UP, trend.direction)
        assertEquals(now + 3 * hour, trend.time)
        assertEquals(18.0, trend.temperature.celsius, 0.001)
    }

    @Test
    fun `takes the hour with the largest fall`() {
        val trend = temperatureTrend(current, hours(11.0, 6.0, 9.0), nowMs = now)!!
        assertEquals(TemperatureTrend.Direction.DOWN, trend.direction)
        assertEquals(now + 2 * hour, trend.time)
    }

    @Test
    fun `takes the earlier hour on a tie`() {
        val trend = temperatureTrend(current, hours(18.0, 6.0, 18.0), nowMs = now)!!
        assertEquals(TemperatureTrend.Direction.UP, trend.direction)
        assertEquals(now + hour, trend.time)
    }

    @Test
    fun `shows nothing for 5 degrees Celsius or less, whatever unit shows first`() {
        assertNull(temperatureTrend(current, hours(13.0, 17.0, 7.0), nowMs = now))
    }

    @Test
    fun `compares the exact values`() {
        // 12.4 shows as 12 and 17.3 as 17, but the exact difference is 4.9 °C
        assertNull(temperatureTrend(Temperature.fromCelsius(12.4), hours(17.3), nowMs = now))
        assertEquals(now + hour, temperatureTrend(current, hours(17.1), nowMs = now)?.time)
    }

    @Test
    fun `only looks at the hours after now and at most 6 hours later`() {
        val list = hours(30.0, 12.0, 12.0, 12.0, 12.0, 12.0, 12.0, 30.0)
        // The first hour is now, the 30 °C hour 8 hours later is past the window
        assertNull(temperatureTrend(current, list, nowMs = now + hour))
        assertNull(temperatureTrend(current, emptyList(), nowMs = now))
    }
}
