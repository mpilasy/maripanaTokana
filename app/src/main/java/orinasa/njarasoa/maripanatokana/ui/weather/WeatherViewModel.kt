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
import orinasa.njarasoa.maripanatokana.R
import orinasa.njarasoa.maripanatokana.domain.repository.LocationRepository
import orinasa.njarasoa.maripanatokana.domain.repository.WeatherRepository
import orinasa.njarasoa.maripanatokana.ui.theme.fontPairings
import javax.inject.Inject

data class SupportedLocale(val tag: String, val flag: String)

val supportedLocales = listOf(
    SupportedLocale("en", "\uD83C\uDDEC\uD83C\uDDE7"),
    SupportedLocale("zh", "\uD83C\uDDE8\uD83C\uDDF3"),
    SupportedLocale("hi", "\uD83C\uDDEE\uD83C\uDDF3"),
    SupportedLocale("es", "\uD83C\uDDEA\uD83C\uDDF8"),
    SupportedLocale("fr", "\uD83C\uDDEB\uD83C\uDDF7"),
    SupportedLocale("ar", "\uD83C\uDDF8\uD83C\uDDE6"),
    SupportedLocale("mg", "\uD83C\uDDF2\uD83C\uDDEC"),
    SupportedLocale("ne", "\uD83C\uDDF3\uD83C\uDDF5"),
)

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

    private val _fontIndex = MutableStateFlow(prefs.getInt("font_index", 0).coerceIn(0, fontPairings.lastIndex))
    val fontIndex: StateFlow<Int> = _fontIndex.asStateFlow()

    private val _localeIndex = MutableStateFlow(prefs.getInt("locale_index", 6).coerceIn(0, supportedLocales.lastIndex))
    val localeIndex: StateFlow<Int> = _localeIndex.asStateFlow()

    fun toggleUnits() {
        val newValue = !_metricPrimary.value
        _metricPrimary.value = newValue
        prefs.edit().putBoolean("metric_primary", newValue).apply()
    }

    fun cycleFont() {
        val newIndex = (_fontIndex.value + 1) % fontPairings.size
        _fontIndex.value = newIndex
        prefs.edit().putInt("font_index", newIndex).apply()
    }

    fun cycleLanguage() {
        val newIndex = (_localeIndex.value + 1) % supportedLocales.size
        _localeIndex.value = newIndex
        prefs.edit().putInt("locale_index", newIndex).apply()
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
        // Step 1: try cached location for instant render
        var usedCached = false
        locationRepository.getLastLocation().onSuccess { (lat, lon) ->
            usedCached = true
            saveLocation(lat, lon)
            weatherRepository.getWeather(lat, lon).onSuccess { data ->
                prefs.edit().putString("location_name", data.locationName).apply()
                _uiState.value = WeatherUiState.Success(data)
            }
        }

        // Step 2: get fresh location, re-fetch if moved significantly
        locationRepository.getFreshLocation()
            .onSuccess { (lat, lon) ->
                saveLocation(lat, lon)
                if (!usedCached || movedSignificantly(lat, lon)) {
                    weatherRepository.getWeather(lat, lon)
                        .onSuccess { data ->
                            prefs.edit().putString("location_name", data.locationName).apply()
                            _uiState.value = WeatherUiState.Success(data)
                        }
                        .onFailure {
                            if (!usedCached) {
                                _uiState.value = WeatherUiState.Error(R.string.error_fetch_weather)
                            }
                        }
                }
            }
            .onFailure {
                if (!usedCached) {
                    _uiState.value = WeatherUiState.Error(R.string.error_get_location)
                }
            }
    }

    private fun movedSignificantly(lat: Double, lon: Double): Boolean {
        val oldLat = prefs.getFloat("last_render_lat", Float.MIN_VALUE)
        val oldLon = prefs.getFloat("last_render_lon", Float.MIN_VALUE)
        if (oldLat == Float.MIN_VALUE) return true
        val dlat = lat - oldLat
        val dlon = lon - oldLon
        // ~5 km threshold (0.045 degrees latitude ≈ 5 km)
        return dlat * dlat + dlon * dlon > 0.045 * 0.045
    }

    private fun saveLocation(lat: Double, lon: Double) {
        prefs.edit()
            .putFloat("lat", lat.toFloat())
            .putFloat("lon", lon.toFloat())
            .putFloat("last_render_lat", lat.toFloat())
            .putFloat("last_render_lon", lon.toFloat())
            .apply()
    }
}
