package orinasa.njarasoa.maripanatokana.data.repository

import orinasa.njarasoa.maripanatokana.util.AppLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import orinasa.njarasoa.maripanatokana.data.remote.NominatimApiService
import java.util.Locale

/** [subdivision] is the most granular of county/state; [adminArea] is the state/region only. */
data class CountryInfo(val countryCode: String, val subdivision: String?, val adminArea: String?)

/**
 * Resolves country/subdivision for alert gating: platform Geocoder first, Nominatim as fallback.
 * Successful results are cached by coordinates rounded to ~1 km to respect Nominatim's usage policy.
 */
class CountryResolver(
    private val geocoder: (Double, Double) -> CountryInfo?,
    private val nominatim: NominatimApiService,
) {
    private val cache = object : LinkedHashMap<String, CountryInfo>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CountryInfo>?) = size > MAX_ENTRIES
    }

    suspend fun resolve(lat: Double, lon: Double): CountryInfo? {
        val key = String.format(Locale.US, "%.2f,%.2f", lat, lon)
        synchronized(cache) { cache[key] }?.let { return it }

        val info = fromGeocoder(lat, lon) ?: fromNominatim(lat, lon)
        if (info != null) synchronized(cache) { cache[key] = info }
        return info
    }

    private suspend fun fromGeocoder(lat: Double, lon: Double): CountryInfo? = withContext(Dispatchers.IO) {
        try {
            geocoder(lat, lon)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLog.w("CountryResolver", "geocoder failed", e)
            null
        }
    }

    private suspend fun fromNominatim(lat: Double, lon: Double): CountryInfo? = try {
        val address = nominatim.reverse(lat, lon).address
        address.countryCode?.takeIf { it.isNotBlank() }?.let {
            CountryInfo(
                countryCode = it.lowercase(),
                subdivision = address.county?.takeIf { c -> c.isNotBlank() }
                    ?: address.state?.takeIf { s -> s.isNotBlank() },
                adminArea = address.state?.takeIf { s -> s.isNotBlank() },
            )
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        AppLog.w("CountryResolver", "nominatim failed", e)
        null
    }

    private companion object {
        const val MAX_ENTRIES = 32
    }
}
