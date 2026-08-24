package orinasa.njarasoa.maripanatokana.data.location

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

    override suspend fun getLastLocation(): Result<Pair<Double, Double>> {
        return try {
            val location = fusedLocationClient.lastLocation.await()
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
            val location = withTimeoutOrNull(10_000L) {
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    CancellationTokenSource().token
                ).await()
            }

            val finalLocation = location ?: try { fusedLocationClient.lastLocation.await() } catch (_: Exception) { null }

            if (finalLocation != null) {
                Result.success(Pair(finalLocation.latitude, finalLocation.longitude))
            } else {
                Result.failure(Exception("Unable to get location"))
            }
        } catch (e: SecurityException) {
            Result.failure(Exception("Location permission not granted"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
