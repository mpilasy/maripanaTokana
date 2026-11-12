// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.data.repository

import orinasa.njarasoa.maripanatokana.data.repository.WeatherRepositoryImpl.Companion.areaMatches
import orinasa.njarasoa.maripanatokana.data.repository.WeatherRepositoryImpl.Companion.meteoAlarmAreaMatches
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

    @Test
    fun `in France a department does not match a department that contains its name`() {
        assertTrue(meteoAlarmAreaMatches("fr", "Haute-Loire", "Haute-Loire", "Auvergne-Rhône-Alpes"))
        assertTrue(meteoAlarmAreaMatches("fr", "Côtes-d'Armor", "Cotes d'Armor", null))
        assertFalse(meteoAlarmAreaMatches("fr", "Haute-Loire", "Loire", "Auvergne-Rhône-Alpes"))
        assertFalse(meteoAlarmAreaMatches("fr", "Loire", "Haute-Loire", "Auvergne-Rhône-Alpes"))
        assertFalse(meteoAlarmAreaMatches("fr", "Charente-Maritime", "Charente", null))
        assertFalse(meteoAlarmAreaMatches("fr", "Haute-Savoie", "Savoie", null))
    }

    @Test
    fun `in Italy the region of the place is used, also with its English name`() {
        assertTrue(meteoAlarmAreaMatches("it", "Umbria", "Province of Perugia", "Umbria"))
        assertTrue(meteoAlarmAreaMatches("it", "Toscana", "Metropolitan City of Florence", "Tuscany"))
        assertTrue(meteoAlarmAreaMatches("it", "Emilia e Romagna", "Bologna", "Emilia-Romagna"))
        assertFalse(meteoAlarmAreaMatches("it", "Lombardia", "Perugia", "Umbria"))
        // Without a region, the subdivision is used
        assertTrue(meteoAlarmAreaMatches("it", "Umbria", "Umbria", null))
    }

    @Test
    fun `other countries keep the rule with containment`() {
        assertTrue(meteoAlarmAreaMatches("es", "Litoral norte de Tarragona", "Tarragona", "Catalonia"))
        assertTrue(meteoAlarmAreaMatches("de", "Kreis Breisgau-Hochschwarzwald und Stadt Freiburg", "Freiburg im Breisgau", null))
    }

    @Test
    fun `an alert is kept when the area of the place or of the alert is not known`() {
        assertTrue(meteoAlarmAreaMatches("fr", "Haute-Loire", null, null))
        assertTrue(meteoAlarmAreaMatches("fr", "", "Loire", null))
        assertTrue(meteoAlarmAreaMatches("it", "Umbria", " ", " "))
    }
}
