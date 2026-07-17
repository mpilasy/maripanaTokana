package orinasa.njarasoa.maripanatokana.data.remote

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

@Serializable
data class GeocodingResponse(
    val results: List<GeocodingResult> = emptyList(),
)

@Serializable
data class GeocodingResult(
    val id: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String? = null,
    val admin1: String? = null,
    val admin2: String? = null,
) {
    fun displayName(): String {
        return listOfNotNull(name, admin1, country).joinToString(", ")
    }
}

interface OpenMeteoGeocodingService {
    @GET("v1/search")
    suspend fun searchLocation(
        @Query("name") name: String,
        @Query("count") count: Int = 20,
        @Query("language") language: String = "en",
        @Query("format") format: String = "json"
    ): GeocodingResponse
}
