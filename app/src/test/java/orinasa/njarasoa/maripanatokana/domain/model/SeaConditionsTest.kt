// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SeaConditionsTest {
    private val hour = 60 * 60 * 1000L
    private val times = (0 until 12).map { it * hour }

    @Test
    fun `next high and low are the first local extremes after now`() {
        //                      0     1    2    3    4     5     6     7    8    9    10    11
        val levels = listOf(-0.2, 0.1, 0.4, 0.3, 0.0, -0.3, -0.4, -0.1, 0.2, 0.5, 0.45, 0.1)
        assertEquals(Tides(TidePoint(2 * hour, 0.4), TidePoint(6 * hour, -0.4)), nextTides(times, levels, 0L))
        // After 2:00, the next high is at 9:00
        assertEquals(Tides(TidePoint(9 * hour, 0.5), TidePoint(6 * hour, -0.4)), nextTides(times, levels, 2 * hour))
        // After 9:00 no extreme is left
        assertEquals(Tides(null, null), nextTides(times, levels, 9 * hour))
    }

    @Test
    fun `a flat top gives its first hour`() {
        val levels = listOf(0.0, 0.3, 0.3, 0.0, -0.2, 0.0)
        assertEquals(TidePoint(hour, 0.3), nextTides(times.take(6), levels, -1L).high)
        assertEquals(TidePoint(4 * hour, -0.2), nextTides(times.take(6), levels, -1L).low)
    }

    @Test
    fun `missing levels are skipped`() {
        val levels = listOf(0.0, null, 0.5, 0.2, null, 0.1, 0.3)
        val tides = nextTides(times.take(7), levels, -1L)
        assertNull(tides.high)
        assertNull(tides.low)
    }
}
