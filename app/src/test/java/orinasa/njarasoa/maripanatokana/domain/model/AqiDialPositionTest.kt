package orinasa.njarasoa.maripanatokana.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AqiDialPositionTest {
    @Test
    fun `places a reading inside its level segment`() {
        assertEquals(0.1f, aqiDialPosition(25, AqiStandard.US), 0.001f)
        assertEquals(0.3f, aqiDialPosition(75, AqiStandard.US), 0.001f)
        assertEquals(0.5f, aqiDialPosition(150, AqiStandard.US), 0.001f)
        assertEquals(0.3f, aqiDialPosition(30, AqiStandard.EUROPEAN), 0.001f)
    }

    @Test
    fun `agrees with the level the reading is given`() {
        listOf(50 to AqiStandard.US, 51 to AqiStandard.US, 100 to AqiStandard.US, 101 to AqiStandard.US,
            19 to AqiStandard.EUROPEAN, 20 to AqiStandard.EUROPEAN, 99 to AqiStandard.EUROPEAN).forEach { (v, std) ->
            val segment = (aqiDialPosition(v, std) * 5).toInt()
            assertEquals("$v $std", AirQualityIndex.tierFor(v, std).ordinal, segment)
        }
    }

    @Test
    fun `stops at the end of the dial`() {
        assertEquals(0.996f, aqiDialPosition(900, AqiStandard.US), 0.001f)
    }
}
