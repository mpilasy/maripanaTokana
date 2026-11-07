// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.data.remote

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PickModelTest {

    // Same config as di/NetworkModule.provideJson()
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    // A hand-made response for models=icon_seamless,best_match: 3 hours, 3 days, ICON stops after day 2
    private val twoModels = json.parseToJsonElement(
        """
        {
          "utc_offset_seconds": 0,
          "current": {
            "time": "2030-01-01T01:15", "interval": 900,
            "temperature_2m": 5.5, "apparent_temperature": null, "relative_humidity_2m": 80,
            "dew_point_2m": 2.0, "wind_speed_10m": 3.0, "wind_direction_10m": 180,
            "wind_gusts_10m": null, "pressure_msl": 1010.0, "precipitation": 0.0, "rain": 0.0,
            "snowfall": 0.0, "visibility": null, "weather_code": 3, "is_day": 0,
            "uv_index": null, "cloud_cover": 90
          },
          "hourly": {
            "time": ["2030-01-01T00:00", "2030-01-01T01:00", "2030-01-01T02:00"],
            "temperature_2m_icon_seamless": [4.0, 5.0, null],
            "temperature_2m_best_match": [14.0, 15.0, 16.0],
            "weather_code_icon_seamless": [1, null, 3],
            "weather_code_best_match": [61, 62, 63],
            "uv_index_icon_seamless": [null, null, null],
            "uv_index_best_match": [0.1, 0.2, 0.3],
            "apparent_temperature_icon_seamless": [null, null, null],
            "apparent_temperature_best_match": [10.0, 11.0, 12.0],
            "visibility_best_match": [20000.0, 24000.0, 30000.0],
            "wind_gusts_10m_icon_seamless": [7.0, 8.0, 9.0]
          },
          "daily": {
            "time": ["2030-01-01", "2030-01-02", "2030-01-03"],
            "temperature_2m_max_icon_seamless": [6.0, 7.0, null],
            "temperature_2m_max_best_match": [16.0, 17.0, 18.0],
            "temperature_2m_min_icon_seamless": [1.0, 2.0, null],
            "temperature_2m_min_best_match": [11.0, 12.0, 13.0],
            "weather_code_icon_seamless": [3, 3, null],
            "weather_code_best_match": [61, 61, 61],
            "sunrise_icon_seamless": ["2030-01-01T07:00", "2030-01-02T07:00", "2030-01-03T07:00"],
            "sunrise_best_match": ["2030-01-01T07:00", "2030-01-02T07:00", "2030-01-03T07:00"],
            "sunset_icon_seamless": ["2030-01-01T17:00", "2030-01-02T17:00", "2030-01-03T17:00"],
            "sunset_best_match": ["2030-01-01T17:00", "2030-01-02T17:00", "2030-01-03T17:00"]
          },
          "minutely_15": {
            "time": ["2030-01-01T01:00", "2030-01-01T01:15"],
            "precipitation_icon_seamless": [0.5, null],
            "precipitation_best_match": [1.5, 2.5]
          }
        }
        """
    ).jsonObject

    private fun merged() = json.decodeFromJsonElement(OpenMeteoResponse.serializer(), pickModel(twoModels, "icon_seamless"))

    @Test
    fun chosenValuesWin() {
        val r = merged()
        assertEquals(listOf(4.0, 5.0), r.hourly.temperature2m.take(2))
        assertEquals(6.0, r.daily.temperatureMax[0], 0.0)
        assertEquals(0.5, r.minutely15!!.precipitation[0]!!, 0.0)
    }

    @Test
    fun nullsFallBackToBestMatch() {
        val r = merged()
        assertEquals(listOf(1, 62, 3), r.hourly.weatherCode)
        assertEquals(listOf(0.1, 0.2, 0.3), r.hourly.uvIndex)
        assertEquals(2.5, r.minutely15!!.precipitation[1]!!, 0.0)
    }

    @Test
    fun daysAfterTheModelHorizonComeFromBestMatch() {
        val r = merged()
        assertEquals(3, r.daily.time.size)
        assertEquals(listOf(6.0, 7.0, 18.0), r.daily.temperatureMax)
        assertEquals(listOf(1.0, 2.0, 13.0), r.daily.temperatureMin)
        assertEquals(16.0, r.hourly.temperature2m[2], 0.0)
        assertEquals("2030-01-03T17:00", r.daily.sunset[2])
    }

    @Test
    fun currentNullsTakeTheBestMatchValueOfTheCurrentHour() {
        val r = merged()
        // Not null: the chosen model value stays
        assertEquals(5.5, r.current.temperature, 0.0)
        // Null: the best_match hourly value at 01:00
        assertEquals(11.0, r.current.apparentTemperature!!, 0.0)
        assertEquals(24000.0, r.current.visibility!!, 0.0)
        assertEquals(0.2, r.current.uvIndex!!, 0.0)
        // Null and no best_match hourly value: stays null
        assertNull(r.current.windGusts)
    }

    @Test
    fun aVariableOfOnlyOneModelIsKept() {
        val hourly = pickModel(twoModels, "icon_seamless")["hourly"] as JsonObject
        assertTrue("wind_gusts_10m" in hourly)
        assertTrue("visibility" in hourly)
        assertTrue(hourly.keys.none { it.endsWith("_best_match") || it.endsWith("_icon_seamless") })
    }

    @Test
    fun automaticMakesTheSameRequestAsBefore() = runTest {
        val api = RecordingApi()
        api.getForecastFor(1.0, 2.0, null, json)
        assertEquals(listOf("forecast"), api.calls)
        assertEquals(OpenMeteoApiService.HOURLY, api.hourly)
        assertNull(api.models)
    }

    @Test
    fun aChosenModelAsksForItFirstThenBestMatch() = runTest {
        val api = RecordingApi()
        val r = api.getForecastFor(1.0, 2.0, "icon_seamless", json)
        assertEquals(listOf("models"), api.calls)
        assertEquals("icon_seamless,best_match", api.models)
        assertTrue(api.hourly!!.split(",").containsAll(OpenMeteoApiService.HOURLY_CURRENT_FALLBACK.split(",")))
        assertEquals(5.5, r.current.temperature, 0.0)
    }

    /** Records the calls; Kotlin fills in the default parameters before the call, as for Retrofit. */
    private inner class RecordingApi : OpenMeteoApiService {
        val calls = mutableListOf<String>()
        var hourly: String? = null
        var models: String? = null

        override suspend fun getForecast(
            latitude: Double, longitude: Double, current: String, hourly: String, daily: String,
            minutely15: String, forecastDays: Int, timezone: String, windSpeedUnit: String,
        ): OpenMeteoResponse {
            calls += "forecast"
            this.hourly = hourly
            return merged()
        }

        override suspend fun getForecastWithModels(
            latitude: Double, longitude: Double, models: String, current: String, hourly: String, daily: String,
            minutely15: String, forecastDays: Int, timezone: String, windSpeedUnit: String,
        ): JsonObject {
            calls += "models"
            this.hourly = hourly
            this.models = models
            return twoModels
        }
    }
}
