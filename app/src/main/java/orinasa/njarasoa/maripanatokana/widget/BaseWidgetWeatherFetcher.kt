// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.widget

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import orinasa.njarasoa.maripanatokana.data.remote.OpenMeteoApiService
import orinasa.njarasoa.maripanatokana.data.remote.toDomain
import orinasa.njarasoa.maripanatokana.domain.model.WeatherData
import orinasa.njarasoa.maripanatokana.util.AppLog
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType
import java.io.File
import java.util.Locale

/** Result of a widget fetch: [fresh] is true only when the network fetch succeeded now. */
data class WidgetFetchResult(val data: WeatherData?, val fresh: Boolean)

/**
 * Base logic for standalone weather fetcher for Glance widgets.
 */
open class BaseWidgetWeatherFetcher(private val baseUrl: String = "https://api.open-meteo.com/") {

    protected val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    protected val api: OpenMeteoApiService by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(OpenMeteoApiService::class.java)
    }

    @Volatile private var cache: WidgetCache? = null

    // When the last network fetch started, in this process
    @Volatile private var lastFetchStartedAt = 0L

    private fun cache(context: Context): WidgetCache = cache ?: synchronized(this) {
        cache ?: WidgetCache(File(context.applicationContext.filesDir, "widget_cache/forecast.json"), json).also {
            it.migrateFrom(context.getSharedPreferences("private_prefs", Context.MODE_PRIVATE))
            cache = it
        }
    }

    /** The last fetched forecast, mapped again for the current time. No network access. */
    fun cached(context: Context): WeatherData? = cache(context).load()?.let { entry ->
        try {
            entry.response.toDomain(entry.locationName).copy(timestamp = entry.timestamp)
        } catch (e: Exception) {
            AppLog.w(TAG, "cached forecast could not be mapped", e)
            null
        }
    }

    /**
     * The data a widget shows: the cached forecast while it is recent, else a new fetch.
     * The worker and the refresh button do the fetches, so each render does not call the network.
     * Renders just after a fetch (also a failed one) read the cache only, so they do not fetch again.
     */
    suspend fun forRender(context: Context, getCoordinates: suspend () -> Pair<Double, Double>?): WeatherData? {
        val cached = withContext(Dispatchers.IO) { cached(context) }
        val now = System.currentTimeMillis()
        if (cached != null && now - cached.timestamp < RENDER_MAX_AGE_MS) return cached
        if (now - lastFetchStartedAt < RECENT_FETCH_MS) return cached
        return fetchInternal(context, getCoordinates).data
    }

    @SuppressLint("MissingPermission")
    suspend fun fetchInternal(context: Context, getCoordinates: suspend () -> Pair<Double, Double>?): WidgetFetchResult {
        lastFetchStartedAt = System.currentTimeMillis()
        val prefs = context.getSharedPreferences("private_prefs", Context.MODE_PRIVATE)
        return try {
            val (lat, lon) = getCoordinates() ?: throw IllegalStateException("No coordinates")

            val response = api.getForecast(latitude = lat, longitude = lon)

            val locationName = prefs.getString("location_name", null)
                ?: try {
                    withContext(Dispatchers.IO) {
                        @Suppress("DEPRECATION")
                        Geocoder(context, Locale.getDefault())
                            .getFromLocation(lat, lon, 1)
                            ?.firstOrNull()
                            ?.locality
                    } ?: "%.2f, %.2f".format(Locale.US, lat, lon)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    "%.2f, %.2f".format(Locale.US, lat, lon)
                }

            val now = System.currentTimeMillis()
            withContext(Dispatchers.IO) { cache(context).save(WidgetCacheEntry(response, locationName, now)) }

            WidgetFetchResult(response.toDomain(locationName).copy(timestamp = now), fresh = true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLog.w(TAG, "widget fetch failed", e)
            WidgetFetchResult(withContext(Dispatchers.IO) { cached(context) }, fresh = false)
        }
    }

    companion object {
        private const val TAG = "WidgetFetcher"

        /** Longer than the 30-minute worker period, so a render between two worker runs uses the cache. */
        const val RENDER_MAX_AGE_MS = 45 * 60 * 1000L

        /** A render this soon after a fetch started does not fetch again. */
        const val RECENT_FETCH_MS = 2 * 60 * 1000L
    }
}
