package orinasa.njarasoa.maripanatokana.data.remote

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApiAlertService {
    @GET("v1/forecast.json")
    suspend fun getForecastWithAlerts(
        @Query("key") apiKey: String,
        @Query("q") location: String, // "lat,lon"
        @Query("alerts") alerts: String = "yes"
    ): WeatherApiAlertResponse
}

@Serializable
data class WeatherApiAlertResponse(
    val alerts: WeatherApiAlerts? = null
)

@Serializable
data class WeatherApiAlerts(
    val alert: List<WeatherApiAlert> = emptyList()
)

@Serializable
data class WeatherApiAlert(
    val headline: String = "",
    val severity: String = "",
    val event: String = "",
    val desc: String = "",
    val instruction: String = ""
)
