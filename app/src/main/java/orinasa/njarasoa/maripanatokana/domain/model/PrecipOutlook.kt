// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.domain.model

/** What the next 24 hours hold for rain or snow, for the one-line summary on the first page. */
data class PrecipOutlook(val kind: Kind, val snow: Boolean, val time: Long? = null) {
    enum class Kind { STARTS, STOPS, ALL_DAY, DRY }

    companion object {
        private val SNOW_CODES = setOf(71, 73, 75, 77, 85, 86)
        private const val HOUR = 3_600_000L

        // Drizzle, rain, freezing rain, snow, showers and thunderstorms
        private fun isPrecipCode(code: Int) = code in 51..67 || code in 71..77 || code in 80..86 || code >= 95

        /**
         * An hour is wet when the nowcast covering it shows precipitation (as the nowcast card does), or,
         * past the nowcast, when its weather code is precipitation (as its icon does). The chance of rain
         * alone doesn't count: it comes from a different model and can say 50% for an hour drawn as plain cloud.
         */
        private fun HourlyForecast.isWet(minutely: List<MinutelyForecast>): Boolean {
            val covering = minutely.filter { it.time >= time && it.time < time + HOUR }
            if (covering.isNotEmpty()) return covering.any { it.precipitation.mm > 0.05 }
            return isPrecipCode(weatherCode)
        }

        fun from(hourly: List<HourlyForecast>, minutely: List<MinutelyForecast> = emptyList()): PrecipOutlook? {
            if (hourly.isEmpty()) return null
            val wet = hourly.map { it.isWet(minutely) }
            val wetNow = wet[0]
            val change = wet.indexOfFirst { it != wetNow }
            // The precipitation the summary talks about: the current one, or the one that starts
            val wetHour = if (wetNow) hourly[0] else hourly.getOrNull(change)
            val snow = wetHour != null && wetHour.weatherCode in SNOW_CODES
            if (change == -1) return PrecipOutlook(if (wetNow) Kind.ALL_DAY else Kind.DRY, snow)
            return PrecipOutlook(if (wetNow) Kind.STOPS else Kind.STARTS, snow, hourly[change].time)
        }
    }
}
