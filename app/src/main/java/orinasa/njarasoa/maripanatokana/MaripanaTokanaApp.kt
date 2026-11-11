// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import orinasa.njarasoa.maripanatokana.widget.WeatherUpdateWorker

@HiltAndroidApp
class MaripanaTokanaApp : Application() {

    override fun onCreate() {
        super.onCreate()
        DefaultLanguage.apply(this)
        PrefsMigration.migrate(this)

        // The widgets refresh in the background only while one is on the home screen
        WeatherUpdateWorker.updatePeriodic(this)
    }
}
