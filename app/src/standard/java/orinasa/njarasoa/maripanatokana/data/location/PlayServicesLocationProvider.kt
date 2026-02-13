package orinasa.njarasoa.maripanatokana.data.location

import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await

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
            val location = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                CancellationTokenSource().token
            ).await()

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
}
