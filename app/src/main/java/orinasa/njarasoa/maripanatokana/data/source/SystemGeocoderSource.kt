package orinasa.njarasoa.maripanatokana.data.source

import android.content.Context
import android.location.Geocoder
import dagger.hilt.android.qualifiers.ApplicationContext
import orinasa.njarasoa.maripanatokana.data.remote.GeocodingResult
import orinasa.njarasoa.maripanatokana.data.remote.OpenMeteoGeocodingService
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemGeocoderSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val geocodingApiService: OpenMeteoGeocodingService,
) : GeocodingDataSource {
    override val displayName = "System Geocoder"

    @Suppress("DEPRECATION")
    override suspend fun reverseGeocode(lat: Double, lon: Double, locale: Locale): Pair<String, String?> {
        val geocoder = Geocoder(context, locale)
        val addr = geocoder.getFromLocation(lat, lon, 1)?.firstOrNull()
        val rawName = addr?.locality
            ?: addr?.subAdminArea
            ?: addr?.adminArea
            ?: "%.2f, %.2f".format(Locale.US, lat, lon)
        val name = rawName.split(",")[0].split(";")[0].split("-")[0].trim()
        val subtext = if (addr != null) {
            val parts = mutableListOf<String>()
            if (addr.adminArea != null && !name.contains(addr.adminArea) && !addr.adminArea.contains(name) && !rawName.contains(addr.adminArea)) {
                parts.add(addr.adminArea)
            }
            if (addr.countryName != null) parts.add(addr.countryName)
            if (parts.isNotEmpty()) parts.joinToString(", ") else null
        } else null
        return name to subtext
    }

    override suspend fun searchLocations(query: String, locale: Locale): List<GeocodingResult> =
        geocodingApiService.searchLocation(name = query, language = locale.language).results
}
