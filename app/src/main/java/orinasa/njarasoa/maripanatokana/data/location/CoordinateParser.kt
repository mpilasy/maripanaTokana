package orinasa.njarasoa.maripanatokana.data.location

private val COORDS_PATTERN = Regex("^(-?\\d+(?:\\.\\d+)?)\\s*,\\s*(-?\\d+(?:\\.\\d+)?)$")

/** Parses "lat, lon" (integers or decimals). Returns null unless lat is in [-90, 90] and lon in [-180, 180]. */
internal fun parseCoordinates(query: String): Pair<Double, Double>? {
    val (latStr, lonStr) = COORDS_PATTERN.find(query.trim())?.destructured ?: return null
    val lat = latStr.toDoubleOrNull() ?: return null
    val lon = lonStr.toDoubleOrNull() ?: return null
    if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return null
    return lat to lon
}
