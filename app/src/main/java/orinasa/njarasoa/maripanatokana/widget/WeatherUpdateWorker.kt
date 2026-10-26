// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.widget

import android.content.Context
import androidx.core.content.edit
import androidx.glance.appwidget.updateAll
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters

class WeatherUpdateWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val result = WidgetWeatherFetcher.fetch(applicationContext)

        if (!result.fresh) {
            // Cached data is not a refresh. Retry a few times, then wait for the next period.
            return if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
        }

        // Save refresh timestamp
        applicationContext
            .getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
            .edit {
                putLong("last_refresh", System.currentTimeMillis())
            }

        // Trigger all widget re-renders; they read the forecast just cached
        WeatherWidget().updateAll(applicationContext)
        WeatherWidgetLarge().updateAll(applicationContext)

        return Result.success()
    }

    companion object {
        private const val MAX_RETRIES = 3
        private const val REFRESH_WORK = "weather_refresh"

        /** Fetches now in a one-time worker. A tap while a refresh waits or runs does not add a second one. */
        fun refreshNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<WeatherUpdateWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(REFRESH_WORK, ExistingWorkPolicy.KEEP, request)
        }
    }
}
