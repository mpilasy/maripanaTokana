package orinasa.njarasoa.maripanatokana.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

@Serializable
data class OWMResponse(
    @SerialName("timezone_offset") val timezoneOffset: Int = 0,
    @SerialName("current") val current: OWMCurrent,
    @SerialName("hourly") val hourly: List<OWMHourly> = emptyList(),
    @SerialName("daily") val daily: List<OWMDaily> = emptyList(),
)

@Serializable
data class OWMCurrent(
    @SerialName("dt") val dt: Long,
    @SerialName("sunrise") val sunrise: Long = 0,
    @SerialName("sunset") val sunset: Long = 0,
    @SerialName("temp") val temp: Double,
    @SerialName("feels_like") val feelsLike: Double,
    @SerialName("pressure") val pressure: Int = 1013,
    @SerialName("humidity") val humidity: Int = 0,
    @SerialName("dew_point") val dewPoint: Double = 0.0,
    @SerialName("uvi") val uvi: Double = 0.0,
    @SerialName("clouds") val clouds: Int = 0,
    @SerialName("visibility") val visibility: Int = 10000,
    @SerialName("wind_speed") val windSpeed: Double = 0.0,
    @SerialName("wind_deg") val windDeg: Int = 0,
    @SerialName("wind_gust") val windGust: Double? = null,
    @SerialName("weather") val weather: List<OWMWeather> = emptyList(),
    @SerialName("rain") val rain: OWMPrecip? = null,
    @SerialName("snow") val snow: OWMPrecip? = null,
)

@Serializable
data class OWMWeather(
    @SerialName("id") val id: Int,
    @SerialName("icon") val icon: String = "01d",
)

@Serializable
data class OWMPrecip(
    @SerialName("1h") val oneHour: Double = 0.0,
)

@Serializable
data class OWMHourly(
    @SerialName("dt") val dt: Long,
    @SerialName("temp") val temp: Double,
    @SerialName("pressure") val pressure: Int = 1013,
    @SerialName("wind_speed") val windSpeed: Double = 0.0,
    @SerialName("wind_deg") val windDeg: Int = 0,
    @SerialName("weather") val weather: List<OWMWeather> = emptyList(),
    @SerialName("pop") val pop: Double = 0.0,
    @SerialName("rain") val rain: OWMPrecip? = null,
)

@Serializable
data class OWMDaily(
    @SerialName("dt") val dt: Long,
    @SerialName("sunrise") val sunrise: Long = 0,
    @SerialName("sunset") val sunset: Long = 0,
    @SerialName("temp") val temp: OWMDailyTemp,
    @SerialName("wind_speed") val windSpeed: Double = 0.0,
    @SerialName("wind_deg") val windDeg: Int = 0,
    @SerialName("weather") val weather: List<OWMWeather> = emptyList(),
    @SerialName("pop") val pop: Double = 0.0,
    @SerialName("rain") val rain: Double? = null,
    @SerialName("snow") val snow: Double? = null,
)

@Serializable
data class OWMDailyTemp(
    @SerialName("min") val min: Double,
    @SerialName("max") val max: Double,
)

interface OpenWeatherMapApiService {
    @GET("data/3.0/onecall")
    suspend fun getWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("units") units: String = "metric",
        @Query("exclude") exclude: String = "minutely,alerts",
        @Query("appid") apiKey: String,
    ): OWMResponse
}
