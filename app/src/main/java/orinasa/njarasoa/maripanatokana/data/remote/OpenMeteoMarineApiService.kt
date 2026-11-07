// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import orinasa.njarasoa.maripanatokana.domain.model.SeaConditions
import orinasa.njarasoa.maripanatokana.domain.model.Temperature
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenMeteoMarineApiService {
    @GET("v1/marine")
    suspend fun getMarine(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String =
            "wave_height,wave_period,wave_direction,swell_wave_height,swell_wave_period,swell_wave_direction," +
                "sea_surface_temperature,sea_level_height_msl",
        @Query("hourly") hourly: String = "sea_level_height_msl",
        @Query("forecast_days") forecastDays: Int = 2,
        @Query("timezone") timezone: String = "auto",
    ): OpenMeteoMarineResponse
}

@Serializable
data class OpenMeteoMarineResponse(
    @SerialName("utc_offset_seconds") val utcOffsetSeconds: Int = 0,
    @SerialName("current") val current: OpenMeteoMarineCurrent? = null,
    @SerialName("hourly") val hourly: OpenMeteoMarineHourly? = null,
)

@Serializable
data class OpenMeteoMarineCurrent(
    @SerialName("wave_height") val waveHeight: Double? = null,
    @SerialName("wave_period") val wavePeriod: Double? = null,
    @SerialName("wave_direction") val waveDirection: Double? = null,
    @SerialName("swell_wave_height") val swellWaveHeight: Double? = null,
    @SerialName("swell_wave_period") val swellWavePeriod: Double? = null,
    @SerialName("swell_wave_direction") val swellWaveDirection: Double? = null,
    @SerialName("sea_surface_temperature") val seaSurfaceTemperature: Double? = null,
    @SerialName("sea_level_height_msl") val seaLevelHeightMsl: Double? = null,
)

@Serializable
data class OpenMeteoMarineHourly(
    @SerialName("time") val time: List<String> = emptyList(),
    @SerialName("sea_level_height_msl") val seaLevelHeightMsl: List<Double?> = emptyList(),
)

/** The sea conditions, or null for an inland place (no wave height and no sea temperature). */
fun mapToSea(response: OpenMeteoMarineResponse): SeaConditions? {
    val c = response.current ?: return null
    if (c.waveHeight == null && c.seaSurfaceTemperature == null) return null
    return SeaConditions(
        waveHeight = c.waveHeight,
        wavePeriod = c.wavePeriod,
        waveDirection = c.waveDirection?.toInt(),
        swellHeight = c.swellWaveHeight,
        swellPeriod = c.swellWavePeriod,
        swellDirection = c.swellWaveDirection?.toInt(),
        seaTemperature = c.seaSurfaceTemperature?.let { Temperature.fromCelsius(it) },
        seaLevel = c.seaLevelHeightMsl,
        tideTimes = response.hourly?.time?.map { parseIsoDateTime(it, response.utcOffsetSeconds) }.orEmpty(),
        tideLevels = response.hourly?.seaLevelHeightMsl.orEmpty(),
    )
}
