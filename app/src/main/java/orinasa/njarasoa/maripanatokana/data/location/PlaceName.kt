package orinasa.njarasoa.maripanatokana.data.location

/**
 * Shortens a geocoder place name to its first segment. Splits only on ',', ';' and a
 * spaced hyphen (" - "), so names like "Saint-Denis" or "Aix-en-Provence" stay intact.
 */
internal fun shortPlaceName(raw: String): String =
    raw.split(",")[0].split(";")[0].split(" - ")[0].trim()
