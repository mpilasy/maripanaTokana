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

    override suspend fun getWeather(lat: Double, lon: Double): Result<WeatherData> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getForecast(latitude = lat, longitude = lon)

            val (locationName, locationSubtext) = try {
                @Suppress("DEPRECATION")
                val addr = geocoder.getFromLocation(lat, lon, 1)?.firstOrNull()
                val rawName = addr?.locality
                    ?: addr?.subAdminArea
                    ?: addr?.adminArea
                    ?: "%.2f, %.2f".format(Locale.US, lat, lon)
                
                val name = rawName.split(",")[0].split(";")[0].split("-")[0].trim()

                val subtext = if (addr != null) {
                    val parts = mutableListOf<String>()
                    if (addr.adminArea != null && !name.contains(addr.adminArea) && !addr.adminArea.contains(name) && !rawName.contains(addr.adminArea)) {
                        parts.add(addr.adminArea)
                    }
                    if (addr.countryName != null) parts.add(addr.countryName)
                    if (parts.isNotEmpty()) parts.joinToString(", ") else null
                } else null

                name to subtext
            } catch (_: Exception) {
                "%.2f, %.2f".format(Locale.US, lat, lon) to null
            }

            val weatherData = response.toDomain(locationName, locationSubtext)
            Result.success(weatherData.copy(alertsLoading = true, alerts = emptyList()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun fetchAlerts(lat: Double, lon: Double): Result<List<WeatherAlert>> = coroutineScope {
        try {
            // 1. Official NWS Alerts
            val nwsDeferred = async { 
                try {
                    val point = String.format(Locale.US, "%.4f,%.4f", lat, lon)
                    val nwsParser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
                    nwsApiService.getActiveAlerts(point).features.map { f ->
                        val p = f.properties
                        val level = if (p.severity == "Extreme" || p.severity == "Severe") AlertLevel.WARNING else AlertLevel.WATCH
                        val time = p.sent?.let {
                            try {
                                nwsParser.parse(it)?.time
                            } catch (_: Exception) { null }
                        }
                        WeatherAlert(level, p.event, p.description + (p.instruction?.let { "\n\n$it" } ?: ""), "official", time, p.headline, f.id)
                    }
                } catch (e: Exception) {
                    emptyList()
                }
            }

            // 2. Global GDACS Alerts
            val gdacsDeferred = async {
                try {
                    val toDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                    val fromDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(System.currentTimeMillis() - GDACS_SEARCH_DAYS * 24 * 60 * 60 * 1000L))
                    val gdacsParser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                    gdacsApiService.searchEvents(fromDate, toDate).features
                        .filter { f -> calculateDistance(lat, lon, f.geometry.coordinates[1], f.geometry.coordinates[0]) < GDACS_SEARCH_RADIUS_KM }
                        .map { f ->
                            val p = f.properties
                            val level = when (p.alertlevel) {
                                "red" -> AlertLevel.EMERGENCY
                                "orange" -> AlertLevel.WARNING
                                else -> AlertLevel.WATCH
                            }
                            val time = p.fromdate?.let {
                                try {
                                    gdacsParser.parse(it)?.time
                                } catch (_: Exception) { null }
                            }
                            val reportUrl = try { p.url?.get("report")?.jsonPrimitive?.content } catch (_: Exception) { null }
                            WeatherAlert(level, "GDACS: ${p.eventtype} - ${p.name}", p.description, "gdacs", time, null, reportUrl)
                        }
                } catch (e: Exception) {
                    emptyList()
                }
            }

            // 3. Derived Alerts (from Open-Meteo)
            val weatherDeferred = async { apiService.getForecast(latitude = lat, longitude = lon) }

            val nwsAlerts = nwsDeferred.await()
            val gdacsAlerts = gdacsDeferred.await()
            val weatherResponse = weatherDeferred.await()
            val weatherData = weatherResponse.toDomain("temp", null) // location name doesn't matter for alerts

            // Optimize: Avoid intermediate lists from `+` and LinkedHashMap from `distinctBy`
            val combinedAlerts = ArrayList<WeatherAlert>(nwsAlerts.size + gdacsAlerts.size + weatherData.alerts.size)
            val seenKeys = HashSet<String>()

            for (alert in nwsAlerts) {
                if (seenKeys.add(alert.titleKey + alert.source)) {
                    combinedAlerts.add(alert)
                }
            }
            for (alert in gdacsAlerts) {
                if (seenKeys.add(alert.titleKey + alert.source)) {
                    combinedAlerts.add(alert)
                }
            }
            for (alert in weatherData.alerts) {
                if (seenKeys.add(alert.titleKey + alert.source)) {
                    combinedAlerts.add(alert)
                }
            }
            
            Result.success(combinedAlerts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = EARTH_RADIUS_KM
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }

    companion object {
        private const val EARTH_RADIUS_KM = 6371.0
        private const val GDACS_SEARCH_RADIUS_KM = 500
        private const val GDACS_SEARCH_DAYS = 7
    }
}
