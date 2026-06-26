package orinasa.njarasoa.maripanatokana.data.remote

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path

interface JmaApiService {
    @GET("bosai/warning/data/warning/{areaCode}.json")
    suspend fun getWarnings(@Path("areaCode") areaCode: String): JmaWarningResponse
}

@Serializable
data class JmaWarningResponse(
    val areaTypes: List<JmaAreaType> = emptyList(),
)

@Serializable
data class JmaAreaType(
    val name: String = "",
    val areas: List<JmaArea> = emptyList(),
)

@Serializable
data class JmaArea(
    val code: String = "",
    val name: String = "",
    val warnings: List<JmaWarningEntry> = emptyList(),
)

@Serializable
data class JmaWarningEntry(
    val code: String = "",
    val status: String = "",
    val name: String = "",
)
