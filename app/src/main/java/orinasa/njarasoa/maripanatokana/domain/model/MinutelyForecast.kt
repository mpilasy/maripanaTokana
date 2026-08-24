package orinasa.njarasoa.maripanatokana.domain.model

data class MinutelyForecast(
    val time: Long, // epoch millis
    val precipitation: Precipitation,
)
