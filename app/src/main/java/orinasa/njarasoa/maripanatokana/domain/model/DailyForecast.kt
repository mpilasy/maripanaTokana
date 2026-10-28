// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class DailyForecast(
    val date: Long,
    val tempMax: Temperature,
    val tempMin: Temperature,
    val weatherCode: Int,
    val precipProbability: Int,
    val windSpeed: WindSpeed,
    val windDirection: Int,
    val precipitation: Precipitation,
    val uvIndexMax: Double,
    /** The night's weather (sunset to next sunrise); null when the hourly data doesn't reach it. */
    val nightWeatherCode: Int? = null,
    /** The day's mean sea-level pressure; missing in data saved before it was fetched. */
    val pressure: Pressure? = null,
    /** The day's mean relative humidity (percentage) and dew point; missing in data saved before they were fetched. */
    val humidity: Int? = null,
    val dewPoint: Temperature? = null,
)
