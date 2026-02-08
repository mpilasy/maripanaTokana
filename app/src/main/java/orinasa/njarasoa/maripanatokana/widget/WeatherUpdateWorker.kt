package orinasa.njarasoa.maripanatokana.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class WeatherUpdateWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val data = WidgetWeatherFetcher.fetch(applicationContext)

        if (data == null) return Result.retry()

        // Save refresh timestamp
        applicationContext
            .getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
            .edit()
            .putLong("last_refresh", System.currentTimeMillis())
            .apply()

        // Trigger all widget re-renders
        WeatherWidget().updateAll(applicationContext)
        WeatherWidgetLarge().updateAll(applicationContext)

        return Result.success()
    }
}
