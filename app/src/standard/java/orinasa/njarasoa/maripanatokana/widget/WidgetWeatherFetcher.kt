package orinasa.njarasoa.maripanatokana.widget

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await
import orinasa.njarasoa.maripanatokana.domain.model.WeatherData

/**
 * Standalone weather fetcher for Glance widgets (no Hilt).
 * Standard flavor using Google Play Services for location.
 */
object WidgetWeatherFetcher : BaseWidgetWeatherFetcher() {

    @SuppressLint("MissingPermission")
    suspend fun fetch(context: Context): WeatherData? {
        return fetchInternal(context) { getCoordinates(context) }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getCoordinates(context: Context): Pair<Double, Double>? {
        try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            val location = fusedClient.lastLocation.await()
                ?: fusedClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    CancellationTokenSource().token,
                ).await()

            if (location != null) {
                return Pair(location.latitude, location.longitude)
            }
        } catch (_: SecurityException) {
            // Background context lacks location permission — fall through to SharedPreferences
        }

        // Fall back to last coordinates saved by the main app
        val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        val lat = prefs.getFloat("lat", Float.MIN_VALUE)
        val lon = prefs.getFloat("lon", Float.MIN_VALUE)
        if (lat == Float.MIN_VALUE) return null
        return Pair(lat.toDouble(), lon.toDouble())
    }
}
