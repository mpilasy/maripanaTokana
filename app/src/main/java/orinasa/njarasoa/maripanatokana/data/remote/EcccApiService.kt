package orinasa.njarasoa.maripanatokana.data.remote

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import retrofit2.http.GET
import retrofit2.http.Query

interface EcccApiService {
    @GET("collections/alerts/items")
    suspend fun getAlerts(
        @Query("lang") lang: String = "en",
        @Query("limit") limit: Int = 20,
        @Query("bbox") bbox: String,
        @Query("f") format: String = "json",
    ): EcccAlertCollection
}

@Serializable
data class EcccAlertCollection(
    val features: List<EcccAlertFeature> = emptyList(),
)

@Serializable
data class EcccAlertFeature(
    val id: String = "",
    val properties: EcccAlertProperties = EcccAlertProperties(),
)

@Serializable
data class EcccAlertProperties(
    val headline: String = "",
    val description: String = "",
    val severity: String = "",
    val urgency: String = "",
    val onset: String? = null,
    val url: JsonElement? = null,
)
