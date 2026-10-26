package orinasa.njarasoa.maripanatokana

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import orinasa.njarasoa.maripanatokana.testutil.FakePrefs

class PrefsMigrationTest {

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
