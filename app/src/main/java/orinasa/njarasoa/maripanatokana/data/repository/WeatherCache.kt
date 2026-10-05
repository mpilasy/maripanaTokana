package orinasa.njarasoa.maripanatokana.data.repository

import orinasa.njarasoa.maripanatokana.util.AppLog
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import orinasa.njarasoa.maripanatokana.domain.model.WeatherData
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Last successful [WeatherData] per location key ("gps", saved-location id, "preview"), as JSON files. */
@Singleton
class WeatherCache(private val dir: File) {
    @Inject constructor(@ApplicationContext context: Context) : this(File(context.filesDir, "weather_cache"))

    private val json = Json {
        ignoreUnknownKeys = true
        allowSpecialFloatingPointValues = true
    }

    private fun sanitize(key: String) = key.replace(Regex("[^A-Za-z0-9._-]"), "_")

    private fun file(key: String) = File(dir, sanitize(key) + ".json")

    @Synchronized
    fun save(key: String, data: WeatherData) {
        try {
            dir.mkdirs()
            val tmp = File(dir, sanitize(key) + ".json.tmp")
            tmp.writeText(json.encodeToString(WeatherData.serializer(), data.copy(alertsLoading = false)))
            val target = file(key)
            if (!tmp.renameTo(target)) {
                tmp.copyTo(target, overwrite = true)
                tmp.delete()
            }
        } catch (e: Exception) {
            AppLog.w("WeatherCache", "save failed", e)
        }
    }

    @Synchronized
    fun load(key: String): WeatherData? = try {
        file(key).takeIf { it.exists() }?.let { json.decodeFromString(WeatherData.serializer(), it.readText()) }
    } catch (e: Exception) {
        AppLog.w("WeatherCache", "load failed", e)
        null
    }
}
