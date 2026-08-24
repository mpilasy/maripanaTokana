package orinasa.njarasoa.maripanatokana.data.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import orinasa.njarasoa.maripanatokana.DefaultSettings
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
        advancedMode = prefs.getBoolean("settings_advanced_mode", false),
        weatherSource = prefs.getString("settings_weather_source", null)
            ?.let { runCatching { WeatherSource.valueOf(it) }.getOrNull() }
            ?: WeatherSource.OPEN_METEO,
        weatherApiKey = prefs.getString("settings_weather_api_key", "") ?: "",
        geocodingSource = prefs.getString("settings_geocoding_source", null)
            ?.let { runCatching { GeocodingSource.valueOf(it) }.getOrNull() }
            ?: DefaultSettings.geocodingSource,
        alertsEnabled = prefs.getBoolean("settings_alerts_enabled", true),
        alertsNwsEnabled = prefs.getBoolean("settings_alerts_nws", true),
        alertsGdacsEnabled = prefs.getBoolean("settings_alerts_gdacs", true),
        alertsMeteoAlarmEnabled = prefs.getBoolean("settings_alerts_meteoalarm", true),
        alertsJmaEnabled = prefs.getBoolean("settings_alerts_jma", true),
        alertsEcccEnabled = prefs.getBoolean("settings_alerts_eccc", true),
        alertsWmoSwicEnabled = prefs.getBoolean("settings_alerts_wmoswic", true),
        alertsBomEnabled = prefs.getBoolean("settings_alerts_bom", true),
        alertsNhcEnabled = prefs.getBoolean("settings_alerts_nhc", true),
    )

    fun updateWeatherSource(source: WeatherSource) {
        prefs.edit { putString("settings_weather_source", source.name) }
    }

    fun updateWeatherApiKey(key: String) {
        prefs.edit { putString("settings_weather_api_key", key) }
    }

    fun updateGeocodingSource(source: GeocodingSource) {
        prefs.edit { putString("settings_geocoding_source", source.name) }
    }

    fun updateAlertsEnabled(enabled: Boolean) {
        prefs.edit { putBoolean("settings_alerts_enabled", enabled) }
    }

    fun updateAlertsNwsEnabled(enabled: Boolean) {
        prefs.edit { putBoolean("settings_alerts_nws", enabled) }
    }

    fun updateAlertsGdacsEnabled(enabled: Boolean) {
        prefs.edit { putBoolean("settings_alerts_gdacs", enabled) }
    }

    fun updateAlertsMeteoAlarmEnabled(enabled: Boolean) {
        prefs.edit { putBoolean("settings_alerts_meteoalarm", enabled) }
    }

    fun updateAlertsJmaEnabled(enabled: Boolean) {
        prefs.edit { putBoolean("settings_alerts_jma", enabled) }
    }

    fun updateAlertsEcccEnabled(enabled: Boolean) {
        prefs.edit { putBoolean("settings_alerts_eccc", enabled) }
    }

    fun updateAlertsWmoSwicEnabled(enabled: Boolean) {
        prefs.edit { putBoolean("settings_alerts_wmoswic", enabled) }
    }

    fun updateAlertsBomEnabled(enabled: Boolean) {
        prefs.edit { putBoolean("settings_alerts_bom", enabled) }
    }

    fun updateAlertsNhcEnabled(enabled: Boolean) {
        prefs.edit { putBoolean("settings_alerts_nhc", enabled) }
    }

    fun updateAdvancedMode(enabled: Boolean) {
        prefs.edit { putBoolean("settings_advanced_mode", enabled) }
    }
}
