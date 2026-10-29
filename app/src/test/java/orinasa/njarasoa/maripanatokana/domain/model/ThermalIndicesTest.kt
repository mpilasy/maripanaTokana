// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Reference values from the NWS heat index and wind chill charts. */
class ThermalIndicesTest {

    private fun f(v: Double) = Temperature.fromFahrenheit(v)
    private fun mph(v: Double) = WindSpeed.fromMph(v)

    @Test
    fun heatIndex_matchesTheNwsChart() {
        assertEquals(106.0, ThermalIndices.heatIndex(f(90.0), 70)!!.fahrenheit, 1.0)
        assertEquals(80.0, ThermalIndices.heatIndex(f(80.0), 40)!!.fahrenheit, 1.0)
        assertEquals(109.0, ThermalIndices.heatIndex(f(100.0), 40)!!.fahrenheit, 1.0)
    }

    @Test
    fun heatIndex_isNullBelow80F() {
        assertNull(ThermalIndices.heatIndex(f(79.0), 90))
        assertNull(ThermalIndices.heatIndex(f(90.0), null))
    }

    @Test
    fun windChill_matchesTheNwsChart() {
        assertEquals(-19.0, ThermalIndices.windChill(f(0.0), mph(15.0))!!.fahrenheit, 0.5)
        assertEquals(34.0, ThermalIndices.windChill(f(40.0), mph(10.0))!!.fahrenheit, 0.5)
        assertEquals(-4.0, ThermalIndices.windChill(f(20.0), mph(60.0))!!.fahrenheit, 0.5)
    }

    @Test
    fun windChill_isNullWhenWarmOrCalm() {
        assertNull(ThermalIndices.windChill(f(51.0), mph(20.0)))
        assertNull(ThermalIndices.windChill(f(30.0), mph(3.0)))
    }

    @Test
    fun relativeHumidity_fromTheDewPoint() {
        assertEquals(55, ThermalIndices.relativeHumidity(Temperature.fromCelsius(30.0), Temperature.fromCelsius(20.0)))
        assertEquals(100, ThermalIndices.relativeHumidity(Temperature.fromCelsius(15.0), Temperature.fromCelsius(15.0)))
        assertNull(ThermalIndices.relativeHumidity(Temperature.fromCelsius(15.0), null))
    }
}
