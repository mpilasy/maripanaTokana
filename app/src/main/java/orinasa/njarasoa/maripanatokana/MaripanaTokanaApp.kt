package orinasa.njarasoa.maripanatokana

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import orinasa.njarasoa.maripanatokana.widget.WeatherUpdateWorker
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class MaripanaTokanaApp : Application() {

    override fun onCreate() {
        super.onCreate()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val weatherWork = PeriodicWorkRequestBuilder<WeatherUpdateWorker>(
            30, TimeUnit.MINUTES,
        ).setConstraints(constraints).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "weather_update",
            ExistingPeriodicWorkPolicy.KEEP,
            weatherWork,
        )
    }
}
