package orinasa.njarasoa.maripanatokana.ui.weather

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import orinasa.njarasoa.maripanatokana.R
import orinasa.njarasoa.maripanatokana.data.remote.GeocodingResult
import orinasa.njarasoa.maripanatokana.domain.model.WeatherData
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
    SupportedLocale("mg", "\uD83C\uDDF2\uD83C\uDDEC"),
    SupportedLocale("ar", "\uD83C\uDDF8\uD83C\uDDE6", '\u0660'),  // ٠
    SupportedLocale("en", "\uD83C\uDDEC\uD83C\uDDE7"),
    SupportedLocale("es", "\uD83C\uDDEA\uD83C\uDDF8"),
    SupportedLocale("fr", "\uD83C\uDDEB\uD83C\uDDF7"),
    SupportedLocale("hi", "\uD83C\uDDEE\uD83C\uDDF3", '\u0966'),  // ०
    SupportedLocale("ne", "\uD83C\uDDF3\uD83C\uDDF5", '\u0966'),  // ०
    SupportedLocale("zh", "\uD83C\uDDE8\uD83C\uDDF3"),
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

    private val _localeIndex = MutableStateFlow(prefs.getInt("locale_index", 0).coerceIn(0, supportedLocales.lastIndex))
    val localeIndex: StateFlow<Int> = _localeIndex.asStateFlow()

    // Dev Mode State
    private val _devModeActive = MutableStateFlow(checkDevModeExpiration())
    val devModeActive: StateFlow<Boolean> = _devModeActive.asStateFlow()

    private val _showGpsCoordinates = MutableStateFlow(false)
    val showGpsCoordinates: StateFlow<Boolean> = _showGpsCoordinates.asStateFlow()

    private val _showLocationOverrideDialog = MutableStateFlow(false)
    val showLocationOverrideDialog: StateFlow<Boolean> = _showLocationOverrideDialog.asStateFlow()

    private val _searchResults = MutableStateFlow<List<GeocodingResult>>(emptyList())
    val searchResults: StateFlow<List<GeocodingResult>> = _searchResults.asStateFlow()

    private var searchJob: Job? = null

    // Cached GPS weather data fetched in background during dev mode
    private var cachedGpsWeatherData: WeatherData? = null

    init {
        // Ensure dev mode is false if expired on startup
        if (!_devModeActive.value) {
            clearDevModeOverride()
        }
    }

    private fun checkDevModeExpiration(): Boolean {
        val expiration = prefs.getLong("dev_mode_expiration", 0L)
        return System.currentTimeMillis() < expiration
    }

    fun onLocationClicked() {
        // Single tap toggles GPS coordinates below the location label
        _showGpsCoordinates.value = !_showGpsCoordinates.value
    }

    private var devModeTapCount = 0
    private var devModeTapJob: Job? = null

    fun onWeatherIconTapped() {
        devModeTapCount++
        devModeTapJob?.cancel()
        if (devModeTapCount >= 7) {
            devModeTapCount = 0
            if (!_devModeActive.value) {
                val now = System.currentTimeMillis()
                val expiration = now + 4 * 60 * 60 * 1000L // 4 hours
                prefs.edit().putLong("dev_mode_expiration", expiration).apply()
                _devModeActive.value = true
                Toast.makeText(appContext, "Developer mode enabled", Toast.LENGTH_SHORT).show()
            }
        } else {
            devModeTapJob = viewModelScope.launch {
                delay(500)
                devModeTapCount = 0
            }
        }
    }

    fun onLocationDoubleClicked() {
        // No longer doing anything on double click
    }

    fun onEditLocationClicked() {
        _showLocationOverrideDialog.value = true
    }

    fun disableDevMode() {
        prefs.edit().remove("dev_mode_expiration").apply()
        _devModeActive.value = false
        clearDevModeOverride()
        _showLocationOverrideDialog.value = false
        // Restore cached GPS weather if available
        val cached = cachedGpsWeatherData
        if (cached != null) {
            _uiState.value = WeatherUiState.Success(cached)
            cachedGpsWeatherData = null
            // Also refresh in background to get truly fresh data
            refresh()
        } else {
            fetchWeather()
        }
    }

    fun searchLocation(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500) // Debounce

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
        prefs.edit()
            .putFloat("dev_override_lat", lat.toFloat())
            .putFloat("dev_override_lon", lon.toFloat())
            .putString("dev_override_name", name)
            .apply()
        _showLocationOverrideDialog.value = false
        fetchWeather()
    }

    fun clearLocationOverride() {
        clearDevModeOverride()
        _showLocationOverrideDialog.value = false
        fetchWeather()
    }

    private fun clearDevModeOverride() {
        prefs.edit()
            .remove("dev_override_lat")
            .remove("dev_override_lon")
            .remove("dev_override_name")
            .apply()
    }

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

    /** Spawn a background GPS weather fetch to cache for when dev mode is disabled */
    private fun spawnGpsCacheRefresh() {
        viewModelScope.launch {
            try {
                locationRepository.getFreshLocation().onSuccess { (lat, lon) ->
                    saveLocation(lat, lon)
                    weatherRepository.getWeather(lat, lon).onSuccess { data ->
                        cachedGpsWeatherData = data.copy(locationSubtext = null)
                        prefs.edit().putString("location_name", data.locationName).apply()
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
                    // Only update if we are still on a non-coordinate-named location (to avoid race)
                    // and it's likely the same location (checking name is a simple heuristic)
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

    fun fetchWeather() {
        viewModelScope.launch {
            // Only show loading if we don't already have data
            if (_uiState.value !is WeatherUiState.Success) {
                _uiState.value = WeatherUiState.Loading
            } else {
                _isRefreshing.value = true
            }

            // Check if dev mode expired
            if (_devModeActive.value && !checkDevModeExpiration()) {
                _devModeActive.value = false
                clearDevModeOverride()
            }

            if (_devModeActive.value && prefs.contains("dev_override_lat")) {
                val overrideLat = prefs.getFloat("dev_override_lat", 0f).toDouble()
                val overrideLon = prefs.getFloat("dev_override_lon", 0f).toDouble()
                val rawOverrideName = prefs.getString("dev_override_name", "Overridden Location") ?: "Overridden Location"
                // Optimize string allocations by using substringBefore instead of creating intermediate lists via split()
                val overrideName = rawOverrideName.substringBefore(",").substringBefore(";").substringBefore("-").trim()

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
        viewModelScope.launch {
            _isRefreshing.value = true

            if (_devModeActive.value && !checkDevModeExpiration()) {
                _devModeActive.value = false
                clearDevModeOverride()
            }

            if (_devModeActive.value && prefs.contains("dev_override_lat")) {
                val overrideLat = prefs.getFloat("dev_override_lat", 0f).toDouble()
                val overrideLon = prefs.getFloat("dev_override_lon", 0f).toDouble()
                val rawOverrideName = prefs.getString("dev_override_name", "Overridden Location") ?: "Overridden Location"
                // Optimize string allocations by using substringBefore instead of creating intermediate lists via split()
                val overrideName = rawOverrideName.substringBefore(",").substringBefore(";").substringBefore("-").trim()

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
