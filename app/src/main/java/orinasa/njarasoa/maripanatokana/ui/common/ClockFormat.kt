// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.ui.common

import android.content.Context
import android.text.format.DateFormat
import java.text.SimpleDateFormat
import java.util.Locale

/** True when the device shows a 24-hour clock. The default comes from the device region. */
fun uses24HourClock(context: Context): Boolean = DateFormat.is24HourFormat(context)

/** A time with minutes: "HH:mm" (24-hour) or "h:mm a" (12-hour, for example "9:05 PM"). */
fun clockPattern(uses24: Boolean): String = if (uses24) "HH:mm" else "h:mm a"

/**
 * The label of a full hour: "HH:mm" (24-hour) or "h a" (12-hour, for example "9 PM").
 * [wholeHour] is false for times that are not on the full hour (a place or a device at a :30 or :45 offset): they keep the minutes.
 */
fun hourLabelPattern(uses24: Boolean, wholeHour: Boolean = true): String = if (uses24 || !wholeHour) clockPattern(uses24) else "h a"

/**
 * A formatter for [pattern] in [locale]. The AM/PM marker has no dots or spaces ("p. m." becomes "pm"):
 * the digit helper changes dots to decimal commas.
 */
fun clockFormat(pattern: String, locale: Locale): SimpleDateFormat =
    SimpleDateFormat(pattern, locale).apply {
        dateFormatSymbols = dateFormatSymbols.apply { amPmStrings = amPmStrings.map { it.replace(AM_PM_JUNK, "") }.toTypedArray() }
    }

private val AM_PM_JUNK = Regex("[.\\s\\u00A0\\u202F]")
