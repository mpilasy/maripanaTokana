package orinasa.njarasoa.maripanatokana.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class WindSpeedTest {

    private val epsilon = 0.00001

    @Test
    fun fromMetersPerSecond_createsCorrectValue() {
        val ms = 5.0
        val windSpeed = WindSpeed.fromMetersPerSecond(ms)
        assertEquals(ms, windSpeed.metersPerSecond, epsilon)
    }

    @Test
    fun fromMph_createsCorrectValue() {
        val mph = 11.1847 // 5.0 * 2.23694
        val windSpeed = WindSpeed.fromMph(mph)
        assertEquals(5.0, windSpeed.metersPerSecond, epsilon)
    }

    @Test
    fun mph_property_conversionIsCorrect() {
        val ms = 10.0
        val expectedMph = 22.3694
        val windSpeed = WindSpeed.fromMetersPerSecond(ms)
        assertEquals(expectedMph, windSpeed.mph, epsilon)
    }

    @Test
    fun displayMetric_isFormattedCorrectly() {
        val windSpeed = WindSpeed.fromMetersPerSecond(5.24)
        assertEquals("5.2 m/s", windSpeed.displayMetric())
    }

    @Test
    fun displayImperial_isFormattedCorrectly() {
        // 5.0 m/s * 2.23694 = 11.1847 mph -> 11.2 mph
        val windSpeed = WindSpeed.fromMetersPerSecond(5.0)
        assertEquals("11.2 mph", windSpeed.displayImperial())
    }

    @Test
    fun displayDual_returnsBothUnits() {
        val windSpeed = WindSpeed.fromMetersPerSecond(5.0)
        assertEquals("5.0 m/s / 11.2 mph", windSpeed.displayDual())
    }

    @Test
    fun displayDual_withMetricPrimary_returnsCorrectPair() {
        val windSpeed = WindSpeed.fromMetersPerSecond(5.0)
        val (primary, secondary) = windSpeed.displayDual(metricPrimary = true)
        assertEquals("5.0 m/s", primary)
        assertEquals("11.2 mph", secondary)
    }

    @Test
    fun displayDual_withImperialPrimary_returnsCorrectPair() {
        val windSpeed = WindSpeed.fromMetersPerSecond(5.0)
        val (primary, secondary) = windSpeed.displayDual(metricPrimary = false)
        assertEquals("11.2 mph", primary)
        assertEquals("5.0 m/s", secondary)
    }

    @Test
    fun formatting_usesLocaleUS() {
        // In some locales (e.g. French), decimal separator is a comma.
        // WindSpeed should always use a dot.
        val windSpeed = WindSpeed.fromMetersPerSecond(5.2)

        // Force a non-US default locale to test if WindSpeed correctly uses Locale.US
        val originalLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.FRANCE)
            assertEquals("5.2 m/s", windSpeed.displayMetric())
            assertEquals("11.6 mph", windSpeed.displayImperial())
        } finally {
            Locale.setDefault(originalLocale)
        }
    }

    @Test
    fun zero_value_isHandled() {
        val windSpeed = WindSpeed.fromMetersPerSecond(0.0)
        assertEquals(0.0, windSpeed.metersPerSecond, epsilon)
        assertEquals(0.0, windSpeed.mph, epsilon)
        assertEquals("0.0 m/s", windSpeed.displayMetric())
        assertEquals("0.0 mph", windSpeed.displayImperial())
    }
}
