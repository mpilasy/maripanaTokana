package orinasa.njarasoa.maripanatokana.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Location provider using Android's built-in LocationManager.
 * F-Droid compatible implementation with no Google Play Services dependency.
 */
class NativeLocationProvider(
    private val context: Context,
) : LocationProvider {

    private val locationManager: LocationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    override suspend fun getLastLocation(): Result<Pair<Double, Double>> {
        return try {
            val gpsLocation = getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val networkLocation = getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            // Use most recent location
            val location = when {
                gpsLocation != null && networkLocation != null -> {
                    if (gpsLocation.time >= networkLocation.time) gpsLocation else networkLocation
                }
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
        } catch (e: Exception) {
            null
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestLocationUpdate(): Location? {
        var bestLocation: Location? = null
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)

        val locationFlow = callbackFlow<Location> {
            val locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    trySend(location)
                }

                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }

            try {
                for (provider in providers) {
                    if (locationManager.isProviderEnabled(provider)) {
                        @Suppress("MissingPermission")
                        locationManager.requestLocationUpdates(
                            provider,
                            0L,
                            0f,
                            locationListener,
                            Looper.getMainLooper()
                        )
                    }
                }
            } catch (e: Exception) {
                close(e)
            }

            awaitClose {
                locationManager.removeUpdates(locationListener)
            }
        }

        try {
            withTimeoutOrNull(30_000L) {
                locationFlow.take(2).collect { location ->
                    if (bestLocation == null || location.accuracy < bestLocation!!.accuracy) {
                        bestLocation = location
                    }
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            // Flow might be closed
        }

        return bestLocation
    }
}
