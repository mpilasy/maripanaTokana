// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.widget

import orinasa.njarasoa.maripanatokana.widget.BaseWidgetWeatherFetcher.Companion.savedNameNear
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SavedNameNearTest {
    private val tanaLat = -18.8792f
    private val tanaLon = 47.5079f

    @Test
    fun samePositionKeepsTheSavedName() {
        assertEquals("Antananarivo", savedNameNear(-18.8792, 47.5079, tanaLat, tanaLon, "Antananarivo"))
    }

    @Test
    fun positionAboutTwoKmAwayKeepsTheSavedName() {
        assertEquals("Antananarivo", savedNameNear(-18.8970, 47.5079, tanaLat, tanaLon, "Antananarivo"))
    }

    @Test
    fun positionInAnotherCityDropsTheSavedName() {
        // The app saved Paris, the widget fix is in Antananarivo
        assertNull(savedNameNear(-18.8792, 47.5079, 48.8566f, 2.3522f, "Paris"))
    }

    @Test
    fun positionAboutTenKmAwayDropsTheSavedName() {
        assertNull(savedNameNear(-18.9692, 47.5079, tanaLat, tanaLon, "Antananarivo"))
    }

    @Test
    fun noSavedPositionOrNameGivesNull() {
        assertNull(savedNameNear(-18.8792, 47.5079, Float.MIN_VALUE, Float.MIN_VALUE, "Antananarivo"))
        assertNull(savedNameNear(-18.8792, 47.5079, tanaLat, tanaLon, null))
    }
}
