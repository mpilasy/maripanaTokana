package orinasa.njarasoa.maripanatokana.data.source

import orinasa.njarasoa.maripanatokana.data.remote.GeocodingResult
import orinasa.njarasoa.maripanatokana.data.remote.NominatimApiService
import orinasa.njarasoa.maripanatokana.data.remote.NominatimPlace
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NominatimGeocodingSource @Inject constructor(
    private val apiService: NominatimApiService,
) : GeocodingDataSource {
    override val displayName = "Nominatim (OpenStreetMap)"

    override suspend fun reverseGeocode(lat: Double, lon: Double, locale: Locale): Pair<String, String?> {
        val place = apiService.reverse(lat = lat, lon = lon, language = locale.language)
        val addr = place.address
        val rawName = addr.city ?: addr.town ?: addr.village ?: addr.municipality
            ?: place.name.ifBlank { "%.2f, %.2f".format(Locale.US, lat, lon) }
        val name = rawName.split(",")[0].trim()
        val subtext = listOfNotNull(addr.state, addr.country)
            .filter { it.isNotBlank() && !name.contains(it) }
            .joinToString(", ")
            .ifBlank { null }
        return name to subtext
    }

    // Nominatim can rank a region above the town it is named after (Kidal region before Kidal
    // town); list the town first so the top result is the place people mean.
    override suspend fun searchLocations(query: String, locale: Locale): List<GeocodingResult> {
        val places = apiService.search(query = query, language = locale.language)
        val settlementNames = places.filter { it.isSettlement }.map { it.toGeocodingResult().name.lowercase() }.toSet()
        return places
            .map { it to it.toGeocodingResult() }
            .sortedBy { (place, result) -> !place.isSettlement && result.name.lowercase() in settlementNames }
            .map { it.second }
    }

    private val NominatimPlace.isSettlement get() = address.city != null || address.town != null || address.village != null

    private fun NominatimPlace.toGeocodingResult() = GeocodingResult(
        id = placeId,
        name = address.city ?: address.town ?: address.village ?: name.ifBlank { displayName.split(",").firstOrNull()?.trim() ?: name },
        latitude = lat.toDoubleOrNull() ?: 0.0,
        longitude = lon.toDoubleOrNull() ?: 0.0,
        country = address.country,
        admin1 = address.state,
        featureCode = if (!isSettlement && placeRank < 16) "ADM" else null,
    )
}
