package orinasa.njarasoa.maripanatokana.domain.repository

import orinasa.njarasoa.maripanatokana.data.remote.GeocodingResult
import orinasa.njarasoa.maripanatokana.domain.model.WeatherAlert
import orinasa.njarasoa.maripanatokana.domain.model.WeatherData

interface WeatherRepository {
    suspend fun getWeather(lat: Double, lon: Double): Result<WeatherData>
    suspend fun fetchAlerts(lat: Double, lon: Double): Result<List<WeatherAlert>>
    suspend fun searchLocation(query: String): Result<List<GeocodingResult>>
}
