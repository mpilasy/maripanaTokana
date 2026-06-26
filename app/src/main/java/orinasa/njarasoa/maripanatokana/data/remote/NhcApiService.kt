package orinasa.njarasoa.maripanatokana.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET

interface NhcApiService {
    @GET("CurrentStorms.json")
    suspend fun getCurrentStorms(): NhcStormCollection
}

@Serializable
data class NhcStormCollection(
    @SerialName("activeStorms") val activeStorms: List<NhcStorm> = emptyList(),
)

@Serializable
data class NhcStorm(
    val id: String = "",
    val name: String = "",
    val classification: String = "",
    val intensity: String = "",   // max sustained winds in knots
    val headline: String = "",
    @SerialName("latitudeNumeric") val lat: Double = 0.0,
    @SerialName("longitudeNumeric") val lon: Double = 0.0,
    @SerialName("publicAdvisory") val advisory: NhcAdvisory? = null,
)

@Serializable
data class NhcAdvisory(
    val issuance: String? = null,
    val url: String? = null,
)
