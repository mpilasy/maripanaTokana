// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class SavedLocationTest {

    private fun place(name: String, lat: Double, lon: Double) = SavedLocation(id = "$lat,$lon", name = name, latitude = lat, longitude = lon)

    @Test
    fun sortedByDistanceFrom_putsTheNearestFirst() {
        val paris = place("Paris", 48.8566, 2.3522)
        val toamasina = place("Toamasina", -18.1492, 49.4023)
        val antsirabe = place("Antsirabe", -19.8659, 47.0333)
        // From Antananarivo
        val sorted = listOf(paris, toamasina, antsirabe).sortedByDistanceFrom(-18.8792, 47.5079)
        assertEquals(listOf("Antsirabe", "Toamasina", "Paris"), sorted.map { it.name })
    }

    @Test
    fun sortedByDistanceFrom_crossesTheDateLine() {
        val suva = place("Suva", -18.1416, 178.4419)
        val apia = place("Apia", -13.8333, -171.7667)
        val sydney = place("Sydney", -33.8688, 151.2093)
        // From Nuku'alofa (-175.2): Apia and Suva are close, across the 180th meridian
        val sorted = listOf(sydney, apia, suva).sortedByDistanceFrom(-21.1394, -175.2049)
        assertEquals(listOf("Suva", "Apia", "Sydney"), sorted.map { it.name })
    }
}
