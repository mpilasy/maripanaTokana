package orinasa.njarasoa.maripanatokana.data.location

import orinasa.njarasoa.maripanatokana.util.AppLog
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Location provider using Google Play Services FusedLocationProviderClient.
 * Provides enhanced accuracy and power efficiency compared to native LocationManager.
 * Standard flavor for Google Play distribution.
 */
class PlayServicesLocationProvider(
    private val fusedLocationClient: FusedLocationProviderClient,
) : LocationProvider {

    // Fixes older than 24h (e.g. from before a flight) are not trusted as a location.
    private fun Location.isStale() = System.currentTimeMillis() - time > MAX_FIX_AGE_MS

    private companion object {
        const val MAX_FIX_AGE_MS = 24 * 60 * 60 * 1000L
    }

    override suspend fun getLastLocation(): Result<Pair<Double, Double>> {
        return try {
            val location = fusedLocationClient.lastLocation.await()
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
            AppLog.w("PlayLocation", "location request failed", e)
            Result.failure(e)
        }
    }

    override suspend fun getFreshLocation(): Result<Pair<Double, Double>> {
        return try {
            val location = withTimeoutOrNull(10_000L) {
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    CancellationTokenSource().token
                ).await()
            }

            val finalLocation = location ?: try {
                fusedLocationClient.lastLocation.await()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }

            if (finalLocation != null && (finalLocation === location || !finalLocation.isStale())) {
                Result.success(Pair(finalLocation.latitude, finalLocation.longitude))
            } else {
                Result.failure(Exception("Unable to get location"))
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: SecurityException) {
            Result.failure(Exception("Location permission not granted"))
        } catch (e: Exception) {
            AppLog.w("PlayLocation", "location request failed", e)
            Result.failure(e)
        }
    }
}
