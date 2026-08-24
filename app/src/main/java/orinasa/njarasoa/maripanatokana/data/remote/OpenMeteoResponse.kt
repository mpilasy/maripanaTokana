package orinasa.njarasoa.maripanatokana.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenMeteoResponse(
    @SerialName("utc_offset_seconds") val utcOffsetSeconds: Int = 0,
    @SerialName("current") val current: OpenMeteoCurrent,
    @SerialName("daily") val daily: OpenMeteoDaily,
    @SerialName("hourly") val hourly: OpenMeteoHourly,
    @SerialName("minutely_15") val minutely15: OpenMeteoMinutely15? = null,
)

@Serializable
data class OpenMeteoMinutely15(
    @SerialName("time") val time: List<String> = emptyList(),
    @SerialName("precipitation") val precipitation: List<Double> = emptyList(),
)

@Serializable
data class OpenMeteoCurrent(
    @SerialName("temperature_2m") val temperature: Double,
    @SerialName("apparent_temperature") val apparentTemperature: Double,
    @SerialName("relative_humidity_2m") val relativeHumidity: Int,
    @SerialName("dew_point_2m") val dewPoint: Double,
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
    @SerialName("uv_index") val uvIndex: Double,
    @SerialName("cloud_cover") val cloudCover: Int,
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
    @SerialName("wind_speed_10m_max") val windSpeed10mMax: List<Double>,
    @SerialName("wind_direction_10m_dominant") val windDirection10mDominant: List<Int>,
    @SerialName("precipitation_sum") val precipitationSum: List<Double>,
    @SerialName("uv_index_max") val uvIndexMax: List<Double> = emptyList(),
)

@Serializable
data class OpenMeteoHourly(
    @SerialName("time") val time: List<String>,
    @SerialName("temperature_2m") val temperature2m: List<Double>,
    @SerialName("weather_code") val weatherCode: List<Int>,
    @SerialName("precipitation_probability") val precipitationProbability: List<Int>,
    @SerialName("wind_speed_10m") val windSpeed10m: List<Double>,
    @SerialName("wind_direction_10m") val windDirection10m: List<Int>,
    @SerialName("pressure_msl") val pressureMsl: List<Double>,
    @SerialName("precipitation") val precipitation: List<Double>,
)
