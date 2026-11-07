// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import orinasa.njarasoa.maripanatokana.domain.model.ForecastChange
import orinasa.njarasoa.maripanatokana.domain.model.ForecastSnapshot
import orinasa.njarasoa.maripanatokana.domain.model.forecastChanges
import orinasa.njarasoa.maripanatokana.domain.model.snapshotToCompare
import orinasa.njarasoa.maripanatokana.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The daily forecasts saved on the device per place, source and model, to find what changed since yesterday.
 * At most one snapshot per 3 hours for each key, for 5 days, for the 10 places loaded last.
 */
@Singleton
class ForecastSnapshotStore(private val prefs: SharedPreferences) {
    @Inject constructor(@ApplicationContext context: Context) :
        this(context.getSharedPreferences("forecast_snapshots", Context.MODE_PRIVATE))

    private val json = Json {
        ignoreUnknownKeys = true
        allowSpecialFloatingPointValues = true
    }
    private val listSerializer = ListSerializer(ForecastSnapshot.serializer())

    private fun load(key: String): List<ForecastSnapshot> = try {
        prefs.getString(key, null)?.let { json.decodeFromString(listSerializer, it) }.orEmpty()
    } catch (e: Exception) {
        AppLog.w("ForecastSnapshots", "load failed", e)
        emptyList()
    }

    /** The changes of [current] since the saved snapshot for [key] that is closest to 24 hours old (18 to 36 hours). */
    @Synchronized
    fun changesSinceYesterday(key: String, current: ForecastSnapshot): List<ForecastChange> =
        snapshotToCompare(load(key), current.savedAt)?.let { forecastChanges(it, current) }.orEmpty()

    /**
     * Saves [current] for [key] when the newest snapshot is 3 hours old or more. Snapshots older than 5 days are
     * deleted, and only the 10 places loaded last are kept, so the store stays small.
     */
    @Synchronized
    fun recordForecast(key: String, current: ForecastSnapshot) {
        val now = current.savedAt
        val snapshots = load(key).filter { now - it.savedAt < MAX_AGE_MS }
        val newest = snapshots.maxOfOrNull { it.savedAt }
        if (newest != null && now - newest < MIN_INTERVAL_MS) return
        val newestByKey = prefs.all.keys.filter { it != key }
            .associateWith { other -> load(other).maxOfOrNull { it.savedAt } ?: 0L }
        val dropped = newestByKey.filter { now - it.value >= MAX_AGE_MS }.keys +
            newestByKey.entries.sortedByDescending { it.value }.drop(MAX_PLACES - 1).map { it.key }
        prefs.edit {
            putString(key, json.encodeToString(listSerializer, snapshots + current))
            dropped.forEach { remove(it) }
        }
    }

    private companion object {
        const val MIN_INTERVAL_MS = 3 * 60 * 60 * 1000L
        const val MAX_AGE_MS = 5 * 24 * 60 * 60 * 1000L
        const val MAX_PLACES = 10
    }
}
