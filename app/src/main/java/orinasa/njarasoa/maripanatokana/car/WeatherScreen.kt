package orinasa.njarasoa.maripanatokana.car

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.Header
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import orinasa.njarasoa.maripanatokana.R
import orinasa.njarasoa.maripanatokana.data.remote.wmoDescriptionRes
import orinasa.njarasoa.maripanatokana.domain.model.WeatherData
import orinasa.njarasoa.maripanatokana.ui.weather.supportedLocales
import orinasa.njarasoa.maripanatokana.widget.WidgetWeatherFetcher
import java.util.Locale

class WeatherScreen(carContext: CarContext) : Screen(carContext) {

    private var weatherData: WeatherData? = null
    private var isLoading = true

    private fun getLocalizedResources(): Resources {
        val prefs = carContext.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        val localeIndex = prefs.getInt("locale_index", 0)
            .coerceIn(supportedLocales.indices)
        val locale = Locale.forLanguageTag(supportedLocales[localeIndex].tag)
        val config = Configuration(carContext.resources.configuration)
        config.setLocale(locale)
        return carContext.createConfigurationContext(config).resources
    }

    private fun getString(resId: Int, vararg args: Any): String {
        return getLocalizedResources().getString(resId, *args)
    }

    init {
        refresh()
    }

    private fun refresh() {
        lifecycleScope.launch {
            isLoading = true
            invalidate()
            weatherData = WidgetWeatherFetcher.fetch(carContext)
            isLoading = false
            invalidate()
        }
    }

    override fun onGetTemplate(): Template {
        val paneBuilder = Pane.Builder()

        if (isLoading) {
            paneBuilder.setLoading(true)
        } else {
            val data = weatherData
            if (data == null) {
                paneBuilder.addRow(
                    Row.Builder()
                        .setTitle(getString(R.string.error_fetch_weather))
                        .addText(getString(R.string.widget_tap_to_refresh))
                        .setOnClickListener { refresh() }
                        .build()
                )
            } else {
                val prefs = carContext.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
                val metricPrimary = prefs.getBoolean("metric_primary", true)

                // Current Weather
                val (tempPrimary, tempSecondary) = data.temperature.displayDual(metricPrimary)
                val description = getString(wmoDescriptionRes(data.weatherCode))

                paneBuilder.addRow(
                    Row.Builder()
                        .setTitle(getString(R.string.widget_now_in, data.locationName))
                        .addText("$tempPrimary / $tempSecondary")
                        .addText(description)
                        .build()
                )

                // Details Row 1: Wind
                val (windP, windS) = data.windSpeed.displayDual(metricPrimary)
                paneBuilder.addRow(
                    Row.Builder()
                        .setTitle(getString(R.string.widget_wind))
                        .addText("$windP / $windS")
                        .build()
                )

                // Details Row 2: Feels Like
                val (feelsP, feelsS) = data.feelsLike.displayDual(metricPrimary)
                paneBuilder.addRow(
                    Row.Builder()
                        .setTitle(getString(R.string.widget_feels_like))
                        .addText("$feelsP / $feelsS")
                        .build()
                )

                // Details Row 3: Humidity
                paneBuilder.addRow(
                    Row.Builder()
                        .setTitle(getString(R.string.widget_humidity))
                        .addText("${data.humidity}%")
                        .build()
                )
            }
        }

        val settingsAction = Action.Builder()
            .setTitle(getString(R.string.settings_title))
            .setOnClickListener {
                screenManager.push(SettingsScreen(carContext))
            }
            .build()

        val refreshAction = Action.Builder()
            .setTitle(getString(R.string.widget_tap_to_refresh))
            .setOnClickListener { refresh() }
            .build()

        return PaneTemplate.Builder(paneBuilder.build())
            .setHeader(
                Header.Builder()
                    .setTitle(getString(R.string.app_name))
                    .setStartHeaderAction(Action.APP_ICON)
                    .addEndHeaderAction(refreshAction)
                    .addEndHeaderAction(settingsAction)
                    .build()
            )
            .build()
    }
}
