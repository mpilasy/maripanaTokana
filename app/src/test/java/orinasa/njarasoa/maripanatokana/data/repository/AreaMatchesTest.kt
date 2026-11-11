// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.data.repository

import orinasa.njarasoa.maripanatokana.data.repository.WeatherRepositoryImpl.Companion.areaMatches
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AreaMatchesTest {
    @Test
    fun `matches the same area with other case, accents or hyphens`() {
        assertTrue(areaMatches("Bouches-du-Rhône", "bouches du rhone"))
    }

    @Test
    fun `matches an area name that is part of the other as whole words`() {
        assertTrue(areaMatches("Grad Zagreb", "Zagreb"))
        assertTrue(areaMatches("Attica", "Αττική Attica"))
    }

    @Test
    fun `does not match a short name inside a longer word`() {
        assertFalse(areaMatches("Maine-et-Loire", "Ain"))
        assertFalse(areaMatches("Var", "Vaucluse"))
    }

    @Test
    fun `matches when the names differ only in words for the type of area`() {
        assertTrue(areaMatches("Zagreb region", "Grad Zagreb"))
        assertTrue(areaMatches("Kreis Heilbronn", "Landkreis Heilbronn"))
        assertTrue(areaMatches("Città metropolitana di Torino", "Provincia di Torino"))
    }

    @Test
    fun `does not match other areas that share one word`() {
        assertFalse(areaMatches("Haute-Garonne", "Haute-Savoie"))
        assertFalse(areaMatches("Haute-Garonne", "Tarn-et-Garonne"))
        assertFalse(areaMatches("Autonomous Community of Madrid", "Autonomous Community of Valencia"))
        assertFalse(areaMatches("North Yorkshire", "South Yorkshire"))
        assertFalse(areaMatches("Greater London", "Greater Manchester"))
        assertFalse(areaMatches("Kreis Heilbronn", "Kreis Esslingen"))
    }

    @Test
    fun `matches names in non-Latin scripts as whole words`() {
        assertTrue(areaMatches("Περιφέρεια Αττικής", "Αττικής"))
    }
}
