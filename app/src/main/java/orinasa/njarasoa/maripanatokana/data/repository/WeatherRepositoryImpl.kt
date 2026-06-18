package orinasa.njarasoa.maripanatokana.data.repository

import android.content.Context
import android.location.Geocoder
import dagger.hilt.android.qualifiers.ApplicationContext
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
import orinasa.njarasoa.maripanatokana.ui.weather.supportedLocales
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class WeatherRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: OpenMeteoApiService,
    private val nwsApiService: NwsApiService,
    private val gdacsApiService: GdacsApiService,
    private val geocodingApiService: OpenMeteoGeocodingService,
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
                val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
                val localeIdx = prefs.getInt("locale_index", 0).coerceIn(supportedLocales.indices)
                val geocoder = Geocoder(context, Locale.forLanguageTag(supportedLocales[localeIdx].tag))
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

    override suspend fun fetchAlerts(lat: Double, lon: Double, derivedAlerts: List<WeatherAlert>): Result<List<WeatherAlert>> = coroutineScope {
        try {
            // 1. Official NWS Alerts
            val nwsDeferred = async {
                try {
                    val point = String.format(Locale.US, "%.4f,%.4f", lat, lon)
                    nwsApiService.getActiveAlerts(point).features.map { f ->
                        val p = f.properties
                        val level = if (p.severity == "Extreme" || p.severity == "Severe") AlertLevel.WARNING else AlertLevel.WATCH
                        val time = p.sent?.let {
                            try {
                                localNwsParser.get()?.parse(it)?.time
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
                    val dateFormat = localDateFormat.get()
                    val toDate = dateFormat?.format(Date()) ?: ""
                    val fromDate = dateFormat?.format(Date(System.currentTimeMillis() - GDACS_SEARCH_DAYS * 24 * 60 * 60 * 1000L)) ?: ""

                    val latDelta = GDACS_SEARCH_RADIUS_KM / 111.0
                    val lonDelta = if (Math.abs(lat) < 89.0) GDACS_SEARCH_RADIUS_KM / (111.0 * Math.cos(Math.toRadians(lat))) else 360.0
                    val minLat = lat - latDelta
                    val maxLat = lat + latDelta

                    gdacsApiService.searchEvents(fromDate, toDate).features
                        .filter { f ->
                            val fLat = f.geometry.coordinates[1]
                            val fLon = f.geometry.coordinates[0]
                            if (fLat < minLat || fLat > maxLat) return@filter false

                            val dLon = Math.abs(fLon - lon)
                            val shortestDLon = if (dLon > 180.0) 360.0 - dLon else dLon
                            if (shortestDLon > lonDelta) return@filter false

                            calculateDistance(lat, lon, fLat, fLon) < GDACS_SEARCH_RADIUS_KM
                        }
                        .map { f ->
                            val p = f.properties
                            val level = when (p.alertlevel) {
                                "red" -> AlertLevel.EMERGENCY
                                "orange" -> AlertLevel.WARNING
                                else -> AlertLevel.WATCH
                            }
                            val time = p.fromdate?.let {
                                try {
                                    localGdacsParser.get()?.parse(it)?.time
                                } catch (_: Exception) { null }
                            }
                            val reportUrl = try { p.url?.get("report")?.jsonPrimitive?.content } catch (_: Exception) { null }
                            WeatherAlert(level, "GDACS: ${p.eventtype} - ${p.name}", p.description, "gdacs", time, null, reportUrl)
                        }
                } catch (e: Exception) {
                    emptyList()
                }
            }

            val nwsAlerts = nwsDeferred.await()
            val gdacsAlerts = gdacsDeferred.await()

            val combinedAlerts = ArrayList<WeatherAlert>(nwsAlerts.size + gdacsAlerts.size + derivedAlerts.size)
            val keys = HashSet<String>()
            for (item in nwsAlerts) {
                val key = item.titleKey + item.source
                if (keys.add(key)) combinedAlerts.add(item)
            }
            for (item in gdacsAlerts) {
                val key = item.titleKey + item.source
                if (keys.add(key)) combinedAlerts.add(item)
            }
            for (item in derivedAlerts) {
                val key = item.titleKey + item.source
                if (keys.add(key)) combinedAlerts.add(item)
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

        // Bolt: Cache SimpleDateFormat instances using ThreadLocal to avoid expensive recreation in coroutines
        private val localNwsParser = object : ThreadLocal<SimpleDateFormat>() {
            override fun initialValue() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
        }
        private val localDateFormat = object : ThreadLocal<SimpleDateFormat>() {
            override fun initialValue() = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        }
        private val localGdacsParser = object : ThreadLocal<SimpleDateFormat>() {
            override fun initialValue() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        }
    }
}
