// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

/** Reference values from SunCalc's own tests (2013-03-05 UTC, 50.5° N, 30.5° E). */
class MoonTest {
    private val lat = 50.5
    private val lon = 30.5
    private val date = Instant.parse("2013-03-05T00:00:00Z").toEpochMilli()

    @Test
    fun position() {
        val p = Moon.position(date, lat, lon)
        // SunCalc: azimuth -0.9783999522438226 rad from south, altitude 0.014551482243892251 rad
        assertEquals(-0.9783999522438226 * 180 / Math.PI + 180, p.azimuth, 1e-6)
        assertEquals(0.014551482243892251 * 180 / Math.PI, p.altitude, 1e-6)
    }

    @Test
    fun illumination() {
        val i = Moon.illumination(date)
        assertEquals(0.4848068202456373, i.fraction, 1e-9)
        assertEquals(0.7548368838538762, i.phase, 1e-9)
        assertEquals(6, Moon.phaseIndex(i.phase)) // last quarter
    }

    @Test
    fun times() {
        val t = Moon.times(Instant.parse("2013-03-04T00:00:00Z").toEpochMilli(), lat, lon)
        // SunCalc: rise Mon, 04 Mar 2013 23:54:29 GMT, set Mon, 04 Mar 2013 07:47:58 GMT
        assertEquals(Instant.parse("2013-03-04T23:54:29Z").toEpochMilli().toDouble(), t.rise!!.toDouble(), 1000.0)
        assertEquals(Instant.parse("2013-03-04T07:47:58Z").toEpochMilli().toDouble(), t.set!!.toDouble(), 1000.0)
    }

    @Test
    fun phaseIndexWrapsAtNewMoon() {
        assertEquals(0, Moon.phaseIndex(0.98))
        assertEquals(4, Moon.phaseIndex(0.5))
    }

    @Test
    fun nextPhaseFindsTheNewAndFullMoon() {
        // January 2024: new moon on the 11th at 11:57 UTC, full moon on the 25th at 17:54 UTC; the formulas are
        // good to some hours
        val from = Instant.parse("2024-01-01T00:00:00Z").toEpochMilli()
        assertEquals(Instant.parse("2024-01-11T11:57:00Z").toEpochMilli().toDouble(), Moon.nextPhase(from, 0.0).toDouble(), 8 * 3_600_000.0)
        assertEquals(Instant.parse("2024-01-25T17:54:00Z").toEpochMilli().toDouble(), Moon.nextPhase(from, 0.5).toDouble(), 8 * 3_600_000.0)
        // From just after a full moon: the next one, about a month later
        val full = Moon.nextPhase(from, 0.5)
        assertEquals(29.5 * 86_400_000, (Moon.nextPhase(full + 60_000, 0.5) - full).toDouble(), 86_400_000.0)
    }

    private fun noon(date: String) = Instant.parse("${date}T12:00:00Z").toEpochMilli()

    private fun assertNear(date: String, ms: Long) = assertEquals(noon(date).toDouble(), ms.toDouble(), 2 * 86_400_000.0)

    @Test
    fun polarNightAtAlert() {
        val p = Moon.polarPeriod(noon("2026-10-29"), 82.5, midnightSun = false)
        assertNear("2026-10-14", p.since)
        assertNear("2027-02-27", p.until)
    }

    @Test
    fun midnightSunAtTheSouthPole() {
        val p = Moon.polarPeriod(noon("2026-10-29"), -89.99, midnightSun = true)
        assertNear("2026-09-22", p.since)
        assertNear("2027-03-21", p.until)
    }
}
