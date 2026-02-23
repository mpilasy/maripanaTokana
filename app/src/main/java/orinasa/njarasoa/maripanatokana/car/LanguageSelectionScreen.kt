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
import orinasa.njarasoa.maripanatokana.ui.weather.supportedLocales
import java.util.Locale

class LanguageSelectionScreen(carContext: CarContext) : Screen(carContext) {

    override fun onGetTemplate(): Template {
        val prefs = carContext.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        val currentLocaleIndex = prefs.getInt("locale_index", 0)

        // Localized context for title
        val locale = Locale.forLanguageTag(supportedLocales[currentLocaleIndex.coerceIn(supportedLocales.indices)].tag)
        val config = Configuration(carContext.resources.configuration)
        config.setLocale(locale)
        val localizedContext = carContext.createConfigurationContext(config)

        val listBuilder = ItemList.Builder()

        supportedLocales.forEachIndexed { index, localeItem ->
            val isSelected = index == currentLocaleIndex
            val javaLocale = Locale.forLanguageTag(localeItem.tag)
            val displayName = javaLocale.getDisplayName(javaLocale)

            val rowBuilder = Row.Builder()
                .setTitle("${localeItem.flag} $displayName")
                .setOnClickListener {
                    prefs.edit().putInt("locale_index", index).apply()
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
                    .setTitle(localizedContext.getString(R.string.language_settings))
                    .setStartHeaderAction(Action.BACK)
                    .build()
            )
            .build()
    }
}
