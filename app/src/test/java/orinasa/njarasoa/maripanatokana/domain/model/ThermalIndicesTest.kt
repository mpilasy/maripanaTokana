// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Reference values in °C and km/h, calculated with the NWS heat index and wind chill formulas. */
class ThermalIndicesTest {

    private fun c(v: Double) = Temperature.fromCelsius(v)
    private fun kmh(v: Double) = WindSpeed.fromMetersPerSecond(v / 3.6)

    @Test
    fun heatIndex_followsTheNwsFormula() {
        assertEquals(40.4, ThermalIndices.heatIndex(c(32.0), 70)!!.celsius, 0.5)
        assertEquals(35.0, ThermalIndices.heatIndex(c(30.0), 70)!!.celsius, 0.5)
        assertEquals(48.6, ThermalIndices.heatIndex(c(38.0), 50)!!.celsius, 0.5)
    }

    @Test
    fun heatIndex_isNullBelow27CelsiusOrWithoutHumidity() {
        assertNull(ThermalIndices.heatIndex(c(26.0), 90))
        assertNull(ThermalIndices.heatIndex(c(32.0), null))
    }

    @Test
    fun heatIndex_isNullWhenLessThan5CelsiusAboveTheTemperature() {
        // Very dry heat (Kidal, Mali: 38 °C at 12 %) gives about 35 °C, under the temperature
        assertNull(ThermalIndices.heatIndex(c(38.0), 12))
        assertNull(ThermalIndices.heatIndex(c(27.0), 40))
        // 28.1 °C at 27 °C and 60 %, 37.2 °C at 35 °C and 40 %
        assertNull(ThermalIndices.heatIndex(c(27.0), 60))
        assertNull(ThermalIndices.heatIndex(c(35.0), 40))
    }

    @Test
    fun windChill_followsTheNwsFormula() {
        assertEquals(-32.5, ThermalIndices.windChill(c(-20.0), kmh(30.0))!!.celsius, 0.3)
        assertEquals(-11.5, ThermalIndices.windChill(c(-5.0), kmh(20.0))!!.celsius, 0.3)
        assertEquals(-24.0, ThermalIndices.windChill(c(-10.0), kmh(80.0))!!.celsius, 0.3)
        // Just under freezing: -0.9 °C at 2 °C with 10 km/h
        assertEquals(-0.9, ThermalIndices.windChill(c(2.0), kmh(10.0))!!.celsius, 0.3)
    }

    @Test
    fun windChill_isNullWhenWarmCalmOrAboveFreezing() {
        assertNull(ThermalIndices.windChill(c(11.0), kmh(30.0)))
        assertNull(ThermalIndices.windChill(c(-5.0), kmh(4.0)))
        // 0.5 °C at 4 °C with 15 km/h
        assertNull(ThermalIndices.windChill(c(4.0), kmh(15.0)))
    }

    @Test
    fun frostbiteMinutes_followsTheEnvironmentCanadaBands() {
        assertNull(ThermalIndices.frostbiteMinutes(c(-27.0)))
        assertEquals(10 to 30, ThermalIndices.frostbiteMinutes(c(-28.0)))
        assertEquals(10 to 30, ThermalIndices.frostbiteMinutes(c(-39.4)))
        assertEquals(5 to 10, ThermalIndices.frostbiteMinutes(c(-40.0)))
        assertEquals(2 to 5, ThermalIndices.frostbiteMinutes(c(-54.0)))
        assertEquals(0 to 2, ThermalIndices.frostbiteMinutes(c(-55.0)))
    }

    @Test
    fun relativeHumidity_fromTheDewPoint() {
        assertEquals(55, ThermalIndices.relativeHumidity(Temperature.fromCelsius(30.0), Temperature.fromCelsius(20.0)))
        assertEquals(100, ThermalIndices.relativeHumidity(Temperature.fromCelsius(15.0), Temperature.fromCelsius(15.0)))
        assertNull(ThermalIndices.relativeHumidity(Temperature.fromCelsius(15.0), null))
    }
}
