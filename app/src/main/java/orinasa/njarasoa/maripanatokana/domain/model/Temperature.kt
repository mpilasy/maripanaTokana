package orinasa.njarasoa.maripanatokana.domain.model

import java.util.Locale

/**
 * Value object encapsulating a temperature. Stores the canonical value in Celsius;
 * Fahrenheit is always derived so both units are available once a value is set.
 */
@JvmInline
value class Temperature private constructor(val celsius: Double) {

    val fahrenheit: Double
        get() = celsius * 9.0 / 5.0 + 32.0

    fun displayCelsius(): String = "%.0f°C".format(Locale.US, celsius)

    fun displayFahrenheit(): String = "%.0f°F".format(Locale.US, fahrenheit)

    /** Dual-unit display as required by spec: "2°C / 36°F" */
    fun displayDual(): String = "${displayCelsius()} / ${displayFahrenheit()}"

    /** Returns (primary, secondary) based on preferred unit system. */
    fun displayDual(metricPrimary: Boolean): Pair<String, String> =
        if (metricPrimary) displayCelsius() to displayFahrenheit()
        else displayFahrenheit() to displayCelsius()

    companion object {
        fun fromCelsius(c: Double): Temperature = Temperature(c)

        fun fromFahrenheit(f: Double): Temperature = Temperature((f - 32.0) * 5.0 / 9.0)
    }
}
