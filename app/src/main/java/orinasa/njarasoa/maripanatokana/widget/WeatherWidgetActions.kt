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
        // Trigger all widget re-renders (provideGlance will call fetch again)
        WeatherWidget().updateAll(context)
        WeatherWidgetLarge().updateAll(context)
    }
}
