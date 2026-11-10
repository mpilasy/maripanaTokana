// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.domain.model

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToLong
import kotlin.math.sin

/**
 * The sun's declination, and the solstices and equinoxes, calculated on the device. The formulas are those of
 * Jean Meeus, "Astronomical Algorithms" (2nd edition): chapter 25 (low accuracy, about 0.01°) for the
 * declination, chapter 27 (about a minute, for the years 1000 to 3000) for the solstices and equinoxes.
 * The web app has the same code in `web/src/lib/domain/seasons.ts`; keep the two the same.
 */
object Seasons {
    private const val RAD = PI / 180
    private const val DAY_MS = 86_400_000.0
    private const val UNIX_EPOCH_JD = 2440587.5

    /** The largest declination of the sun, north or south: the tilt of the Earth's axis, in degrees. */
    const val MAX_DECLINATION = 23.44

    /** A solstice or equinox: [kind] 0 = March equinox, 1 = June solstice, 2 = September equinox, 3 = December solstice. */
    data class Event(val time: Long, val kind: Int)

    /** The solstice or equinox before a time and the one after it. */
    data class Around(val previous: Event, val next: Event)

    /** The latitude where the sun is straight overhead at [ms] (epoch millis), in degrees: north positive, south negative. */
    fun declination(ms: Long): Double {
        val t = (ms / DAY_MS + UNIX_EPOCH_JD - 2451545.0) / 36525
        val l0 = 280.46646 + t * (36000.76983 + t * 0.0003032)
        val m = RAD * (357.52911 + t * (35999.05029 - t * 0.0001537))
        val c = (1.914602 - t * (0.004817 + t * 0.000014)) * sin(m) + (0.019993 - t * 0.000101) * sin(2 * m) + 0.000289 * sin(3 * m)
        val omega = RAD * (125.04 - 1934.136 * t)
        // The apparent longitude of the sun, with the nutation and the aberration
        val lambda = RAD * (l0 + c - 0.00569 - 0.00478 * sin(omega))
        val epsilon = RAD * (23.439291 - t * 0.0130042 + 0.00256 * cos(omega))
        return asin(sin(epsilon) * sin(lambda)) / RAD
    }

    // The mean time of each event in Julian Ephemeris Days, as a polynomial in (year - 2000) / 1000
    private val MEAN = arrayOf(
        doubleArrayOf(2451623.80984, 365242.37404, 0.05169, -0.00411, -0.00057),
        doubleArrayOf(2451716.56767, 365241.62603, 0.00325, 0.00888, -0.00030),
        doubleArrayOf(2451810.21715, 365242.01767, -0.11575, 0.00337, 0.00078),
        doubleArrayOf(2451900.05952, 365242.74049, -0.06223, -0.00823, 0.00032),
    )

    // The periodic terms (A, B, C) of Meeus table 27.C
    private val TERMS = arrayOf(
        doubleArrayOf(485.0, 324.96, 1934.136), doubleArrayOf(203.0, 337.23, 32964.467), doubleArrayOf(199.0, 342.08, 20.186),
        doubleArrayOf(182.0, 27.85, 445267.112), doubleArrayOf(156.0, 73.14, 45036.886), doubleArrayOf(136.0, 171.52, 22518.443),
        doubleArrayOf(77.0, 222.54, 65928.934), doubleArrayOf(74.0, 296.72, 3034.906), doubleArrayOf(70.0, 243.58, 9037.513),
        doubleArrayOf(58.0, 119.81, 33718.147), doubleArrayOf(52.0, 297.17, 150.678), doubleArrayOf(50.0, 21.02, 2281.226),
        doubleArrayOf(45.0, 247.54, 29929.562), doubleArrayOf(44.0, 325.15, 31555.956), doubleArrayOf(29.0, 60.93, 4443.417),
        doubleArrayOf(18.0, 155.12, 67555.328), doubleArrayOf(17.0, 288.79, 4562.452), doubleArrayOf(16.0, 198.04, 62894.029),
        doubleArrayOf(14.0, 199.76, 31436.921), doubleArrayOf(12.0, 95.39, 14577.848), doubleArrayOf(12.0, 287.11, 31931.756),
        doubleArrayOf(12.0, 320.81, 34777.259), doubleArrayOf(9.0, 227.73, 1222.114), doubleArrayOf(8.0, 15.45, 16859.074),
    )

    /** The time (epoch millis) of event [kind] (see [Event]) in [year]. */
    fun eventTime(year: Int, kind: Int): Long {
        val y = (year - 2000) / 1000.0
        val c = MEAN[kind]
        val jde0 = c[0] + y * (c[1] + y * (c[2] + y * (c[3] + y * c[4])))
        val t = (jde0 - 2451545.0) / 36525
        val w = RAD * (35999.373 * t - 2.47)
        val dl = 1 + 0.0334 * cos(w) + 0.0007 * cos(2 * w)
        val s = TERMS.sumOf { (a, b, cc) -> a * cos(RAD * (b + cc * t)) }
        val jde = jde0 + 0.00001 * s / dl
        // Dynamical time is about 69 seconds ahead of universal time in the 2020s
        return ((jde - UNIX_EPOCH_JD) * DAY_MS).roundToLong() - 69_000
    }

    /** The solstice or equinox at or before [ms] and the first one after it. */
    fun around(ms: Long): Around {
        val year = 1970 + floor(ms / DAY_MS / 365.2425).toInt()
        val events = (year - 1..year + 1).flatMap { y -> (0..3).map { Event(eventTime(y, it), it) } }
        val next = events.indexOfFirst { it.time > ms }
        return Around(events[next - 1], events[next])
    }

    /**
     * How far the noon sun is from straight overhead at latitude [lat] on the day of [ms], in degrees: positive when it
     * is to the south, negative when it is to the north. Above 90°, the sun stays below the horizon all day.
     */
    fun noonOffset(lat: Double, ms: Long): Double = lat - declination(ms)

    /** The noon offsets (see [noonOffset]) at the solstices at latitude [lat]: the one nearer overhead first. */
    fun noonRange(lat: Double): Pair<Double, Double> {
        val a = lat - MAX_DECLINATION
        val b = lat + MAX_DECLINATION
        return if (abs(a) <= abs(b)) a to b else b to a
    }

    /**
     * The index of a solstice or equinox of [kind] in season_events (spring equinox, summer solstice, autumn equinox,
     * winter solstice) at latitude [lat]: south of the equator the seasons are the other way round.
     */
    fun seasonNameIndex(kind: Int, lat: Double): Int = if (lat < 0) (kind + 2) % 4 else kind

    /** The number of days at the place (UTC offset [utcOffsetSeconds]) from the day of [fromMs] to the day of [toMs]. */
    fun daysUntil(fromMs: Long, toMs: Long, utcOffsetSeconds: Int): Int {
        fun day(ms: Long) = Math.floorDiv(ms + utcOffsetSeconds * 1000L, DAY_MS.toLong())
        return (day(toMs) - day(fromMs)).toInt()
    }
}
