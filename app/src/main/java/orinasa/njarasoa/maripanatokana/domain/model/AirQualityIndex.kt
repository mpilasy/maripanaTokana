package orinasa.njarasoa.maripanatokana.domain.model

enum class AqiStandard { US, EUROPEAN }

enum class AqiTier { GOOD, MODERATE, UNHEALTHY, VERY_UNHEALTHY, HAZARDOUS }

data class AirQualityIndex(
    val usValue: Int,
    val europeanValue: Int,
    val primaryStandard: AqiStandard,
) {
    val usTier: AqiTier
        get() = when {
            usValue <= 50 -> AqiTier.GOOD
            usValue <= 100 -> AqiTier.MODERATE
            usValue <= 200 -> AqiTier.UNHEALTHY
            usValue <= 300 -> AqiTier.VERY_UNHEALTHY
            else -> AqiTier.HAZARDOUS
        }

    val europeanTier: AqiTier
        get() = when {
            europeanValue < 20 -> AqiTier.GOOD
            europeanValue < 40 -> AqiTier.MODERATE
            europeanValue < 60 -> AqiTier.UNHEALTHY
            europeanValue < 100 -> AqiTier.VERY_UNHEALTHY
            else -> AqiTier.HAZARDOUS
        }

    /**
     * Dual-unit display. Unlike Temperature/Pressure, primary/secondary here is NOT tied to the
     * metric/imperial toggle — it's fixed by location (European countries show EU AQI as primary,
     * everywhere else shows US AQI as primary) and doesn't flip when units are toggled.
     */
    fun displayDual(): Pair<String, String> =
        if (primaryStandard == AqiStandard.EUROPEAN) europeanValue.toString() to usValue.toString()
        else usValue.toString() to europeanValue.toString()

    /** Unit labels matching the order returned by displayDual(). */
    fun unitDual(): Pair<String, String> =
        if (primaryStandard == AqiStandard.EUROPEAN) "EU AQI" to "US AQI"
        else "US AQI" to "EU AQI"

    val primaryTier: AqiTier
        get() = if (primaryStandard == AqiStandard.EUROPEAN) europeanTier else usTier

    companion object {
        // Countries covered by Open-Meteo's CAMS-Europe air quality domain. Mirrors the country
        // set used for MeteoAlarm coverage — see METEOALARM_COUNTRIES in WeatherRepositoryImpl.kt.
        private val EUROPEAN_COUNTRY_CODES = setOf(
            "at", "ba", "be", "bg", "hr", "cy", "cz", "dk", "ee", "fi", "fr", "de", "gr", "hu", "ie", "it",
            "lv", "lt", "lu", "mt", "md", "me", "nl", "mk", "no", "pl", "pt", "ro", "rs", "sk", "si",
            "es", "se", "ch", "ua", "gb",
        )

        fun from(usAqi: Int?, europeanAqi: Int?, countryCode: String?): AirQualityIndex? {
            if (usAqi == null || europeanAqi == null) return null
            val standard = if (countryCode != null && countryCode in EUROPEAN_COUNTRY_CODES) AqiStandard.EUROPEAN else AqiStandard.US
            return AirQualityIndex(usAqi, europeanAqi, standard)
        }
    }
}
