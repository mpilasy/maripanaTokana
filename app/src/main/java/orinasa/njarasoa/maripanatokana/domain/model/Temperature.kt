package orinasa.njarasoa.maripanatokana.domain.model

import kotlin.math.roundToInt

/**
 * Value object encapsulating a temperature. Stores the canonical value in Celsius;
 * Fahrenheit is always derived so both units are available once a value is set.
 */
@JvmInline
value class Temperature private constructor(val celsius: Double) {

    val fahrenheit: Double
        get() = celsius * 9.0 / 5.0 + 32.0

    fun displayCelsius(): String = "${celsius.roundToInt()}°C"

    fun displayFahrenheit(): String = "${fahrenheit.roundToInt()}°F"

    /** Dual-unit display as required by spec: "2°C / 36°F" */
    fun displayDual(): String = "${displayCelsius()} / ${displayFahrenheit()}"

    companion object {
        fun fromCelsius(c: Double): Temperature = Temperature(c)

        fun fromFahrenheit(f: Double): Temperature = Temperature((f - 32.0) * 5.0 / 9.0)
    }
}
