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
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import orinasa.njarasoa.maripanatokana.R
import orinasa.njarasoa.maripanatokana.data.remote.GeocodingResult
import orinasa.njarasoa.maripanatokana.data.repository.WeatherCache
import orinasa.njarasoa.maripanatokana.data.source.toFetchError
import orinasa.njarasoa.maripanatokana.domain.model.FetchError
import orinasa.njarasoa.maripanatokana.domain.model.SavedLocation
import orinasa.njarasoa.maripanatokana.domain.model.WeatherData
import orinasa.njarasoa.maripanatokana.data.settings.AppSettingsRepository
import orinasa.njarasoa.maripanatokana.domain.model.WeatherSource
import orinasa.njarasoa.maripanatokana.domain.repository.LocationRepository
import orinasa.njarasoa.maripanatokana.domain.repository.WeatherRepository
import orinasa.njarasoa.maripanatokana.ui.theme.fontPairings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import orinasa.njarasoa.maripanatokana.data.location.PlusCode
import orinasa.njarasoa.maripanatokana.data.location.SharedLocationParser
import orinasa.njarasoa.maripanatokana.data.location.isLatinScript
import orinasa.njarasoa.maripanatokana.data.location.parseCoordinates
import orinasa.njarasoa.maripanatokana.data.location.shortPlaceName
import java.util.Locale
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
    private val weatherCache: WeatherCache,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val prefs = appContext.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
    private val privatePrefs = appContext.getSharedPreferences("private_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.PermissionRequired)
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _refreshFailed = MutableStateFlow(false)
    /** True when a refresh failed while older data is still on screen; reset on the next success. */
    val refreshFailed: StateFlow<Boolean> = _refreshFailed.asStateFlow()

    private val _refreshError = MutableStateFlow<FetchError?>(null)
    /** Classified reason for [refreshFailed]; null when unknown or Generic. */
    val refreshError: StateFlow<FetchError?> = _refreshError.asStateFlow()

    private fun flagRefreshFailed(e: Throwable?) {
        _refreshError.value = e?.toFetchError(weatherSource.value == WeatherSource.PIRATE_WEATHER)
            ?.takeIf { it != FetchError.Generic }
        _refreshFailed.value = true
    }

    private val _metricPrimary = MutableStateFlow(prefs.getBoolean("metric_primary", true))
    val metricPrimary: StateFlow<Boolean> = _metricPrimary.asStateFlow()

    private val _fontIndex = MutableStateFlow(prefs.getInt("font_index", 0).coerceIn(0, fontPairings.lastIndex))
    val fontIndex: StateFlow<Int> = _fontIndex.asStateFlow()

    private val _localeIndex = MutableStateFlow(prefs.getInt("locale_index", 0).coerceIn(0, supportedLocales.lastIndex))
    val localeIndex: StateFlow<Int> = _localeIndex.asStateFlow()

    val weatherSource: StateFlow<WeatherSource> = settingsRepository.settings
        .map { it.weatherSource }
        .stateIn(viewModelScope, SharingStarted.Eagerly, settingsRepository.current.weatherSource)

    // Advanced Mode State (derived from AppSettings)
    val advancedModeActive: StateFlow<Boolean> = settingsRepository.settings
        .map { it.advancedMode }
        .stateIn(viewModelScope, SharingStarted.Eagerly, settingsRepository.current.advancedMode)

    private val _advancedOverrideLat = MutableStateFlow<Double?>(
        privatePrefs.getFloat("advanced_override_lat", Float.NaN).takeUnless { it.isNaN() }?.toDouble()
    )
    val advancedOverrideLat: StateFlow<Double?> = _advancedOverrideLat.asStateFlow()

    private val _advancedOverrideLon = MutableStateFlow<Double?>(
        privatePrefs.getFloat("advanced_override_lon", Float.NaN).takeUnless { it.isNaN() }?.toDouble()
    )
    val advancedOverrideLon: StateFlow<Double?> = _advancedOverrideLon.asStateFlow()

    private val _searchResults = MutableStateFlow<List<GeocodingResult>>(emptyList())
    val searchResults: StateFlow<List<GeocodingResult>> = _searchResults.asStateFlow()

    private val savedLocationListSerializer = ListSerializer(SavedLocation.serializer())

    private val _savedLocations = MutableStateFlow(loadSavedLocations())
    val savedLocations: StateFlow<List<SavedLocation>> = _savedLocations.asStateFlow()

    private val _activeLocationId = MutableStateFlow(prefs.getString("active_location_id", null))
    val activeLocationId: StateFlow<String?> = _activeLocationId.asStateFlow()

    private val _showSavedLocationsDialog = MutableStateFlow(false)
    val showSavedLocationsDialog: StateFlow<Boolean> = _showSavedLocationsDialog.asStateFlow()

    private var fetchJob: Job? = null
    private var searchJob: Job? = null
    private var alertJob: Job? = null
    private var sharedTextJob: Job? = null
    private var gpsCacheJob: Job? = null
    // Coordinates of the weather currently displayed; alert results for other coordinates are dropped
    private var alertCoords: Pair<Double, Double>? = null

    // True while a shared text is resolved to a place; fetchWeather waits for it
    private var isResolvingSharedLocation = false

    init {
        checkOverrideExpiry()
        // When advanced mode is turned off, clear location override and refresh
        viewModelScope.launch {
            var prev = advancedModeActive.value
            advancedModeActive.collect { active ->
                if (!active && prev) {
                    clearAdvancedModeOverride()
                    fetchWeather()
                }
                prev = active
            }
        }
    }

    private fun checkOverrideExpiry() {
        val setTime = privatePrefs.getLong("advanced_override_set_time", 0L)
        if (setTime != 0L && System.currentTimeMillis() - setTime >= 12 * 60 * 60 * 1000L) {
            clearAdvancedModeOverride()
        }
    }

    fun onLocationDoubleClicked() {
        // No longer doing anything on double click
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
            val coords = parseCoordinates(query)
            if (coords != null) {
                val (lat, lon) = coords
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

            // A short Plus Code with no place given: read it near the location on screen
            val plusCode = PlusCode.find(query)
            if (plusCode != null && !plusCode.isFull && plusCode.locality.isBlank()) {
                val shown = (_uiState.value as? WeatherUiState.Success)?.data
                val refLat = shown?.latitude
                val refLon = shown?.longitude
                _searchResults.value = if (refLat == null || refLon == null) emptyList() else {
                    val (lat, lon) = PlusCode.recoverNearest(plusCode.code, refLat, refLon)
                    listOf(GeocodingResult(id = 0, name = plusCode.code, latitude = lat, longitude = lon, admin1 = shown.locationName))
                }
                return@launch
            }

            weatherRepository.searchLocation(query).onSuccess { results ->
                _searchResults.value = results
            }
        }
    }

    /** Previews a location without saving it: switches the displayed weather to it, but does not
     * add it to the saved-locations list. Expires after 12h. Mutually exclusive with
     * [switchToLocation] — starting a preview clears the active saved-location id, and switching
     * to a real location clears an active preview. Use [favoriteCurrentLocation] to save it. */
    fun setLocationOverride(lat: Double, lon: Double, name: String) {
        cancelSharedText()
        applyLocationOverride(lat, lon, name)
    }

    /** [setLocationOverride] without cancelling the shared-text job, which calls it when it finds the place. */
    private fun applyLocationOverride(lat: Double, lon: Double, name: String) {
        _uiState.value = WeatherUiState.Loading
        privatePrefs.edit {
            putFloat("advanced_override_lat", lat.toFloat())
            putFloat("advanced_override_lon", lon.toFloat())
            putString("advanced_override_name", name)
            putLong("advanced_override_set_time", System.currentTimeMillis())
        }
        prefs.edit { remove("active_location_id") }
        _advancedOverrideLat.value = lat
        _advancedOverrideLon.value = lon
        _activeLocationId.value = null
        _showSavedLocationsDialog.value = false
        _searchResults.value = emptyList()
        fetchWeather()
    }

    fun handleSharedText(text: String) {
        _uiState.value = WeatherUiState.Loading
        cancelSharedText()
        isResolvingSharedLocation = true
        fetchJob?.cancel()
        sharedTextJob = viewModelScope.launch {
            try {
                // The parser can follow short links over the network
                val parsed = withContext(Dispatchers.IO) { SharedLocationParser.parseLocationText(text) }
                if (parsed != null) {
                    val name = parsed.name ?: "%.4f, %.4f".format(Locale.US, parsed.latitude, parsed.longitude)
                    finishSharedText()
                    applyLocationOverride(parsed.latitude, parsed.longitude, name)
                    return@launch
                }
                val searchQuery = withContext(Dispatchers.IO) { SharedLocationParser.extractSearchQuery(text) }
                val match = if (searchQuery.isNullOrBlank()) null
                    else weatherRepository.searchLocation(searchQuery).getOrNull()?.firstOrNull()
                finishSharedText()
                if (match != null) applyLocationOverride(match.latitude, match.longitude, match.name) else fetchWeather()
            } finally {
                if (sharedTextJob === coroutineContext[Job]) isResolvingSharedLocation = false
            }
        }
    }

    /** Stops a shared text that is still being resolved, so it cannot replace a place the user picked after it. */
    private fun cancelSharedText() {
        sharedTextJob?.cancel()
        finishSharedText()
    }

    /** Marks the shared text as resolved; called by its own job, which must not cancel itself. */
    private fun finishSharedText() {
        sharedTextJob = null
        isResolvingSharedLocation = false
    }

    fun clearLocationOverride() {
        cancelSharedText()
        _uiState.value = WeatherUiState.Loading
        clearAdvancedModeOverride()
        fetchWeather()
    }

    private fun clearAdvancedModeOverride() {
        privatePrefs.edit {
            remove("advanced_override_lat")
            remove("advanced_override_lon")
            remove("advanced_override_name")
            remove("advanced_override_set_time")
        }
        _advancedOverrideLat.value = null
        _advancedOverrideLon.value = null
    }

    private fun loadSavedLocations(): List<SavedLocation> {
        val raw = prefs.getString("saved_locations", null) ?: return emptyList()
        return runCatching { Json.decodeFromString(savedLocationListSerializer, raw) }.getOrDefault(emptyList())
    }

    private fun persistSavedLocations(list: List<SavedLocation>) {
        prefs.edit { putString("saved_locations", Json.encodeToString(savedLocationListSerializer, list)) }
    }

    fun onManageLocationsClicked() {
        _showSavedLocationsDialog.value = true
    }

    fun setShowSavedLocationsDialog(show: Boolean) {
        _showSavedLocationsDialog.value = show
        if (!show) _searchResults.value = emptyList()
    }

    /** Adds a search result as a saved location (or reuses an existing one at the same
     * coordinates) and immediately switches to it. */
    fun addSavedLocation(result: GeocodingResult) {
        val id = "${result.latitude},${result.longitude}"
        val subtext = listOfNotNull(result.admin1, result.country).joinToString(", ").ifBlank { null }
        val current = _savedLocations.value
        val updated = if (current.any { it.id == id }) current else {
            current + SavedLocation(id = id, name = result.name, subtext = subtext, latitude = result.latitude, longitude = result.longitude)
        }
        _savedLocations.value = updated
        persistSavedLocations(updated)
        switchToLocation(id)
    }

    fun removeSavedLocation(id: String) {
        val updated = _savedLocations.value.filterNot { it.id == id }
        _savedLocations.value = updated
        persistSavedLocations(updated)
        if (_activeLocationId.value == id) switchToLocation(null)
    }

    /** Promotes the location currently being previewed (via [setLocationOverride]) into a saved
     * favorite, and switches to it via its saved-location id. No-op if nothing is being previewed. */
    fun favoriteCurrentLocation() {
        val lat = _advancedOverrideLat.value ?: return
        val lon = _advancedOverrideLon.value ?: return
        val rawName = privatePrefs.getString("advanced_override_name", null) ?: return
        val parts = rawName.split(",", limit = 2).map { it.trim() }
        val name = parts.getOrElse(0) { rawName }
        val subtext = parts.getOrNull(1)?.ifBlank { null }
        val id = "$lat,$lon"
        val current = _savedLocations.value
        val shownName = current.find { it.id == id }?.displayName ?: name
        if (current.none { it.id == id }) {
            val updated = current + SavedLocation(id = id, name = name, subtext = subtext, latitude = lat, longitude = lon)
            _savedLocations.value = updated
            persistSavedLocations(updated)
        }
        // The same place is already on screen: keep it under the saved id instead of reloading
        val shown = (_uiState.value as? WeatherUiState.Success)?.data ?: return switchToLocation(id)
        clearAdvancedModeOverride()
        _activeLocationId.value = id
        prefs.edit { putString("active_location_id", id) }
        publish(id, shown.copy(locationName = shownName, locationSubtext = subtext))
    }

    /** Gives a favorite the user's own name; a blank [label] restores the place name. */
    fun renameSavedLocation(id: String, label: String?) {
        val newLabel = label?.trim()?.ifBlank { null }
        val updated = _savedLocations.value.map { if (it.id == id) it.copy(label = newLabel) else it }
        _savedLocations.value = updated
        persistSavedLocations(updated)
        val renamed = updated.find { it.id == id } ?: return
        val shown = (_uiState.value as? WeatherUiState.Success)?.data ?: return
        if (_activeLocationId.value == id && _advancedOverrideLat.value == null) {
            publish(id, shown.copy(locationName = renamed.displayName, romanName = null, romanSubtext = null))
        }
    }

    /** Unfavorites the currently active saved location, if any. */
    fun unfavoriteCurrentLocation() {
        activeLocationId.value?.let { removeSavedLocation(it) }
    }

    /** Switches the active location. Pass null to switch back to GPS ("Current Location"). Clears
     * any active preview (see [setLocationOverride]) — the two are mutually exclusive. */
    fun switchToLocation(id: String?) {
        cancelSharedText()
        _uiState.value = WeatherUiState.Loading
        clearAdvancedModeOverride()
        _activeLocationId.value = id
        prefs.edit {
            if (id == null) remove("active_location_id") else putString("active_location_id", id)
        }
        _showSavedLocationsDialog.value = false
        _searchResults.value = emptyList()
        fetchWeather()
    }

    private suspend fun fetchForSavedLocation(location: SavedLocation) {
        weatherRepository.getWeather(location.latitude, location.longitude).onSuccess { data ->
            // The fetch looked the place up in the current language: the favorite keeps its name (or the
            // user's own) but takes the fresh region and country line, falling back to the saved one
            data.locationSubtext?.let { setSavedLocationSubtext(location.id, it) }
            val locationData = data.copy(locationName = location.displayName, locationSubtext = data.locationSubtext ?: location.subtext)
            publish(location.id, locationData)
            fetchAlertsForData(location.latitude, location.longitude)
        }.onFailure { onWeatherFailure(it) }
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
        relocalizePlaceName()
    }

    /**
     * After a language change, looks the shown place up again in the new language: a favorite takes the new
     * region and country line (keeping its own name), the GPS place its new name and line. A place typed in through
     * the advanced override keeps the name it was given.
     */
    private fun relocalizePlaceName() {
        val data = (_uiState.value as? WeatherUiState.Success)?.data ?: return
        val lat = data.latitude ?: return
        val lon = data.longitude ?: return
        if (_advancedOverrideLat.value != null) return
        val favoriteId = _activeLocationId.value
        viewModelScope.launch {
            val (name, subtext) = weatherRepository.localPlaceName(lat, lon) ?: return@launch
            val current = (_uiState.value as? WeatherUiState.Success)?.data
            if (current?.latitude != lat || current.longitude != lon || _activeLocationId.value != favoriteId) return@launch
            val updated = if (favoriteId != null) {
                if (subtext == null) return@launch
                setSavedLocationSubtext(favoriteId, subtext)
                current.copy(locationSubtext = subtext)
            } else {
                current.copy(locationName = name, locationSubtext = subtext, romanName = null, romanSubtext = null)
            }
            publish(favoriteId ?: "gps", updated)
        }
    }

    /** Keeps a favorite's region and country line in the language last looked up, so its next load matches. */
    private fun setSavedLocationSubtext(id: String, subtext: String) {
        val current = _savedLocations.value
        if (current.find { it.id == id }?.subtext == subtext) return
        val updated = current.map { if (it.id == id) it.copy(subtext = subtext) else it }
        _savedLocations.value = updated
        persistSavedLocations(updated)
    }

    /**
     * While a preview is shown, refreshes the GPS weather in the background and caches it, so it shows at once
     * when the preview is closed. Runs one at a time, and only when the cached GPS weather is old.
     */
    private fun spawnGpsCacheRefresh() {
        if (gpsCacheJob?.isActive == true) return
        gpsCacheJob = viewModelScope.launch {
            try {
                val cachedAt = withContext(Dispatchers.IO) { weatherCache.load("gps") }?.timestamp ?: 0L
                if (System.currentTimeMillis() - cachedAt < GPS_CACHE_MAX_AGE_MS) return@launch
                locationRepository.getFreshLocation().onSuccess { (lat, lon) ->
                    saveLocation(lat, lon)
                    weatherRepository.getWeather(lat, lon).onSuccess { data ->
                        privatePrefs.edit { putString("location_name", data.locationName) }
                        withContext(Dispatchers.IO) { weatherCache.save("gps", data) }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Silently fail - this is a best-effort background refresh
            }
        }
    }

    private fun fetchAlertsForData(lat: Double, lon: Double) {
        alertJob?.cancel()
        alertCoords = lat to lon
        alertJob = viewModelScope.launch {
            weatherRepository.fetchAlerts(lat, lon).onSuccess { result ->
                val current = _uiState.value
                if (current is WeatherUiState.Success && alertCoords == lat to lon) {
                    _uiState.value = WeatherUiState.Success(current.data.copy(alerts = result.alerts, failedAlertSources = result.failedSources, alertsLoading = false))
                }
            }.onFailure {
                val current = _uiState.value
                if (current is WeatherUiState.Success && alertCoords == lat to lon) {
                    _uiState.value = WeatherUiState.Success(current.data.copy(alertsLoading = false))
                }
            }
        }
    }

    /** True when the screen shows a place the user picked (a favorite or a preview), which needs no location access. */
    fun showsChosenPlace(): Boolean =
        privatePrefs.contains("advanced_override_lat") ||
            activeLocationId.value?.let { id -> savedLocations.value.any { it.id == id } } == true

    fun onPermissionRevoked() {
        if (_uiState.value is WeatherUiState.Success && !showsChosenPlace()) {
            _uiState.value = WeatherUiState.PermissionRequired
        }
    }

    fun fetchWeather() {
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            try {
                if (!isResolvingSharedLocation) {
                    // Only show loading if we don't already have data
                    if (_uiState.value !is WeatherUiState.Success) {
                        _refreshFailed.value = false; _refreshError.value = null
                        // Show the last successful data for this location right away, then refresh
                        val cached = withContext(Dispatchers.IO) { loadCached() }
                        if (cached != null) {
                            _uiState.value = WeatherUiState.Success(cached)
                            _isRefreshing.value = true
                        } else {
                            _uiState.value = WeatherUiState.Loading
                        }
                    } else {
                        _isRefreshing.value = true
                    }
                    fetchShownPlace()
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun refresh() {
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            try {
                _isRefreshing.value = true
                fetchShownPlace()
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    /** Fetches the place to show: the preview, else the active favorite, else the GPS location. */
    private suspend fun fetchShownPlace() {
        // Check 12-hour non-local override expiry
        checkOverrideExpiry()

        if (privatePrefs.contains("advanced_override_lat")) {
            val overrideLat = privatePrefs.getFloat("advanced_override_lat", 0f).toDouble()
            val overrideLon = privatePrefs.getFloat("advanced_override_lon", 0f).toDouble()
            val rawOverrideName = privatePrefs.getString("advanced_override_name", "Overridden Location") ?: "Overridden Location"
            val overrideName = shortPlaceName(rawOverrideName)

            weatherRepository.getWeather(overrideLat, overrideLon).onSuccess { data ->
                val overrideData = data.copy(locationName = overrideName)
                publish("preview", overrideData)
                fetchAlertsForData(overrideLat, overrideLon)
                // Spawn background GPS cache refresh
                spawnGpsCacheRefresh()
            }.onFailure { onWeatherFailure(it) }
        } else {
            val savedLocation = activeLocationId.value?.let { id -> savedLocations.value.find { it.id == id } }
            if (savedLocation != null) fetchForSavedLocation(savedLocation) else doFetch()
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
        var locationObtained = false
        // Outcomes are recorded here and the final error is decided once both steps finish, so a
        // fast failure in one step can't flash an Error while the other is still in flight.
        var displayedThisRun = false
        var weatherError: Throwable? = null
        val completed = withTimeoutOrNull(45_000L) {
            kotlinx.coroutines.coroutineScope {
                // Step 1: try cached location for instant render
                var cachedWeatherDisplayed = false
                var freshWeatherDisplayed = false

                launch {
                    locationRepository.getLastLocation().onSuccess { (lat, lon) ->
                        locationObtained = true
                        saveLocation(lat, lon)
                        weatherRepository.getWeather(lat, lon)
                            .onSuccess { data ->
                                if (!freshWeatherDisplayed) {
                                    privatePrefs.edit().putString("location_name", data.locationName).apply()
                                    publish("gps", data)
                                    cachedWeatherDisplayed = true
                                    displayedThisRun = true
                                    fetchAlertsForData(lat, lon)
                                }
                            }
                            .onFailure { weatherError = it }
                    }
                }

                // Step 2: get fresh location, re-fetch if moved significantly
                launch {
                    locationRepository.getFreshLocation()
                        .onSuccess { (lat, lon) ->
                            locationObtained = true
                            saveLocation(lat, lon)
                            if (!cachedWeatherDisplayed || movedSignificantly(lat, lon)) {
                                weatherRepository.getWeather(lat, lon)
                                    .onSuccess { data ->
                                        freshWeatherDisplayed = true
                                        displayedThisRun = true
                                        privatePrefs.edit().putString("location_name", data.locationName).apply()
                                        publish("gps", data)
                                        fetchAlertsForData(lat, lon)
                                    }
                                    .onFailure {
                                        if (_uiState.value is WeatherUiState.Success) flagRefreshFailed(it)
                                        else weatherError = it
                                    }
                            }
                        }
                    // A failed fresh fix is not a refresh failure: step 1 may still show weather.
                }
            }
        }
        if (completed == null) {
            // Safety net: if the entire fetch timed out, guarantee we exit Loading.
            if (_uiState.value is WeatherUiState.Loading) {
                _uiState.value = WeatherUiState.Error(
                    if (locationObtained) R.string.error_fetch_weather else R.string.error_get_location
                )
            } else if (_uiState.value is WeatherUiState.Success && !displayedThisRun) {
                flagRefreshFailed(null)
            }
        } else if (!displayedThisRun) {
            if (_uiState.value is WeatherUiState.Success) {
                if (weatherError != null) flagRefreshFailed(weatherError)
            } else {
                _uiState.value = WeatherUiState.Error(
                    weatherError?.toFetchError(weatherSource.value == WeatherSource.PIRATE_WEATHER)?.messageResId
                        ?: R.string.error_get_location
                )
            }
        }
    }

    /** Shows [data] as the current weather and persists it as the last good result for [cacheKey]. */
    private fun publish(cacheKey: String, data: WeatherData) {
        // A refresh of the same place keeps the Latin-letter name already found for it
        val previous = (_uiState.value as? WeatherUiState.Success)?.data
        val shown = if (data.romanName == null && previous?.romanName != null &&
            previous.locationName == data.locationName && previous.latitude == data.latitude
        ) data.copy(romanName = previous.romanName, romanSubtext = previous.romanSubtext) else data
        _uiState.value = WeatherUiState.Success(shown)
        _refreshFailed.value = false; _refreshError.value = null
        viewModelScope.launch(Dispatchers.IO) { weatherCache.save(cacheKey, shown) }
        lookUpRomanName(cacheKey, shown)
    }

    /** For a name in a non-Latin script, finds its Latin-letter version in the background. */
    private fun lookUpRomanName(cacheKey: String, data: WeatherData) {
        val lat = data.latitude ?: return
        val lon = data.longitude ?: return
        if (data.romanName != null || isLatinScript(data.locationName)) return
        // A favorite the user renamed keeps their name; there is no other script to offer
        if (savedLocations.value.find { it.id == cacheKey }?.label != null) return
        viewModelScope.launch {
            val (name, subtext) = weatherRepository.romanPlaceName(lat, lon) ?: return@launch
            val current = (_uiState.value as? WeatherUiState.Success)?.data
            if (current?.latitude != lat || current.locationName != data.locationName || name == data.locationName) return@launch
            val updated = current.copy(romanName = name, romanSubtext = subtext)
            _uiState.value = WeatherUiState.Success(updated)
            withContext(Dispatchers.IO) { weatherCache.save(cacheKey, updated) }
        }
    }

    /** With data on screen, keep it and flag the failed refresh; otherwise show a classified error. */
    private fun onWeatherFailure(e: Throwable, fallbackResId: Int? = null) {
        if (_uiState.value is WeatherUiState.Success) {
            flagRefreshFailed(e)
        } else {
            _uiState.value = WeatherUiState.Error(
                fallbackResId ?: e.toFetchError(weatherSource.value == WeatherSource.PIRATE_WEATHER).messageResId
            )
        }
    }

    private fun loadCached(): WeatherData? {
        if (privatePrefs.contains("advanced_override_lat")) {
            val name = shortPlaceName(privatePrefs.getString("advanced_override_name", "Overridden Location") ?: "Overridden Location")
            // A single "preview" slot: only reuse it if it holds this same place
            return weatherCache.load("preview")?.takeIf { it.locationName == name }
        }
        val saved = activeLocationId.value?.let { id -> savedLocations.value.find { it.id == id } }
        val cached = weatherCache.load(saved?.id ?: "gps")
        // Data cached before WeatherData carried coordinates: take them from the saved location
        return if (saved != null && cached != null && cached.latitude == null) {
            cached.copy(latitude = saved.latitude, longitude = saved.longitude)
        } else {
            cached
        }
    }

    private fun movedSignificantly(lat: Double, lon: Double): Boolean {
        val oldLat = privatePrefs.getFloat("last_render_lat", Float.MIN_VALUE)
        val oldLon = privatePrefs.getFloat("last_render_lon", Float.MIN_VALUE)
        val isMoved = if (oldLat == Float.MIN_VALUE) {
            true
        } else {
            val dlat = lat - oldLat
            val cosLat = Math.cos(Math.toRadians((lat + oldLat) / 2.0))
            val dlon = (lon - oldLon) * cosLat
            // ~5 km threshold (0.045 degrees latitude ≈ 5 km)
            dlat * dlat + dlon * dlon > 0.045 * 0.045
        }
        return isMoved
    }

    private companion object {
        /** A background GPS refresh during a preview is skipped while the cached GPS weather is newer. */
        const val GPS_CACHE_MAX_AGE_MS = 30 * 60 * 1000L
    }

    private fun saveLocation(lat: Double, lon: Double) {
        privatePrefs.edit()
            .putFloat("lat", lat.toFloat())
            .putFloat("lon", lon.toFloat())
            .putFloat("last_render_lat", lat.toFloat())
            .putFloat("last_render_lon", lon.toFloat())
            .apply()
    }
}
