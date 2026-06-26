package orinasa.njarasoa.maripanatokana.data.remote

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Path

interface MeteoAlarmApiService {
    @GET("feeds/meteoalarm-legacy-atom-{country}")
    suspend fun getAlerts(@Path("country") countryCode: String): ResponseBody
}
