package orinasa.njarasoa.maripanatokana.data.source

import orinasa.njarasoa.maripanatokana.data.remote.PirateWeatherApiService
import orinasa.njarasoa.maripanatokana.data.remote.toDomain
import orinasa.njarasoa.maripanatokana.data.settings.AppSettingsRepository
import orinasa.njarasoa.maripanatokana.domain.model.WeatherData
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PirateWeatherSource @Inject constructor(
    private val apiService: PirateWeatherApiService,
    private val settingsRepository: AppSettingsRepository,
) : WeatherDataSource {
    override val requiresApiKey = true
    override val displayName = "Pirate Weather"

    override suspend fun getForecast(lat: Double, lon: Double): WeatherData =
        apiService.getForecast(apiKey = settingsRepository.current.weatherApiKey, lat = lat, lon = lon).toDomain()
}
