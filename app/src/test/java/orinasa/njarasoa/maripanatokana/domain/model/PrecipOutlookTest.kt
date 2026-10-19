package orinasa.njarasoa.maripanatokana.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PrecipOutlookTest {
    private val h = 3_600_000L

    private fun hours(vararg wet: Boolean, code: Int = 61) = wet.mapIndexed { i, w ->
        HourlyForecast(
            time = i * h,
            temperature = Temperature.fromCelsius(10.0),
            weatherCode = if (w) code else 1,
            precipProbability = if (w) 80 else 10,
            windSpeed = WindSpeed.fromMetersPerSecond(1.0),
            windDirection = 0,
            pressure = Pressure.fromHPa(1013.0),
            precipitation = Precipitation.fromMm(if (w) 1.0 else 0.0),
        )
    }

    @Test
    fun `says when rain starts`() {
        assertEquals(PrecipOutlook(PrecipOutlook.Kind.STARTS, false, 2 * h), PrecipOutlook.from(hours(false, false, true)))
    }

    @Test
    fun `says when the current rain stops`() {
        assertEquals(PrecipOutlook(PrecipOutlook.Kind.STOPS, false, 2 * h), PrecipOutlook.from(hours(true, true, false)))
    }

    @Test
    fun `covers a dry or a wet day`() {
        assertEquals(PrecipOutlook(PrecipOutlook.Kind.DRY, false), PrecipOutlook.from(hours(false, false)))
        assertEquals(PrecipOutlook(PrecipOutlook.Kind.ALL_DAY, true), PrecipOutlook.from(hours(true, true, code = 73)))
    }

    @Test
    fun `ignores a trace of rain the model thinks unlikely`() {
        val trace = hours(false, false).map { it.copy(precipitation = Precipitation.fromMm(0.1), precipProbability = 2) }
        assertEquals(PrecipOutlook(PrecipOutlook.Kind.DRY, false), PrecipOutlook.from(trace))
    }

    @Test
    fun `is null without hourly data`() {
        assertNull(PrecipOutlook.from(emptyList()))
    }
}
