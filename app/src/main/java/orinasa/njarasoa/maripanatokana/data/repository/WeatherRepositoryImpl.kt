package orinasa.njarasoa.maripanatokana.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonPrimitive
import orinasa.njarasoa.maripanatokana.data.remote.GdacsApiService
import orinasa.njarasoa.maripanatokana.data.remote.GeocodingResult
import orinasa.njarasoa.maripanatokana.data.remote.NwsApiService
import orinasa.njarasoa.maripanatokana.data.settings.AppSettingsRepository
import orinasa.njarasoa.maripanatokana.data.source.GeocodingSourceSelector
import orinasa.njarasoa.maripanatokana.data.source.WeatherSourceSelector
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
    private val nwsApiService: NwsApiService,
    private val gdacsApiService: GdacsApiService,
    private val settingsRepository: AppSettingsRepository,
    private val weatherSourceSelector: WeatherSourceSelector,
    private val geocodingSelector: GeocodingSourceSelector,
) : WeatherRepository {

    private val prefs get() = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)

    private fun currentLocale(): Locale {
        val idx = prefs.getInt("locale_index", 0).coerceIn(supportedLocales.indices)
        return Locale.forLanguageTag(supportedLocales[idx].tag)
    }

    override suspend fun searchLocation(query: String): Result<List<GeocodingResult>> {
        return try {
            Result.success(geocodingSelector.current().searchLocations(query, currentLocale()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getWeather(lat: Double, lon: Double): Result<WeatherData> = withContext(Dispatchers.IO) {
        try {
            val sourceData = weatherSourceSelector.current().getForecast(lat, lon)

            val (locationName, locationSubtext) = try {
                geocodingSelector.current().reverseGeocode(lat, lon, currentLocale())
            } catch (_: Exception) {
                "%.2f, %.2f".format(Locale.US, lat, lon) to null
            }

            Result.success(sourceData.copy(
                locationName = locationName,
                locationSubtext = locationSubtext,
                alertsLoading = true,
                // derived alerts from the source are preserved and passed to fetchAlerts later
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun fetchAlerts(lat: Double, lon: Double, derivedAlerts: List<WeatherAlert>): Result<List<WeatherAlert>> = coroutineScope {
        val settings = settingsRepository.current
        if (!settings.alertsEnabled) return@coroutineScope Result.success(emptyList())

        try {
            // 1. Official NWS Alerts
            val nwsDeferred = async {
                if (!settings.alertsNwsEnabled) return@async emptyList<WeatherAlert>()
                try {
                    val point = String.format(Locale.US, "%.4f,%.4f", lat, lon)
                    val nwsParser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
                    nwsApiService.getActiveAlerts(point).features.map { f ->
                        val p = f.properties
                        val level = if (p.severity == "Extreme" || p.severity == "Severe") AlertLevel.WARNING else AlertLevel.WATCH
                        val time = p.sent?.let {
                            try { nwsParser.parse(it)?.time } catch (_: Exception) { null }
                        }
                        WeatherAlert(level, p.event, p.description + (p.instruction?.let { "\n\n$it" } ?: ""), "official", time, p.headline, f.id)
                    }
                } catch (e: Exception) {
                    emptyList()
                }
            }

            // 2. Global GDACS Alerts
            val gdacsDeferred = async {
                if (!settings.alertsGdacsEnabled) return@async emptyList<WeatherAlert>()
                try {
                    val toDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                    val fromDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(System.currentTimeMillis() - GDACS_SEARCH_DAYS * 24 * 60 * 60 * 1000L))
                    val gdacsParser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)

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
                                try { gdacsParser.parse(it)?.time } catch (_: Exception) { null }
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
            if (settings.alertsDerivedEnabled) {
                for (item in derivedAlerts) {
                    val key = item.titleKey + item.source
                    if (keys.add(key)) combinedAlerts.add(item)
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
