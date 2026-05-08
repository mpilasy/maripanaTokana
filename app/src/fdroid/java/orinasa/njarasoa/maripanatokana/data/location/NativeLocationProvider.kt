package orinasa.njarasoa.maripanatokana.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.withTimeoutOrNull

class NativeLocationProvider(
    private val context: Context,
) : LocationProvider {

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

            if (location != null) {
                Result.success(Pair(location.latitude, location.longitude))
            } else {
                Result.failure(Exception("No cached location"))
            }
        } catch (e: SecurityException) {
            Result.failure(Exception("Location permission not granted"))
        } catch (e: Exception) {
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
        } catch (e: SecurityException) {
            Result.failure(Exception("Location permission not granted"))
        } catch (e: Exception) {
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

    // Returns any stale cached fix from any provider, ignoring whether the provider
    // is currently enabled. Used as a last-resort fallback when a fresh fix times out.
    @SuppressLint("MissingPermission")
    private fun getStaleFallbackLocation(): Location? {
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER,
        )
        return providers.mapNotNull { provider ->
            try { locationManager.getLastKnownLocation(provider) } catch (_: Exception) { null }
        }.maxByOrNull { it.time }
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestLocationUpdate(): Location? {
        val locationFlow = callbackFlow<Location> {
            val locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    trySend(location)
                }
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }

            try {
                for (provider in listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)) {
                    if (locationManager.isProviderEnabled(provider)) {
                        locationManager.requestLocationUpdates(
                            provider, 0L, 0f, locationListener, Looper.getMainLooper()
                        )
                    }
                }
            } catch (e: Exception) {
                close(e)
            }

            awaitClose { locationManager.removeUpdates(locationListener) }
        }

        // Wait up to 10s for the first update from any enabled provider. On timeout, fall
        // back to any stale cached fix rather than returning null immediately — a device may
        // have an old fix even when no live provider is firing (e.g. emulator, airplane mode).
        val liveLocation = try {
            withTimeoutOrNull(10_000L) {
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
