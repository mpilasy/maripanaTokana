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

    override suspend fun searchLocations(query: String, locale: Locale): List<GeocodingResult> =
        apiService.search(query = query, language = locale.language).map { it.toGeocodingResult() }

    private fun NominatimPlace.toGeocodingResult() = GeocodingResult(
        id = placeId,
        name = address.city ?: address.town ?: address.village ?: name.ifBlank { displayName.split(",").firstOrNull()?.trim() ?: name },
        latitude = lat.toDoubleOrNull() ?: 0.0,
        longitude = lon.toDoubleOrNull() ?: 0.0,
        country = address.country,
        admin1 = address.state,
    )
}
