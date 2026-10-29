// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonPrimitive
import orinasa.njarasoa.maripanatokana.data.location.PlusCode
import orinasa.njarasoa.maripanatokana.data.location.isLatinScript
import orinasa.njarasoa.maripanatokana.data.remote.BomApiService
import orinasa.njarasoa.maripanatokana.data.remote.CountryBoundingBoxes
import orinasa.njarasoa.maripanatokana.data.remote.EcccApiService
import orinasa.njarasoa.maripanatokana.data.remote.GdacsAlertResponse
import orinasa.njarasoa.maripanatokana.data.remote.GdacsApiService
import orinasa.njarasoa.maripanatokana.data.remote.GeocodingResult
import orinasa.njarasoa.maripanatokana.data.remote.JmaApiService
import orinasa.njarasoa.maripanatokana.data.remote.JmaAreaCodes
import orinasa.njarasoa.maripanatokana.data.remote.MeteoAlarmApiService
import orinasa.njarasoa.maripanatokana.data.remote.NhcApiService
import orinasa.njarasoa.maripanatokana.data.remote.NwsApiService
import orinasa.njarasoa.maripanatokana.data.settings.AppSettingsRepository
import orinasa.njarasoa.maripanatokana.data.source.GeocodingSourceSelector
import orinasa.njarasoa.maripanatokana.data.source.WeatherSourceSelector
import orinasa.njarasoa.maripanatokana.domain.model.AlertLevel
import orinasa.njarasoa.maripanatokana.domain.model.WeatherAlert
import orinasa.njarasoa.maripanatokana.domain.model.WeatherData
import orinasa.njarasoa.maripanatokana.domain.repository.AlertsResult
import orinasa.njarasoa.maripanatokana.domain.repository.WeatherRepository
import orinasa.njarasoa.maripanatokana.data.remote.dedupeByPlace
import orinasa.njarasoa.maripanatokana.ui.weather.supportedLocales
import orinasa.njarasoa.maripanatokana.util.AppLog
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class WeatherRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val nwsApiService: NwsApiService,
    private val gdacsApiService: GdacsApiService,
    private val meteoAlarmApiService: MeteoAlarmApiService,
    private val jmaApiService: JmaApiService,
    private val ecccApiService: EcccApiService,
    private val bomApiService: BomApiService,
    private val nhcApiService: NhcApiService,
    private val settingsRepository: AppSettingsRepository,
    private val weatherSourceSelector: WeatherSourceSelector,
    private val geocodingSelector: GeocodingSourceSelector,
    private val countryResolver: CountryResolver,
) : WeatherRepository {

    // The GDACS feed lists events for the whole world: one download serves all places for a few minutes
    private val gdacsFeed = ExpiringValue<Pair<String, String>, GdacsAlertResponse>(GDACS_FEED_TTL_MS)

    private val prefs get() = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)

    private fun currentLocale(): Locale {
        val idx = prefs.getInt("locale_index", 0).coerceIn(supportedLocales.indices)
        return Locale.forLanguageTag(supportedLocales[idx].tag)
    }

    override suspend fun searchLocation(query: String): Result<List<GeocodingResult>> {
        return try {
            val plusCode = PlusCode.find(query)
            if (plusCode != null) Result.success(searchPlusCode(plusCode))
            else Result.success(geocodingSelector.current().searchLocations(query, currentLocale()).dedupeByPlace())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun romanPlaceName(lat: Double, lon: Double): Pair<String, String?>? = try {
        geocodingSelector.current().reverseGeocode(lat, lon, Locale.ENGLISH).takeIf { isLatinScript(it.first) }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        AppLog.w(TAG, "roman reverse geocode failed", e)
        null
    }

    override suspend fun localPlaceName(lat: Double, lon: Double): Pair<String, String?>? = try {
        geocodingSelector.current().reverseGeocode(lat, lon, currentLocale())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        AppLog.w(TAG, "local reverse geocode failed", e)
        null
    }

    /** A full code is its own location; a short one is resolved near the place named with it. */
    private suspend fun searchPlusCode(match: PlusCode.Match): List<GeocodingResult> {
        if (match.isFull) {
            val (lat, lon) = PlusCode.decode(match.code)
            return listOf(GeocodingResult(id = 0, name = match.code, latitude = lat, longitude = lon))
        }
        if (match.locality.isBlank()) return emptyList()
        val place = geocodingSelector.current().searchLocations(match.locality, currentLocale()).firstOrNull()
            ?: return emptyList()
        val (lat, lon) = PlusCode.recoverNearest(match.code, place.latitude, place.longitude)
        return listOf(place.copy(id = 0, latitude = lat, longitude = lon, featureCode = null))
    }

    override suspend fun getWeather(lat: Double, lon: Double): Result<WeatherData> = withContext(Dispatchers.IO) {
        try {
            // The place name does not depend on the forecast: look it up while the forecast loads
            val (sourceData, place) = coroutineScope {
                val placeDeferred = async {
                    try {
                        geocodingSelector.current().reverseGeocode(lat, lon, currentLocale())
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        AppLog.w(TAG, "reverse geocode failed", e)
                        "%.2f, %.2f".format(Locale.US, lat, lon) to null
                    }
                }
                weatherSourceSelector.current().getForecast(lat, lon) to placeDeferred.await()
            }
            val (locationName, locationSubtext) = place

            Result.success(sourceData.copy(
                locationName = locationName,
                locationSubtext = locationSubtext,
                latitude = lat,
                longitude = lon,
                alertsLoading = true,
                // derived alerts from the source are preserved and passed to fetchAlerts later
            ))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLog.w(TAG, "getWeather failed", e)
            Result.failure(e)
        }
    }

    override suspend fun fetchAlerts(lat: Double, lon: Double): Result<AlertsResult> = coroutineScope {
        val settings = settingsRepository.current
        if (!settings.alertsEnabled) return@coroutineScope Result.success(AlertsResult(emptyList(), emptyList()))

        val countryInfo = countryResolver.resolve(lat, lon)
        val countryCode = countryInfo?.countryCode

        // Coordinate-based fallbacks so a Geocoder failure (e.g. no geocoder backend on some
        // de-Googled/F-Droid devices) doesn't silently suppress a country-gated source.
        val inUS = countryCode == "us" || CountryBoundingBoxes.isInUS(lat, lon)
        val inCanada = countryCode == "ca" || CountryBoundingBoxes.isInCanada(lat, lon)
        val inAustralia = countryCode == "au" || CountryBoundingBoxes.isInAustralia(lat, lon)
        val inJapan = JmaAreaCodes.isInJapan(lat, lon)
        val meteoAlarmSlug = countryCode?.let { METEOALARM_SLUGS[it] }

        val coveredByRegional = inUS || inCanada || inAustralia || countryCode in METEOALARM_COUNTRIES || inJapan

        val failed = java.util.concurrent.CopyOnWriteArrayList<String>()

        // Each source runs at the same time; a failure gives no alerts from that source and adds its name to `failed`
        fun source(name: String, enabled: Boolean, fetch: suspend () -> List<WeatherAlert>) = async {
            if (!enabled) return@async emptyList()
            try {
                fetch()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                failed.add(name)
                AppLog.w(TAG, "$name alerts failed", e)
                emptyList()
            }
        }

        try {
            val sources = listOf(
                source("NWS", settings.alertsNwsEnabled && inUS) { nwsAlerts(lat, lon) },
                // Skipped when a regional source covers the area
                source("GDACS", settings.alertsGdacsEnabled && !coveredByRegional) { gdacsAlerts(lat, lon) },
                source("MeteoAlarm", settings.alertsMeteoAlarmEnabled) {
                    meteoAlarmSlug?.let { meteoAlarmAlerts(it, countryInfo?.subdivision) } ?: emptyList()
                },
                source("JMA", settings.alertsJmaEnabled && inJapan) { jmaAlerts(lat, lon) },
                source("ECCC", settings.alertsEcccEnabled && inCanada) { ecccAlerts(lat, lon) },
                source("BOM", settings.alertsBomEnabled && inAustralia) { bomAlerts(countryInfo?.adminArea) },
                source("NHC", settings.alertsNhcEnabled) { nhcAlerts(lat, lon) },
            ).map { it.await() }

            val keys = HashSet<String>()
            val combinedAlerts = sources.flatten().filter { keys.add(it.titleKey + it.source) }
            Result.success(AlertsResult(combinedAlerts, ALERT_SOURCE_ORDER.filter { it in failed }))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Official NWS alerts (United States). */
    private suspend fun nwsAlerts(lat: Double, lon: Double): List<WeatherAlert> {
        val point = String.format(Locale.US, "%.4f,%.4f", lat, lon)
        val nwsParser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
        return nwsApiService.getActiveAlerts(point).features.map { f ->
            val p = f.properties
            val level = if (p.severity == "Extreme" || p.severity == "Severe") AlertLevel.WARNING else AlertLevel.WATCH
            val time = p.sent?.let {
                try { nwsParser.parse(it)?.time } catch (_: Exception) { null }
            }
            WeatherAlert(level, p.event, p.description + (p.instruction?.let { "\n\n$it" } ?: ""), "nws", time, p.headline, f.id)
        }
    }

    /** Global GDACS alerts within GDACS_SEARCH_RADIUS_KM. */
    private suspend fun gdacsAlerts(lat: Double, lon: Double): List<WeatherAlert> {
        val toDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val fromDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(System.currentTimeMillis() - GDACS_SEARCH_DAYS * 24 * 60 * 60 * 1000L))
        val gdacsParser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)

        val latDelta = GDACS_SEARCH_RADIUS_KM / 111.0
        val lonDelta = if (Math.abs(lat) < 89.0) GDACS_SEARCH_RADIUS_KM / (111.0 * Math.cos(Math.toRadians(lat))) else 360.0
        val minLat = lat - latDelta
        val maxLat = lat + latDelta

        return gdacsFeed.get(fromDate to toDate) { gdacsApiService.searchEvents(fromDate, toDate) }.features
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
    }

    /** MeteoAlarm alerts (Europe) for the country feed [slug], kept to [subdivision] when it is known. */
    private suspend fun meteoAlarmAlerts(slug: String, subdivision: String?): List<WeatherAlert> =
        parseMeteoAlarmAtom(meteoAlarmApiService.getAlerts(slug).string(), subdivision)

    /** JMA warnings (Japan), one alert for each warning type with the prefectures it covers. */
    private suspend fun jmaAlerts(lat: Double, lon: Double): List<WeatherAlert> {
        val areaCode = JmaAreaCodes.nearestPrefectureCode(lat, lon)
        val response = jmaApiService.getWarnings(areaCode)
        val warningMap = linkedMapOf<String, Pair<AlertLevel, LinkedHashSet<String>>>()
        response.areaTypes.flatMap { it.areas }.forEach { area ->
            val areaName = JmaAreaCodes.prefectureName(area.code)
            area.warnings
                .filter { it.status == "発表" || it.status == "継続" }
                .forEach { w ->
                    val level = when {
                        w.code == "01" -> AlertLevel.EMERGENCY
                        w.code.toIntOrNull()?.let { it <= 8 } == true -> AlertLevel.WARNING
                        else -> AlertLevel.WATCH
                    }
                    val warnName = jmaWarningName(w.code)
                    val existing = warningMap[warnName]
                    if (existing == null) {
                        warningMap[warnName] = Pair(level, linkedSetOf<String>().also { if (areaName != null) it.add(areaName) })
                    } else {
                        if (areaName != null) existing.second.add(areaName)
                        if (level.ordinal > existing.first.ordinal) warningMap[warnName] = Pair(level, existing.second)
                    }
                }
        }
        return warningMap.map { (warnName, pair) ->
            WeatherAlert(pair.first, warnName, pair.second.joinToString(", "), "jma", null, null, null)
        }
    }

    /** ECCC alerts (Canada) within about 1° of the place. */
    private suspend fun ecccAlerts(lat: Double, lon: Double): List<WeatherAlert> {
        val delta = 1.0
        val bbox = String.format(Locale.US, "%.4f,%.4f,%.4f,%.4f", lon - delta, lat - delta, lon + delta, lat + delta)
        val ecccParser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US)
        return ecccApiService.getAlerts(bbox = bbox).features.map { f ->
            val p = f.properties
            val level = when (p.alertType.lowercase()) {
                "warning" -> AlertLevel.WARNING
                else -> AlertLevel.WATCH
            }
            val time = p.publicationDatetime?.let { try { ecccParser.parse(it)?.time } catch (_: Exception) { null } }
            WeatherAlert(level, p.alertNameEn.ifBlank { "ECCC Alert" }, p.alertTextEn, "eccc", time, null, null)
        }
    }

    /** BOM warnings (Australia) for the state named [adminArea], or all states when it is not known. */
    private suspend fun bomAlerts(adminArea: String?): List<WeatherAlert> {
        val stateCode = AUSTRALIAN_STATE_CODES[adminArea ?: ""]
        val bomParser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
        return bomApiService.getWarnings().data
            .filter { it.warningAction != "cancelled" }
            .filter { w -> stateCode == null || w.states.isEmpty() || stateCode in w.states }
            .map { w ->
                val level = when (w.phase.lowercase()) {
                    "warning" -> AlertLevel.WARNING
                    else -> AlertLevel.WATCH
                }
                val time = w.issueTime?.let { try { bomParser.parse(it)?.time } catch (_: Exception) { null } }
                val eventType = w.shortTitle.ifBlank { w.shortDescription }
                val area = if (w.state.isNotBlank()) "${w.state}: ${w.title}" else w.title
                WeatherAlert(level, eventType, area, "bom", time, null, null)
            }
    }

    /** NHC (National Hurricane Center) storms in the Atlantic and Eastern Pacific within NHC_SEARCH_RADIUS_KM. */
    private suspend fun nhcAlerts(lat: Double, lon: Double): List<WeatherAlert> {
        val nhcParser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
        return nhcApiService.getCurrentStorms().activeStorms
            .filter { calculateDistance(lat, lon, it.lat, it.lon) < NHC_SEARCH_RADIUS_KM }
            .map { storm ->
                val knots = storm.intensity.toIntOrNull() ?: 0
                val level = when {
                    storm.classification == "HU" && knots >= 96 -> AlertLevel.EMERGENCY
                    storm.classification == "HU" -> AlertLevel.WARNING
                    storm.classification == "TS" -> AlertLevel.WARNING
                    else -> AlertLevel.WATCH
                }
                val time = storm.advisory?.issuance?.let { try { nhcParser.parse(it)?.time } catch (_: Exception) { null } }

                val classificationName = NHC_CLASSIFICATION_NAMES[storm.classification]
                val title = if (classificationName != null) "$classificationName ${storm.name}" else "NHC: ${storm.name}"

                val statsParts = mutableListOf<String>()
                if (knots > 0) statsParts.add("winds ~${Math.round(knots * 1.15078)} mph")
                storm.pressure.toIntOrNull()?.let { statsParts.add("central pressure $it mb") }
                storm.movementSpeed?.let { speed ->
                    statsParts.add(
                        if (speed > 0) "moving ${compassFromDegrees(storm.movementDir ?: 0)} at $speed mph"
                        else "stationary"
                    )
                }
                val statsSentence = statsParts.joinToString(", ").ifBlank { null }
                    ?.replaceFirstChar { it.uppercase() }?.plus(".")

                val distanceKm = Math.round(calculateDistance(lat, lon, storm.lat, storm.lon))
                val distanceMi = Math.round(distanceKm * 0.621371)
                val distanceSentence = "~$distanceKm km ($distanceMi mi) from your location."

                val description = listOfNotNull(statsSentence, distanceSentence).joinToString(" ").ifBlank { storm.name }

                WeatherAlert(level, title, description, "nhc", time, null, storm.advisory?.url)
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

    private fun compassFromDegrees(deg: Int): String {
        val idx = Math.round((((deg % 360) + 360) % 360) / 22.5).toInt() % 16
        return NHC_COMPASS_DIRECTIONS[idx]
    }

    private fun normalizeArea(s: String): String =
        s.lowercase().replace('-', ' ').trim()
            .let { java.text.Normalizer.normalize(it, java.text.Normalizer.Form.NFD) }
            .replace(DIACRITICS, "")

    private fun areaMatches(areaDesc: String, subdivision: String): Boolean {
        val a = normalizeArea(areaDesc)
        val b = normalizeArea(subdivision)
        // Whole words only: a plain substring match let "Ain" match "Maine et Loire"
        val tokensA = areaWords(a)
        val tokensB = areaWords(b)
        if (a == b || containsWords(tokensA, tokensB) || containsWords(tokensB, tokensA)) return true
        // Word intersection: handles "Grad Zagreb" vs "Zagreb region" etc.
        val wordsA = tokensA.filter { it.length >= 4 }
        val wordsB = tokensB.filter { it.length >= 4 }
        return wordsA.any { it in wordsB }
    }

    private fun areaWords(s: String): List<String> =
        s.split(NON_WORD).filter { it.isNotEmpty() }

    /** True when [needle] occurs in [hay] as a run of whole words. */
    private fun containsWords(hay: List<String>, needle: List<String>): Boolean =
        needle.isNotEmpty() && hay.windowed(needle.size).any { it == needle }

    private fun parseMeteoAlarmAtom(xml: String, subdivisionName: String? = null): List<WeatherAlert> {
        val alerts = mutableListOf<WeatherAlert>()
        val factory = XmlPullParserFactory.newInstance().apply { isNamespaceAware = true }
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xml))
        val CAP_NS = "urn:oasis:names:tc:emergency:cap:1.2"
        // One parser for all entries; a feed can hold hundreds
        val onsetParser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
        var inEntry = false
        var currentLocalName = ""
        var currentNs = ""
        var capEvent = ""; var capSeverity = ""; var capOnset = ""; var capStatus = ""
        var capDescription = ""; var capAreaDesc = ""; var linkHref: String? = null
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    currentLocalName = parser.name ?: ""
                    currentNs = parser.namespace ?: ""
                    if (currentLocalName == "entry" && currentNs != CAP_NS) {
                        inEntry = true; capEvent = ""; capSeverity = ""; capOnset = ""; capStatus = ""; capDescription = ""; capAreaDesc = ""; linkHref = null
                    } else if (inEntry && currentLocalName == "link" && parser.getAttributeValue(null, "rel") == "alternate") {
                        linkHref = parser.getAttributeValue(null, "href")
                    }
                }
                XmlPullParser.TEXT -> if (inEntry && currentNs == CAP_NS) {
                    val text = parser.text ?: ""
                    when (currentLocalName) {
                        "event" -> capEvent = text
                        "severity" -> capSeverity = text
                        "onset" -> capOnset = text
                        "status" -> capStatus = text
                        "description" -> capDescription = text
                        "areaDesc" -> capAreaDesc = text
                    }
                }
                XmlPullParser.END_TAG -> {
                    if ((parser.name ?: "") == "entry" && (parser.namespace ?: "") != CAP_NS && inEntry) {
                        inEntry = false
                        if (capEvent.isNotBlank() && (capStatus.isBlank() || capStatus == "Actual")) {
                            // Filter to user's subdivision when known
                            if (subdivisionName != null && capAreaDesc.isNotBlank() && !areaMatches(capAreaDesc, subdivisionName)) {
                                // skip — alert is for a different area
                            } else {
                                val level = when (capSeverity.lowercase()) {
                                    "extreme" -> AlertLevel.EMERGENCY
                                    "severe" -> AlertLevel.WARNING
                                    else -> AlertLevel.WATCH
                                }
                                val time = try { onsetParser.parse(capOnset)?.time } catch (_: Exception) { null }
                                alerts.add(WeatherAlert(level, capEvent, capDescription, "meteoalarm", time, capAreaDesc.ifBlank { null }, linkHref))
                            }
                        }
                    }
                    currentLocalName = ""; currentNs = ""
                }
            }
            event = parser.next()
        }
        return alerts
    }

    private fun jmaWarningName(code: String) = when (code) {
        "01" -> "Special Warning"
        "02" -> "Heavy Rain Warning"
        "03" -> "Flood Warning"
        "04" -> "Storm Warning"
        "05" -> "Snowstorm Warning"
        "06" -> "Heavy Snow Warning"
        "07" -> "Wave Warning"
        "08" -> "Storm Surge Warning"
        "10" -> "Heavy Rain Advisory"
        "12" -> "Strong Wind Advisory"
        "13" -> "Wave Advisory"
        "14" -> "Storm Surge Advisory"
        "16" -> "Flood Advisory"
        "17" -> "Frost Advisory"
        "18" -> "Thunder Advisory"
        "19" -> "Dry Advisory"
        "20" -> "Dense Fog Advisory"
        "21" -> "Low Temperature Advisory"
        "22" -> "Heavy Snow Advisory"
        else -> "Weather Warning ($code)"
    }

    companion object {
        private const val TAG = "WeatherRepo"
        private val ALERT_SOURCE_ORDER = listOf("NWS", "GDACS", "MeteoAlarm", "JMA", "ECCC", "BOM", "NHC")
        private const val EARTH_RADIUS_KM = 6371.0
        // Compiled once: area matching runs for each MeteoAlarm entry
        private val DIACRITICS = Regex("\\p{InCombiningDiacriticalMarks}+")
        private val NON_WORD = Regex("[^\\p{L}\\p{N}]+")
        private const val GDACS_SEARCH_RADIUS_KM = 500
        private const val GDACS_SEARCH_DAYS = 7
        private const val GDACS_FEED_TTL_MS = 10 * 60 * 1000L
        private const val NHC_SEARCH_RADIUS_KM = 1500
        private val NHC_CLASSIFICATION_NAMES = mapOf(
            "TD" to "Tropical Depression", "SD" to "Subtropical Depression",
            "TS" to "Tropical Storm", "SS" to "Subtropical Storm",
            "HU" to "Hurricane", "EX" to "Post-Tropical Cyclone",
            "PTC" to "Potential Tropical Cyclone", "LO" to "Remnant Low",
            "DB" to "Disturbance", "WV" to "Tropical Wave",
        )
        private val AUSTRALIAN_STATE_CODES = mapOf(
            "New South Wales" to "NSW", "Victoria" to "VIC", "Queensland" to "QLD",
            "Western Australia" to "WA", "South Australia" to "SA", "Tasmania" to "TAS",
            "Australian Capital Territory" to "ACT", "Northern Territory" to "NT"
        )
        private val NHC_COMPASS_DIRECTIONS = arrayOf(
            "N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
            "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW",
        )
        private val METEOALARM_SLUGS = mapOf(
            "at" to "austria", "ba" to "bosnia-herzegovina", "be" to "belgium",
            "bg" to "bulgaria", "hr" to "croatia", "cy" to "cyprus", "cz" to "czechia",
            "dk" to "denmark", "ee" to "estonia", "fi" to "finland", "fr" to "france",
            "de" to "germany", "gr" to "greece", "hu" to "hungary", "ie" to "ireland",
            "it" to "italy", "lv" to "latvia", "lt" to "lithuania", "lu" to "luxembourg",
            "mt" to "malta", "md" to "moldova", "me" to "montenegro", "nl" to "netherlands",
            "mk" to "republic-of-north-macedonia", "no" to "norway", "pl" to "poland",
            "pt" to "portugal", "ro" to "romania", "rs" to "serbia", "sk" to "slovakia",
            "si" to "slovenia", "es" to "spain", "se" to "sweden", "ch" to "switzerland",
            "ua" to "ukraine", "gb" to "united-kingdom"
        )
        private val METEOALARM_COUNTRIES = METEOALARM_SLUGS.keys
    }
}
