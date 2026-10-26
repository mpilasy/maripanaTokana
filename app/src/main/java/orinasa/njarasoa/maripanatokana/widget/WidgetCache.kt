package orinasa.njarasoa.maripanatokana.widget

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import orinasa.njarasoa.maripanatokana.data.remote.OpenMeteoResponse
import orinasa.njarasoa.maripanatokana.util.AppLog
import java.io.File

/** The last forecast the widget fetched, kept raw so the "now" values are mapped again on each render. */
@Serializable
data class WidgetCacheEntry(
    val response: OpenMeteoResponse,
    val locationName: String,
    val timestamp: Long,
)

/**
 * One JSON file for the widget forecast. The forecast was kept in SharedPreferences before;
 * that file is read whole into memory on first access, so a large forecast slowed every prefs read.
 */
class WidgetCache(private val file: File, private val json: Json) {

    @Synchronized
    fun save(entry: WidgetCacheEntry) {
        try {
            file.parentFile?.mkdirs()
            val tmp = File(file.path + ".tmp")
            tmp.writeText(json.encodeToString(WidgetCacheEntry.serializer(), entry))
            if (!tmp.renameTo(file)) {
                tmp.copyTo(file, overwrite = true)
                tmp.delete()
            }
        } catch (e: Exception) {
            AppLog.w(TAG, "save failed", e)
        }
    }

    @Synchronized
    fun load(): WidgetCacheEntry? = try {
        file.takeIf { it.exists() }?.let { json.decodeFromString(WidgetCacheEntry.serializer(), it.readText()) }
    } catch (e: Exception) {
        AppLog.w(TAG, "load failed", e)
        null
    }

    /** Moves a forecast cached in [prefs] by older versions into the file, then removes the prefs keys. */
    @Synchronized
    fun migrateFrom(prefs: SharedPreferences) {
        if (!prefs.contains(KEY_RESPONSE)) return
        if (!file.exists()) {
            val raw = prefs.getString(KEY_RESPONSE, null)
            val name = prefs.getString(KEY_LOCATION_NAME, null)
            if (raw != null && name != null) {
                try {
                    val response = json.decodeFromString(OpenMeteoResponse.serializer(), raw)
                    save(WidgetCacheEntry(response, name, prefs.getLong(KEY_TIMESTAMP, 0L)))
                } catch (e: Exception) {
                    AppLog.w(TAG, "migrate failed", e)
                }
            }
        }
        prefs.edit {
            remove(KEY_RESPONSE)
            remove(KEY_LOCATION_NAME)
            remove(KEY_TIMESTAMP)
        }
    }

    companion object {
        private const val TAG = "WidgetCache"
        const val KEY_RESPONSE = "cached_response"
        const val KEY_LOCATION_NAME = "cached_location_name"
        const val KEY_TIMESTAMP = "cached_timestamp"
    }
}
