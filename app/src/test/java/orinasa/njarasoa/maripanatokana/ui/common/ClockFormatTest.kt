// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class ClockFormatTest {
    // 2026-10-05 21:05 UTC
    private val evening = Date(1791234300000L)
    // 2026-10-05 21:00 UTC
    private val fullHour = Date(1791234000000L)

    private fun format(pattern: String, date: Date, locale: Locale = Locale.ENGLISH) =
        clockFormat(pattern, locale).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(date)

    @Test
    fun patterns24Hour() {
        assertEquals("HH:mm", clockPattern(true))
        assertEquals("HH:mm", hourLabelPattern(true))
        assertEquals("HH:mm", hourLabelPattern(true, wholeHour = false))
    }

    @Test
    fun patterns12Hour() {
        assertEquals("h:mm a", clockPattern(false))
        assertEquals("h a", hourLabelPattern(false))
        // Not on the full hour: the minutes stay
        assertEquals("h:mm a", hourLabelPattern(false, wholeHour = false))
    }

    @Test
    fun formats24Hour() {
        assertEquals("21:05", format(clockPattern(true), evening))
        assertEquals("21:00", format(hourLabelPattern(true), fullHour))
    }

    @Test
    fun formats12Hour() {
        assertEquals("9:05 PM", format(clockPattern(false), evening))
        assertEquals("9 PM", format(hourLabelPattern(false), fullHour))
        assertEquals("Mon 5 Oct, 9:05 PM", format("EEE d MMM, ${clockPattern(false)}", evening))
    }

    @Test
    fun amPmMarkerHasNoDots() {
        // Spanish "p. m.": the digit helper would change the dots to decimal commas
        assertEquals("9:05 pm", format(clockPattern(false), evening, Locale.forLanguageTag("es")))
    }

    @Test
    fun malagasyShortWeekdays() {
        val mg = Locale.forLanguageTag("mg")
        val day = 24 * 3600 * 1000L
        // 2026-10-09 is a Friday: "Zoma" is already short, so it stays in full
        val friday = Date(evening.time + 4 * day)
        val thursday = Date(evening.time + 3 * day)
        fun weekday(date: Date) = appDateFormat("EEE", mg).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(date)
        assertEquals("Zoma", weekday(friday))
        assertEquals("Alk", weekday(thursday))
        // The other days from Saturday 2026-10-10: each short name is different
        assertEquals(listOf("Sab", "Alh", "Alt", "Tal", "Alr"), (5..9).map { weekday(Date(evening.time + it * day)) })
        // The full name and other languages do not change
        assertEquals("Zoma", appDateFormat("EEEE", mg).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(friday))
        assertEquals("Fri", appDateFormat("EEE", Locale.ENGLISH).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(friday))
        // clockFormat uses the same names
        assertEquals("Zoma, 21:05", format("EEE, ${clockPattern(true)}", friday, mg))
    }
}
