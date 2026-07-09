package orinasa.njarasoa.maripanatokana.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

interface EcccApiService {
    @GET("collections/weather-alerts/items")
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
    @SerialName("alert_type") val alertType: String = "",
    @SerialName("alert_name_en") val alertNameEn: String = "",
    @SerialName("alert_text_en") val alertTextEn: String = "",
    @SerialName("publication_datetime") val publicationDatetime: String? = null,
)
