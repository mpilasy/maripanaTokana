package orinasa.njarasoa.maripanatokana.data.source

import orinasa.njarasoa.maripanatokana.data.remote.OpenMeteoApiService
import orinasa.njarasoa.maripanatokana.data.remote.toDomain
import orinasa.njarasoa.maripanatokana.domain.model.WeatherData
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenMeteoWeatherSource @Inject constructor(
    private val apiService: OpenMeteoApiService,
) : WeatherDataSource {
    override val requiresApiKey = false
    override val displayName = "Open-Meteo (default)"

    override suspend fun getForecast(lat: Double, lon: Double): WeatherData =
        apiService.getForecast(latitude = lat, longitude = lon).toDomain("", null)
}
