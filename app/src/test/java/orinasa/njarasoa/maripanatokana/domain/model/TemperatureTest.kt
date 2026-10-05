package orinasa.njarasoa.maripanatokana.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class TemperatureTest {

    @Test
    fun nonFiniteValues_renderPlaceholder() {
        for (v in listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)) {
            val t = Temperature.fromCelsius(v)
            assertEquals("--°C", t.displayCelsius())
            assertEquals("--°C", t.displayCelsius(1))
            assertEquals("--°F", t.displayFahrenheit())
            assertEquals("--°F", t.displayFahrenheit(1))
        }
    }

    @Test
    fun finiteValues_formatUnchanged() {
        assertEquals("20°C", Temperature.fromCelsius(20.0).displayCelsius())
        assertEquals("68°F", Temperature.fromCelsius(20.0).displayFahrenheit())
    }
}
