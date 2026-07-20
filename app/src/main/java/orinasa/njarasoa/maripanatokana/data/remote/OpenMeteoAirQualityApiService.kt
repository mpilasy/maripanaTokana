package orinasa.njarasoa.maripanatokana.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import orinasa.njarasoa.maripanatokana.domain.model.AirQualityIndex
import orinasa.njarasoa.maripanatokana.domain.model.HourlyAirQuality
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenMeteoAirQualityApiService {
    @GET("v1/air-quality")
    suspend fun getAirQuality(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String =
            "us_aqi,european_aqi,pm2_5,pm10,carbon_monoxide,nitrogen_dioxide,sulphur_dioxide,ozone,ammonia,dust," +
                "us_aqi_pm2_5,us_aqi_pm10,us_aqi_carbon_monoxide,us_aqi_nitrogen_dioxide,us_aqi_sulphur_dioxide,us_aqi_ozone," +
                "european_aqi_pm2_5,european_aqi_pm10,european_aqi_nitrogen_dioxide,european_aqi_sulphur_dioxide,european_aqi_ozone",
        @Query("hourly") hourly: String = "us_aqi,european_aqi",
        @Query("forecast_days") forecastDays: Int = 3,
    ): OpenMeteoAirQualityResponse
}

@Serializable
data class OpenMeteoAirQualityResponse(
    @SerialName("current") val current: OpenMeteoAirQualityCurrent,
    @SerialName("hourly") val hourly: OpenMeteoAirQualityHourly? = null,
)

@Serializable
data class OpenMeteoAirQualityHourly(
    @SerialName("time") val time: List<String> = emptyList(),
    @SerialName("us_aqi") val usAqi: List<Int?> = emptyList(),
    @SerialName("european_aqi") val europeanAqi: List<Int?> = emptyList(),
)

@Serializable
data class OpenMeteoAirQualityCurrent(
    @SerialName("us_aqi") val usAqi: Int? = null,
    @SerialName("european_aqi") val europeanAqi: Int? = null,
    @SerialName("pm2_5") val pm25: Double? = null,
    @SerialName("pm10") val pm10: Double? = null,
    @SerialName("carbon_monoxide") val carbonMonoxide: Double? = null,
    @SerialName("nitrogen_dioxide") val nitrogenDioxide: Double? = null,
    @SerialName("sulphur_dioxide") val sulphurDioxide: Double? = null,
    @SerialName("ozone") val ozone: Double? = null,
    @SerialName("ammonia") val ammonia: Double? = null,
    @SerialName("dust") val dust: Double? = null,
    @SerialName("us_aqi_pm2_5") val usAqiPm25: Int? = null,
    @SerialName("us_aqi_pm10") val usAqiPm10: Int? = null,
    @SerialName("us_aqi_carbon_monoxide") val usAqiCarbonMonoxide: Int? = null,
    @SerialName("us_aqi_nitrogen_dioxide") val usAqiNitrogenDioxide: Int? = null,
    @SerialName("us_aqi_sulphur_dioxide") val usAqiSulphurDioxide: Int? = null,
    @SerialName("us_aqi_ozone") val usAqiOzone: Int? = null,
    @SerialName("european_aqi_pm2_5") val europeanAqiPm25: Int? = null,
    @SerialName("european_aqi_pm10") val europeanAqiPm10: Int? = null,
    @SerialName("european_aqi_nitrogen_dioxide") val europeanAqiNitrogenDioxide: Int? = null,
    @SerialName("european_aqi_sulphur_dioxide") val europeanAqiSulphurDioxide: Int? = null,
    @SerialName("european_aqi_ozone") val europeanAqiOzone: Int? = null,
)

data class AirQualityResult(
    val current: AirQualityIndex?,
    val hourly: List<HourlyAirQuality>,
)

fun OpenMeteoAirQualityResponse.toDomain(countryCode: String?): AirQualityResult {
    val currentIndex = AirQualityIndex.from(
        usAqi = current.usAqi,
        europeanAqi = current.europeanAqi,
        countryCode = countryCode,
        pm25 = current.pm25,
        pm10 = current.pm10,
        carbonMonoxide = current.carbonMonoxide,
        nitrogenDioxide = current.nitrogenDioxide,
        sulphurDioxide = current.sulphurDioxide,
        ozone = current.ozone,
        ammonia = current.ammonia,
        dust = current.dust,
        usAqiPm25 = current.usAqiPm25,
        usAqiPm10 = current.usAqiPm10,
        usAqiCarbonMonoxide = current.usAqiCarbonMonoxide,
        usAqiNitrogenDioxide = current.usAqiNitrogenDioxide,
        usAqiSulphurDioxide = current.usAqiSulphurDioxide,
        usAqiOzone = current.usAqiOzone,
        europeanAqiPm25 = current.europeanAqiPm25,
        europeanAqiPm10 = current.europeanAqiPm10,
        europeanAqiNitrogenDioxide = current.europeanAqiNitrogenDioxide,
        europeanAqiSulphurDioxide = current.europeanAqiSulphurDioxide,
        europeanAqiOzone = current.europeanAqiOzone,
    )

    val h = hourly
    val hourlyList = if (h == null) emptyList() else {
        val nowMillis = System.currentTimeMillis()
        val parsedTimes = h.time.map { parseIsoDateTime(it, 0) }
        val startIndex = parsedTimes.indexOfFirst { it >= nowMillis }.takeIf { it != -1 } ?: 0
        val endIndex = minOf(startIndex + 48, h.time.size)
        (startIndex until endIndex).mapNotNull { i ->
            val us = h.usAqi.getOrNull(i)
            val eu = h.europeanAqi.getOrNull(i)
            if (us == null || eu == null) null
            else HourlyAirQuality(time = parsedTimes[i], usValue = us, europeanValue = eu)
        }.distinctBy { it.time }
    }

    return AirQualityResult(currentIndex, hourlyList)
}
