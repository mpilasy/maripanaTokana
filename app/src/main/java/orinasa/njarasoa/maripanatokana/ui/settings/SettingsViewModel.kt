package orinasa.njarasoa.maripanatokana.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import orinasa.njarasoa.maripanatokana.data.remote.PirateWeatherApiService
import orinasa.njarasoa.maripanatokana.data.settings.AppSettingsRepository
import orinasa.njarasoa.maripanatokana.domain.model.AppSettings
import orinasa.njarasoa.maripanatokana.domain.model.GeocodingSource
import orinasa.njarasoa.maripanatokana.domain.model.WeatherSource
import retrofit2.HttpException
import javax.inject.Inject

sealed class ApiKeyTestState {
    object Idle : ApiKeyTestState()
    object Loading : ApiKeyTestState()
    object Success : ApiKeyTestState()
    data class Failure(val message: String) : ApiKeyTestState()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: AppSettingsRepository,
    private val pirateWeatherService: PirateWeatherApiService,
) : ViewModel() {
    val settings: StateFlow<AppSettings> = repository.settings

    private val _pendingApiKey = MutableStateFlow(repository.current.weatherApiKey)
    val pendingApiKey: StateFlow<String> = _pendingApiKey.asStateFlow()

    private val _testState = MutableStateFlow<ApiKeyTestState>(ApiKeyTestState.Idle)
    val testState: StateFlow<ApiKeyTestState> = _testState.asStateFlow()

    fun updateWeatherSource(source: WeatherSource) {
        repository.updateWeatherSource(source)
        _pendingApiKey.value = ""
        _testState.value = ApiKeyTestState.Idle
    }

    fun updatePendingApiKey(key: String) {
        _pendingApiKey.value = key
        _testState.value = ApiKeyTestState.Idle
    }

    fun testApiKey() {
        val key = _pendingApiKey.value.trim()
        val source = settings.value.weatherSource
        viewModelScope.launch {
            _testState.value = ApiKeyTestState.Loading
            try {
                when (source) {
                    WeatherSource.PIRATE_WEATHER -> pirateWeatherService.getForecast(key, 0.0, 0.0)
                    WeatherSource.OPEN_METEO -> return@launch
                }
                repository.updateWeatherApiKey(key)
                _testState.value = ApiKeyTestState.Success
            } catch (e: HttpException) {
                _testState.value = ApiKeyTestState.Failure(
                    when (e.code()) {
                        401 -> "Invalid API key"
                        403 -> "API key forbidden"
                        429 -> "Rate limit exceeded — key may still be valid"
                        else -> "API error (HTTP ${e.code()})"
                    }
                )
            } catch (_: Exception) {
                _testState.value = ApiKeyTestState.Failure("Network error — check your connection")
            }
        }
    }

    fun updateExpertMode(enabled: Boolean) = repository.updateExpertMode(enabled)
    fun updateGeocodingSource(source: GeocodingSource) = repository.updateGeocodingSource(source)
    fun updateAlertsEnabled(enabled: Boolean) = repository.updateAlertsEnabled(enabled)
    fun updateAlertsNwsEnabled(enabled: Boolean) = repository.updateAlertsNwsEnabled(enabled)
    fun updateAlertsGdacsEnabled(enabled: Boolean) = repository.updateAlertsGdacsEnabled(enabled)
    fun updateAlertsMeteoAlarmEnabled(enabled: Boolean) = repository.updateAlertsMeteoAlarmEnabled(enabled)
    fun updateAlertsJmaEnabled(enabled: Boolean) = repository.updateAlertsJmaEnabled(enabled)
    fun updateAlertsEcccEnabled(enabled: Boolean) = repository.updateAlertsEcccEnabled(enabled)
    fun updateAlertsWmoSwicEnabled(enabled: Boolean) = repository.updateAlertsWmoSwicEnabled(enabled)
    fun updateAlertsBomEnabled(enabled: Boolean) = repository.updateAlertsBomEnabled(enabled)
    fun updateAlertsNhcEnabled(enabled: Boolean) = repository.updateAlertsNhcEnabled(enabled)
}
