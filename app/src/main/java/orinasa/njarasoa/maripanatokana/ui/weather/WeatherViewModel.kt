package orinasa.njarasoa.maripanatokana.ui.weather

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import orinasa.njarasoa.maripanatokana.domain.repository.LocationRepository
import orinasa.njarasoa.maripanatokana.domain.repository.WeatherRepository
import javax.inject.Inject

@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val weatherRepository: WeatherRepository,
    private val locationRepository: LocationRepository,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val prefs = appContext.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.PermissionRequired)
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _metricPrimary = MutableStateFlow(prefs.getBoolean("metric_primary", true))
    val metricPrimary: StateFlow<Boolean> = _metricPrimary.asStateFlow()

    fun toggleUnits() {
        val newValue = !_metricPrimary.value
        _metricPrimary.value = newValue
        prefs.edit().putBoolean("metric_primary", newValue).apply()
    }

    fun fetchWeather() {
        viewModelScope.launch {
            _uiState.value = WeatherUiState.Loading
            doFetch()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            doFetch()
            _isRefreshing.value = false
        }
    }

    private suspend fun doFetch() {
        locationRepository.getLocation()
            .onSuccess { (lat, lon) ->
                saveLocation(lat, lon)
                weatherRepository.getWeather(lat, lon)
                    .onSuccess { data ->
                        prefs.edit().putString("location_name", data.locationName).apply()
                        _uiState.value = WeatherUiState.Success(data)
                    }
                    .onFailure { error ->
                        _uiState.value = WeatherUiState.Error(
                            error.message ?: "Failed to fetch weather"
                        )
                    }
            }
            .onFailure { error ->
                _uiState.value = WeatherUiState.Error(
                    error.message ?: "Failed to get location"
                )
            }
    }

    private fun saveLocation(lat: Double, lon: Double) {
        prefs.edit()
            .putFloat("lat", lat.toFloat())
            .putFloat("lon", lon.toFloat())
            .apply()
    }
}
