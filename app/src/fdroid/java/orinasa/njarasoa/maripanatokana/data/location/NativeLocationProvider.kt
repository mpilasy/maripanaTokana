// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.data.location

import orinasa.njarasoa.maripanatokana.util.AppLog
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.HandlerThread
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.withTimeoutOrNull

class NativeLocationProvider(
    private val context: Context,
) : LocationProvider {

    private companion object {
        const val MAX_FIX_AGE_MS = 24 * 60 * 60 * 1000L
    }

    private val locationManager: LocationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    override suspend fun getLastLocation(): Result<Pair<Double, Double>> {
        return try {
            val gpsLocation = getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val networkLocation = getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            val location = when {
                gpsLocation != null && networkLocation != null ->
                    if (gpsLocation.time >= networkLocation.time) gpsLocation else networkLocation
                gpsLocation != null -> gpsLocation
                networkLocation != null -> networkLocation
                else -> null
            }

            if (location != null && !location.isStale()) {
                Result.success(Pair(location.latitude, location.longitude))
            } else {
                Result.failure(Exception("No cached location"))
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: SecurityException) {
            Result.failure(Exception("Location permission not granted"))
        } catch (e: Exception) {
            AppLog.w("NativeLocation", "location request failed", e)
            Result.failure(e)
        }
    }

    override suspend fun getFreshLocation(): Result<Pair<Double, Double>> {
        return try {
            val location = requestLocationUpdate()
            if (location != null) {
                Result.success(Pair(location.latitude, location.longitude))
            } else {
                Result.failure(Exception("Unable to get location"))
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: SecurityException) {
            Result.failure(Exception("Location permission not granted"))
        } catch (e: Exception) {
            AppLog.w("NativeLocation", "location request failed", e)
            Result.failure(e)
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLastKnownLocation(provider: String): Location? {
        return try {
            if (locationManager.isProviderEnabled(provider)) {
                locationManager.getLastKnownLocation(provider)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    // The newest cached fix of any provider, also of a disabled one, when it is less than 24 h old.
    // The fallback when a fresh fix times out.
    @SuppressLint("MissingPermission")
    private fun getStaleFallbackLocation(): Location? {
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER,
        )
        return providers.mapNotNull { provider ->
            try { locationManager.getLastKnownLocation(provider) } catch (_: Exception) { null }
        }.filterNot { it.isStale() }.maxByOrNull { it.time }
    }

    // Fixes older than 24h (e.g. from before a flight) are not trusted as a location.
    private fun Location.isStale() = System.currentTimeMillis() - time > MAX_FIX_AGE_MS

    @SuppressLint("MissingPermission")
    private suspend fun requestLocationUpdate(): Location? {
        // A HandlerThread keeps the location callbacks off the main thread, which prevents jank and ANR.
        val handlerThread = HandlerThread("LocationRequest").also { it.start() }

        val locationFlow = callbackFlow<Location> {
            val locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    trySend(location)
                }
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }

            // Register each provider on its own, so a failure on one does not stop the others.
            // PASSIVE_PROVIDER gets the fixes that other apps request: on a device without Google services
            // and without NETWORK_PROVIDER, it can give a fix at once, before the GPS warms up.
            var registered = false
            for (provider in listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER,
            )) {
                try {
                    if (locationManager.isProviderEnabled(provider)) {
                        locationManager.requestLocationUpdates(
                            provider, 0L, 0f, locationListener, handlerThread.looper
                        )
                        registered = true
                    }
                } catch (_: SecurityException) {
                    // The provider needs a permission that the app does not have: try the next one
                } catch (e: Exception) {
                    AppLog.w("NativeLocation", "provider $provider failed", e)
                }
            }
            if (!registered) close()

            awaitClose {
                locationManager.removeUpdates(locationListener)
                handlerThread.quitSafely()
            }
        }

        // Wait up to 30 s: a GPS cold start on a device without Google services can take 20-30 s.
        // On timeout, use the newest cached fix.
        val liveLocation = try {
            withTimeoutOrNull(30_000L) {
                locationFlow.take(1).firstOrNull()
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }

        return liveLocation ?: getStaleFallbackLocation()
    }
}
