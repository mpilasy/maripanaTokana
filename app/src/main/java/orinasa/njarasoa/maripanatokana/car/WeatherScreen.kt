package orinasa.njarasoa.maripanatokana.car

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.CarIcon
import androidx.car.app.model.CarText
import androidx.car.app.model.Header
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import orinasa.njarasoa.maripanatokana.R
import orinasa.njarasoa.maripanatokana.data.remote.wmoDescriptionRes
import orinasa.njarasoa.maripanatokana.domain.model.WeatherData
import orinasa.njarasoa.maripanatokana.ui.theme.fontPairings
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

    private fun cycleLanguage() {
        val prefs = carContext.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        val currentIndex = prefs.getInt("locale_index", 0)
        val newIndex = (currentIndex + 1) % supportedLocales.size
        prefs.edit().putInt("locale_index", newIndex).apply()
        invalidate()
    }

    private fun cycleFont() {
        val prefs = carContext.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        val currentIndex = prefs.getInt("font_index", 0)
        val newIndex = (currentIndex + 1) % fontPairings.size
        prefs.edit().putInt("font_index", newIndex).apply()

        // Show current font name as a transient message if possible, or invalidate to reflect changes if any
        val fontName = fontPairings[newIndex].name
        androidx.car.app.CarToast.makeText(carContext, fontName, androidx.car.app.CarToast.LENGTH_SHORT).show()
        invalidate()
    }

    private fun createTextIcon(text: String): CarIcon {
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 64f
            color = android.graphics.Color.WHITE
            textAlign = android.graphics.Paint.Align.CENTER
        }
        val size = 96
        val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val yPos = (size / 2) - ((paint.descent() + paint.ascent()) / 2)
        canvas.drawText(text, (size / 2).toFloat(), yPos, paint)
        return CarIcon.Builder(IconCompat.createWithBitmap(bitmap)).build()
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

                // Settings Actions (Language & Font)
                val localeIndex = prefs.getInt("locale_index", 0).coerceIn(supportedLocales.indices)
                val flag = supportedLocales[localeIndex].flag

                val languageAction = Action.Builder()
                    .setIcon(createTextIcon(flag))
                    .setOnClickListener { cycleLanguage() }
                    .build()

                val fontAction = Action.Builder()
                    .setIcon(CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_font)).build())
                    .setOnClickListener { cycleFont() }
                    .build()

                paneBuilder.addAction(languageAction)
                paneBuilder.addAction(fontAction)
            }
        }

        val refreshAction = Action.Builder()
            .setIcon(CarIcon.APP_ICON) // Reuse app icon as a refresh indicator if generic refresh isn't available, or rely on pane click
            .setTitle(getString(R.string.widget_tap_to_refresh))
            .setOnClickListener { refresh() }
            .build()

        // Note: PaneTemplate Header Actions are limited. We'll use the pane actions for settings.
        // Refresh can be a header action.

        return PaneTemplate.Builder(paneBuilder.build())
            .setHeader(
                Header.Builder()
                    .setTitle(getString(R.string.app_name))
                    .setStartHeaderAction(Action.APP_ICON)
                    .addEndHeaderAction(refreshAction)
                    .build()
            )
            .build()
    }
}
