package orinasa.njarasoa.maripanatokana.data.remote

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface NwsApiService {
    @Headers("Accept: application/geo+json", "User-Agent: maripanaTokana (contact@orinasa.mg)")
    @GET("alerts/active")
    suspend fun getActiveAlerts(
        @Query("point") point: String // "lat,lon"
    ): NwsAlertResponse
}

@Serializable
data class NwsAlertResponse(
    val features: List<NwsFeature>
)

@Serializable
data class NwsFeature(
    val properties: NwsProperties,
    val id: String? = null
)

@Serializable
data class NwsProperties(
    val severity: String,
    val event: String,
    val description: String,
    val instruction: String? = null,
    val sent: String? = null,
    val headline: String? = null
)
