package orinasa.njarasoa.maripanatokana.widget

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import orinasa.njarasoa.maripanatokana.data.remote.OpenMeteoApiService
import orinasa.njarasoa.maripanatokana.data.remote.toDomain
import orinasa.njarasoa.maripanatokana.domain.model.WeatherData
import retrofit2.Retrofit
import java.util.Locale

/**
 * Standalone weather fetcher for Glance widgets (no Hilt).
 * Builds its own Retrofit client and uses lastLocation for speed.
 */
object WidgetWeatherFetcher {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val api: OpenMeteoApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(OpenMeteoApiService::class.java)
    }

    @SuppressLint("MissingPermission")
    suspend fun fetch(context: Context): WeatherData? {
        return try {
            val (lat, lon) = getCoordinates(context) ?: return null

            val response = api.getForecast(latitude = lat, longitude = lon)

            val locationName = try {
                @Suppress("DEPRECATION")
                Geocoder(context, Locale.getDefault())
                    .getFromLocation(lat, lon, 1)
                    ?.firstOrNull()
                    ?.locality
                    ?: "%.2f, %.2f".format(Locale.US, lat, lon)
            } catch (_: Exception) {
                "%.2f, %.2f".format(Locale.US, lat, lon)
            }

            response.toDomain(locationName)
        } catch (_: Exception) {
            null
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getCoordinates(context: Context): Pair<Double, Double>? {
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        val location = fusedClient.lastLocation.await()
            ?: fusedClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                CancellationTokenSource().token,
            ).await()

        if (location != null) {
            return Pair(location.latitude, location.longitude)
        }

        // Fall back to last coordinates saved by the main app
        val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        val lat = prefs.getFloat("lat", Float.MIN_VALUE)
        val lon = prefs.getFloat("lon", Float.MIN_VALUE)
        if (lat == Float.MIN_VALUE) return null
        return Pair(lat.toDouble(), lon.toDouble())
    }
}
