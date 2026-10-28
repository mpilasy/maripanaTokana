// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.domain.model

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * The moon's phase, place in the sky, rise and set, calculated on the device. The formulas are
 * those of SunCalc (Vladimir Agafonkin, BSD-2-Clause), after "Astronomy Answers" by Aart Kerkhoven.
 * The web app has the same code in `web/src/lib/domain/moon.ts`; keep the two the same.
 */
object Moon {
    private const val RAD = PI / 180
    private const val DAY_MS = 86_400_000.0
    private const val J1970 = 2440588.0
    private const val J2000 = 2451545.0
    private const val E = RAD * 23.4397 // obliquity of the Earth

    /** [fraction] lit (0-1); [phase] 0 = new, 0.25 = first quarter, 0.5 = full, 0.75 = last quarter. */
    data class Illumination(val fraction: Double, val phase: Double)

    /** [azimuth] in degrees from north, clockwise; [altitude] in degrees above the horizon. */
    data class Position(val azimuth: Double, val altitude: Double)

    /** Epoch millis; null when the moon does not rise (or set) in the 24 hours searched. */
    data class Times(val rise: Long?, val set: Long?)

    private fun toDays(ms: Long) = ms / DAY_MS - 0.5 + J1970 - J2000

    private fun rightAscension(l: Double, b: Double) = atan2(sin(l) * cos(E) - tan(b) * sin(E), cos(l))
    private fun declination(l: Double, b: Double) = asin(sin(b) * cos(E) + cos(b) * sin(E) * sin(l))
    private fun siderealTime(d: Double, lw: Double) = RAD * (280.16 + 360.9856235 * d) - lw

    private fun astroRefraction(h: Double): Double {
        val a = if (h < 0) 0.0 else h
        return 0.0002967 / tan(a + 0.00312536 / (a + 0.08901179))
    }

    private class Coords(val ra: Double, val dec: Double, val dist: Double)

    private fun sunCoords(d: Double): Coords {
        val m = RAD * (357.5291 + 0.98560028 * d)
        val c = RAD * (1.9148 * sin(m) + 0.02 * sin(2 * m) + 0.0003 * sin(3 * m))
        val l = m + c + RAD * 102.9372 + PI
        return Coords(rightAscension(l, 0.0), declination(l, 0.0), 0.0)
    }

    private fun moonCoords(d: Double): Coords {
        val l = RAD * (218.316 + 13.176396 * d)
        val m = RAD * (134.963 + 13.064993 * d)
        val f = RAD * (93.272 + 13.229350 * d)
        val lon = l + RAD * 6.289 * sin(m)
        val lat = RAD * 5.128 * sin(f)
        return Coords(rightAscension(lon, lat), declination(lon, lat), 385001 - 20905 * cos(m))
    }

    /** Altitude in radians, with refraction; azimuth in radians from south, westward (SunCalc's). */
    private fun rawPosition(ms: Long, lat: Double, lon: Double): Pair<Double, Double> {
        val lw = RAD * -lon
        val phi = RAD * lat
        val d = toDays(ms)
        val c = moonCoords(d)
        val h = siderealTime(d, lw) - c.ra
        val az = atan2(sin(h), cos(h) * sin(phi) - tan(c.dec) * cos(phi))
        val alt = asin(sin(phi) * sin(c.dec) + cos(phi) * cos(c.dec) * cos(h))
        return az to alt + astroRefraction(alt)
    }

    fun position(ms: Long, lat: Double, lon: Double): Position {
        val (az, alt) = rawPosition(ms, lat, lon)
        return Position(((az / RAD + 180) % 360 + 360) % 360, alt / RAD)
    }

    fun illumination(ms: Long): Illumination {
        val d = toDays(ms)
        val s = sunCoords(d)
        val m = moonCoords(d)
        val sdist = 149598000.0
        val phi = acos(sin(s.dec) * sin(m.dec) + cos(s.dec) * cos(m.dec) * cos(s.ra - m.ra))
        val inc = atan2(sdist * sin(phi), m.dist - sdist * cos(phi))
        val angle = atan2(cos(s.dec) * sin(s.ra - m.ra), sin(s.dec) * cos(m.dec) - cos(s.dec) * sin(m.dec) * cos(s.ra - m.ra))
        return Illumination((1 + cos(inc)) / 2, 0.5 + 0.5 * inc * (if (angle < 0) -1 else 1) / PI)
    }

    /** The rise and set in the 24 hours from [dayStart] (epoch millis, e.g. midnight at the place). */
    fun times(dayStart: Long, lat: Double, lon: Double): Times {
        val hc = 0.133 * RAD
        fun altAt(hours: Int) = rawPosition(dayStart + hours * 3_600_000L, lat, lon).second - hc
        var h0 = altAt(0)
        var rise: Double? = null
        var set: Double? = null
        // Fits a parabola through each two hours of altitudes and finds where it crosses the horizon
        for (i in 1..23 step 2) {
            val h1 = altAt(i)
            val h2 = altAt(i + 1)
            val a = (h0 + h2) / 2 - h1
            val b = (h2 - h0) / 2
            val xe = -b / (2 * a)
            val ye = (a * xe + b) * xe + h1
            val disc = b * b - 4 * a * h1
            var roots = 0
            var x1 = 0.0
            var x2 = 0.0
            if (disc >= 0) {
                val dx = sqrt(disc) / (abs(a) * 2)
                x1 = xe - dx
                x2 = xe + dx
                if (abs(x1) <= 1) roots++
                if (abs(x2) <= 1) roots++
                if (x1 < -1) x1 = x2
            }
            if (roots == 1) {
                if (h0 < 0) rise = i + x1 else set = i + x1
            } else if (roots == 2) {
                rise = i + if (ye < 0) x2 else x1
                set = i + if (ye < 0) x1 else x2
            }
            if (rise != null && set != null) break
            h0 = h2
        }
        return Times(rise?.let { dayStart + (it * 3_600_000).toLong() }, set?.let { dayStart + (it * 3_600_000).toLong() })
    }

    /** Index into moon_phases: new, waxing crescent, first quarter, waxing gibbous, full, waning gibbous, last quarter, waning crescent. */
    fun phaseIndex(phase: Double): Int = ((phase * 8 + 0.5).toInt()) % 8
}
