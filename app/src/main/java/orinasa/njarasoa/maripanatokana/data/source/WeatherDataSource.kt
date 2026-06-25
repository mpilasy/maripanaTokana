package orinasa.njarasoa.maripanatokana.data.source

import orinasa.njarasoa.maripanatokana.domain.model.WeatherData

interface WeatherDataSource {
    suspend fun getForecast(lat: Double, lon: Double): WeatherData
    val requiresApiKey: Boolean
    val displayName: String
}
