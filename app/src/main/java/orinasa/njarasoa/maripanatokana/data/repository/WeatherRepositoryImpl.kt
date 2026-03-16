package orinasa.njarasoa.maripanatokana.data.repository

import android.location.Geocoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonPrimitive
import orinasa.njarasoa.maripanatokana.data.remote.GdacsApiService
import orinasa.njarasoa.maripanatokana.data.remote.GeocodingResult
import orinasa.njarasoa.maripanatokana.data.remote.NwsApiService
import orinasa.njarasoa.maripanatokana.data.remote.OpenMeteoApiService
import orinasa.njarasoa.maripanatokana.data.remote.OpenMeteoGeocodingService
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
    private val nwsApiService: NwsApiService,
    private val gdacsApiService: GdacsApiService,
    private val geocodingApiService: OpenMeteoGeocodingService,
    private val geocoder: Geocoder,
) : WeatherRepository {

    override suspend fun searchLocation(query: String): Result<List<GeocodingResult>> {
        return try {
            val response = geocodingApiService.searchLocation(name = query)
            Result.success(response.results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getWeather(lat: Double, lon: Double): Result<WeatherData> = coroutineScope {
        try {
            val weatherDeferred = async { apiService.getForecast(latitude = lat, longitude = lon) }
            val nwsDeferred = async { 
                try {
                    // Use Locale.US to ensure dot decimal separator for NWS API
                    val point = String.format(Locale.US, "%.4f,%.4f", lat, lon)
                    nwsApiService.getActiveAlerts(point).features.map { f ->
                        val p = f.properties
                        val level = if (p.severity == "Extreme" || p.severity == "Severe") AlertLevel.WARNING else AlertLevel.WATCH
                        val time = p.sent?.let {
                            try {
                                val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
                                parser.parse(it)?.time
                            } catch (_: Exception) { null }
                        }
                        WeatherAlert(level, p.event, p.description + (p.instruction?.let { "\n\n$it" } ?: ""), "official", time, p.headline, f.id)
                    }
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
                            val time = p.fromdate?.let {
                                try {
                                    val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                                    parser.parse(it)?.time
                                } catch (_: Exception) { null }
                            }
                            val reportUrl = try { p.url?.get("report")?.jsonPrimitive?.content } catch (_: Exception) { null }
                            WeatherAlert(level, "GDACS: ${p.eventtype} - ${p.name}", p.description, "gdacs", time, null, reportUrl)
                        }
                } catch (e: Exception) {
                    emptyList()
                }
            }

            val response = weatherDeferred.await()
            val nwsAlerts = nwsDeferred.await()
            val gdacsAlerts = gdacsDeferred.await()

            val (locationName, locationSubtext) = try {
                withContext(Dispatchers.IO) {
                    @Suppress("DEPRECATION")
                    val addr = geocoder.getFromLocation(lat, lon, 1)?.firstOrNull()
                    val rawName = addr?.locality
                        ?: addr?.subAdminArea
                        ?: addr?.adminArea
                        ?: "%.2f, %.2f".format(Locale.US, lat, lon)
                    
                    // Bolt: Strictly take only the first part before any common separators
                    val name = rawName.split(",")[0].split(";")[0].split("-")[0].trim()

                    val subtext = if (addr != null) {
                        val parts = mutableListOf<String>()
                        // Bolt: Ensure subtext doesn't repeat the main name even if it's a partial match
                        if (addr.adminArea != null && !name.contains(addr.adminArea) && !addr.adminArea.contains(name) && !rawName.contains(addr.adminArea)) {
                            parts.add(addr.adminArea)
                        }
                        if (addr.countryName != null) parts.add(addr.countryName)
                        if (parts.isNotEmpty()) parts.joinToString(", ") else null
                    } else null

                    name to subtext
                }
            } catch (_: Exception) {
                "%.2f, %.2f".format(Locale.US, lat, lon) to null
            }

            val weatherData = response.toDomain(locationName, locationSubtext)
            // Combine all alerts and remove exact duplicates based on title and source
            val combinedAlerts = (nwsAlerts + gdacsAlerts + weatherData.alerts)
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
