package orinasa.njarasoa.maripanatokana.data.source

import orinasa.njarasoa.maripanatokana.data.settings.AppSettingsRepository
import orinasa.njarasoa.maripanatokana.domain.model.WeatherSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeatherSourceSelector @Inject constructor(
    private val settingsRepository: AppSettingsRepository,
    private val openMeteo: OpenMeteoWeatherSource,
    private val openWeatherMap: OpenWeatherMapWeatherSource,
    private val pirateWeather: PirateWeatherSource,
) {
    fun current(): WeatherDataSource = when (settingsRepository.current.weatherSource) {
        WeatherSource.OPEN_METEO -> openMeteo
        WeatherSource.OPEN_WEATHER_MAP -> openWeatherMap
        WeatherSource.PIRATE_WEATHER -> pirateWeather
    }
}
