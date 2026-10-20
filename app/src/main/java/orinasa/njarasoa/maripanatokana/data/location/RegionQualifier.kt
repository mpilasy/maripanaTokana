package orinasa.njarasoa.maripanatokana.data.location

// Postal abbreviations people type after a city name ("Sudbury, ON", "Austin, TX")
private val REGION_ABBREVIATIONS = mapOf(
    "AL" to "Alabama", "AK" to "Alaska", "AZ" to "Arizona", "AR" to "Arkansas", "CA" to "California",
    "CO" to "Colorado", "CT" to "Connecticut", "DE" to "Delaware", "DC" to "Washington, D.C.", "FL" to "Florida",
    "GA" to "Georgia", "HI" to "Hawaii", "ID" to "Idaho", "IL" to "Illinois", "IN" to "Indiana", "IA" to "Iowa",
    "KS" to "Kansas", "KY" to "Kentucky", "LA" to "Louisiana", "ME" to "Maine", "MD" to "Maryland",
    "MA" to "Massachusetts", "MI" to "Michigan", "MN" to "Minnesota", "MS" to "Mississippi", "MO" to "Missouri",
    "MT" to "Montana", "NE" to "Nebraska", "NV" to "Nevada", "NH" to "New Hampshire", "NJ" to "New Jersey",
    "NM" to "New Mexico", "NY" to "New York", "NC" to "North Carolina", "ND" to "North Dakota", "OH" to "Ohio",
    "OK" to "Oklahoma", "OR" to "Oregon", "PA" to "Pennsylvania", "RI" to "Rhode Island", "SC" to "South Carolina",
    "SD" to "South Dakota", "TN" to "Tennessee", "TX" to "Texas", "UT" to "Utah", "VT" to "Vermont",
    "VA" to "Virginia", "WA" to "Washington", "WV" to "West Virginia", "WI" to "Wisconsin", "WY" to "Wyoming",
    "PR" to "Puerto Rico",
    "AB" to "Alberta", "BC" to "British Columbia", "MB" to "Manitoba", "NB" to "New Brunswick",
    "NL" to "Newfoundland and Labrador", "NS" to "Nova Scotia", "NT" to "Northwest Territories", "NU" to "Nunavut",
    "ON" to "Ontario", "PE" to "Prince Edward Island", "QC" to "Quebec", "SK" to "Saskatchewan", "YT" to "Yukon",
)

/**
 * Whether the text after the comma names one of these regions: a postal abbreviation of the state or
 * province, or the start of a word in it ("on" is Ontario, not the end of "Washington").
 */
fun matchesRegionQualifier(qualifier: String, vararg regions: String?): Boolean {
    val q = qualifier.trim().lowercase()
    if (q.isEmpty()) return true
    val expanded = REGION_ABBREVIATIONS[q.uppercase()]?.lowercase()
    return regions.filterNotNull().map { it.lowercase() }.any { region ->
        region == expanded || region.startsWith(q) || region.contains(" $q") || region.contains("-$q")
    }
}
