package orinasa.njarasoa.maripanatokana.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenMeteoResponse(
    @SerialName("current") val current: OpenMeteoCurrent,
    @SerialName("daily") val daily: OpenMeteoDaily,
    @SerialName("hourly") val hourly: OpenMeteoHourly,
)

@Serializable
data class OpenMeteoCurrent(
    @SerialName("temperature_2m") val temperature: Double,
    @SerialName("apparent_temperature") val apparentTemperature: Double,
    @SerialName("relative_humidity_2m") val relativeHumidity: Int,
    @SerialName("wind_speed_10m") val windSpeed: Double,
    @SerialName("wind_direction_10m") val windDirection: Int,
    @SerialName("wind_gusts_10m") val windGusts: Double,
    @SerialName("pressure_msl") val pressureMsl: Double,
    @SerialName("precipitation") val precipitation: Double,
    @SerialName("rain") val rain: Double,
    @SerialName("snowfall") val snowfall: Double,
    @SerialName("visibility") val visibility: Double,
    @SerialName("weather_code") val weatherCode: Int,
    @SerialName("is_day") val isDay: Int,
)

@Serializable
data class OpenMeteoDaily(
    @SerialName("time") val time: List<String>,
    @SerialName("temperature_2m_max") val temperatureMax: List<Double>,
    @SerialName("temperature_2m_min") val temperatureMin: List<Double>,
    @SerialName("weather_code") val weatherCode: List<Int>,
    @SerialName("precipitation_probability_max") val precipitationProbabilityMax: List<Int>,
    @SerialName("sunrise") val sunrise: List<String>,
    @SerialName("sunset") val sunset: List<String>,
)

@Serializable
data class OpenMeteoHourly(
    @SerialName("time") val time: List<String>,
    @SerialName("temperature_2m") val temperature2m: List<Double>,
    @SerialName("weather_code") val weatherCode: List<Int>,
    @SerialName("precipitation_probability") val precipitationProbability: List<Int>,
)
