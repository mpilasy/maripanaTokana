package orinasa.njarasoa.maripanatokana.widget

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.content.edit
import android.location.Geocoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import orinasa.njarasoa.maripanatokana.data.remote.OpenMeteoApiService
import orinasa.njarasoa.maripanatokana.data.remote.OpenMeteoResponse
import orinasa.njarasoa.maripanatokana.data.remote.toDomain
import orinasa.njarasoa.maripanatokana.domain.model.WeatherData
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType
import java.util.Locale

/**
 * Base logic for standalone weather fetcher for Glance widgets.
 */
open class BaseWidgetWeatherFetcher(private val baseUrl: String = "https://api.open-meteo.com/") {

    protected val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    protected val api: OpenMeteoApiService by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(OpenMeteoApiService::class.java)
    }

    @SuppressLint("MissingPermission")
    suspend fun fetchInternal(context: Context, getCoordinates: suspend () -> Pair<Double, Double>?): WeatherData? {
        val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        return try {
            val (lat, lon) = getCoordinates() ?: throw Exception("No coordinates")

            val response = api.getForecast(latitude = lat, longitude = lon)

            val locationName = prefs.getString("location_name", null)
                ?: try {
                    withContext(Dispatchers.IO) {
                        @Suppress("DEPRECATION")
                        Geocoder(context, Locale.getDefault())
                            .getFromLocation(lat, lon, 1)
                            ?.firstOrNull()
                            ?.locality
                    } ?: "%.2f, %.2f".format(Locale.US, lat, lon)
                } catch (_: Exception) {
                    "%.2f, %.2f".format(Locale.US, lat, lon)
                }

            // Cache the response and location name
            prefs.edit {
                putString("cached_response", json.encodeToString(OpenMeteoResponse.serializer(), response))
                putString("cached_location_name", locationName)
                putLong("cached_timestamp", System.currentTimeMillis())
            }

            response.toDomain(locationName)
        } catch (_: Exception) {
            val cachedJson = prefs.getString("cached_response", null)
            val cachedName = prefs.getString("cached_location_name", null)
            val cachedTimestamp = prefs.getLong("cached_timestamp", 0L)
            
            if (cachedJson != null && cachedName != null) {
                try {
                    val response = json.decodeFromString(OpenMeteoResponse.serializer(), cachedJson)
                    response.toDomain(cachedName).copy(timestamp = cachedTimestamp)
                } catch (_: Exception) {
                    null
                }
            } else {
                null
            }
        }
    }
}
