# Plan: Source Customization & Settings Page (Android — F-Droid Anti-Features)

## Context

F-Droid flags `TetheredNet` because the app hardcodes every network service it uses (Open-Meteo, GDACS, NWS, Android Geocoder). Users have no ability to opt out of or swap any source. This plan adds a developer-accessible settings screen where users can choose weather data sources, choose geocoding sources, enter API keys for commercial sources, and opt out of alerts — eliminating the "forced dependency" concern. Settings persist globally (dev mode is only the *access gate*, not a scope).

---

## Overview of Changes

1. **Settings infrastructure** — `AppSettings` data class + `AppSettingsRepository` (SharedPreferences-backed)
2. **Alerts opt-out** — simplest win: skip `fetchAlerts()` entirely based on a boolean setting
3. **Pluggable weather sources** — abstract `WeatherDataSource` interface, multiple implementations
4. **Pluggable geocoding source** — abstract `GeocodingSource` interface (reverse + forward), Nominatim as F-Droid-clean alternative
5. **Settings screen UI** — full-screen Compose destination, gear icon in the dev mode row
6. **Navigation** — add `NavHost` to host WeatherScreen + SettingsScreen

---

## Phase 1 — Settings Infrastructure

### New file: `domain/model/AppSettings.kt`

```kotlin
enum class WeatherSource { OPEN_METEO, OPEN_WEATHER_MAP, PIRATE_WEATHER }
enum class GeocodingSource { SYSTEM_GEOCODER, NOMINATIM }

data class AppSettings(
    val weatherSource: WeatherSource = WeatherSource.OPEN_METEO,
    val weatherApiKey: String = "",
    val geocodingSource: GeocodingSource = GeocodingSource.SYSTEM_GEOCODER,
    val alertsEnabled: Boolean = true,
    val alertsNwsEnabled: Boolean = true,
    val alertsGdacsEnabled: Boolean = true,
    val alertsDerivedEnabled: Boolean = true,
)
```

### New file: `data/settings/AppSettingsRepository.kt`

- Reads/writes from existing `widget_prefs` SharedPreferences
- Keys (new, no collision with existing keys):
  - `settings_weather_source` (String, default `"OPEN_METEO"`)
  - `settings_weather_api_key` (String, default `""`)
  - `settings_geocoding_source` (String, default `"SYSTEM_GEOCODER"`)
  - `settings_alerts_enabled` (Boolean, default `true`)
  - `settings_alerts_nws` (Boolean, default `true`)
  - `settings_alerts_gdacs` (Boolean, default `true`)
  - `settings_alerts_derived` (Boolean, default `true`)
- Exposes a `StateFlow<AppSettings>` so the ViewModel can react to changes live
- Inject via Hilt: `@Singleton` in a new `SettingsModule.kt` under `di/`

---

## Phase 2 — Alerts Opt-Out (Quickest F-Droid Win)

**File:** `data/repository/WeatherRepositoryImpl.kt`

Add `AppSettingsRepository` to the constructor. In `fetchAlerts()`, gate the entire body:

```kotlin
if (!settings.current.alertsEnabled) return Result.success(emptyList())
```

Then gate each source individually:
- Skip `nwsDeferred` if `!settings.alertsNwsEnabled`
- Skip `gdacsDeferred` if `!settings.alertsGdacsEnabled`
- Skip appending `derivedAlerts` if `!settings.alertsDerivedEnabled`

No other changes needed to `WeatherRepositoryImpl` for this phase.

---

## Phase 3 — Pluggable Weather Source

### New interface: `data/source/WeatherDataSource.kt`

```kotlin
interface WeatherDataSource {
    suspend fun getForecast(lat: Double, lon: Double): OpenMeteoResponse
    val requiresApiKey: Boolean
    val displayName: String
}
```

### New file: `data/source/OpenMeteoWeatherSource.kt`

- Wraps existing `OpenMeteoApiService` (no change to the service itself)
- `requiresApiKey = false`, `displayName = "Open-Meteo (default)"`

### New file: `data/source/OpenWeatherMapWeatherSource.kt`

- New `data/remote/OpenWeatherMapApiService.kt` Retrofit interface → `https://api.openweathermap.org/data/3.0/`
- New `data/remote/OpenWeatherMapMapper.kt` → maps OWM JSON to existing domain `WeatherData`
- OWM One Call 3.0 endpoint: `GET /onecall?lat=&lon=&appid=<key>&units=metric`
- `requiresApiKey = true`, `displayName = "OpenWeatherMap"`

### New file: `data/source/PirateWeatherSource.kt`

- New `data/remote/PirateWeatherApiService.kt` → `https://api.pirateweather.net/forecast/`
- Dark Sky-compatible JSON → reuse OWM mapper structure
- `requiresApiKey = true`, `displayName = "Pirate Weather"`

### New file: `data/source/WeatherSourceSelector.kt`

```kotlin
@Singleton class WeatherSourceSelector @Inject constructor(
    private val settings: AppSettingsRepository,
    private val openMeteo: OpenMeteoWeatherSource,
    private val openWeatherMap: OpenWeatherMapWeatherSource,
    private val pirateWeather: PirateWeatherSource,
) {
    fun current(): WeatherDataSource = when (settings.current.weatherSource) {
        WeatherSource.OPEN_METEO -> openMeteo
        WeatherSource.OPEN_WEATHER_MAP -> openWeatherMap
        WeatherSource.PIRATE_WEATHER -> pirateWeather
    }
}
```

`WeatherRepositoryImpl.getWeather()` calls `sourceSelector.current().getForecast(lat, lon)`.

### Changes to `di/NetworkModule.kt`

Add two more `@Provides` functions:
- `provideOpenWeatherMapApiService()` → base URL `https://api.openweathermap.org/`
- `providePirateWeatherApiService()` → base URL `https://api.pirateweather.net/`

---

## Phase 4 — Pluggable Geocoding Source

### New interface: `data/source/GeocodingSource.kt`

```kotlin
interface GeocodingSource {
    suspend fun reverseGeocode(lat: Double, lon: Double, locale: Locale): Pair<String, String?>
    suspend fun searchLocations(query: String, locale: Locale): List<GeocodingResult>
}
```

### New file: `data/source/SystemGeocoderSource.kt`

- Wraps existing `Geocoder` logic currently in `WeatherRepositoryImpl` lines 48–73
- Also wraps existing `OpenMeteoGeocodingService` for forward search (location search in dev mode)

### New file: `data/source/NominatimGeocodingSource.kt`

- New `data/remote/NominatimApiService.kt` Retrofit interface → `https://nominatim.openstreetmap.org/`
- Reverse: `GET /reverse?lat=&lon=&format=json&accept-language=<locale>`
- Forward: `GET /search?q=&format=json&accept-language=<locale>&limit=5`
- Maps response to existing `GeocodingResult` and the `(name, subtext)` pair
- User-Agent header: `"maripanaTokana (contact@orinasa.mg)"` (Nominatim requires a custom User-Agent)
- `displayName = "Nominatim (OpenStreetMap)"`

### New file: `data/source/GeocodingSourceSelector.kt`

Same selector pattern as weather source — reads `settings.geocodingSource` and returns the active implementation.

### Changes to `WeatherRepositoryImpl.kt`

- Remove direct `Geocoder` and `OpenMeteoGeocodingService` calls
- Delegate to `GeocodingSourceSelector.current().reverseGeocode(...)` and `.searchLocations(...)`
- Remove `@ApplicationContext context` from constructor (moved to `SystemGeocoderSource`)

### Changes to `di/NetworkModule.kt`

Add `provideNominatimApiService()` → base URL `https://nominatim.openstreetmap.org/`.

---

## Phase 5 — Settings Screen UI

### New file: `ui/settings/SettingsScreen.kt`

Full-screen Compose screen with these sections:

**Weather Source**
- `RadioGroup` listing the three `WeatherSource` values (using `displayName`)
- When the selected source has `requiresApiKey = true`, show a `TextField` (password visual transform) for the API key immediately beneath
- Writes to `AppSettingsRepository` on change (no separate save button)

**Alerts**
- Master `Switch`: "Show weather alerts" → `settings_alerts_enabled`
- When enabled, three indented `Checkbox` rows: NWS alerts, GDACS alerts, Derived alerts

**Location / Geocoding**
- `RadioGroup`: System Geocoder vs Nominatim (OpenStreetMap)
- Note on Nominatim option: "Works on all builds including F-Droid, no API key required"

### Navigation

Add a `NavHost` in `WeatherScreen.kt`. Two destinations:
- `"weather"` → existing `WeatherContent` composable
- `"settings"` → `SettingsScreen`

### Gear Icon in Dev Mode (`WeatherContent.kt`, lines 218–230)

Add a gear `IconButton` immediately before the existing Edit Location `IconButton`, visible only when `devModeActive`:

```kotlin
if (devModeActive) {
    IconButton(onClick = onOpenSettings, modifier = Modifier.size(32.sd(scale))) {
        Icon(Icons.Default.Settings, contentDescription = "Settings",
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(18.sd(scale)))
    }
    // existing edit icon + DEV badge unchanged
}
```

Pass `onOpenSettings: () -> Unit` down through the call chain:
`WeatherScreen` (holds `navController`) → `WeatherContent` → gear icon `onClick`

---

## Files Created

| File | Purpose |
|------|---------|
| `domain/model/AppSettings.kt` | Settings data model + enums |
| `data/settings/AppSettingsRepository.kt` | SharedPreferences persistence + StateFlow |
| `di/SettingsModule.kt` | Hilt binding for AppSettingsRepository |
| `data/source/WeatherDataSource.kt` | Weather source interface |
| `data/source/OpenMeteoWeatherSource.kt` | Wraps existing Open-Meteo service |
| `data/source/OpenWeatherMapWeatherSource.kt` | OWM implementation |
| `data/source/PirateWeatherSource.kt` | Pirate Weather implementation |
| `data/source/WeatherSourceSelector.kt` | Runtime source picker |
| `data/source/GeocodingSource.kt` | Geocoding source interface |
| `data/source/SystemGeocoderSource.kt` | Wraps Android Geocoder + Open-Meteo geocoding |
| `data/source/NominatimGeocodingSource.kt` | Nominatim implementation |
| `data/source/GeocodingSourceSelector.kt` | Runtime geocoding source picker |
| `data/remote/OpenWeatherMapApiService.kt` | Retrofit interface for OWM |
| `data/remote/OpenWeatherMapMapper.kt` | OWM JSON → domain model |
| `data/remote/PirateWeatherApiService.kt` | Retrofit interface for Pirate Weather |
| `data/remote/PirateWeatherMapper.kt` | Pirate Weather JSON → domain model |
| `data/remote/NominatimApiService.kt` | Retrofit interface for Nominatim |
| `ui/settings/SettingsScreen.kt` | Settings UI composable |

## Files Modified

| File | Change |
|------|--------|
| `data/repository/WeatherRepositoryImpl.kt` | Inject selectors, remove direct API deps, gate each alert source |
| `di/NetworkModule.kt` | Add OWM, Pirate Weather, Nominatim Retrofit instances |
| `ui/weather/WeatherContent.kt` | Add gear icon + `onOpenSettings` param |
| `ui/weather/WeatherScreen.kt` | Add NavHost, wire gear icon → navigate("settings") |
| `ui/weather/WeatherViewModel.kt` | Inject AppSettingsRepository, expose settings state |

---

## Other Anti-Features Addressed

| Concern | Resolution |
|---------|-----------|
| Forced Open-Meteo for weather | User can switch to OpenWeatherMap or Pirate Weather |
| Forced GDACS/NWS alerts | Per-source disable toggles + master opt-out |
| Android Geocoder (potential Google backend on standard build) | User can switch to Nominatim (fully open, works on F-Droid build) |
| No network control | Auto-refresh threshold setting (stretch — add `settings_refresh_threshold_minutes`, default 30) |

The `TetheredNet` anti-feature declaration stays (unavoidable for a weather app), but source freedom removes the "forced specific service" concern.

---

## What's Explicitly Out of Scope

- Web PWA — user stated Android only
- Encrypting the stored API key — EncryptedSharedPreferences adds a dependency; the key is user-entered and device-local, so plain SharedPreferences is acceptable
- Feature parity with web — user explicitly scoped to Android

---

## Verification

1. **Build**: `./gradlew assembleFdroidRelease` must succeed with no new Google dependencies
2. **Settings persist across dev mode**: Enter dev mode → open settings → switch to Nominatim → exit dev mode → re-enter dev mode → setting still shows Nominatim
3. **Alerts opt-out**: Disable alerts in settings → trigger weather refresh → no `WeatherAlertBanner` shown; no NWS/GDACS network calls in HTTP logs
4. **OWM source**: Set source to OpenWeatherMap + enter a valid API key → weather loads using OWM response
5. **Nominatim geocoding**: Switch geocoding to Nominatim → dev mode location search returns results (Nominatim requests visible in HTTP logs)
6. **Gear icon gating**: Gear icon absent when not in dev mode; visible in dev mode row alongside Edit icon; absent again after closing dev mode
7. **F-Droid build clean**: `find /tmp/fdroid-apk -name "*.dex" | xargs strings | grep -i "google.android.gms"` returns nothing
