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
        // Fetch fresh weather (also warms the SharedPreferences cache for widgets)
        WidgetWeatherFetcher.fetch(applicationContext)

        // Trigger all widget re-renders
        WeatherWidget().updateAll(applicationContext)
        WeatherWidgetLarge().updateAll(applicationContext)

        return Result.success()
    }
}
