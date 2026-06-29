package orinasa.njarasoa.maripanatokana.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET

interface BomApiService {
    @GET("v1/warnings")
    suspend fun getWarnings(): BomWarningCollection
}

@Serializable
data class BomWarningCollection(
    val data: List<BomWarning> = emptyList(),
)

@Serializable
data class BomWarning(
    val id: String = "",
    val type: String = "",
    val title: String = "",
    val phase: String = "",
    @SerialName("short_title") val shortTitle: String = "",
    @SerialName("short_description") val shortDescription: String = "",
    val state: String = "",
    val states: List<String> = emptyList(),
    @SerialName("issue_time") val issueTime: String? = null,
    @SerialName("warning_action") val warningAction: String = "",
)
