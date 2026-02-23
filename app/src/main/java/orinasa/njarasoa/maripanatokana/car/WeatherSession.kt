package orinasa.njarasoa.maripanatokana.car

import android.content.Intent
import androidx.car.app.Screen
import androidx.car.app.Session

class WeatherSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen {
        return WeatherScreen(carContext)
    }
}
