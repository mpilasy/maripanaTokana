// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.widget

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import orinasa.njarasoa.maripanatokana.domain.model.WeatherData

/**
 * Standalone weather fetcher for Glance widgets (no Hilt).
 * Standard flavor using Google Play Services for location.
 */
object WidgetWeatherFetcher : BaseWidgetWeatherFetcher() {

    /** Fetches from the network now; falls back to the cache when the fetch fails. */
    @SuppressLint("MissingPermission")
    suspend fun fetch(context: Context): WidgetFetchResult {
        return fetchInternal(context) { getCoordinates(context) }
    }

    /** The data a widget shows; fetches only when the cache is missing or old. */
    @SuppressLint("MissingPermission")
    suspend fun render(context: Context): WeatherData? {
        return forRender(context) { getCoordinates(context) }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getCoordinates(context: Context): Pair<Double, Double>? {
        try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            // City-level accuracy is enough for a forecast; GPS in the background uses battery
            val location = fusedClient.lastLocation.await()
                ?: run {
                    val cancellation = CancellationTokenSource()
                    try {
                        withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
                            fusedClient.getCurrentLocation(
                                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                                cancellation.token,
                            ).await()
                        }
                    } finally {
                        cancellation.cancel()
                    }
                }

            if (location != null) {
                return Pair(location.latitude, location.longitude)
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Exception) {
            // No permission or provider failure (SecurityException etc.) — fall through to SharedPreferences
        }

        // Fall back to last coordinates saved by the main app
        val prefs = context.getSharedPreferences("private_prefs", Context.MODE_PRIVATE)
        val lat = prefs.getFloat("lat", Float.MIN_VALUE)
        val lon = prefs.getFloat("lon", Float.MIN_VALUE)
        if (lat == Float.MIN_VALUE) return null
        return Pair(lat.toDouble(), lon.toDouble())
    }

    private const val LOCATION_TIMEOUT_MS = 20_000L
}
