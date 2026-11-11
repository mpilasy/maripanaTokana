// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

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

        // Trigger all widget re-renders; they read the forecast just cached
        WeatherWidget().updateAll(applicationContext)
        WeatherWidgetLarge().updateAll(applicationContext)

        return Result.success()
    }

    companion object {
        private const val MAX_RETRIES = 3
        private const val REFRESH_WORK = "weather_refresh"
        private const val PERIODIC_WORK = "weather_update"
        private val RECEIVERS = listOf(WeatherWidgetReceiver::class.java, WeatherWidgetLargeReceiver::class.java)

        private fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<WeatherUpdateWorker>(30, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(PERIODIC_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        /**
         * Fetches every 30 minutes while a widget is on the home screen, and stops when none is.
         * [removed] is a widget type whose last widget was just removed.
         */
        fun updatePeriodic(context: Context, removed: Class<*>? = null) {
            // No widget manager on a device without widgets (Android TV)
            val manager = AppWidgetManager.getInstance(context)
            val hasWidget = manager != null && RECEIVERS.any { it != removed && manager.getAppWidgetIds(ComponentName(context, it)).isNotEmpty() }
            if (hasWidget) schedulePeriodic(context) else WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK)
        }

        /** Fetches now in a one-time worker. A tap while a refresh waits or runs does not add a second one. */
        fun refreshNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<WeatherUpdateWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(REFRESH_WORK, ExistingWorkPolicy.KEEP, request)
        }
    }
}
