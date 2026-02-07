package orinasa.njarasoa.maripanatokana.domain.repository

import orinasa.njarasoa.maripanatokana.domain.model.WeatherData

interface WeatherRepository {
    suspend fun getWeather(lat: Double, lon: Double): Result<WeatherData>
}
