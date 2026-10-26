// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.data.source

import android.content.Context
import android.location.Geocoder
import dagger.hilt.android.qualifiers.ApplicationContext
import orinasa.njarasoa.maripanatokana.data.location.matchesRegionQualifier
import orinasa.njarasoa.maripanatokana.data.location.shortPlaceName
import orinasa.njarasoa.maripanatokana.data.remote.GeocodingResult
import orinasa.njarasoa.maripanatokana.data.remote.OpenMeteoGeocodingService
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemGeocoderSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val geocodingApiService: OpenMeteoGeocodingService,
    private val nominatim: NominatimGeocodingSource,
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
        val name = shortPlaceName(rawName)
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

    // Open-Meteo's `name` param only matches the place name itself — a combined "City, State"
    // query returns zero results. Split off the qualifier (state/country) and use it to filter
    // the results client-side instead. Open-Meteo also only matches the start of a name, so when
    // nothing fits, ask Nominatim with the whole query ("Sudbury, ON" is Greater Sudbury there).
    override suspend fun searchLocations(query: String, locale: Locale): List<GeocodingResult> {
        val commaIndex = query.indexOf(',')
        val namePart = if (commaIndex >= 0) query.substring(0, commaIndex).trim() else query.trim()
        val qualifier = if (commaIndex >= 0) query.substring(commaIndex + 1).trim() else ""

        val results = geocodingApiService.searchLocation(name = namePart, language = locale.language).results
        val filtered = results.filter { matchesRegionQualifier(qualifier, it.admin1, it.admin2, it.country) }
        if (filtered.isNotEmpty()) return filtered
        val fallback = runCatching { nominatim.searchLocations(query, locale) }.getOrDefault(emptyList())
        return fallback.ifEmpty { results }
    }
}
