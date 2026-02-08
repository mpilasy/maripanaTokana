package orinasa.njarasoa.maripanatokana

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import orinasa.njarasoa.maripanatokana.ui.theme.MaripanaTokanaTheme
import orinasa.njarasoa.maripanatokana.ui.weather.WeatherScreen

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                moveTaskToBack(true)
            }
        })

        setContent {
            MaripanaTokanaTheme {
                WeatherScreen()
            }
        }
    }
}
