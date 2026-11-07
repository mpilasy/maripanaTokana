// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.domain.model

import kotlinx.serialization.Serializable

/** One hour of the chart view. A value that the source does not give is null. The web has the same type. */
@Serializable
data class ChartHour(
    val time: Long, // epoch millis
    val temperature: Temperature?,
    val dewPoint: Temperature?,
    val weatherCode: Int,
    val isNight: Boolean,
    val precipitation: Precipitation?,
    val precipProbability: Int?, // percentage
    val windSpeed: WindSpeed?,
    val windDirection: Int?, // degrees
    val windGust: WindSpeed?,
    val pressure: Pressure?,
    val humidity: Int?, // percentage
    val uvIndex: Double?,
)
