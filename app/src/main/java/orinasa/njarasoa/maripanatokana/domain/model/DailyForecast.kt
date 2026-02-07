package orinasa.njarasoa.maripanatokana.domain.model

data class DailyForecast(
    val date: Long,
    val tempMax: Temperature,
    val tempMin: Temperature,
    val weatherCode: Int,
    val precipProbability: Int,
)
