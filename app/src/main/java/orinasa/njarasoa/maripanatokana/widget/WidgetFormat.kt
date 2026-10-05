package orinasa.njarasoa.maripanatokana.widget

import orinasa.njarasoa.maripanatokana.ui.weather.buildLocationTimeZone
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Short weekday name for a forecast day. [dateMillis] is epoch MILLIS of local midnight at the
 * forecast location, so format in that location's timezone, with the app locale.
 */
internal fun widgetDayName(dateMillis: Long, utcOffsetSeconds: Int, locale: Locale): String =
    SimpleDateFormat("EEE", locale).apply { timeZone = buildLocationTimeZone(utcOffsetSeconds) }.format(Date(dateMillis))

/** Wraps [s] in an LTR isolate so "16.1°C" keeps number-then-unit order in RTL locales. */
internal fun ltr(s: String): String = "\u2066$s\u2069"
