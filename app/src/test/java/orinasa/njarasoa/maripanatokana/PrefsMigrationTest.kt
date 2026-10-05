package orinasa.njarasoa.maripanatokana

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrefsMigrationTest {

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
    fun movesSensitiveKeysPreservingTypesAndLeavesOthers() {
        val source = FakePrefs(mutableMapOf(
            "lat" to 1.5f, "location_name" to "Tana", "cached_timestamp" to 99L,
            "settings_weather_api_key" to "k", "locale_index" to 2,
        ))
        val target = FakePrefs()

        PrefsMigration.migrate(source, target)

        assertEquals(1.5f, target.data["lat"])
        assertEquals("Tana", target.data["location_name"])
        assertEquals(99L, target.data["cached_timestamp"])
        assertEquals("k", target.data["settings_weather_api_key"])
        assertFalse(source.data.containsKey("lat"))
        assertFalse(source.data.containsKey("settings_weather_api_key"))
        assertEquals(2, source.data["locale_index"])
        assertFalse(target.data.containsKey("locale_index"))
    }

    @Test
    fun doesNotOverwriteExistingTargetValue() {
        val source = FakePrefs(mutableMapOf("lat" to 1f))
        val target = FakePrefs(mutableMapOf("lat" to 7f))

        PrefsMigration.migrate(source, target)

        assertEquals(7f, target.data["lat"])
        assertTrue(source.data.isEmpty())
    }
}
