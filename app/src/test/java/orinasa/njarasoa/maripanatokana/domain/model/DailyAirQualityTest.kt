package orinasa.njarasoa.maripanatokana.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DailyAirQualityTest {
    private val h = 3_600_000L

    private fun hours(from: Int, count: Int, us: (Int) -> Int) =
        (0 until count).map { HourlyAirQuality(time = (from + it) * h, usValue = us(it), europeanValue = 10 + it) }

    @Test
    fun `takes each day's highest value for both standards`() {
        val result = dailyAirQuality(listOf(0L, 24 * h), hours(0, 48) { if (it == 30) 90 else 40 }, AqiStandard.US)
        assertEquals(listOf(40 to 33, 90 to 57), result)
    }

    @Test
    fun `puts the European value first for European locations`() {
        assertEquals(listOf(33 to 40), dailyAirQuality(listOf(0L), hours(0, 24) { 40 }, AqiStandard.EUROPEAN))
    }

    @Test
    fun `is null for a day with under 6 hours of data`() {
        assertNull(dailyAirQuality(listOf(0L, 24 * h), hours(0, 28) { 40 }, AqiStandard.US)[1])
    }
}
