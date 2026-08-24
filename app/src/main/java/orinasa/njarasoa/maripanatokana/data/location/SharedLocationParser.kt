package orinasa.njarasoa.maripanatokana.data.location

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.util.Locale

data class ParsedLocation(
    val latitude: Double,
    val longitude: Double,
    val name: String? = null
)

object SharedLocationParser {

    fun parseLocationText(text: String?): ParsedLocation? {
        var result: ParsedLocation? = null
        if (!text.isNullOrBlank()) {
            val input = text.trim()
            val geoMatch = parseGeoUri(input)
            val dmsMatch = parseDmsCoordinates(input)
            val urlCoords = parseUrlCoordinates(input)
            val rawCoords = parseRawCoordinates(input)

            val coords = geoMatch ?: dmsMatch ?: urlCoords ?: rawCoords
            if (coords != null) {
                val label = extractLabel(input)
                result = ParsedLocation(coords.first, coords.second, label)
            } else {
                val shortUrl = extractShortUrl(input)
                if (shortUrl != null) {
                    val resolvedUrl = resolveUrlRedirect(shortUrl)
                    if (resolvedUrl != null) {
                        val resolvedCoords = parseUrlCoordinates(resolvedUrl) ?: parseRawCoordinates(resolvedUrl)
                        if (resolvedCoords != null) {
                            val label = extractLabel(input) ?: extractPlaceNameFromUrl(resolvedUrl)
                            result = ParsedLocation(resolvedCoords.first, resolvedCoords.second, label)
                        }
                    }
                }
            }
        }
        return result
    }

    fun extractSearchQuery(text: String?): String? {
        var result: String? = null
        if (!text.isNullOrBlank()) {
            val label = extractLabel(text)
            val shortUrl = extractShortUrl(text)
            var placeFromUrl: String? = null
            if (shortUrl != null) {
                val resolved = resolveUrlRedirect(shortUrl)
                if (resolved != null) {
                    placeFromUrl = extractPlaceNameFromUrl(resolved)
                }
            }
            result = label ?: placeFromUrl
        }
        return result
    }

    private fun parseGeoUri(input: String): Pair<Double, Double>? {
        var result: Pair<Double, Double>? = null
        val regex = Regex("""geo:(?:0,0\?q=)?(-?\d{1,2}\.\d+)\s*,\s*(-?\d{1,3}\.\d+)""", RegexOption.IGNORE_CASE)
        val match = regex.find(input)
        if (match != null) {
            val lat = match.groupValues[1].toDoubleOrNull()
            val lon = match.groupValues[2].toDoubleOrNull()
            if (lat != null && lon != null && isValidCoordinate(lat, lon)) {
                result = Pair(lat, lon)
            }
        }
        return result
    }

    private fun parseUrlCoordinates(input: String): Pair<Double, Double>? {
        var result: Pair<Double, Double>? = null
        val atRegex = Regex("""/@(-?\d{1,2}\.\d+),(-?\d{1,3}\.\d+)""")
        val atMatch = atRegex.find(input)

        val qRegex = Regex("""(?:query|q|place)[=/](-?\d{1,2}\.\d+)\s*,\s*(-?\d{1,3}\.\d+)""", RegexOption.IGNORE_CASE)
        val qMatch = qRegex.find(input)

        val osmRegex = Regex("""#map=\d+/(-?\d{1,2}\.\d+)/(-?\d{1,3}\.\d+)""")
        val osmMatch = osmRegex.find(input)

        val paramRegex = Regex("""(?:mlat|ll)=(-?\d{1,2}\.\d+)[&,]?(?:mlon=)?(-?\d{1,3}\.\d+)""", RegexOption.IGNORE_CASE)
        val paramMatch = paramRegex.find(input)

        val match = atMatch ?: qMatch ?: osmMatch ?: paramMatch
        if (match != null) {
            val lat = match.groupValues[1].toDoubleOrNull()
            val lon = match.groupValues[2].toDoubleOrNull()
            if (lat != null && lon != null && isValidCoordinate(lat, lon)) {
                result = Pair(lat, lon)
            }
        }
        return result
    }

    private fun parseRawCoordinates(input: String): Pair<Double, Double>? {
        var result: Pair<Double, Double>? = null
        val regex = Regex("""(-?\d{1,2}\.\d+)\s*,\s*(-?\d{1,3}\.\d+)""")
        val match = regex.find(input)
        if (match != null) {
            val lat = match.groupValues[1].toDoubleOrNull()
            val lon = match.groupValues[2].toDoubleOrNull()
            if (lat != null && lon != null && isValidCoordinate(lat, lon)) {
                result = Pair(lat, lon)
            }
        }
        return result
    }

    private fun parseDmsCoordinates(input: String): Pair<Double, Double>? {
        var result: Pair<Double, Double>? = null
        val dmsRegex = Regex("""(\d+)[°\s]+(\d+)['\s]+(\d+(?:\.\d+)?)["\s]*([NSns])\s*[,]?\s*(\d+)[°\s]+(\d+)['\s]+(\d+(?:\.\d+)?)["\s]*([EWew])""")
        val match = dmsRegex.find(input)
        if (match != null) {
            val latDeg = match.groupValues[1].toDoubleOrNull() ?: 0.0
            val latMin = match.groupValues[2].toDoubleOrNull() ?: 0.0
            val latSec = match.groupValues[3].toDoubleOrNull() ?: 0.0
            val latDir = match.groupValues[4].uppercase(Locale.US)

            val lonDeg = match.groupValues[5].toDoubleOrNull() ?: 0.0
            val lonMin = match.groupValues[6].toDoubleOrNull() ?: 0.0
            val lonSec = match.groupValues[7].toDoubleOrNull() ?: 0.0
            val lonDir = match.groupValues[8].uppercase(Locale.US)

            var lat = latDeg + (latMin / 60.0) + (latSec / 3600.0)
            if (latDir == "S") {
                lat = -lat
            }

            var lon = lonDeg + (lonMin / 60.0) + (lonSec / 3600.0)
            if (lonDir == "W") {
                lon = -lon
            }

            if (isValidCoordinate(lat, lon)) {
                result = Pair(lat, lon)
            }
        }
        return result
    }

    private fun extractShortUrl(input: String): String? {
        var result: String? = null
        val shortUrlRegex = Regex("""https?://(?:maps\.app\.goo\.gl|goo\.gl/maps|t\.co|bit\.ly|tinyurl\.com)/[^\s]+""", RegexOption.IGNORE_CASE)
        val match = shortUrlRegex.find(input)
        if (match != null) {
            result = match.value
        }
        return result
    }

    private fun resolveUrlRedirect(urlString: String): String? {
        var result: String? = null
        var currentUrl = urlString
        var hops = 0
        val maxHops = 5

        while (hops < maxHops && currentUrl.isNotBlank()) {
            try {
                val connection = URL(currentUrl).openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = false
                connection.connectTimeout = 4000
                connection.readTimeout = 4000
                connection.requestMethod = "GET"
                connection.setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                )
                connection.connect()

                val statusCode = connection.responseCode
                val locationHeader = connection.getHeaderField("Location")

                if ((statusCode in 300..399) && !locationHeader.isNullOrBlank()) {
                    currentUrl = locationHeader
                    hops++
                    connection.disconnect()
                } else {
                    if (statusCode == 200) {
                        val bodyText = connection.inputStream.bufferedReader().use { it.readText() }
                        val metaMatch = Regex("""content=["']([^"']*@(-?\d+\.\d+),(-?\d+\.\d+)[^"']*)["']""", RegexOption.IGNORE_CASE).find(bodyText)
                        if (metaMatch != null) {
                            currentUrl = metaMatch.groupValues[1]
                        } else {
                            currentUrl = connection.url.toString()
                        }
                    } else {
                        currentUrl = connection.url.toString()
                    }
                    connection.disconnect()
                    result = currentUrl
                    hops = maxHops
                }
            } catch (_: Exception) {
                result = currentUrl.ifBlank { null }
                hops = maxHops
            }
        }

        if (result == null && currentUrl.isNotBlank()) {
            result = currentUrl
        }
        return result
    }

    private fun extractPlaceNameFromUrl(urlString: String): String? {
        var result: String? = null
        val placeRegex = Regex("""/maps/place/([^/@?]+)""", RegexOption.IGNORE_CASE)
        val match = placeRegex.find(urlString)
        if (match != null) {
            val rawName = match.groupValues[1].replace("+", " ")
            val decoded = runCatching { URLDecoder.decode(rawName, "UTF-8") }.getOrDefault(rawName)
            if (decoded.isNotBlank()) {
                result = decoded.trim()
            }
        }
        return result
    }

    private fun extractLabel(input: String): String? {
        var result: String? = null
        val lines = input.lines()
        val nonUrlLines = lines.filter { line ->
            !line.contains("http://") && !line.contains("https://") && !line.contains("geo:")
        }
        if (nonUrlLines.isNotEmpty()) {
            val candidate = nonUrlLines.first().trim()
            if (candidate.isNotBlank() && candidate != "Dropped pin") {
                result = candidate
            }
        }
        return result
    }

    private fun isValidCoordinate(lat: Double, lon: Double): Boolean {
        var valid = false
        if (lat >= -90.0 && lat <= 90.0 && lon >= -180.0 && lon <= 180.0) {
            valid = true
        }
        return valid
    }
}
