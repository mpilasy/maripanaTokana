package orinasa.njarasoa.maripanatokana

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/** Moves sensitive keys (API key, coordinates, cached forecasts) out of the backed-up
 * `widget_prefs` file into `private_prefs`, which is excluded from backup. Runs once. */
object PrefsMigration {
    private const val DONE_FLAG = "migrated_private_prefs_v1"

    private val keys = listOf(
        "settings_weather_api_key",
        "lat", "lon", "last_render_lat", "last_render_lon",
        "cached_response", "cached_location_name", "cached_timestamp",
        "location_name",
        "advanced_override_lat", "advanced_override_lon",
        "advanced_override_name", "advanced_override_set_time",
    )

    fun migrate(context: Context) {
        val target = context.getSharedPreferences("private_prefs", Context.MODE_PRIVATE)
        if (target.getBoolean(DONE_FLAG, false)) return
        val source = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        migrate(source, target)
    }

    internal fun migrate(source: SharedPreferences, target: SharedPreferences) {
        val all = source.all
        target.edit {
            for (key in keys) {
                val value = all[key] ?: continue
                if (!target.contains(key)) {
                    when (value) {
                        is Float -> putFloat(key, value)
                        is String -> putString(key, value)
                        is Long -> putLong(key, value)
                        is Int -> putInt(key, value)
                        is Boolean -> putBoolean(key, value)
                    }
                }
            }
            putBoolean(DONE_FLAG, true)
        }
        source.edit { keys.forEach { remove(it) } }
    }
}
