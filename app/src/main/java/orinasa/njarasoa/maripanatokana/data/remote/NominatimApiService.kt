package orinasa.njarasoa.maripanatokana.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

@Serializable
data class NominatimPlace(
    @SerialName("place_id") val placeId: Long = 0,
    @SerialName("lat") val lat: String = "0",
    @SerialName("lon") val lon: String = "0",
    @SerialName("name") val name: String = "",
    @SerialName("display_name") val displayName: String = "",
    @SerialName("address") val address: NominatimAddress = NominatimAddress(),
)

@Serializable
data class NominatimAddress(
    @SerialName("city") val city: String? = null,
    @SerialName("town") val town: String? = null,
    @SerialName("village") val village: String? = null,
    @SerialName("municipality") val municipality: String? = null,
    @SerialName("county") val county: String? = null,
    @SerialName("state") val state: String? = null,
    @SerialName("country") val country: String? = null,
)

interface NominatimApiService {
    @Headers("User-Agent: $APP_USER_AGENT")
    @GET("reverse")
    suspend fun reverse(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("format") format: String = "jsonv2",
        @Query("accept-language") language: String = "en",
    ): NominatimPlace

    @Headers("User-Agent: $APP_USER_AGENT")
    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("format") format: String = "jsonv2",
        @Query("accept-language") language: String = "en",
        @Query("limit") limit: Int = 5,
        @Query("addressdetails") addressDetails: Int = 1,
    ): List<NominatimPlace>
}
