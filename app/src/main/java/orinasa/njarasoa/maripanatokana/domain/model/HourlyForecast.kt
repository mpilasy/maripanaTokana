// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class HourlyForecast(
    val time: Long,
    val temperature: Temperature,
    val weatherCode: Int,
    val precipProbability: Int,
    val windSpeed: WindSpeed,
    val windDirection: Int,
    val pressure: Pressure,
    val precipitation: Precipitation,
    val uvIndex: Double? = null,
)
