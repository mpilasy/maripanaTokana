package orinasa.njarasoa.maripanatokana.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PrecipOutlookTest {
    private val h = 3_600_000L

    private fun hours(vararg codes: Int, probability: Int = 80) = codes.mapIndexed { i, code ->
        HourlyForecast(
            time = i * h,
            temperature = Temperature.fromCelsius(10.0),
            weatherCode = code,
            precipProbability = probability,
            windSpeed = WindSpeed.fromMetersPerSecond(1.0),
            windDirection = 0,
            pressure = Pressure.fromHPa(1013.0),
            precipitation = Precipitation.fromMm(0.0),
        )
    }

    private fun nowcast(vararg mm: Double) = mm.mapIndexed { i, v -> MinutelyForecast(time = i * 15 * 60_000L, precipitation = Precipitation.fromMm(v)) }

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
            PrecipOutlook.from(hours(61, 61, 61), nowcast(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)))
        // Hourly codes say dry, but the nowcast shows rain in the first hour
        assertEquals(PrecipOutlook(PrecipOutlook.Kind.STOPS, false, h),
            PrecipOutlook.from(hours(3, 3), nowcast(0.0, 0.4, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)))
    }

    @Test
    fun `is null without hourly data`() {
        assertNull(PrecipOutlook.from(emptyList()))
    }
}
