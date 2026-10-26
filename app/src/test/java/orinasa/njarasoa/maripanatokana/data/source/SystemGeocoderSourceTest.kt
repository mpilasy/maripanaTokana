// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.data.source

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import orinasa.njarasoa.maripanatokana.data.remote.GeocodingResponse
import orinasa.njarasoa.maripanatokana.data.remote.GeocodingResult
import orinasa.njarasoa.maripanatokana.data.remote.OpenMeteoGeocodingService
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class SystemGeocoderSourceTest {
    private val openMeteo = mockk<OpenMeteoGeocodingService>()
    private val nominatim = mockk<NominatimGeocodingSource>()
    private val source = SystemGeocoderSource(mockk(), openMeteo, nominatim)

    private val sudburyMa = GeocodingResult(1, "Sudbury", 42.38, -71.42, "United States", "Massachusetts")
    private val sudburyWa = GeocodingResult(2, "Sudbury", 46.0, -118.0, "United States", "Washington")
    private val greaterSudbury = GeocodingResult(3, "Greater Sudbury", 46.49, -80.99, "Canada", "Ontario")

    @Test
    fun `qualifier filters Open-Meteo results without asking Nominatim`() = runTest {
        coEvery { openMeteo.searchLocation(any(), any(), any(), any()) } returns GeocodingResponse(listOf(sudburyMa, sudburyWa))
        assertEquals(listOf(1L), source.searchLocations("Sudbury, MA", Locale.ENGLISH).map { it.id })
        coVerify(exactly = 0) { nominatim.searchLocations(any(), any()) }
    }

    @Test
    fun `no match in the region falls back to Nominatim with the whole query`() = runTest {
        coEvery { openMeteo.searchLocation(any(), any(), any(), any()) } returns GeocodingResponse(listOf(sudburyMa, sudburyWa))
        coEvery { nominatim.searchLocations("Sudbury, ON", any()) } returns listOf(greaterSudbury)
        assertEquals(listOf(3L), source.searchLocations("Sudbury, ON", Locale.ENGLISH).map { it.id })
    }

    @Test
    fun `Nominatim failure keeps the Open-Meteo results`() = runTest {
        coEvery { openMeteo.searchLocation(any(), any(), any(), any()) } returns GeocodingResponse(listOf(sudburyMa, sudburyWa))
        coEvery { nominatim.searchLocations(any(), any()) } throws RuntimeException("offline")
        assertEquals(listOf(1L, 2L), source.searchLocations("Sudbury, ON", Locale.ENGLISH).map { it.id })
    }
}
