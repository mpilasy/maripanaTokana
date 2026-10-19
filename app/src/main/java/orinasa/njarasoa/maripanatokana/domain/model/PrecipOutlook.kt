package orinasa.njarasoa.maripanatokana.domain.model

/** What the next 24 hours hold for rain or snow, for the one-line summary on the first page. */
data class PrecipOutlook(val kind: Kind, val snow: Boolean, val time: Long? = null) {
    enum class Kind { STARTS, STOPS, ALL_DAY, DRY }

    companion object {
        private val SNOW_CODES = setOf(71, 73, 75, 77, 85, 86)

        // An hour counts as wet with an even-or-better chance of rain, or a measurable amount that's at
        // least somewhat likely; a trace the model gives a 2% chance isn't rain worth announcing
        private fun HourlyForecast.isWet() = precipProbability >= 50 || (precipitation.mm >= 0.1 && precipProbability >= 30)

        fun from(hourly: List<HourlyForecast>): PrecipOutlook? {
            if (hourly.isEmpty()) return null
            val wetNow = hourly[0].isWet()
            val change = hourly.indexOfFirst { it.isWet() != wetNow }
            // The precipitation the summary talks about: the current one, or the one that starts
            val wetHour = if (wetNow) hourly[0] else hourly.getOrNull(change)
            val snow = wetHour != null && wetHour.weatherCode in SNOW_CODES
            if (change == -1) return PrecipOutlook(if (wetNow) Kind.ALL_DAY else Kind.DRY, snow)
            return PrecipOutlook(if (wetNow) Kind.STOPS else Kind.STARTS, snow, hourly[change].time)
        }
    }
}
