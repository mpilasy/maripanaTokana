// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.ui.common

/** [name] is the language's own name, for the language list. */
data class SupportedLocale(val tag: String, val flag: String, val name: String, val nativeZero: Char? = null) {
    // Java has no CLDR data for "mg": use French, which also has a decimal comma
    private val decimalSep: Char = java.text.DecimalFormatSymbols(
        java.util.Locale.forLanguageTag(if (tag == "mg") "fr" else tag)
    ).decimalSeparator

    /** Replace ASCII digits 0-9 with native script digits and '.' with locale decimal separator. */
    fun localizeDigits(s: String): String {
        if (nativeZero == null && decimalSep == '.') return s
        val z = nativeZero
        return buildString(s.length) {
            for (c in s) {
                append(when {
                    z != null && c in '0'..'9' -> z + (c - '0')
                    c == '.' && decimalSep != '.' -> decimalSep
                    else -> c
                })
            }
        }
    }
}

val supportedLocales = listOf(
    SupportedLocale("mg", "🇲🇬", "Malagasy"),
    SupportedLocale("ar", "🇸🇦", "العربية", '٠'),  // ٠
    SupportedLocale("en", "🇬🇧", "English"),
    SupportedLocale("es", "🇪🇸", "Español"),
    SupportedLocale("fr", "🇫🇷", "Français"),
    SupportedLocale("hi", "🇮🇳", "हिन्दी", '०'),  // ०
    SupportedLocale("ne", "🇳🇵", "नेपाली", '०'),  // ०
    SupportedLocale("zh", "🇨🇳", "中文"),
)
