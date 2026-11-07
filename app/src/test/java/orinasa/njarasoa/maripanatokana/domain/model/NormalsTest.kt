// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class NormalsTest {
    /** Every day of [years], with the value [value] gives for a day. */
    private fun series(years: IntRange, value: (Calendar) -> Double): Triple<List<String>, List<Double?>, List<Double?>> {
        val dates = mutableListOf<String>()
        val max = mutableListOf<Double?>()
        val min = mutableListOf<Double?>()
        val c = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { clear(); set(years.first, Calendar.JANUARY, 1) }
        while (c.get(Calendar.YEAR) <= years.last) {
            dates += "%04d-%02d-%02d".format(Locale.US, c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
            max += value(c)
            min += value(c) - 10
            c.add(Calendar.DAY_OF_MONTH, 1)
        }
        return Triple(dates, max, min)
    }

    @Test
    fun `day of year counts as in a leap year`() {
        assertEquals(1, dayOfYear("2025-01-01"))
        assertEquals(59, dayOfYear("2025-02-28"))
        assertEquals(60, dayOfYear("2024-02-29"))
        assertEquals(61, dayOfYear("2025-03-01"))
        assertEquals(366, dayOfYear("2025-12-31"))
        assertNull(dayOfYear("bad"))
    }

    @Test
    fun `a steady value gives that value every day`() {
        val (dates, max, min) = series(2016..2025) { 20.0 }
        val normals = dailyNormals(dates, max, min)
        assertEquals(366, normals.max.size)
        (1..366).forEach { assertEquals(20.0, normals.max[it - 1]!!, 1e-9) }
        assertEquals(NormalTemps(20.0, 10.0), normalFor(normals, "2026-07-14"))
    }

    @Test
    fun `the mean takes 7 days on each side and all years`() {
        // One year at 10, one at 20; in each, March 10 is 15 degrees warmer
        val (dates, max, min) = series(2023..2024) { c ->
            val base = if (c.get(Calendar.YEAR) == 2023) 10.0 else 20.0
            if (c.get(Calendar.MONTH) == Calendar.MARCH && c.get(Calendar.DAY_OF_MONTH) == 10) base + 15 else base
        }
        val normals = dailyNormals(dates, max, min)
        // 30 values around March 10: mean 15, plus 2 × 15 / 30
        assertEquals(16.0, normalFor(normals, "2026-03-10")!!.high, 1e-9)
        assertEquals(16.0, normalFor(normals, "2026-03-17")!!.high, 1e-9)
        assertEquals(15.0, normalFor(normals, "2026-03-18")!!.high, 1e-9)
    }

    @Test
    fun `the window wraps around the year end`() {
        // December at 0, the other months at 30
        val (dates, max, min) = series(2020..2025) { c -> if (c.get(Calendar.MONTH) == Calendar.DECEMBER) 0.0 else 30.0 }
        val normals = dailyNormals(dates, max, min)
        // January 1: December 25-31 (7 days at 0) and January 1-8 (8 days at 30)
        assertEquals(16.0, normalFor(normals, "2026-01-01")!!.high, 1e-9)
        // December 31: December 24-31 (8 days at 0) and January 1-7 (7 days at 30)
        assertEquals(14.0, normalFor(normals, "2026-12-31")!!.high, 1e-9)
    }

    @Test
    fun `February 29 has a normal from the days near it`() {
        val (dates, max, min) = series(2021..2023) { 5.0 }
        val normals = dailyNormals(dates, max, min)
        assertEquals(5.0, normalFor(normals, "2024-02-29")!!.high, 1e-9)
    }

    @Test
    fun `missing values are skipped`() {
        val normals = dailyNormals(listOf("2025-06-01", "2025-06-02"), listOf(null, 24.0), listOf(12.0, null))
        assertEquals(NormalTemps(24.0, 12.0), normalFor(normals, "2026-06-01"))
        assertNull(normalFor(normals, "2026-01-01"))
    }

    @Test
    fun `local date is the date at the place`() {
        // 2026-11-07 22:00 UTC is November 8 in Tokyo (UTC+9) and November 7 in New York (UTC-5)
        val instant = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { clear(); set(2026, Calendar.NOVEMBER, 7, 22, 0) }.timeInMillis
        assertEquals("2026-11-08", localDate(instant, 9 * 3600))
        assertEquals("2026-11-07", localDate(instant, -5 * 3600))
    }
}
