package orinasa.njarasoa.maripanatokana.data.remote

/**
 * Coarse coordinate fallbacks for when the on-device Geocoder fails to resolve a country code
 * (e.g. no geocoder backend available on some de-Googled/F-Droid devices) so country-gated
 * alert sources (NWS, ECCC, BOM) don't silently disappear. Mirrors JmaAreaCodes.isInJapan — a
 * false positive just costs one harmless empty fetch.
 */
object CountryBoundingBoxes {
    // Three sub-regions (rather than one wide box) to avoid a false positive over
    // Mexico/Cuba/the Caribbean.
    fun isInUS(lat: Double, lon: Double): Boolean {
        val conus = lat in 24.0..49.5 && lon in -125.0..-66.0
        val alaska = lat in 51.0..72.0 && lon in -180.0..-129.0
        val hawaii = lat in 18.5..22.5 && lon in -160.5..-154.5
        return conus || alaska || hawaii
    }

    fun isInCanada(lat: Double, lon: Double) = lat in 41.0..84.0 && lon in -141.0..-52.0

    // Matches web's isInAustralia (bom.ts) — isolated enough in lat/lon space that a single
    // box doesn't risk false-positiving over a neighboring country.
    fun isInAustralia(lat: Double, lon: Double) = lat in -44.0..-10.0 && lon in 113.0..154.0
}
