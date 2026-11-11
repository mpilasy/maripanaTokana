// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class WeatherWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WeatherWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WeatherUpdateWorker.updatePeriodic(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WeatherUpdateWorker.updatePeriodic(context, removed = WeatherWidgetReceiver::class.java)
    }
}
