package orinasa.njarasoa.maripanatokana.data.remote

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

data class GdacsAlertResponse(
    val features: List<GdacsFeature>
)

data class GdacsFeature(
    val properties: GdacsProperties,
    val geometry: GdacsGeometry
)

data class GdacsProperties(
    val name: String,
    val eventtype: String,
    val alertlevel: String,
    val description: String
)

data class GdacsGeometry(
    val coordinates: List<Double> // [lon, lat]
)
