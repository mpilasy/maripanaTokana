package orinasa.njarasoa.maripanatokana.domain.model

import java.util.Locale
import kotlin.math.roundToInt

/**
 * Value object encapsulating a temperature. Stores the canonical value in Celsius;
 * Fahrenheit is always derived so both units are available once a value is set.
 */
@JvmInline
value class Temperature private constructor(val celsius: Double) {

    val fahrenheit: Double
        get() = celsius * 9.0 / 5.0 + 32.0

    fun displayCelsius(decimals: Int = 0): String =
        if (decimals > 0) "%.${decimals}f°C".format(Locale.US, celsius)
        else "%d°C".format(Locale.US, celsius.roundToInt())

    fun displayFahrenheit(decimals: Int = 0): String =
        if (decimals > 0) "%.${decimals}f°F".format(Locale.US, fahrenheit)
        else "%d°F".format(Locale.US, fahrenheit.roundToInt())

    /** Dual-unit display as required by spec: "2°C / 36°F" */
    fun displayDual(): String = "${displayCelsius()} / ${displayFahrenheit()}"

    /** Returns (primary, secondary) based on preferred unit system. */
    fun displayDual(metricPrimary: Boolean, decimals: Int = 0): Pair<String, String> =
        if (metricPrimary) displayCelsius(decimals) to displayFahrenheit(decimals)
        else displayFahrenheit(decimals) to displayCelsius(decimals)

    companion object {
        fun fromCelsius(c: Double): Temperature = Temperature(c)

        fun fromFahrenheit(f: Double): Temperature = Temperature((f - 32.0) * 5.0 / 9.0)
    }
}
