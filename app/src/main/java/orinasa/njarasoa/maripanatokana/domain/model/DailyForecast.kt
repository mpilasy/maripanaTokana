package orinasa.njarasoa.maripanatokana.domain.model

data class DailyForecast(
    val date: Long,
    val tempMax: Temperature,
    val tempMin: Temperature,
    val weatherCode: Int,
    val precipProbability: Int,
    val windSpeedMax: WindSpeed,
    val windDeg: Int,
    val precipitationSum: Precipitation,
)
