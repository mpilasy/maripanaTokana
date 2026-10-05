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
)
