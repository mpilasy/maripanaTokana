package orinasa.njarasoa.maripanatokana.ui.settings

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import orinasa.njarasoa.maripanatokana.data.settings.AppSettingsRepository
import orinasa.njarasoa.maripanatokana.domain.model.AppSettings
import orinasa.njarasoa.maripanatokana.domain.model.GeocodingSource
import orinasa.njarasoa.maripanatokana.domain.model.WeatherSource
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: AppSettingsRepository,
) : ViewModel() {
    val settings: StateFlow<AppSettings> = repository.settings

    fun updateWeatherSource(source: WeatherSource) = repository.updateWeatherSource(source)
    fun updateWeatherApiKey(key: String) = repository.updateWeatherApiKey(key)
    fun updateGeocodingSource(source: GeocodingSource) = repository.updateGeocodingSource(source)
    fun updateAlertsEnabled(enabled: Boolean) = repository.updateAlertsEnabled(enabled)
    fun updateAlertsNwsEnabled(enabled: Boolean) = repository.updateAlertsNwsEnabled(enabled)
    fun updateAlertsGdacsEnabled(enabled: Boolean) = repository.updateAlertsGdacsEnabled(enabled)
    fun updateAlertsDerivedEnabled(enabled: Boolean) = repository.updateAlertsDerivedEnabled(enabled)
}
