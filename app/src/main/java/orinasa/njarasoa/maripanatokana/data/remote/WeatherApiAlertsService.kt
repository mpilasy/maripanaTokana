package orinasa.njarasoa.maripanatokana.data.remote

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApiAlertsService {
    @GET("forecast.json")
    suspend fun getAlerts(
        @Query("key") key: String,
        @Query("q") q: String,
        @Query("days") days: Int = 1,
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
    val headline: String? = null,
    val msgtype: String? = null,
    val severity: String? = null,
    val urgency: String? = null,
    val areas: String? = null,
    val category: String? = null,
    val certainty: String? = null,
    val event: String? = null,
    val note: String? = null,
    val effective: String? = null,
    val expires: String? = null,
    val desc: String? = null,
    val instruction: String? = null
)
