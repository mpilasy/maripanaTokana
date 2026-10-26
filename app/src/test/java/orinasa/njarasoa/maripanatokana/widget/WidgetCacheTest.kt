// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.widget

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import orinasa.njarasoa.maripanatokana.data.remote.OpenMeteoResponse
import orinasa.njarasoa.maripanatokana.testutil.FakePrefs
import java.io.File

class WidgetCacheTest {
    @get:Rule val tmp = TemporaryFolder()

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val payload = """
        {
          "current": { "temperature_2m": 21.5, "weather_code": 3, "is_day": 1 },
          "daily": {
            "time": ["2030-01-01"], "temperature_2m_max": [25.0], "temperature_2m_min": [15.0],
            "weather_code": [3], "sunrise": ["2030-01-01T06:00"], "sunset": ["2030-01-01T18:00"]
          },
          "hourly": { "time": ["2030-01-01T00:00"], "temperature_2m": [20.0], "weather_code": [1] }
        }
    """.trimIndent()

    private fun response() = json.decodeFromString(OpenMeteoResponse.serializer(), payload)

    @Test
    fun roundTripsEntry() {
        val cache = WidgetCache(File(tmp.newFolder("c"), "forecast.json"), json)
        cache.save(WidgetCacheEntry(response(), "Tana", 123L))

        val loaded = cache.load()!!
        assertEquals("Tana", loaded.locationName)
        assertEquals(123L, loaded.timestamp)
        assertEquals(21.5, loaded.response.current.temperature, 0.0)
    }

    @Test
    fun missingOrCorruptFileLoadsNull() {
        val file = File(tmp.newFolder("m"), "forecast.json")
        val cache = WidgetCache(file, json)
        assertNull(cache.load())
        file.writeText("{not json")
        assertNull(cache.load())
    }

    @Test
    fun migratesForecastOutOfPrefs() {
        val prefs = FakePrefs(mutableMapOf(
            WidgetCache.KEY_RESPONSE to payload,
            WidgetCache.KEY_LOCATION_NAME to "Tana",
            WidgetCache.KEY_TIMESTAMP to 99L,
            "lat" to 1.5f,
        ))
        val cache = WidgetCache(File(tmp.newFolder("p"), "forecast.json"), json)
        cache.migrateFrom(prefs)

        val loaded = cache.load()!!
        assertEquals("Tana", loaded.locationName)
        assertEquals(99L, loaded.timestamp)
        assertFalse(prefs.contains(WidgetCache.KEY_RESPONSE))
        assertFalse(prefs.contains(WidgetCache.KEY_LOCATION_NAME))
        assertFalse(prefs.contains(WidgetCache.KEY_TIMESTAMP))
        assertEquals(1.5f, prefs.getFloat("lat", 0f))
    }

    @Test
    fun migrationKeepsNewerFile() {
        val cache = WidgetCache(File(tmp.newFolder("n"), "forecast.json"), json)
        cache.save(WidgetCacheEntry(response(), "New", 200L))
        val prefs = FakePrefs(mutableMapOf(
            WidgetCache.KEY_RESPONSE to payload,
            WidgetCache.KEY_LOCATION_NAME to "Old",
        ))
        cache.migrateFrom(prefs)

        assertEquals("New", cache.load()!!.locationName)
        assertFalse(prefs.contains(WidgetCache.KEY_RESPONSE))
    }
}
