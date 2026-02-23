package orinasa.njarasoa.maripanatokana.domain.model

data class HourlyForecast(
    val time: Long,
    val temperature: Temperature,
    val weatherCode: Int,
    val precipProbability: Int,
    val windSpeed: WindSpeed,
    val windDeg: Int,
    val precipitation: Precipitation,
    val pressure: Pressure,
)
