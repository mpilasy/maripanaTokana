package orinasa.njarasoa.maripanatokana.data.source

import orinasa.njarasoa.maripanatokana.util.AppLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import orinasa.njarasoa.maripanatokana.data.remote.OpenMeteoAirQualityApiService
import orinasa.njarasoa.maripanatokana.data.remote.OpenMeteoApiService
import orinasa.njarasoa.maripanatokana.data.remote.toDomain
import orinasa.njarasoa.maripanatokana.data.repository.CountryResolver
import orinasa.njarasoa.maripanatokana.domain.model.WeatherData
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenMeteoWeatherSource @Inject constructor(
    private val apiService: OpenMeteoApiService,
    private val airQualityApiService: OpenMeteoAirQualityApiService,
    private val countryResolver: CountryResolver,
) : WeatherDataSource {
    override val requiresApiKey = false
    override val displayName = "Open-Meteo (default)"

    override suspend fun getForecast(lat: Double, lon: Double): WeatherData = coroutineScope {
        val forecastDeferred = async { retryOnce { apiService.getForecast(latitude = lat, longitude = lon) } }
        val airQualityDeferred = async {
            try {
                airQualityApiService.getAirQuality(latitude = lat, longitude = lon)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.w("OpenMeteo", "air quality failed", e)
                null
            }
        }
        // Country decides which AQI standard is primary (european_aqi vs us_aqi) — same
        // resolver used for alert-source gating in WeatherRepositoryImpl.fetchAlerts().
        val countryCodeDeferred = async {
            try {
                countryResolver.resolve(lat, lon)?.countryCode
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.w("OpenMeteo", "country lookup failed", e)
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
