package orinasa.njarasoa.maripanatokana.data.source

import orinasa.njarasoa.maripanatokana.data.remote.GeocodingResult
import java.util.Locale

interface GeocodingDataSource {
    suspend fun reverseGeocode(lat: Double, lon: Double, locale: Locale): Pair<String, String?>
    suspend fun searchLocations(query: String, locale: Locale): List<GeocodingResult>
    val displayName: String
}
