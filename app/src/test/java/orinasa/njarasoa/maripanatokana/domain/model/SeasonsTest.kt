// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import kotlin.math.abs

/** Reference values from Meeus's examples and from the US Naval Observatory, as in seasons.test.ts. */
class SeasonsTest {
    private val minute = 60_000L
    private fun ms(text: String) = Instant.parse(text).toEpochMilli()

    @Test
    fun declination() {
        // Meeus example 25.a: 1992 October 13 at 0h, -7° 47' 06"
        assertEquals(-7.78507, Seasons.declination(ms("1992-10-13T00:00:00Z")), 1e-3)
        // Near the solstices the sun is over the tropics
        assertEquals(-23.44, Seasons.declination(ms("2026-12-21T20:50:00Z")), 0.05)
        assertEquals(23.44, Seasons.declination(ms("2027-06-21T14:11:00Z")), 0.05)
    }

    @Test
    fun eventTimes() {
        // Meeus example 27.a: the June solstice of 1962, 21:25 dynamical time
        assertTrue(abs(Seasons.eventTime(1962, 1) + 69_000 -ms("1962-06-21T21:25:08Z")) < minute)
        // US Naval Observatory, UTC
        assertTrue(abs(Seasons.eventTime(2024, 3) - ms("2024-12-21T09:20:00Z")) < 2 * minute)
        assertTrue(abs(Seasons.eventTime(2026, 3) - ms("2026-12-21T20:50:00Z")) < 2 * minute)
        assertTrue(abs(Seasons.eventTime(2027, 0) - ms("2027-03-20T20:25:00Z")) < 2 * minute)
        assertTrue(abs(Seasons.eventTime(2030, 1) - ms("2030-06-21T07:31:00Z")) < 2 * minute)
    }

    @Test
    fun noonOffsetAndRange() {
        // At the equator on the June solstice the noon sun is 23.44° to the north (negative)
        assertEquals(-23.44, Seasons.noonOffset(0.0, ms("2027-06-21T12:00:00Z")), 0.05)
        // Roseville (45° N): 21.6° to 68.4° to the south; the equator: 23.44° north to 23.44° south
        assertEquals(45 - 23.44 to 45 + 23.44, Seasons.noonRange(45.0))
        assertEquals(-23.44 to 23.44, Seasons.noonRange(0.0))
        // Antananarivo (18.9° S): 4.5° south to 42.3° north, the one nearer overhead first
        val (near, far) = Seasons.noonRange(-18.9)
        assertEquals(4.54, near, 0.005)
        assertEquals(-42.34, far, 0.005)
    }

    @Test
    fun seasonNamesAndDaysUntil() {
        assertEquals(3, Seasons.seasonNameIndex(3, 45.0)) // December: winter solstice in the north
        assertEquals(1, Seasons.seasonNameIndex(3, -18.9)) // summer solstice in the south
        assertEquals(2, Seasons.seasonNameIndex(0, -18.9)) // March: autumn equinox in the south
        // Days at the place: 23:00 to 01:00 the next day is one day, 01:00 to 23:00 the same day is none
        val offset = -6 * 3600
        assertEquals(1, Seasons.daysUntil(ms("2026-11-10T05:00:00Z"), ms("2026-11-10T07:00:00Z"), offset))
        assertEquals(0, Seasons.daysUntil(ms("2026-11-10T07:00:00Z"), ms("2026-11-11T05:00:00Z"), offset))
    }

    @Test
    fun around() {
        val a = Seasons.around(ms("2026-11-10T00:00:00Z"))
        assertEquals(2, a.previous.kind)
        assertEquals(3, a.next.kind)
        assertEquals(Seasons.eventTime(2026, 3), a.next.time)
        // Across the new year: the December solstice before, the March equinox after
        val b = Seasons.around(ms("2027-01-05T00:00:00Z"))
        assertEquals(Seasons.Event(Seasons.eventTime(2026, 3), 3), b.previous)
        assertEquals(Seasons.Event(Seasons.eventTime(2027, 0), 0), b.next)
    }
}
