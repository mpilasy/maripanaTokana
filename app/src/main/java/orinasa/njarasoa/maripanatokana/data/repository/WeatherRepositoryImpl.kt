package orinasa.njarasoa.maripanatokana.data.repository

import android.content.Context
import android.location.Geocoder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonPrimitive
import orinasa.njarasoa.maripanatokana.data.remote.BomApiService
import orinasa.njarasoa.maripanatokana.data.remote.EcccApiService
import orinasa.njarasoa.maripanatokana.data.remote.GdacsApiService
import orinasa.njarasoa.maripanatokana.data.remote.GeocodingResult
import orinasa.njarasoa.maripanatokana.data.remote.JmaApiService
import orinasa.njarasoa.maripanatokana.data.remote.JmaAreaCodes
import orinasa.njarasoa.maripanatokana.data.remote.MeteoAlarmApiService
import orinasa.njarasoa.maripanatokana.data.remote.NhcApiService
import orinasa.njarasoa.maripanatokana.data.remote.NwsApiService
import orinasa.njarasoa.maripanatokana.data.remote.WmoSwicApiService
import orinasa.njarasoa.maripanatokana.data.settings.AppSettingsRepository
import orinasa.njarasoa.maripanatokana.data.source.GeocodingSourceSelector
import orinasa.njarasoa.maripanatokana.data.source.WeatherSourceSelector
import orinasa.njarasoa.maripanatokana.domain.model.AlertLevel
import orinasa.njarasoa.maripanatokana.domain.model.WeatherAlert
import orinasa.njarasoa.maripanatokana.domain.model.WeatherData
import orinasa.njarasoa.maripanatokana.domain.repository.WeatherRepository
import orinasa.njarasoa.maripanatokana.ui.weather.supportedLocales
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
    private val wmoSwicApiService: WmoSwicApiService,
    private val bomApiService: BomApiService,
    private val nhcApiService: NhcApiService,
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

    override suspend fun fetchAlerts(lat: Double, lon: Double): Result<List<WeatherAlert>> = coroutineScope {
        val settings = settingsRepository.current
        if (!settings.alertsEnabled) return@coroutineScope Result.success(emptyList())

        val geoAddress = try {
            @Suppress("DEPRECATION")
            Geocoder(context, Locale.US).getFromLocation(lat, lon, 1)?.firstOrNull()
        } catch (_: Exception) { null }
        val countryCode = geoAddress?.countryCode?.lowercase()
        val australianStateCode = mapOf(
            "New South Wales" to "NSW", "Victoria" to "VIC", "Queensland" to "QLD",
            "Western Australia" to "WA", "South Australia" to "SA", "Tasmania" to "TAS",
            "Australian Capital Territory" to "ACT", "Northern Territory" to "NT"
        )[geoAddress?.adminArea ?: ""]

        val coveredByRegional = countryCode == "us" ||
            countryCode == "ca" ||
            countryCode == "au" ||
            countryCode in METEOALARM_COUNTRIES ||
            JmaAreaCodes.isInJapan(lat, lon)

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
                        WeatherAlert(level, p.event, p.description + (p.instruction?.let { "\n\n$it" } ?: ""), "nws", time, p.headline, f.id)
                    }
                } catch (e: Exception) {
                    emptyList()
                }
            }

            // 2. Global GDACS Alerts (skipped when a regional source covers the area)
            val gdacsDeferred = async {
                if (!settings.alertsGdacsEnabled || coveredByRegional) return@async emptyList<WeatherAlert>()
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

            // 3. MeteoAlarm (Europe)
            val meteoAlarmDeferred = async {
                if (!settings.alertsMeteoAlarmEnabled) return@async emptyList<WeatherAlert>()
                val code = countryCode ?: return@async emptyList()
                val slug = METEOALARM_SLUGS[code] ?: return@async emptyList()
                // subAdminArea = county/département; adminArea = region — prefer the more granular one
                val subdivision = geoAddress?.subAdminArea?.takeIf { it.isNotBlank() }
                    ?: geoAddress?.adminArea?.takeIf { it.isNotBlank() }
                try {
                    parseMeteoAlarmAtom(meteoAlarmApiService.getAlerts(slug).string(), subdivision)
                } catch (_: Exception) { emptyList() }
            }

            // 4. JMA (Japan)
            val jmaDeferred = async {
                if (!settings.alertsJmaEnabled) return@async emptyList<WeatherAlert>()
                if (!JmaAreaCodes.isInJapan(lat, lon)) return@async emptyList()
                try {
                    val areaCode = JmaAreaCodes.nearestPrefectureCode(lat, lon)
                    val response = jmaApiService.getWarnings(areaCode)
                    val isoParser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
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
                    warningMap.map { (warnName, pair) ->
                        WeatherAlert(pair.first, warnName, pair.second.joinToString(", "), "jma", null, null, null)
                    }
                } catch (_: Exception) { emptyList() }
            }

            // 5. ECCC (Canada)
            val ecccDeferred = async {
                if (!settings.alertsEcccEnabled) return@async emptyList<WeatherAlert>()
                if (countryCode != "ca") return@async emptyList()
                try {
                    val delta = 1.0
                    val bbox = String.format(Locale.US, "%.4f,%.4f,%.4f,%.4f", lon - delta, lat - delta, lon + delta, lat + delta)
                    val ecccParser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US)
                    ecccApiService.getAlerts(bbox = bbox).features.map { f ->
                        val p = f.properties
                        val level = when (p.alertType.lowercase()) {
                            "warning" -> AlertLevel.WARNING
                            else -> AlertLevel.WATCH
                        }
                        val time = p.publicationDatetime?.let { try { ecccParser.parse(it)?.time } catch (_: Exception) { null } }
                        WeatherAlert(level, p.alertNameEn.ifBlank { "ECCC Alert" }, p.alertTextEn, "eccc", time, null, null)
                    }
                } catch (_: Exception) { emptyList() }
            }

            // 6. WMO SWIC (Global, skipped when a regional source covers the area)
            val wmoDeferred = async {
                if (!settings.alertsWmoSwicEnabled || coveredByRegional) return@async emptyList<WeatherAlert>()
                val code = countryCode?.uppercase() ?: return@async emptyList()
                try {
                    wmoSwicApiService.getAlerts(code).Warning.map { w ->
                        WeatherAlert(AlertLevel.WARNING, w.Summary.ifBlank { "WMO SWIC Warning" }, w.Detail.ifBlank { w.Summary }, "wmoswic", null, w.City.ifBlank { null }, w.Url.ifBlank { null })
                    }
                } catch (_: Exception) { emptyList() }
            }

            // 7. BOM (Australia)
            val bomDeferred = async {
                if (!settings.alertsBomEnabled || countryCode != "au") return@async emptyList<WeatherAlert>()
                try {
                    val bomParser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
                    bomApiService.getWarnings().data
                        .filter { it.warningAction != "cancelled" }
                        .filter { w -> australianStateCode == null || w.states.isEmpty() || australianStateCode in w.states }
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
                } catch (_: Exception) { emptyList() }
            }

            // 8. NHC — National Hurricane Center (Atlantic + Eastern Pacific basins, proximity-filtered)
            val nhcDeferred = async {
                if (!settings.alertsNhcEnabled) return@async emptyList<WeatherAlert>()
                try {
                    val nhcParser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
                    nhcApiService.getCurrentStorms().activeStorms
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
                            WeatherAlert(level, "NHC: ${storm.name}", storm.headline.ifBlank { storm.name }, "nhc", time, null, storm.advisory?.url)
                        }
                } catch (_: Exception) { emptyList() }
            }

            val nwsAlerts = nwsDeferred.await()
            val gdacsAlerts = gdacsDeferred.await()
            val meteoAlarmAlerts = meteoAlarmDeferred.await()
            val jmaAlerts = jmaDeferred.await()
            val ecccAlerts = ecccDeferred.await()
            val wmoAlerts = wmoDeferred.await()
            val bomAlerts = bomDeferred.await()
            val nhcAlerts = nhcDeferred.await()

            val combinedAlerts = ArrayList<WeatherAlert>(
                nwsAlerts.size + gdacsAlerts.size + meteoAlarmAlerts.size + jmaAlerts.size +
                    ecccAlerts.size + wmoAlerts.size + bomAlerts.size + nhcAlerts.size
            )
            val keys = HashSet<String>()
            for (item in nwsAlerts) {
                val key = item.titleKey + item.source
                if (keys.add(key)) combinedAlerts.add(item)
            }
            for (item in gdacsAlerts) {
                val key = item.titleKey + item.source
                if (keys.add(key)) combinedAlerts.add(item)
            }
            for (item in meteoAlarmAlerts) {
                val key = item.titleKey + item.source
                if (keys.add(key)) combinedAlerts.add(item)
            }
            for (item in jmaAlerts) {
                val key = item.titleKey + item.source
                if (keys.add(key)) combinedAlerts.add(item)
            }
            for (item in ecccAlerts) {
                val key = item.titleKey + item.source
                if (keys.add(key)) combinedAlerts.add(item)
            }
            for (item in wmoAlerts) {
                val key = item.titleKey + item.source
                if (keys.add(key)) combinedAlerts.add(item)
            }
            for (item in bomAlerts) {
                val key = item.titleKey + item.source
                if (keys.add(key)) combinedAlerts.add(item)
            }
            for (item in nhcAlerts) {
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

    private fun normalizeArea(s: String): String =
        s.lowercase().replace('-', ' ').trim()
            .let { java.text.Normalizer.normalize(it, java.text.Normalizer.Form.NFD) }
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")

    private fun areaMatches(areaDesc: String, subdivision: String): Boolean {
        val a = normalizeArea(areaDesc)
        val b = normalizeArea(subdivision)
        if (a == b || a.contains(b) || b.contains(a)) return true
        // Word intersection: handles "Grad Zagreb" vs "Zagreb region" etc.
        val wordsA = a.split(Regex("\\s+")).filter { it.length >= 4 }
        val wordsB = b.split(Regex("\\s+")).filter { it.length >= 4 }
        return wordsA.any { it in wordsB }
    }

    private fun parseMeteoAlarmAtom(xml: String, subdivisionName: String? = null): List<WeatherAlert> {
        val alerts = mutableListOf<WeatherAlert>()
        val factory = XmlPullParserFactory.newInstance().apply { isNamespaceAware = true }
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xml))
        val CAP_NS = "urn:oasis:names:tc:emergency:cap:1.2"
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
                                val time = try { SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).parse(capOnset)?.time } catch (_: Exception) { null }
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
        private const val EARTH_RADIUS_KM = 6371.0
        private const val GDACS_SEARCH_RADIUS_KM = 500
        private const val GDACS_SEARCH_DAYS = 7
        private const val NHC_SEARCH_RADIUS_KM = 1500
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
