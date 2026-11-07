// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import orinasa.njarasoa.maripanatokana.domain.model.DailyNormals
import orinasa.njarasoa.maripanatokana.util.AppLog
import java.io.File
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** The normal temperatures per place (lat/lon to 2 decimals), as JSON files, kept for 30 days. */
@Singleton
class NormalsCache(private val dir: File, private val now: () -> Long = System::currentTimeMillis) {
    @Inject constructor(@ApplicationContext context: Context) : this(File(context.filesDir, "normals_cache"))

    @Serializable
    private data class Entry(val savedAt: Long, val normals: DailyNormals)

    private val json = Json { ignoreUnknownKeys = true }

    private fun file(lat: Double, lon: Double) = File(dir, "%.2f_%.2f.json".format(Locale.US, lat, lon))

    @Synchronized
    fun load(lat: Double, lon: Double): DailyNormals? = try {
        file(lat, lon).takeIf { it.exists() }
            ?.let { json.decodeFromString(Entry.serializer(), it.readText()) }
            ?.takeIf { now() - it.savedAt < MAX_AGE_MS }
            ?.normals
    } catch (e: Exception) {
        AppLog.w("NormalsCache", "load failed", e)
        null
    }

    @Synchronized
    fun save(lat: Double, lon: Double, normals: DailyNormals) {
        try {
            dir.mkdirs()
            // Old files of other places are removed, so the folder stays small
            dir.listFiles()?.filter { now() - it.lastModified() >= MAX_AGE_MS }?.forEach { it.delete() }
            file(lat, lon).writeText(json.encodeToString(Entry.serializer(), Entry(now(), normals)))
        } catch (e: Exception) {
            AppLog.w("NormalsCache", "save failed", e)
        }
    }

    private companion object {
        const val MAX_AGE_MS = 30L * 24 * 60 * 60 * 1000
    }
}
