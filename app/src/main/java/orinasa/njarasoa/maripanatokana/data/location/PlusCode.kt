package orinasa.njarasoa.maripanatokana.data.location

import kotlin.math.floor

/**
 * Plus Codes (Open Location Code, an open standard: https://github.com/google/open-location-code).
 * Full codes ("6GCRPR6C+24") decode offline; short codes ("PR6C+24 Nairobi") need a nearby
 * reference point, usually the place named next to them. Keep in sync with web/src/lib/domain/plusCode.ts.
 */
object PlusCode {
    private const val ALPHABET = "23456789CFGHJMPQRVWX"

    /** A code found in free text, plus the rest of the text (the locality, for short codes). */
    data class Match(val code: String, val isFull: Boolean, val locality: String)

    private val FULL = Regex("""(?<![A-Z0-9])[$ALPHABET]{8}\+[$ALPHABET]{2,}(?![A-Z0-9])""", RegexOption.IGNORE_CASE)
    private val SHORT = Regex("""(?<![A-Z0-9])(?:[$ALPHABET]{4}|[$ALPHABET]{6})\+[$ALPHABET]{2,}(?![A-Z0-9])""", RegexOption.IGNORE_CASE)

    // Typed without the "+": a whole word of 6-7 (short) or 10-11 (full) code characters that
    // mixes letters and digits, so all-digit postcodes and plain words don't match
    private val NO_PLUS = Regex("""(?<![A-Z0-9+])(?=[$ALPHABET]*[0-9])(?=[$ALPHABET]*[A-Z])(?:[$ALPHABET]{10,11}|[$ALPHABET]{6,7})(?![A-Z0-9+])""", RegexOption.IGNORE_CASE)

    fun find(text: String): Match? {
        val full = FULL.find(text)
        val m = full ?: SHORT.find(text) ?: NO_PLUS.find(text) ?: return null
        val raw = m.value.uppercase()
        val code = when {
            '+' in raw -> raw
            raw.length >= 10 -> raw.substring(0, 8) + "+" + raw.substring(8)
            else -> raw.substring(0, 4) + "+" + raw.substring(4)
        }
        val isFull = code.indexOf('+') == 8
        if (isFull && !isValidFullPrefix(code)) return null
        val locality = text.removeRange(m.range).trim(' ', ',', '\n', '\t')
        return Match(code, isFull, locality)
    }

    /** Standard 10-digit code (about 14 m x 14 m) for a point, e.g. "6GCRPR6C+24". */
    fun encode(lat: Double, lon: Double): String = prefix(lat, lon, 10).let { it.take(8) + "+" + it.drop(8) }

    /** Centre of a full code's area as (lat, lon). */
    fun decode(fullCode: String): Pair<Double, Double> {
        val digits = fullCode.uppercase().replace("+", "")
        var lat = -90.0
        var lon = -180.0
        var pairValue = 20.0
        var latSize = 0.0
        var lonSize = 0.0
        var i = 0
        while (i < minOf(digits.length, 10)) {
            lat += ALPHABET.indexOf(digits[i]) * pairValue
            lon += ALPHABET.indexOf(digits[i + 1]) * pairValue
            latSize = pairValue
            lonSize = pairValue
            pairValue /= 20
            i += 2
        }
        // Beyond 10 digits each one picks a cell in a 4-column, 5-row grid
        while (i < digits.length) {
            latSize /= 5
            lonSize /= 4
            val d = ALPHABET.indexOf(digits[i])
            lat += (d / 4) * latSize
            lon += (d % 4) * lonSize
            i++
        }
        return minOf(lat + latSize / 2, 90.0) to lon + lonSize / 2
    }

    /** Centre of a short code's area, choosing the cell nearest to (refLat, refLon). */
    fun recoverNearest(shortCode: String, refLat: Double, refLon: Double): Pair<Double, Double> {
        val code = shortCode.uppercase()
        val prefixLength = 8 - code.indexOf('+')
        val resolution = Math.pow(20.0, 2.0 - prefixLength / 2)
        val (lat, lon) = decode(prefix(refLat, refLon, prefixLength) + code)
        val half = resolution / 2
        val nearLat = when {
            refLat + half < lat && lat - resolution >= -90 -> lat - resolution
            refLat - half > lat && lat + resolution <= 90 -> lat + resolution
            else -> lat
        }
        val nearLon = when {
            refLon + half < lon -> lon - resolution
            refLon - half > lon -> lon + resolution
            else -> lon
        }
        return nearLat to ((nearLon + 540) % 360 - 180)
    }

    private fun prefix(lat: Double, lon: Double, length: Int): String {
        var latV = (lat.coerceIn(-90.0, 90.0) + 90).coerceAtMost(179.999999)
        var lonV = ((lon + 180) % 360 + 360) % 360
        var pairValue = 20.0
        val sb = StringBuilder()
        repeat(length / 2) {
            val a = floor(latV / pairValue).toInt()
            val b = floor(lonV / pairValue).toInt()
            sb.append(ALPHABET[a]).append(ALPHABET[b])
            latV -= a * pairValue
            lonV -= b * pairValue
            pairValue /= 20
        }
        return sb.toString()
    }

    // First digit encodes latitude in 20° bands (max 9 bands), second longitude (max 18)
    private fun isValidFullPrefix(code: String) =
        ALPHABET.indexOf(code[0]) < 9 && ALPHABET.indexOf(code[1]) < 18
}
