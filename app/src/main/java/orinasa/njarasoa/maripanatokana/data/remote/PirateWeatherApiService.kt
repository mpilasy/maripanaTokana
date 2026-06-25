package orinasa.njarasoa.maripanatokana.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path

@Serializable
data class PirateWeatherResponse(
    @SerialName("offset") val offsetHours: Double = 0.0,
    @SerialName("currently") val currently: PirateWeatherCurrent,
    @SerialName("hourly") val hourly: PirateWeatherHourlyBlock = PirateWeatherHourlyBlock(),
    @SerialName("daily") val daily: PirateWeatherDailyBlock = PirateWeatherDailyBlock(),
)

@Serializable
data class PirateWeatherCurrent(
    @SerialName("time") val time: Long,
    @SerialName("icon") val icon: String = "clear-day",
    @SerialName("precipIntensity") val precipIntensity: Double = 0.0,
    @SerialName("precipType") val precipType: String? = null,
    @SerialName("temperature") val temperature: Double,
    @SerialName("apparentTemperature") val apparentTemperature: Double,
    @SerialName("dewPoint") val dewPoint: Double = 0.0,
    @SerialName("humidity") val humidity: Double = 0.0,
    @SerialName("pressure") val pressure: Double = 1013.0,
    @SerialName("windSpeed") val windSpeed: Double = 0.0,
    @SerialName("windGust") val windGust: Double? = null,
    @SerialName("windBearing") val windBearing: Int = 0,
    @SerialName("cloudCover") val cloudCover: Double = 0.0,
    @SerialName("uvIndex") val uvIndex: Double = 0.0,
    @SerialName("visibility") val visibility: Double = 16.0,
)

@Serializable
data class PirateWeatherHourlyBlock(
    @SerialName("data") val data: List<PirateWeatherHourly> = emptyList(),
)

@Serializable
data class PirateWeatherHourly(
    @SerialName("time") val time: Long,
    @SerialName("icon") val icon: String = "clear-day",
    @SerialName("precipIntensity") val precipIntensity: Double = 0.0,
    @SerialName("precipProbability") val precipProbability: Double = 0.0,
    @SerialName("temperature") val temperature: Double,
    @SerialName("windSpeed") val windSpeed: Double = 0.0,
    @SerialName("windBearing") val windBearing: Int = 0,
    @SerialName("pressure") val pressure: Double = 1013.0,
)

@Serializable
data class PirateWeatherDailyBlock(
    @SerialName("data") val data: List<PirateWeatherDaily> = emptyList(),
)

@Serializable
data class PirateWeatherDaily(
    @SerialName("time") val time: Long,
    @SerialName("icon") val icon: String = "clear-day",
    @SerialName("sunriseTime") val sunriseTime: Long = 0,
    @SerialName("sunsetTime") val sunsetTime: Long = 0,
    @SerialName("precipIntensity") val precipIntensity: Double = 0.0,
    @SerialName("precipProbability") val precipProbability: Double = 0.0,
    @SerialName("precipType") val precipType: String? = null,
    @SerialName("temperatureMax") val temperatureMax: Double,
    @SerialName("temperatureMin") val temperatureMin: Double,
    @SerialName("windSpeed") val windSpeed: Double = 0.0,
    @SerialName("windBearing") val windBearing: Int = 0,
    @SerialName("uvIndex") val uvIndex: Double = 0.0,
)

interface PirateWeatherApiService {
    @GET("forecast/{apiKey}/{lat},{lon}?units=si")
    suspend fun getForecast(
        @Path("apiKey") apiKey: String,
        @Path("lat") lat: Double,
        @Path("lon") lon: Double,
    ): PirateWeatherResponse
}
