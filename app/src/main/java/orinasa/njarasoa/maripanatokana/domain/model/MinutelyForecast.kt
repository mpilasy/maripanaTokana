package orinasa.njarasoa.maripanatokana.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class MinutelyForecast(
    val time: Long, // epoch millis
    val precipitation: Precipitation,
)
