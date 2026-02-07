package orinasa.njarasoa.maripanatokana.domain.model

data class WeatherData(
    val temperature: Temperature,
    val feelsLike: Temperature,
    val tempMin: Temperature,
    val tempMax: Temperature,
    val weatherCode: Int,
    val description: String,
    val iconCode: String,
    val locationName: String,
    val pressure: Pressure,
    val humidity: Int, // percentage
    val windSpeed: WindSpeed,
    val windDeg: Int, // degrees
    val windGust: WindSpeed?,
    val rain: Precipitation?,
    val snow: Precipitation?,
    val visibility: Int, // meters
    val sunrise: Long, // epoch seconds
    val sunset: Long, // epoch seconds
    val hourlyForecast: List<HourlyForecast> = emptyList(),
    val dailyForecast: List<DailyForecast> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
)
