// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test

class NightWeatherTest {
    private val h = 3_600_000L
    // Hourly data for 48 hours from t=0; sunset at 18h, sunrise at 6h
    private val times = (0 until 48).map { it * h }

    @Test
    fun `takes the most severe code between sunset and the next sunrise`() {
        val codes = times.map { if (it == 22 * h) 61 else 2 }
        assertEquals(61, nightWeatherCodes(times, codes, listOf(18 * h, 42 * h), listOf(6 * h, 30 * h))[0])
    }

    @Test
    fun `ignores the daytime hours`() {
        val codes = times.map { if (it == 12 * h) 95 else 1 }
        assertEquals(listOf(1), nightWeatherCodes(times, codes, listOf(18 * h), listOf(6 * h, 30 * h)))
    }

    @Test
    fun `is null for a night the hourly data does not cover`() {
        val codes = times.map { 3 }
        // The second night runs 42h-54h but the data stops at 47h
        assertEquals(listOf(3, null), nightWeatherCodes(times, codes, listOf(18 * h, 42 * h), listOf(6 * h, 30 * h)))
    }
}
