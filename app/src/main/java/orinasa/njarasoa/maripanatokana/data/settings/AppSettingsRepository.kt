package orinasa.njarasoa.maripanatokana.data.settings

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import orinasa.njarasoa.maripanatokana.domain.model.AppSettings
import orinasa.njarasoa.maripanatokana.domain.model.GeocodingSource
import orinasa.njarasoa.maripanatokana.domain.model.WeatherSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppSettingsRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()
    val current: AppSettings get() = _settings.value

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key?.startsWith("settings_") == true) _settings.value = load()
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    private fun load() = AppSettings(
        weatherSource = prefs.getString("settings_weather_source", null)
            ?.let { runCatching { WeatherSource.valueOf(it) }.getOrNull() }
            ?: WeatherSource.OPEN_METEO,
        weatherApiKey = prefs.getString("settings_weather_api_key", "") ?: "",
        geocodingSource = prefs.getString("settings_geocoding_source", null)
            ?.let { runCatching { GeocodingSource.valueOf(it) }.getOrNull() }
            ?: GeocodingSource.SYSTEM_GEOCODER,
        alertsEnabled = prefs.getBoolean("settings_alerts_enabled", true),
        alertsNwsEnabled = prefs.getBoolean("settings_alerts_nws", true),
        alertsGdacsEnabled = prefs.getBoolean("settings_alerts_gdacs", true),
        alertsDerivedEnabled = prefs.getBoolean("settings_alerts_derived", true),
    )

    fun updateWeatherSource(source: WeatherSource) =
        prefs.edit().putString("settings_weather_source", source.name).apply()

    fun updateWeatherApiKey(key: String) =
        prefs.edit().putString("settings_weather_api_key", key).apply()

    fun updateGeocodingSource(source: GeocodingSource) =
        prefs.edit().putString("settings_geocoding_source", source.name).apply()

    fun updateAlertsEnabled(enabled: Boolean) =
        prefs.edit().putBoolean("settings_alerts_enabled", enabled).apply()

    fun updateAlertsNwsEnabled(enabled: Boolean) =
        prefs.edit().putBoolean("settings_alerts_nws", enabled).apply()

    fun updateAlertsGdacsEnabled(enabled: Boolean) =
        prefs.edit().putBoolean("settings_alerts_gdacs", enabled).apply()

    fun updateAlertsDerivedEnabled(enabled: Boolean) =
        prefs.edit().putBoolean("settings_alerts_derived", enabled).apply()
}
