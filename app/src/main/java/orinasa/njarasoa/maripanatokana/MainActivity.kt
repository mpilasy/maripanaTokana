package orinasa.njarasoa.maripanatokana

import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import orinasa.njarasoa.maripanatokana.ui.theme.MaripanaTokanaTheme
import orinasa.njarasoa.maripanatokana.ui.weather.WeatherScreen
import orinasa.njarasoa.maripanatokana.ui.weather.supportedLocales

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var localeResources: Resources? = null

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase)
        val prefs = newBase.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        val localeIndex = prefs.getInt("locale_index", 6)
            .coerceIn(supportedLocales.indices)
        val locale = java.util.Locale.forLanguageTag(supportedLocales[localeIndex].tag)
        val config = android.content.res.Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        localeResources = newBase.createConfigurationContext(config).resources
    }

    override fun getResources(): Resources = localeResources ?: super.getResources()

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
