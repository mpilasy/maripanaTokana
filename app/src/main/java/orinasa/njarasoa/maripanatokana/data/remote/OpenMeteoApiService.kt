// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.data.remote

import kotlinx.serialization.json.JsonObject
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenMeteoApiService {
    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = CURRENT,
        @Query("hourly") hourly: String = HOURLY,
        @Query("daily") daily: String = DAILY,
        @Query("minutely_15") minutely15: String = MINUTELY_15,
        @Query("forecast_days") forecastDays: Int = 10,
        @Query("timezone") timezone: String = "auto",
        @Query("wind_speed_unit") windSpeedUnit: String = "ms",
    ): OpenMeteoResponse

    /**
     * The same request for two or more [models]. The hourly, daily and minutely keys come with a model suffix,
     * so the raw JSON comes back for [pickModel]. The hourly list also has the current variables that
     * [HOURLY] does not have, so that each current value has a fallback.
     */
    @GET("v1/forecast")
    suspend fun getForecastWithModels(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("models") models: String,
        @Query("current") current: String = CURRENT,
        @Query("hourly") hourly: String = "$HOURLY,$HOURLY_CURRENT_FALLBACK",
        @Query("daily") daily: String = DAILY,
        @Query("minutely_15") minutely15: String = MINUTELY_15,
        @Query("forecast_days") forecastDays: Int = 10,
        @Query("timezone") timezone: String = "auto",
        @Query("wind_speed_unit") windSpeedUnit: String = "ms",
    ): JsonObject

    companion object {
        const val CURRENT = "temperature_2m,apparent_temperature,relative_humidity_2m,dew_point_2m,wind_speed_10m,wind_direction_10m,wind_gusts_10m,pressure_msl,precipitation,rain,snowfall,visibility,weather_code,is_day,uv_index,cloud_cover"
        const val HOURLY = "temperature_2m,weather_code,precipitation_probability,wind_speed_10m,wind_direction_10m,pressure_msl,precipitation,uv_index,relative_humidity_2m,dew_point_2m"
        const val DAILY = "temperature_2m_max,temperature_2m_min,weather_code,precipitation_probability_max,sunrise,sunset,wind_speed_10m_max,wind_direction_10m_dominant,precipitation_sum,uv_index_max,pressure_msl_mean,relative_humidity_2m_mean,dew_point_2m_mean"
        const val MINUTELY_15 = "precipitation"

        /** The variables of [CURRENT] that are not in [HOURLY]. */
        const val HOURLY_CURRENT_FALLBACK = "apparent_temperature,wind_gusts_10m,rain,snowfall,visibility,is_day,cloud_cover"
    }
}
