// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

/** Past weather (ERA5 reanalysis), for the normal temperatures. */
interface OpenMeteoArchiveApiService {
    @GET("v1/archive")
    suspend fun getDailyTemperatures(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String,
        @Query("daily") daily: String = "temperature_2m_max,temperature_2m_min",
        @Query("timezone") timezone: String = "auto",
    ): OpenMeteoArchiveResponse
}

@Serializable
data class OpenMeteoArchiveResponse(
    @SerialName("daily") val daily: OpenMeteoArchiveDaily? = null,
)

@Serializable
data class OpenMeteoArchiveDaily(
    @SerialName("time") val time: List<String> = emptyList(),
    @SerialName("temperature_2m_max") val temperatureMax: List<Double?> = emptyList(),
    @SerialName("temperature_2m_min") val temperatureMin: List<Double?> = emptyList(),
)
