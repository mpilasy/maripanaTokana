package orinasa.njarasoa.maripanatokana.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll

class RefreshActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        // Fetch once here; the re-renders read the cache
        WidgetWeatherFetcher.fetch(context)
        WeatherWidget().updateAll(context)
        WeatherWidgetLarge().updateAll(context)
    }
}
