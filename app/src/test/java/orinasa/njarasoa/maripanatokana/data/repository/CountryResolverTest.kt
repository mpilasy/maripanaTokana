package orinasa.njarasoa.maripanatokana.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import orinasa.njarasoa.maripanatokana.data.remote.NominatimAddress
import orinasa.njarasoa.maripanatokana.data.remote.NominatimApiService
import orinasa.njarasoa.maripanatokana.data.remote.NominatimPlace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CountryResolverTest {
    private val nominatim = mockk<NominatimApiService>()
    private val geoInfo = CountryInfo("fr", "Gironde", "Nouvelle-Aquitaine")

    private fun place(address: NominatimAddress) = NominatimPlace(address = address)

    @Test
    fun `geocoder success does not call nominatim`() = runTest {
        val resolver = CountryResolver({ _, _ -> geoInfo }, nominatim)
        assertEquals(geoInfo, resolver.resolve(44.84, -0.58))
        coVerify(exactly = 0) { nominatim.reverse(any(), any(), any(), any()) }
    }

    @Test
    fun `geocoder null falls back to nominatim`() = runTest {
        coEvery { nominatim.reverse(any(), any(), any(), any()) } returns
            place(NominatimAddress(county = "Gironde", state = "Nouvelle-Aquitaine", countryCode = "FR"))
        val resolver = CountryResolver({ _, _ -> null }, nominatim)
        assertEquals(geoInfo, resolver.resolve(44.84, -0.58))
    }

    @Test
    fun `geocoder throw falls back to nominatim with state as subdivision`() = runTest {
        coEvery { nominatim.reverse(any(), any(), any(), any()) } returns
            place(NominatimAddress(state = "Victoria", countryCode = "au"))
        val resolver = CountryResolver({ _, _ -> error("no backend") }, nominatim)
        assertEquals(CountryInfo("au", "Victoria", "Victoria"), resolver.resolve(-37.8, 144.9))
    }

    @Test
    fun `both failing returns null`() = runTest {
        coEvery { nominatim.reverse(any(), any(), any(), any()) } throws java.io.IOException("offline")
        val resolver = CountryResolver({ _, _ -> null }, nominatim)
        assertNull(resolver.resolve(1.0, 2.0))
    }

    @Test
    fun `cache hit avoids second lookup`() = runTest {
        var calls = 0
        coEvery { nominatim.reverse(any(), any(), any(), any()) } returns
            place(NominatimAddress(countryCode = "fr"))
        val resolver = CountryResolver({ _, _ -> calls++; null }, nominatim)
        resolver.resolve(44.841, -0.581)
        resolver.resolve(44.842, -0.582)
        assertEquals(1, calls)
        coVerify(exactly = 1) { nominatim.reverse(any(), any(), any(), any()) }
    }
}
