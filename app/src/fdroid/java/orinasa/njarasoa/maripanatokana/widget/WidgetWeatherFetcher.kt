package orinasa.njarasoa.maripanatokana.widget

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import orinasa.njarasoa.maripanatokana.domain.model.WeatherData

/**
 * Standalone weather fetcher for Glance widgets (no Hilt).
 * F-Droid flavor using native LocationManager (no Google Play Services).
 */
object WidgetWeatherFetcher : BaseWidgetWeatherFetcher() {

    @SuppressLint("MissingPermission")
    suspend fun fetch(context: Context): WeatherData? {
        return fetchInternal(context) { getCoordinates(context) }
    }

    @SuppressLint("MissingPermission")
    private fun getCoordinates(context: Context): Pair<Double, Double>? {
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

            // Try GPS first, then network
            val gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            val location = when {
                gpsLocation != null && networkLocation != null -> {
                    if (gpsLocation.time >= networkLocation.time) gpsLocation else networkLocation
                }
                gpsLocation != null -> gpsLocation
                networkLocation != null -> networkLocation
                else -> null
            }

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
