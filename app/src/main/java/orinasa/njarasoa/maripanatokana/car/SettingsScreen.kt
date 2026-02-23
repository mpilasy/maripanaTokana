package orinasa.njarasoa.maripanatokana.car

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.Header
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import orinasa.njarasoa.maripanatokana.R
import orinasa.njarasoa.maripanatokana.ui.weather.supportedLocales
import java.util.Locale

class SettingsScreen(carContext: CarContext) : Screen(carContext) {

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

    override fun onGetTemplate(): Template {
        val listBuilder = ItemList.Builder()

        listBuilder.addItem(
            Row.Builder()
                .setTitle(getString(R.string.language_settings))
                .setBrowsable(true)
                .setOnClickListener {
                    screenManager.push(LanguageSelectionScreen(carContext))
                }
                .build()
        )

        listBuilder.addItem(
            Row.Builder()
                .setTitle(getString(R.string.font_settings))
                .setBrowsable(true)
                .setOnClickListener {
                    screenManager.push(FontSelectionScreen(carContext))
                }
                .build()
        )

        return ListTemplate.Builder()
            .setSingleList(listBuilder.build())
            .setHeader(
                Header.Builder()
                    .setTitle(getString(R.string.settings_title))
                    .setStartHeaderAction(Action.BACK)
                    .build()
            )
            .build()
    }
}
