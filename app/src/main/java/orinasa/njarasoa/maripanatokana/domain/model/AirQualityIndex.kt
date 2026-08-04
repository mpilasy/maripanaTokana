package orinasa.njarasoa.maripanatokana.domain.model

enum class AqiStandard { US, EUROPEAN }

enum class AqiTier { GOOD, MODERATE, UNHEALTHY, VERY_UNHEALTHY, HAZARDOUS }

// Ordinals line up with uv_labels[0..3] ("Low"/"Moderate"/"High"/"Very High") — pollen reuses that
// array instead of a dedicated one, since the wording is identical and it's already translated.
enum class PollenTier { LOW, MODERATE, HIGH, VERY_HIGH }

/**
 * Pollen concentrations in grains/m³ from Open-Meteo's CAMS-Europe pollen model. Only populated
 * for locations within CAMS-Europe coverage — null (and [hasData] false) everywhere else.
 */
data class PollenReadings(
    val alder: Double? = null,
    val birch: Double? = null,
    val grass: Double? = null,
    val mugwort: Double? = null,
    val olive: Double? = null,
    val ragweed: Double? = null,
) {
    val hasData: Boolean
        get() = alder != null || birch != null || grass != null || mugwort != null || olive != null || ragweed != null
}

data class AirQualityIndex(
    val usValue: Int,
    val europeanValue: Int,
    val primaryStandard: AqiStandard,
    // Pollutant concentrations in µg/m³, straight from the Open-Meteo air-quality "current" call.
    // Null when a given pollutant isn't covered by the domain for this location (e.g. ammonia is
    // CAMS-Europe only) or the API omitted it.
    val pm25: Double? = null,
    val pm10: Double? = null,
    val carbonMonoxide: Double? = null,
    val nitrogenDioxide: Double? = null,
    val sulphurDioxide: Double? = null,
    val ozone: Double? = null,
    val ammonia: Double? = null,
    val dust: Double? = null,
    // Per-pollutant AQI tier, resolved from Open-Meteo's precomputed sub-indices for whichever
    // standard is primary at this location. Null when Open-Meteo doesn't publish a sub-index for
    // this pollutant+standard pair (the EU index has no CO sub-index) or ammonia/dust, which have
    // no official AQI breakpoints at all.
    val pm25Tier: AqiTier? = null,
    val pm10Tier: AqiTier? = null,
    val carbonMonoxideTier: AqiTier? = null,
    val nitrogenDioxideTier: AqiTier? = null,
    val sulphurDioxideTier: AqiTier? = null,
    val ozoneTier: AqiTier? = null,
    val pollen: PollenReadings = PollenReadings(),
) {
    val usTier: AqiTier
        get() = tierFor(usValue, AqiStandard.US)

    val europeanTier: AqiTier
        get() = tierFor(europeanValue, AqiStandard.EUROPEAN)

    /**
     * Dual-unit display. Unlike Temperature/Pressure, primary/secondary here is NOT tied to the
     * metric/imperial toggle — it's fixed by location (European countries show EU AQI as primary,
     * everywhere else shows US AQI as primary) and doesn't flip when units are toggled.
     */
    fun displayDual(): Pair<String, String> =
        if (primaryStandard == AqiStandard.EUROPEAN) europeanValue.toString() to usValue.toString()
        else usValue.toString() to europeanValue.toString()

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

        fun tierFor(value: Int, standard: AqiStandard): AqiTier = if (standard == AqiStandard.US) {
            when {
                value <= 50 -> AqiTier.GOOD
                value <= 100 -> AqiTier.MODERATE
                value <= 200 -> AqiTier.UNHEALTHY
                value <= 300 -> AqiTier.VERY_UNHEALTHY
                else -> AqiTier.HAZARDOUS
            }
        } else {
            when {
                value < 20 -> AqiTier.GOOD
                value < 40 -> AqiTier.MODERATE
                value < 60 -> AqiTier.UNHEALTHY
                value < 100 -> AqiTier.VERY_UNHEALTHY
                else -> AqiTier.HAZARDOUS
            }
        }

        // No official breakpoints are published for pollen (unlike AQI). These are commonly-used
        // generic grains/m³ bands applied uniformly across species; treat as an approximation.
        fun pollenTierFor(value: Double): PollenTier = when {
            value < 10 -> PollenTier.LOW
            value < 50 -> PollenTier.MODERATE
            value < 200 -> PollenTier.HIGH
            else -> PollenTier.VERY_HIGH
        }

        fun from(
            usAqi: Int?,
            europeanAqi: Int?,
            countryCode: String?,
            pm25: Double? = null,
            pm10: Double? = null,
            carbonMonoxide: Double? = null,
            nitrogenDioxide: Double? = null,
            sulphurDioxide: Double? = null,
            ozone: Double? = null,
            ammonia: Double? = null,
            dust: Double? = null,
            usAqiPm25: Int? = null,
            usAqiPm10: Int? = null,
            usAqiCarbonMonoxide: Int? = null,
            usAqiNitrogenDioxide: Int? = null,
            usAqiSulphurDioxide: Int? = null,
            usAqiOzone: Int? = null,
            europeanAqiPm25: Int? = null,
            europeanAqiPm10: Int? = null,
            europeanAqiNitrogenDioxide: Int? = null,
            europeanAqiSulphurDioxide: Int? = null,
            europeanAqiOzone: Int? = null,
            pollen: PollenReadings = PollenReadings(),
        ): AirQualityIndex? {
            if (usAqi == null || europeanAqi == null) return null
            val standard = if (countryCode != null && countryCode in EUROPEAN_COUNTRY_CODES) AqiStandard.EUROPEAN else AqiStandard.US
            fun tier(us: Int?, eu: Int?): AqiTier? = (if (standard == AqiStandard.EUROPEAN) eu else us)?.let { tierFor(it, standard) }
            return AirQualityIndex(
                usAqi, europeanAqi, standard,
                pm25, pm10, carbonMonoxide, nitrogenDioxide, sulphurDioxide, ozone, ammonia, dust,
                pm25Tier = tier(usAqiPm25, europeanAqiPm25),
                pm10Tier = tier(usAqiPm10, europeanAqiPm10),
                carbonMonoxideTier = tier(usAqiCarbonMonoxide, null),
                nitrogenDioxideTier = tier(usAqiNitrogenDioxide, europeanAqiNitrogenDioxide),
                sulphurDioxideTier = tier(usAqiSulphurDioxide, europeanAqiSulphurDioxide),
                ozoneTier = tier(usAqiOzone, europeanAqiOzone),
                pollen = pollen,
            )
        }
    }
}
