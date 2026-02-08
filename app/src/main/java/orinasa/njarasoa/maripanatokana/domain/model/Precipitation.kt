package orinasa.njarasoa.maripanatokana.domain.model

import java.util.Locale

/**
 * Value object encapsulating precipitation (rain/snow). Stores the canonical value in mm;
 * inches is always derived so both units are available once a value is set.
 */
@JvmInline
value class Precipitation private constructor(val mm: Double) {

    val inches: Double
        get() = mm * 0.03937

    fun displayMetric(): String = "%.1f mm".format(Locale.US, mm)

    fun displayImperial(): String = "%.2f in".format(Locale.US, inches)

    /** Dual-unit display: "2.5 mm / 0.10 in" */
    fun displayDual(): String = "${displayMetric()} / ${displayImperial()}"

    /** Returns (primary, secondary) based on preferred unit system. */
    fun displayDual(metricPrimary: Boolean): Pair<String, String> =
        if (metricPrimary) displayMetric() to displayImperial()
        else displayImperial() to displayMetric()

    companion object {
        fun fromMm(mm: Double): Precipitation = Precipitation(mm)

        fun fromInches(inches: Double): Precipitation = Precipitation(inches / 0.03937)
    }
}
