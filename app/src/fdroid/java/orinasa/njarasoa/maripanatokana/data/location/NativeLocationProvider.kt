package orinasa.njarasoa.maripanatokana.data.location

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
        // Use a dedicated HandlerThread so location callbacks never run on the main thread.
        // Running on Looper.getMainLooper() risks jank and ANR if the callback or any
        // downstream work touches the UI thread while a frame is being rendered.
        val handlerThread = HandlerThread("LocationRequest").also { it.start() }

        val locationFlow = callbackFlow<Location> {
            val locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    trySend(location)
                }
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }

            // Register each provider independently so a failure on one never blocks the others.
            // PASSIVE_PROVIDER piggybacks on other apps' requests — on non-GMS devices where
            // NETWORK_PROVIDER is absent, it immediately delivers any fix GPSTest or other apps
            // are already producing, bypassing the GPS warm-up delay entirely.
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
                    // Provider requires a permission we don't have; try the next one
                } catch (e: Exception) {
                    close(e)
                    return@callbackFlow
                }
            }
            if (!registered) close()

            awaitClose {
                locationManager.removeUpdates(locationListener)
                handlerThread.quitSafely()
            }
        }

        // Wait up to 30s: GPS cold-start on non-GMS devices can take 20-30s even when the
        // hardware is warm. On timeout, fall back to any stale cached fix.
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
