package orinasa.njarasoa.maripanatokana.data.source

import orinasa.njarasoa.maripanatokana.data.remote.OpenWeatherMapApiService
import orinasa.njarasoa.maripanatokana.data.remote.toDomain
import orinasa.njarasoa.maripanatokana.data.settings.AppSettingsRepository
import orinasa.njarasoa.maripanatokana.domain.model.WeatherData
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenWeatherMapWeatherSource @Inject constructor(
    private val apiService: OpenWeatherMapApiService,
    private val settingsRepository: AppSettingsRepository,
) : WeatherDataSource {
    override val requiresApiKey = true
    override val displayName = "OpenWeatherMap"

    override suspend fun getForecast(lat: Double, lon: Double): WeatherData =
        apiService.getWeather(lat = lat, lon = lon, apiKey = settingsRepository.current.weatherApiKey).toDomain()
}
