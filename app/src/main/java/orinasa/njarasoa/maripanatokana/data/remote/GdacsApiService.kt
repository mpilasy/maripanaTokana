package orinasa.njarasoa.maripanatokana.data.remote

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

interface GdacsApiService {
    @GET("gdacsapi/api/events/geteventlist/SEARCH")
    suspend fun searchEvents(
        @Query("fromdate") fromDate: String,
        @Query("todate") toDate: String,
        @Query("alertlevel") alertLevel: String = "green;orange;red"
    ): GdacsAlertResponse
}

@Serializable
data class GdacsAlertResponse(
    val features: List<GdacsFeature>
)

@Serializable
data class GdacsFeature(
    val properties: GdacsProperties,
    val geometry: GdacsGeometry
)

@Serializable
data class GdacsProperties(
    val name: String,
    val eventtype: String,
    val alertlevel: String,
    val description: String,
    val fromdate: String? = null,
    val url: kotlinx.serialization.json.JsonObject? = null
)

@Serializable
data class GdacsGeometry(
    val coordinates: List<Double> // [lon, lat]
)
