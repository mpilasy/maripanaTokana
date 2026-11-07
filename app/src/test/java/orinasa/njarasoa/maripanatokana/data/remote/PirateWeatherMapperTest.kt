// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PirateWeatherMapperTest {

    @Test
    fun chartHourlyStartsAtTheCurrentHourWithGustsAndDewPoint() {
        val currentHour = System.currentTimeMillis() / 3_600_000L * 3_600 // epoch seconds
        // From 2 hours ago, 170 hours as with extend=hourly
        val hourly = (-2 until 168).map { i ->
            PirateWeatherHourly(
                time = currentHour + i * 3600L,
                temperature = 10.0,
                windGust = if (i == 0) null else 8.0,
                dewPoint = 3.0,
            )
        }
        val response = PirateWeatherResponse(
            currently = PirateWeatherCurrent(time = currentHour, temperature = 10.0, apparentTemperature = 9.0),
            hourly = PirateWeatherHourlyBlock(hourly),
        )

        val data = response.toDomain()

        assertEquals(168, data.chartHourly.size)
        assertEquals(currentHour * 1000, data.chartHourly.first().time)
        assertNull(data.chartHourly[0].windGust)
        assertEquals(8.0, data.chartHourly[1].windGust!!.metersPerSecond, 0.0)
        assertEquals(3.0, data.chartHourly[1].dewPoint!!.celsius, 0.0)
        assertEquals(24, data.hourlyForecast.size)
        // The 24 hours take the gust from the same hours
        assertEquals(8.0, data.hourlyForecast[1].windGust!!.metersPerSecond, 0.0)
    }
}
