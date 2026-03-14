package orinasa.njarasoa.maripanatokana.data.repository

import android.location.Geocoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import orinasa.njarasoa.maripanatokana.data.remote.GdacsApiService
import orinasa.njarasoa.maripanatokana.BuildConfig
import orinasa.njarasoa.maripanatokana.data.remote.WeatherApiAlertsService
import orinasa.njarasoa.maripanatokana.data.remote.OpenMeteoApiService
import orinasa.njarasoa.maripanatokana.data.remote.toDomain
import orinasa.njarasoa.maripanatokana.domain.model.AlertLevel
import orinasa.njarasoa.maripanatokana.domain.model.WeatherAlert
import orinasa.njarasoa.maripanatokana.domain.model.WeatherData
import orinasa.njarasoa.maripanatokana.domain.repository.WeatherRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class WeatherRepositoryImpl @Inject constructor(
    private val apiService: OpenMeteoApiService,
    private val weatherApiAlertsService: WeatherApiAlertsService,
    private val gdacsApiService: GdacsApiService,
    private val geocoder: Geocoder,
) : WeatherRepository {

    override suspend fun getWeather(lat: Double, lon: Double): Result<WeatherData> = coroutineScope {
        try {
            val weatherDeferred = async { apiService.getForecast(latitude = lat, longitude = lon) }
            val weatherApiDeferred = async {
                try {
                    val point = String.format(Locale.US, "%.4f,%.4f", lat, lon)
                    val apiKey = BuildConfig.WEATHERAPI_KEY
                    val response = weatherApiAlertsService.getAlerts(key = apiKey, q = point)
                    response.alerts?.alert?.map { a ->
                        val level = if (a.severity == "Extreme" || a.severity == "Severe") AlertLevel.WARNING else AlertLevel.WATCH
                        val desc = a.desc ?: ""
                        val instruction = a.instruction?.let { "\n\n$it" } ?: ""
                        WeatherAlert(level, a.event ?: "Alert", desc + instruction, "official")
                    } ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }
            }
            val gdacsDeferred = async {
                try {
                    val toDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                    val fromDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000))
                    gdacsApiService.searchEvents(fromDate, toDate).features
                        .filter { f -> calculateDistance(lat, lon, f.geometry.coordinates[1], f.geometry.coordinates[0]) < 500 }
                        .map { f ->
                            val p = f.properties
                            val level = when (p.alertlevel) {
                                "red" -> AlertLevel.EMERGENCY
                                "orange" -> AlertLevel.WARNING
                                else -> AlertLevel.WATCH
                            }
                            WeatherAlert(level, "GDACS: ${p.eventtype} - ${p.name}", p.description, "gdacs")
                        }
                } catch (e: Exception) {
                    emptyList()
                }
            }

            val response = weatherDeferred.await()
            val weatherApiAlerts = weatherApiDeferred.await()
            val gdacsAlerts = gdacsDeferred.await()

            val locationName = try {
                withContext(Dispatchers.IO) {
                    @Suppress("DEPRECATION")
                    val addr = geocoder.getFromLocation(lat, lon, 1)?.firstOrNull()
                    addr?.locality
                        ?: addr?.subAdminArea
                        ?: addr?.adminArea
                } ?: "%.2f, %.2f".format(Locale.US, lat, lon)
            } catch (_: Exception) {
                "%.2f, %.2f".format(Locale.US, lat, lon)
            }
            
            val weatherData = response.toDomain(locationName)
            // Combine all alerts and remove exact duplicates based on title and source
            val combinedAlerts = (weatherApiAlerts + gdacsAlerts + weatherData.alerts)
                .distinctBy { it.titleKey + it.source }
            
            Result.success(weatherData.copy(alerts = combinedAlerts))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }
}
