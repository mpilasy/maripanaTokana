package orinasa.njarasoa.maripanatokana.data.remote

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class OpenMeteoMapperTest {

    // Same config as di/NetworkModule.provideJson()
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private fun isoHour(offsetHours: Int): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:00", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        return fmt.format(Date(System.currentTimeMillis() + offsetHours * 3_600_000L))
    }

    @Test
    fun nullsInSecondaryFields_doNotDiscardForecast() {
        val t1 = isoHour(2)
        val t2 = isoHour(3)
        val t3 = isoHour(4)
        val payload = """
            {
              "utc_offset_seconds": 0,
              "current": {
                "temperature_2m": 21.5, "apparent_temperature": null, "relative_humidity_2m": null,
                "dew_point_2m": null, "wind_speed_10m": null, "wind_direction_10m": null,
                "wind_gusts_10m": null, "pressure_msl": null, "precipitation": null, "rain": null,
                "snowfall": null, "visibility": null, "weather_code": 3, "is_day": 1,
                "uv_index": null, "cloud_cover": null
              },
              "daily": {
                "time": ["2030-01-01", "2030-01-02"],
                "temperature_2m_max": [25.0, 26.0], "temperature_2m_min": [15.0, 16.0],
                "weather_code": [3, 61],
                "precipitation_probability_max": [null, 40],
                "sunrise": ["2030-01-01T06:00", "2030-01-02T06:01"],
                "sunset": ["2030-01-01T18:00", "2030-01-02T18:01"],
                "wind_speed_10m_max": [null, 4.0],
                "wind_direction_10m_dominant": [null, 90],
                "precipitation_sum": [null, 1.2],
                "uv_index_max": [null, 7.0]
              },
              "hourly": {
                "time": ["$t1", "$t2", "$t3"],
                "temperature_2m": [20.0, 19.0, 18.0],
                "weather_code": [1, 2, 3],
                "precipitation_probability": [null, 10, null],
                "wind_speed_10m": [null, 3.0, 2.0],
                "wind_direction_10m": [100, null, 120],
                "pressure_msl": [1010.0, null, 1008.0],
                "precipitation": [0.0, null, 0.5]
              },
              "minutely_15": { "time": ["$t1"], "precipitation": [null] }
            }
        """.trimIndent()

        val data = json.decodeFromString(OpenMeteoResponse.serializer(), payload)
            .toDomain("Test")

        assertEquals(21.5, data.temperature.celsius, 0.001)
        assertEquals(3, data.weatherCode)
        assertNull(data.humidity)
        assertNull(data.uvIndex)
        assertNull(data.visibility)
        assertNull(data.windGust)
        assertEquals("--°C", data.feelsLike.displayCelsius())
        assertEquals("-- hPa", data.pressure.displayHPa())

        assertEquals(2, data.dailyForecast.size)
        assertEquals(25.0, data.dailyForecast[0].tempMax.celsius, 0.001)
        assertEquals(40, data.dailyForecast[1].precipProbability)
        assertEquals("-- mm", data.dailyForecast[0].precipitation.displayMetric())

        assertEquals(3, data.hourlyForecast.size)
        assertEquals(20.0, data.hourlyForecast[0].temperature.celsius, 0.001)
        assertEquals(19.0, data.hourlyForecast[1].temperature.celsius, 0.001)
        assertEquals("-- m/s", data.hourlyForecast[0].windSpeed.displayMetric())
        assertTrue(data.hourlyForecast[1].pressure.hPa.isNaN())
    }

    @Test
    fun shortSecondaryLists_areIndexedSafely() {
        val t1 = isoHour(2)
        val payload = """
            {
              "current": { "temperature_2m": 10.0, "weather_code": 0, "is_day": 0 },
              "daily": {
                "time": ["2030-01-01"], "temperature_2m_max": [12.0], "temperature_2m_min": [5.0],
                "weather_code": [0], "sunrise": ["2030-01-01T06:00"], "sunset": ["2030-01-01T18:00"]
              },
              "hourly": { "time": ["$t1"], "temperature_2m": [9.0], "weather_code": [0] }
            }
        """.trimIndent()

        val data = json.decodeFromString(OpenMeteoResponse.serializer(), payload).toDomain("Test")

        assertEquals(10.0, data.temperature.celsius, 0.001)
        assertEquals(1, data.dailyForecast.size)
        assertEquals(1, data.hourlyForecast.size)
        assertNull(data.humidity)
    }
}
