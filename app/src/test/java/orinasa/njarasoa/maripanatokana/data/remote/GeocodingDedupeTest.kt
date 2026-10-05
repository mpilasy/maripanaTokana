package orinasa.njarasoa.maripanatokana.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test

class GeocodingDedupeTest {
    private fun r(id: Long, name: String, lat: Double, lon: Double) =
        GeocodingResult(id, name, lat, lon, country = "France", admin1 = "Île-de-France")

    @Test
    fun dropsSameNameAndNearbyCoordinates() {
        val list = listOf(
            r(1, "Paris", 48.8566, 2.3522),
            r(2, "Paris", 48.8567, 2.3521),
            r(3, "Paris", 33.66, -95.55),
        )
        assertEquals(listOf(1L, 3L), list.dedupeByPlace().map { it.id })
    }

    @Test
    fun keepsDifferentNamesAtSameSpot() {
        val list = listOf(r(1, "Paris", 48.85, 2.35), r(2, "Lutetia", 48.85, 2.35))
        assertEquals(2, list.dedupeByPlace().size)
    }
}
