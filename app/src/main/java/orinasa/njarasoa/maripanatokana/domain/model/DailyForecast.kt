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
)
