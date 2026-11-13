// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.domain.model

/** What the next 24 hours hold for rain or snow, for the one-line summary in the hero card. */
data class PrecipOutlook(val kind: Kind, val snow: Boolean, val time: Long? = null) {
    enum class Kind { STARTS, STOPS, ALL_DAY, DRY }

    companion object {
        private val SNOW_CODES = setOf(71, 73, 75, 77, 85, 86)

        // Drizzle, rain, freezing rain, snow, showers and thunderstorms
        private fun isPrecipCode(code: Int) = code in 51..67 || code in 71..77 || code in 80..86 || code >= 95

        /** A nowcast value covers this long: 15 minutes (Open-Meteo) or 1 minute (Pirate Weather). */
        private fun stepOf(minutely: List<MinutelyForecast>) =
            if (minutely.size >= 2) minutely[1].time - minutely[0].time else 15 * 60_000L

        /**
         * Rain or snow now comes from the nowcast value that covers [nowMs], as on the nowcast card. The first
         * hourly value is the next full hour, so it cannot tell. A change inside the nowcast gets its time to the
         * minute. Past the nowcast, an hour is wet when its weather code is precipitation (as its icon is). The
         * chance of rain alone doesn't count: it comes from a different model and can say 50% for an hour drawn
         * as plain cloud. Without a nowcast, the first hour stands for now.
         */
        fun from(
            hourly: List<HourlyForecast>,
            minutely: List<MinutelyForecast> = emptyList(),
            nowMs: Long = System.currentTimeMillis(),
        ): PrecipOutlook? {
            if (hourly.isEmpty()) return null
            val step = stepOf(minutely)
            val nowcast = minutely.filter { it.time + step > nowMs }
            val nowcastEnd = nowcast.lastOrNull()?.let { it.time + step } ?: Long.MIN_VALUE
            // (start time, wet) from now to the end of the hourly forecast
            val steps = nowcast.map { it.time to (it.precipitation.mm > 0.05) } +
                hourly.filter { it.time >= nowcastEnd }.map { it.time to isPrecipCode(it.weatherCode) }
            if (steps.isEmpty()) return null
            val wetNow = steps[0].second
            val change = steps.indexOfFirst { it.second != wetNow }
            // The precipitation the summary talks about: the current one, or the one that starts
            val wetTime = if (wetNow) steps[0].first else steps.getOrNull(change)?.first
            val snow = wetTime != null && (hourly.lastOrNull { it.time <= wetTime } ?: hourly[0]).weatherCode in SNOW_CODES
            if (change == -1) return PrecipOutlook(if (wetNow) Kind.ALL_DAY else Kind.DRY, snow)
            return PrecipOutlook(if (wetNow) Kind.STOPS else Kind.STARTS, snow, steps[change].first)
        }
    }
}
