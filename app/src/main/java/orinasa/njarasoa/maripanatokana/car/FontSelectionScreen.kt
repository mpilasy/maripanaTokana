package orinasa.njarasoa.maripanatokana.car

import android.content.Context
import android.content.res.Configuration
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.Header
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import orinasa.njarasoa.maripanatokana.R
import orinasa.njarasoa.maripanatokana.ui.theme.fontPairings
import orinasa.njarasoa.maripanatokana.ui.weather.supportedLocales
import java.util.Locale

class FontSelectionScreen(carContext: CarContext) : Screen(carContext) {

    override fun onGetTemplate(): Template {
        val prefs = carContext.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        val currentFontIndex = prefs.getInt("font_index", 0)

        // Localized context for title
        val localeIndex = prefs.getInt("locale_index", 0).coerceIn(supportedLocales.indices)
        val locale = Locale.forLanguageTag(supportedLocales[localeIndex].tag)
        val config = Configuration(carContext.resources.configuration)
        config.setLocale(locale)
        val localizedContext = carContext.createConfigurationContext(config)

        val listBuilder = ItemList.Builder()

        fontPairings.forEachIndexed { index, pairing ->
            val isSelected = index == currentFontIndex

            val rowBuilder = Row.Builder()
                .setTitle(pairing.name)
                .setOnClickListener {
                    prefs.edit().putInt("font_index", index).apply()
                    screenManager.pop()
                }

            if (isSelected) {
                rowBuilder.addText("✓")
            }

            listBuilder.addItem(rowBuilder.build())
        }

        return ListTemplate.Builder()
            .setSingleList(listBuilder.build())
            .setHeader(
                Header.Builder()
                    .setTitle(localizedContext.getString(R.string.font_settings))
                    .setStartHeaderAction(Action.BACK)
                    .build()
            )
            .build()
    }
}
