package orinasa.njarasoa.maripanatokana.domain.model

data class WeatherData(
    val temperature: Temperature,
    val feelsLike: Temperature,
    val tempMin: Temperature,
    val tempMax: Temperature,
    val description: String,
    val iconCode: String,
    val locationName: String,
    val timestamp: Long = System.currentTimeMillis(),
)
