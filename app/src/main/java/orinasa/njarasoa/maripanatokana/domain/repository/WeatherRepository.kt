package orinasa.njarasoa.maripanatokana.domain.repository

import orinasa.njarasoa.maripanatokana.data.remote.GeocodingResult
import orinasa.njarasoa.maripanatokana.domain.model.WeatherAlert
import orinasa.njarasoa.maripanatokana.domain.model.WeatherData

/** [failedSources]: display names of enabled, applicable alert sources that threw. */
data class AlertsResult(val alerts: List<WeatherAlert>, val failedSources: List<String>)

interface WeatherRepository {
    suspend fun getWeather(lat: Double, lon: Double): Result<WeatherData>
    suspend fun fetchAlerts(lat: Double, lon: Double): Result<AlertsResult>
    suspend fun searchLocation(query: String): Result<List<GeocodingResult>>
}
