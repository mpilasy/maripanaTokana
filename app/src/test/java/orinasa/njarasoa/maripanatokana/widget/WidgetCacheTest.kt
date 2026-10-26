package orinasa.njarasoa.maripanatokana.widget

import android.content.SharedPreferences
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import orinasa.njarasoa.maripanatokana.data.remote.OpenMeteoResponse
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

    private class FakePrefs(val data: MutableMap<String, Any?> = mutableMapOf()) : SharedPreferences {
        override fun getAll(): MutableMap<String, *> = data.toMutableMap()
        override fun getString(key: String, defValue: String?) = data[key] as String? ?: defValue
        override fun getStringSet(key: String, defValues: MutableSet<String>?) = defValues
        override fun getInt(key: String, defValue: Int) = data[key] as Int? ?: defValue
        override fun getLong(key: String, defValue: Long) = data[key] as Long? ?: defValue
        override fun getFloat(key: String, defValue: Float) = data[key] as Float? ?: defValue
        override fun getBoolean(key: String, defValue: Boolean) = data[key] as Boolean? ?: defValue
        override fun contains(key: String) = data.containsKey(key)
        override fun registerOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
            override fun putString(key: String, value: String?) = apply { data[key] = value }
            override fun putStringSet(key: String, values: MutableSet<String>?) = apply { data[key] = values }
            override fun putInt(key: String, value: Int) = apply { data[key] = value }
            override fun putLong(key: String, value: Long) = apply { data[key] = value }
            override fun putFloat(key: String, value: Float) = apply { data[key] = value }
            override fun putBoolean(key: String, value: Boolean) = apply { data[key] = value }
            override fun remove(key: String) = apply { data.remove(key) }
            override fun clear() = apply { data.clear() }
            override fun commit() = true
            override fun apply() {}
        }
    }

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
