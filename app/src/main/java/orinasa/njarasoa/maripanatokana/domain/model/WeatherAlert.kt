package orinasa.njarasoa.maripanatokana.domain.model

enum class AlertLevel {
    WATCH, WARNING, EMERGENCY
}

data class WeatherAlert(
    val level: AlertLevel,
    val titleKey: String,
    val descKey: String,
    val source: String = "derived" // "official", "derived", "gdacs"
)
