package orinasa.njarasoa.maripanatokana.domain.model

enum class WeatherSource { OPEN_METEO, PIRATE_WEATHER }
enum class GeocodingSource { SYSTEM_GEOCODER, NOMINATIM }

data class AppSettings(
    val expertMode: Boolean = false,
    val weatherSource: WeatherSource = WeatherSource.OPEN_METEO,
    val weatherApiKey: String = "",
    val geocodingSource: GeocodingSource = GeocodingSource.SYSTEM_GEOCODER,
    val alertsEnabled: Boolean = true,
    val alertsNwsEnabled: Boolean = true,
    val alertsGdacsEnabled: Boolean = true,
    val alertsDerivedEnabled: Boolean = true,
    val alertsMeteoAlarmEnabled: Boolean = true,
    val alertsJmaEnabled: Boolean = true,
    val alertsEcccEnabled: Boolean = true,
    val alertsWmoSwicEnabled: Boolean = true,
    val alertsBomEnabled: Boolean = true,
    val alertsNhcEnabled: Boolean = true,
)
