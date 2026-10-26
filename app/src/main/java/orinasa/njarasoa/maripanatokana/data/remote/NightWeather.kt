// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.data.remote

private const val HOUR = 3_600_000L

/**
 * The weather code for each day's night (sunset to next sunrise), taken from hourly data as the most
 * severe code, the way Open-Meteo picks a day's code. Null when the hourly data doesn't cover that night.
 * Times are epoch millis; the last night, with no next sunrise, runs 12 hours past sunset.
 */
internal fun nightWeatherCodes(times: List<Long>, codes: List<Int>, sunsets: List<Long>, sunrises: List<Long>): List<Int?> =
    sunsets.mapIndexed { i, sunset ->
        val end = sunrises.getOrNull(i + 1) ?: (sunset + 12 * HOUR)
        val night = times.indices.filter { times[it] in (sunset + 1) until end }.mapNotNull { codes.getOrNull(it) }
        // Every hour but one must be there, so a night cut off by the end of the data shows nothing
        if (night.isNotEmpty() && night.size >= (end - sunset) / HOUR - 1) night.max() else null
    }
