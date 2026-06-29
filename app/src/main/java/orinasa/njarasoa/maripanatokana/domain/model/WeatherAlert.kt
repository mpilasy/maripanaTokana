package orinasa.njarasoa.maripanatokana.domain.model

enum class AlertLevel {
    WATCH, WARNING, EMERGENCY
}

data class WeatherAlert(
    val level: AlertLevel,
    val titleKey: String,
    val descKey: String,
    val source: String = "derived", // "nws", "derived", "gdacs", "bom", etc.
    val time: Long? = null,
    val headline: String? = null,
    val link: String? = null
)
