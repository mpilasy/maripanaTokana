// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.data.source

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import orinasa.njarasoa.maripanatokana.data.remote.NominatimAddress
import orinasa.njarasoa.maripanatokana.data.remote.NominatimApiService
import orinasa.njarasoa.maripanatokana.data.remote.NominatimPlace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class NominatimGeocodingSourceTest {
    private val api = mockk<NominatimApiService>()
    private val source = NominatimGeocodingSource(api)

    private val kidalRegion = NominatimPlace(placeId = 1, lat = "19.80", lon = "0.72", name = "Kidal", placeRank = 8,
        address = NominatimAddress(state = "Kidal", country = "Mali"))
    private val kidalTown = NominatimPlace(placeId = 2, lat = "18.44", lon = "1.41", name = "Kidal", placeRank = 16,
        address = NominatimAddress(city = "Kidal", state = "Kidal", country = "Mali"))

    @Test
    fun `town is listed before the region it shares a name with`() = runTest {
        coEvery { api.search(any(), any(), any(), any(), any()) } returns listOf(kidalRegion, kidalTown)
        assertEquals(listOf(2L, 1L), source.searchLocations("Kidal, Mali", Locale.ENGLISH).map { it.id })
    }

    @Test
    fun `region is flagged as an area, town is not`() = runTest {
        coEvery { api.search(any(), any(), any(), any(), any()) } returns listOf(kidalRegion, kidalTown)
        val byId = source.searchLocations("Kidal", Locale.ENGLISH).associateBy { it.id }
        assertTrue(byId.getValue(1L).isArea)
        assertFalse(byId.getValue(2L).isArea)
    }

    @Test
    fun `region with no same-named town keeps its rank`() = runTest {
        val bavaria = NominatimPlace(placeId = 3, name = "Bavaria", address = NominatimAddress(state = "Bavaria"))
        val town = NominatimPlace(placeId = 4, name = "Bavaria Town", address = NominatimAddress(town = "Bavaria Town"))
        coEvery { api.search(any(), any(), any(), any(), any()) } returns listOf(bavaria, town)
        assertEquals(listOf(3L, 4L), source.searchLocations("Bavaria", Locale.ENGLISH).map { it.id })
    }
}
