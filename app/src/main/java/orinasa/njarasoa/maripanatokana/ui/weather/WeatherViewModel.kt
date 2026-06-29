package orinasa.njarasoa.maripanatokana.ui.weather

import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.launch
import orinasa.njarasoa.maripanatokana.R
import orinasa.njarasoa.maripanatokana.data.remote.GeocodingResult
import orinasa.njarasoa.maripanatokana.domain.model.WeatherData
import orinasa.njarasoa.maripanatokana.data.settings.AppSettingsRepository
import orinasa.njarasoa.maripanatokana.domain.model.WeatherSource
import orinasa.njarasoa.maripanatokana.domain.repository.LocationRepository
import orinasa.njarasoa.maripanatokana.domain.repository.WeatherRepository
import orinasa.njarasoa.maripanatokana.ui.theme.fontPairings
import javax.inject.Inject

data class SupportedLocale(val tag: String, val flag: String, val nativeZero: Char? = null) {
    // Java lacks CLDR data for "mg" — fall back to French (same convention)
    private val decimalSep: Char = java.text.DecimalFormatSymbols(
        java.util.Locale.forLanguageTag(if (tag == "mg") "fr" else tag)
    ).decimalSeparator

    /** Replace ASCII digits 0-9 with native script digits and '.' with locale decimal separator. */
    fun localizeDigits(s: String): String {
        if (nativeZero == null && decimalSep == '.') return s
        val z = nativeZero
        return buildString(s.length) {
            for (c in s) {
                append(when {
                    z != null && c in '0'..'9' -> z + (c - '0')
                    c == '.' && decimalSep != '.' -> decimalSep
                    else -> c
                })
            }
        }
    }
}

val supportedLocales = listOf(
    SupportedLocale("mg", "🇲🇬"),
    SupportedLocale("ar", "🇸🇦", '٠'),  // ٠
    SupportedLocale("en", "🇬🇧"),
    SupportedLocale("es", "🇪🇸"),
    SupportedLocale("fr", "🇫🇷"),
    SupportedLocale("hi", "🇮🇳", '०'),  // ०
    SupportedLocale("ne", "🇳🇵", '०'),  // ०
    SupportedLocale("zh", "🇨🇳"),
)

@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val weatherRepository: WeatherRepository,
    private val locationRepository: LocationRepository,
    private val settingsRepository: AppSettingsRepository,
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

    private val _localeIndex = MutableStateFlow(prefs.getInt("locale_index", 0).coerceIn(0, supportedLocales.lastIndex))
    val localeIndex: StateFlow<Int> = _localeIndex.asStateFlow()

    val weatherSource: StateFlow<WeatherSource> = settingsRepository.settings
        .map { it.weatherSource }
        .stateIn(viewModelScope, SharingStarted.Eagerly, settingsRepository.current.weatherSource)

    // Expert Mode State (derived from AppSettings)
    val expertModeActive: StateFlow<Boolean> = settingsRepository.settings
        .map { it.expertMode }
        .stateIn(viewModelScope, SharingStarted.Eagerly, settingsRepository.current.expertMode)

    private val _devOverrideLat = MutableStateFlow<Double?>(
        prefs.getFloat("dev_override_lat", Float.NaN).takeUnless { it.isNaN() }?.toDouble()
    )
    val devOverrideLat: StateFlow<Double?> = _devOverrideLat.asStateFlow()

    private val _devOverrideLon = MutableStateFlow<Double?>(
        prefs.getFloat("dev_override_lon", Float.NaN).takeUnless { it.isNaN() }?.toDouble()
    )
    val devOverrideLon: StateFlow<Double?> = _devOverrideLon.asStateFlow()

    private val _showGpsCoordinates = MutableStateFlow(false)
    val showGpsCoordinates: StateFlow<Boolean> = _showGpsCoordinates.asStateFlow()

    private val _showLocationOverrideDialog = MutableStateFlow(false)
    val showLocationOverrideDialog: StateFlow<Boolean> = _showLocationOverrideDialog.asStateFlow()

    private val _searchResults = MutableStateFlow<List<GeocodingResult>>(emptyList())
    val searchResults: StateFlow<List<GeocodingResult>> = _searchResults.asStateFlow()

    private var fetchJob: Job? = null
    private var searchJob: Job? = null

    // Cached GPS weather data fetched in background during expert mode with override
    private var cachedGpsWeatherData: WeatherData? = null

    init {
        checkOverrideExpiry()
        // When expert mode is turned off, clear location override and refresh
        viewModelScope.launch {
            var prev = expertModeActive.value
            expertModeActive.collect { active ->
                if (!active && prev) {
                    clearDevModeOverride()
                    fetchWeather()
                }
                prev = active
            }
        }
    }

    private fun checkOverrideExpiry() {
        val setTime = prefs.getLong("expert_override_set_time", 0L)
        if (setTime != 0L && System.currentTimeMillis() - setTime >= 12 * 60 * 60 * 1000L) {
            clearDevModeOverride()
        }
    }

    fun onLocationClicked() {
        _showGpsCoordinates.value = !_showGpsCoordinates.value
    }

    fun onLocationDoubleClicked() {
        // No longer doing anything on double click
    }

    fun onEditLocationClicked() {
        _showLocationOverrideDialog.value = true
    }

    fun searchLocation(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            kotlinx.coroutines.delay(500) // Debounce

            // Check for direct coordinates
            val coordsPattern = Regex("^(-?\\d+\\.\\d+)\\s*,\\s*(-?\\d+\\.\\d+)$")
            val match = coordsPattern.find(query.trim())
            if (match != null) {
                val (latStr, lonStr) = match.destructured
                val lat = latStr.toDoubleOrNull()
                val lon = lonStr.toDoubleOrNull()
                if (lat != null && lon != null) {
                     _searchResults.value = listOf(
                         GeocodingResult(
                             id = 0,
                             name = "$lat, $lon",
                             latitude = lat,
                             longitude = lon,
                             country = "Coordinates"
                         )
                     )
                     return@launch
                }
            }

            weatherRepository.searchLocation(query).onSuccess { results ->
                _searchResults.value = results
            }
        }
    }

    fun setShowLocationOverrideDialog(show: Boolean) {
        _showLocationOverrideDialog.value = show
    }

    fun setLocationOverride(lat: Double, lon: Double, name: String) {
        prefs.edit {
            putFloat("dev_override_lat", lat.toFloat())
            putFloat("dev_override_lon", lon.toFloat())
            putString("dev_override_name", name)
            putLong("expert_override_set_time", System.currentTimeMillis())
        }
        _devOverrideLat.value = lat
        _devOverrideLon.value = lon
        _showLocationOverrideDialog.value = false
        fetchWeather()
    }

    fun clearLocationOverride() {
        clearDevModeOverride()
        _showLocationOverrideDialog.value = false
        fetchWeather()
    }

    private fun clearDevModeOverride() {
        prefs.edit {
            remove("dev_override_lat")
            remove("dev_override_lon")
            remove("dev_override_name")
            remove("expert_override_set_time")
        }
        _devOverrideLat.value = null
        _devOverrideLon.value = null
    }

    fun toggleUnits() {
        val newValue = !_metricPrimary.value
        _metricPrimary.value = newValue
        prefs.edit { putBoolean("metric_primary", newValue) }
    }

    fun cycleFont() {
        val newIndex = (_fontIndex.value + 1) % fontPairings.size
        _fontIndex.value = newIndex
        prefs.edit { putInt("font_index", newIndex) }
    }

    fun cycleLanguage() {
        val newIndex = (_localeIndex.value + 1) % supportedLocales.size
        _localeIndex.value = newIndex
        prefs.edit { putInt("locale_index", newIndex) }
    }

    /** Spawn a background GPS weather fetch to cache for when override is cleared */
    private fun spawnGpsCacheRefresh() {
        viewModelScope.launch {
            try {
                locationRepository.getFreshLocation().onSuccess { (lat, lon) ->
                    saveLocation(lat, lon)
                    weatherRepository.getWeather(lat, lon).onSuccess { data ->
                        cachedGpsWeatherData = data.copy(locationSubtext = null)
                        prefs.edit { putString("location_name", data.locationName) }
                    }
                }
            } catch (_: Exception) {
                // Silently fail - this is a best-effort background refresh
            }
        }
    }

    private fun fetchAlertsForData(lat: Double, lon: Double) {
        viewModelScope.launch {
            weatherRepository.fetchAlerts(lat, lon).onSuccess { alerts ->
                val current = _uiState.value
                if (current is WeatherUiState.Success && !current.data.locationName.contains(",")) {
                    _uiState.value = WeatherUiState.Success(current.data.copy(alerts = alerts, alertsLoading = false))
                } else if (current is WeatherUiState.Success) {
                    _uiState.value = WeatherUiState.Success(current.data.copy(alerts = alerts, alertsLoading = false))
                }
            }.onFailure {
                val current = _uiState.value
                if (current is WeatherUiState.Success) {
                    _uiState.value = WeatherUiState.Success(current.data.copy(alertsLoading = false))
                }
            }
        }
    }

    fun onPermissionRevoked() {
        if (_uiState.value is WeatherUiState.Success) {
            _uiState.value = WeatherUiState.PermissionRequired
        }
    }

    fun fetchWeather() {
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            // Only show loading if we don't already have data
            if (_uiState.value !is WeatherUiState.Success) {
                _uiState.value = WeatherUiState.Loading
            } else {
                _isRefreshing.value = true
            }

            // Check 12-hour non-local override expiry
            checkOverrideExpiry()

            if (expertModeActive.value && prefs.contains("dev_override_lat")) {
                val overrideLat = prefs.getFloat("dev_override_lat", 0f).toDouble()
                val overrideLon = prefs.getFloat("dev_override_lon", 0f).toDouble()
                val rawOverrideName = prefs.getString("dev_override_name", "Overridden Location") ?: "Overridden Location"
                val overrideName = rawOverrideName.split(",")[0].split(";")[0].split("-")[0].trim()

                weatherRepository.getWeather(overrideLat, overrideLon).onSuccess { data ->
                    val overrideData = data.copy(locationName = overrideName)
                    _uiState.value = WeatherUiState.Success(overrideData)
                    fetchAlertsForData(overrideLat, overrideLon)
                    // Spawn background GPS cache refresh
                    spawnGpsCacheRefresh()
                }.onFailure {
                    if (_uiState.value !is WeatherUiState.Success) {
                        _uiState.value = WeatherUiState.Error(R.string.error_fetch_weather)
                    }
                }
            } else {
                doFetch()
            }
            _isRefreshing.value = false
        }
    }

    fun refresh() {
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            _isRefreshing.value = true

            // Check 12-hour non-local override expiry
            checkOverrideExpiry()

            if (expertModeActive.value && prefs.contains("dev_override_lat")) {
                val overrideLat = prefs.getFloat("dev_override_lat", 0f).toDouble()
                val overrideLon = prefs.getFloat("dev_override_lon", 0f).toDouble()
                val rawOverrideName = prefs.getString("dev_override_name", "Overridden Location") ?: "Overridden Location"
                val overrideName = rawOverrideName.split(",")[0].split(";")[0].split("-")[0].trim()

                weatherRepository.getWeather(overrideLat, overrideLon).onSuccess { data ->
                    val overrideData = data.copy(locationName = overrideName)
                    _uiState.value = WeatherUiState.Success(overrideData)
                    fetchAlertsForData(overrideLat, overrideLon)
                    // Spawn background GPS cache refresh
                    spawnGpsCacheRefresh()
                }.onFailure {
                    if (_uiState.value !is WeatherUiState.Success) {
                        _uiState.value = WeatherUiState.Error(R.string.error_fetch_weather)
                    }
                }
            } else {
                doFetch()
            }
            _isRefreshing.value = false
        }
    }

    fun refreshIfStale() {
        val current = _uiState.value
        if (current is WeatherUiState.Success) {
            val age = System.currentTimeMillis() - current.data.timestamp
            if (age > 30 * 60 * 1000L) refresh()
        }
    }

    private suspend fun doFetch() {
        val completed = withTimeoutOrNull(45_000L) {
            kotlinx.coroutines.coroutineScope {
                // Step 1: try cached location for instant render
                var usedCached = false
                var freshWeatherDisplayed = false

                launch {
                    locationRepository.getLastLocation().onSuccess { (lat, lon) ->
                        usedCached = true
                        saveLocation(lat, lon)
                        weatherRepository.getWeather(lat, lon).onSuccess { data ->
                            if (!freshWeatherDisplayed) {
                                val displayData = data.copy(locationSubtext = null)
                                prefs.edit().putString("location_name", displayData.locationName).apply()
                                _uiState.value = WeatherUiState.Success(displayData)
                                fetchAlertsForData(lat, lon)
                            }
                        }
                    }
                }

                // Step 2: get fresh location, re-fetch if moved significantly
                launch {
                    locationRepository.getFreshLocation()
                        .onSuccess { (lat, lon) ->
                            saveLocation(lat, lon)
                            if (!usedCached || movedSignificantly(lat, lon)) {
                                weatherRepository.getWeather(lat, lon)
                                    .onSuccess { data ->
                                        freshWeatherDisplayed = true
                                        val displayData = data.copy(locationSubtext = null)
                                        prefs.edit().putString("location_name", displayData.locationName).apply()
                                        _uiState.value = WeatherUiState.Success(displayData)
                                        fetchAlertsForData(lat, lon)
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
            }
        }
        // Safety net: if the entire fetch timed out, guarantee we exit Loading.
        if (completed == null && _uiState.value is WeatherUiState.Loading) {
            _uiState.value = WeatherUiState.Error(R.string.error_get_location)
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
