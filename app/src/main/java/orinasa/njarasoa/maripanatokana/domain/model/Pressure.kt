package orinasa.njarasoa.maripanatokana.domain.model

import kotlin.math.roundToInt

/**
 * Value object encapsulating atmospheric pressure. Stores the canonical value in hPa;
 * inHg is always derived so both units are available once a value is set.
 */
@JvmInline
value class Pressure private constructor(val hPa: Double) {

    val inHg: Double
        get() = hPa * 0.02953

    fun displayHPa(): String = "${hPa.roundToInt()} hPa"

    fun displayInHg(): String = "%.2f inHg".format(inHg)

    /** Dual-unit display: "1013 hPa / 29.92 inHg" */
    fun displayDual(): String = "${displayHPa()} / ${displayInHg()}"

    companion object {
        fun fromHPa(hPa: Double): Pressure = Pressure(hPa)

        fun fromInHg(inHg: Double): Pressure = Pressure(inHg / 0.02953)
    }
}
