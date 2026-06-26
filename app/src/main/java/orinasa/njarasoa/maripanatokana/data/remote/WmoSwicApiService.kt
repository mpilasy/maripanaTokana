package orinasa.njarasoa.maripanatokana.data.remote

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path

interface WmoSwicApiService {
    @GET("json/{country}.json")
    suspend fun getAlerts(@Path("country") countryCode: String): WmoSwicResponse
}

@Serializable
data class WmoSwicResponse(
    val Warning: List<WmoSwicWarning> = emptyList(),
)

@Serializable
data class WmoSwicWarning(
    val Summary: String = "",
    val Detail: String = "",
    val Url: String = "",
    val Issuance: String = "",
    val Begin: String = "",
    val End: String = "",
    val City: String = "",
    val Official: String = "",
)
