package orinasa.njarasoa.maripanatokana

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import orinasa.njarasoa.maripanatokana.ui.theme.MaripanaTokanaTheme
import orinasa.njarasoa.maripanatokana.ui.weather.WeatherScreen
import orinasa.njarasoa.maripanatokana.ui.weather.WeatherViewModel
import orinasa.njarasoa.maripanatokana.ui.weather.supportedLocales
import orinasa.njarasoa.maripanatokana.ui.permission.PermissionHandler
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var permissionHandler: PermissionHandler

    private val viewModel: WeatherViewModel by viewModels()

    private var localeResources: Resources? = null

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase)
        val prefs = newBase.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        val localeIndex = prefs.getInt("locale_index", 0)
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

        handleSharedIntent(intent)

        setContent {
            MaripanaTokanaTheme {
                WeatherScreen(viewModel = viewModel, permissionHandler = permissionHandler)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleSharedIntent(intent)
    }

    private fun handleSharedIntent(incomingIntent: Intent?) {
        if (incomingIntent != null) {
            val action = incomingIntent.action
            var sharedText: String? = null

            if (Intent.ACTION_SEND == action) {
                sharedText = incomingIntent.getStringExtra(Intent.EXTRA_TEXT)
                    ?: incomingIntent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()

                if (sharedText.isNullOrBlank() && incomingIntent.clipData != null) {
                    val clipData = incomingIntent.clipData
                    val items = mutableListOf<String>()
                    if (clipData != null) {
                        for (i in 0 until clipData.itemCount) {
                            val item = clipData.getItemAt(i)
                            val itemText = item.text?.toString() ?: item.uri?.toString()
                            if (!itemText.isNullOrBlank()) {
                                items.add(itemText)
                            }
                        }
                    }
                    if (items.isNotEmpty()) {
                        sharedText = items.joinToString("\n")
                    }
                }
            } else if (Intent.ACTION_VIEW == action && incomingIntent.data != null) {
                sharedText = incomingIntent.dataString
            }

            if (!sharedText.isNullOrBlank()) {
                viewModel.handleSharedText(sharedText)
            }
        }
    }
}
