// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class WeatherWidgetLargeReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WeatherWidgetLarge()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WeatherUpdateWorker.updatePeriodic(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WeatherUpdateWorker.updatePeriodic(context, removed = WeatherWidgetLargeReceiver::class.java)
    }
}
