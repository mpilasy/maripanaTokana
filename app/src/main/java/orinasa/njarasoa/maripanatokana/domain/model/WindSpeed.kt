package orinasa.njarasoa.maripanatokana.domain.model

import java.util.Locale

/**
 * Value object encapsulating wind speed. Stores the canonical value in m/s;
 * mph is always derived so both units are available once a value is set.
 */
@JvmInline
value class WindSpeed private constructor(val metersPerSecond: Double) {

    val mph: Double
        get() = metersPerSecond * 2.23694

    fun displayMetric(): String = "%.1f m/s".format(Locale.US, metersPerSecond)

    fun displayImperial(): String = "%.1f mph".format(Locale.US, mph)

    /** Dual-unit display: "5.2 m/s / 11.6 mph" */
    fun displayDual(): String = "${displayMetric()} / ${displayImperial()}"

    /** Returns (primary, secondary) based on preferred unit system. */
    fun displayDual(metricPrimary: Boolean): Pair<String, String> =
        if (metricPrimary) displayMetric() to displayImperial()
        else displayImperial() to displayMetric()

    companion object {
        fun fromMetersPerSecond(ms: Double): WindSpeed = WindSpeed(ms)

        fun fromMph(mph: Double): WindSpeed = WindSpeed(mph / 2.23694)
    }
}
