// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana

import android.content.Context
import android.content.res.Resources
import androidx.core.content.edit
import orinasa.njarasoa.maripanatokana.ui.weather.supportedLocales

/**
 * Sets the app language on the first start after install: the first language of the device that the app has, else
 * English. An app that was installed before keeps its language (Malagasy when the user never changed it).
 * Runs before PrefsMigration, which marks every install as started.
 */
object DefaultLanguage {
    private const val KEY = "locale_index"

    fun apply(context: Context) {
        val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        if (prefs.contains(KEY)) return
        val started = prefs.all.isNotEmpty() ||
            context.getSharedPreferences("private_prefs", Context.MODE_PRIVATE).all.isNotEmpty()
        val locales = Resources.getSystem().configuration.locales
        val tags = (0 until locales.size()).map { locales[it].language }
        prefs.edit { putInt(KEY, if (started) 0 else indexFor(tags)) }
    }

    /** The index in supportedLocales of the first of [languages] (ISO 639 codes) that the app has, else English. */
    internal fun indexFor(languages: List<String>): Int =
        languages.firstNotNullOfOrNull { lang -> supportedLocales.indexOfFirst { it.tag == lang }.takeIf { it >= 0 } }
            ?: supportedLocales.indexOfFirst { it.tag == "en" }
}
