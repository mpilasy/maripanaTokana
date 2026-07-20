package orinasa.njarasoa.maripanatokana.data.source

import android.content.Context
import android.location.Geocoder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import orinasa.njarasoa.maripanatokana.data.remote.OpenMeteoAirQualityApiService
import orinasa.njarasoa.maripanatokana.data.remote.OpenMeteoApiService
import orinasa.njarasoa.maripanatokana.data.remote.toDomain
import orinasa.njarasoa.maripanatokana.domain.model.WeatherData
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenMeteoWeatherSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: OpenMeteoApiService,
    private val airQualityApiService: OpenMeteoAirQualityApiService,
) : WeatherDataSource {
    override val requiresApiKey = false
    override val displayName = "Open-Meteo (default)"

    override suspend fun getForecast(lat: Double, lon: Double): WeatherData = coroutineScope {
        val forecastDeferred = async { apiService.getForecast(latitude = lat, longitude = lon) }
        val airQualityDeferred = async {
            try {
                airQualityApiService.getAirQuality(latitude = lat, longitude = lon)
            } catch (e: Exception) {
                null
            }
        }
        // Country decides which AQI standard is primary (european_aqi vs us_aqi) — same
        // Geocoder used for alert-source gating in WeatherRepositoryImpl.fetchAlerts().
        val countryCodeDeferred = async(Dispatchers.IO) {
            try {
                @Suppress("DEPRECATION")
                Geocoder(context, Locale.US).getFromLocation(lat, lon, 1)?.firstOrNull()?.countryCode?.lowercase()
            } catch (e: Exception) {
                null
            }
        }

        val airQualityResult = airQualityDeferred.await()?.toDomain(countryCodeDeferred.await())
        forecastDeferred.await().toDomain("", null).copy(
            airQuality = airQualityResult?.current,
            hourlyAirQuality = airQualityResult?.hourly ?: emptyList(),
        )
    }
}
