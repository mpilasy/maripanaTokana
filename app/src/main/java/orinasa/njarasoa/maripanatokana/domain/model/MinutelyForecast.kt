// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class MinutelyForecast(
    val time: Long, // epoch millis
    val precipitation: Precipitation,
)
