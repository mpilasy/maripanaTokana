package orinasa.njarasoa.maripanatokana.data.repository

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

    private fun file(key: String) = File(dir, key.replace(Regex("[^A-Za-z0-9._-]"), "_") + ".json")

    fun save(key: String, data: WeatherData) {
        try {
            dir.mkdirs()
            val tmp = File(dir, "tmp.json")
            tmp.writeText(json.encodeToString(WeatherData.serializer(), data.copy(alertsLoading = false)))
            tmp.renameTo(file(key))
        } catch (_: Exception) {
            // best-effort
        }
    }

    fun load(key: String): WeatherData? = try {
        file(key).takeIf { it.exists() }?.let { json.decodeFromString(WeatherData.serializer(), it.readText()) }
    } catch (_: Exception) {
        null
    }
}
