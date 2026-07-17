package orinasa.njarasoa.maripanatokana.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import orinasa.njarasoa.maripanatokana.domain.model.AirQualityIndex
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenMeteoAirQualityApiService {
    @GET("v1/air-quality")
    suspend fun getAirQuality(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "us_aqi,european_aqi",
    ): OpenMeteoAirQualityResponse
}

@Serializable
data class OpenMeteoAirQualityResponse(
    @SerialName("current") val current: OpenMeteoAirQualityCurrent,
)

@Serializable
data class OpenMeteoAirQualityCurrent(
    @SerialName("us_aqi") val usAqi: Int? = null,
    @SerialName("european_aqi") val europeanAqi: Int? = null,
)

fun OpenMeteoAirQualityResponse.toDomain(countryCode: String?): AirQualityIndex? =
    AirQualityIndex.from(current.usAqi, current.europeanAqi, countryCode)
