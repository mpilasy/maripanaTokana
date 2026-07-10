# maripànaTokana - Design & Implementation Guide

**maripànaTokana** (Malagasy for "a single thermometer") is a phone-only Android weather app that shows current conditions, hourly forecasts, and a 10-day outlook. What makes it standout is that it shows both standard units at all times.
It can be surfaced to the homescreen via one of two home screen widget options.

There is also a web version of the app built with SvelteKit. See [`web/docs/DESIGN.md`](../web/docs/DESIGN.md) for its design documentation.

---

## Table of Contents

1. [Technology Stack](#1-technology-stack)
2. [Project Structure](#2-project-structure)
3. [Architecture Overview](#3-architecture-overview)
4. [App Startup Flow](#4-app-startup-flow)
5. [Domain Models](#5-domain-models)
6. [Data Layer (API & Repositories)](#6-data-layer-api--repositories)
7. [Dependency Injection](#7-dependency-injection)
8. [UI Layer](#8-ui-layer)
9. [Internationalization (i18n)](#9-internationalization-i18n)
10. [Font System](#10-font-system)
11. [Widgets](#11-widgets)
12. [Expert Mode](#12-expert-mode)
13. [Settings & Pluggable Data Sources](#13-settings--pluggable-data-sources)
14. [Weather Alerts](#14-weather-alerts)
15. [Build Flavors](#15-build-flavors)
16. [Build Configuration](#16-build-configuration)
17. [Signing & Release](#17-signing--release)
18. [Key Design Decisions](#18-key-design-decisions)

---

## 1. Technology Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Language | Kotlin | 2.2.10 |
| Build system | Gradle + AGP | 9.2.1 |
| UI framework | Jetpack Compose | BOM 2024.09.00 |
| Widget framework | Glance | 1.1.1 |
| Dependency injection | Hilt | 2.59 |
| Networking | Retrofit + OkHttp | 2.11.0 / 4.12.0 |
| Serialization | Kotlinx Serialization | 1.7.3 |
| Location | Google Play Services | 21.3.0 |
| Background work | WorkManager | 2.10.0 |
| Permissions | Accompanist Permissions | 0.36.0 |
| Min SDK | Android 7.0 (API 24) | |
| Target SDK | Android 16 (API 36) | |

**Weather API:** [Open-Meteo](https://open-meteo.com) -- free, no API key required.

---

## 2. Project Structure

```
maripanaTokana/
+-- app/
|   +-- build.gradle.kts                    # App build config, signing, dependencies
|   +-- src/main/
|   |   +-- AndroidManifest.xml             # Permissions, activities, widget receivers
|   |   +-- java/orinasa/njarasoa/maripanatokana/
|   |   |   +-- MaripanaTokanaApp.kt        # Application class (Hilt + WorkManager init)
|   |   |   +-- MainActivity.kt             # Entry point, locale setup, Compose host
|   |   |   +-- data/
|   |   |   |   +-- location/
|   |   |   |   |   +-- LocationProvider.kt      # Interface (flavor-implemented)
|   |   |   |   +-- remote/
|   |   |   |   |   +-- OpenMeteoApiService.kt   # Retrofit API interface
|   |   |   |   |   +-- OpenMeteoResponse.kt     # JSON response data classes
|   |   |   |   |   +-- OpenMeteoMapper.kt       # API response -> domain model
|   |   |   |   |   +-- WmoWeatherCode.kt        # Weather code -> emoji/description
|   |   |   |   +-- repository/
|   |   |   |   |   +-- WeatherRepositoryImpl.kt  # Fetches weather, alerts + geocodes city
|   |   |   |   |   +-- LocationRepositoryImpl.kt # GPS location provider
|   |   |   |   +-- settings/
|   |   |   |   |   +-- AppSettingsRepository.kt # SharedPreferences-backed settings + StateFlow
|   |   |   |   +-- source/                      # Pluggable weather + geocoding sources
|   |   |   |       +-- WeatherDataSource.kt / WeatherSourceSelector.kt
|   |   |   |       +-- OpenMeteoWeatherSource.kt / PirateWeatherSource.kt
|   |   |   |       +-- GeocodingDataSource.kt / GeocodingSourceSelector.kt
|   |   |   |       +-- SystemGeocoderSource.kt / NominatimGeocodingSource.kt
|   |   |   +-- domain/
|   |   |   |   +-- model/
|   |   |   |   |   +-- WeatherData.kt       # Main weather data container
|   |   |   |   |   +-- Temperature.kt       # Celsius/Fahrenheit value type
|   |   |   |   |   +-- WindSpeed.kt         # m/s and mph value type
|   |   |   |   |   +-- Pressure.kt          # hPa and inHg value type
|   |   |   |   |   +-- Precipitation.kt     # mm and inches value type
|   |   |   |   |   +-- HourlyForecast.kt    # Single hour forecast entry
|   |   |   |   |   +-- DailyForecast.kt     # Single day forecast entry
|   |   |   |   +-- repository/
|   |   |   |       +-- WeatherRepository.kt     # Interface
|   |   |   |       +-- LocationRepository.kt    # Interface
|   |   |   +-- di/
|   |   |   |   +-- NetworkModule.kt         # Hilt: Retrofit, OkHttp, JSON
|   |   |   |   +-- CommonLocationModule.kt  # Hilt: Geocoder (flavor-agnostic bindings)
|   |   |   |   +-- RepositoryModule.kt      # Hilt: binds repo implementations
|   |   |   +-- ui/
|   |   |   |   +-- weather/
|   |   |   |   |   +-- WeatherScreen.kt     # Main UI (all composables)
|   |   |   |   |   +-- WeatherViewModel.kt  # State management, user actions
|   |   |   |   |   +-- WeatherUiState.kt    # Sealed UI state
|   |   |   |   |   +-- components/          # Charts, alert banner, location override dialog
|   |   |   |   +-- settings/
|   |   |   |   |   +-- SettingsScreen.kt / SettingsViewModel.kt
|   |   |   |   +-- permission/
|   |   |   |   |   +-- PermissionHandler.kt # Interface (flavor-implemented)
|   |   |   |   +-- theme/
|   |   |   |       +-- AppFonts.kt          # 22 font pairings + CompositionLocals
|   |   |   |       +-- Theme.kt             # Material3 theme
|   |   |   |       +-- Color.kt             # Color definitions
|   |   |   |       +-- Type.kt              # Typography
|   |   |   +-- widget/
|   |   |       +-- WeatherWidget.kt             # 4x1 widget (Glance)
|   |   |       +-- WeatherWidgetLarge.kt        # 4x2 widget (Glance)
|   |   |       +-- WeatherWidgetReceiver.kt     # 4x1 broadcast receiver
|   |   |       +-- WeatherWidgetLargeReceiver.kt # 4x2 broadcast receiver
|   |   |       +-- BaseWidgetWeatherFetcher.kt  # Standalone weather fetcher (no Hilt)
|   |   |       +-- WeatherUpdateWorker.kt       # Background periodic updater
|   |   |       +-- theme/WidgetTheme.kt         # Widget color palette
|   |   +-- res/
|   |       +-- drawable/                    # Background images, icons
|   |       +-- font/                        # TTF files (font families x 2 weights)
|   |       +-- layout/                      # widget_loading.xml (placeholder)
|   |       +-- mipmap-*/                    # App launcher icons
|   |       +-- values/                      # strings.xml, colors.xml, themes.xml
|   |       +-- values-{ar,es,fr,hi,mg,ne,zh}/strings.xml  # Translations (generated, don't edit directly)
|   |       +-- xml/                         # Widget metadata, backup rules
|   +-- src/standard/                        # Standard flavor: FusedLocationProviderClient,
|   |                                        # Accompanist permission UI, Play Services DI module
|   +-- src/fdroid/                          # F-Droid flavor: LocationManager, simplified
|                                             # permission handling, no Play Services
+-- gradle/
|   +-- libs.versions.toml                   # Centralized dependency versions
+-- build.gradle.kts                         # Root build config (plugin versions)
+-- settings.gradle.kts                      # Project settings
+-- gradle.properties                        # Gradle/Android build properties
+-- keystore.properties                      # Release signing passwords (gitignored)
+-- release.keystore                         # Release signing key (gitignored)
```

---

## 3. Architecture Overview

The app follows **MVVM** (Model-View-ViewModel) with a clean separation of concerns:

```
+-------------------------------------------------+
|  UI Layer                                       |
|  WeatherScreen.kt (Compose)                     |
|  WeatherWidget*.kt (Glance)                     |
+-------------------------------------------------+
         |  observes StateFlow          ^  calls
         v                              |  actions
+-------------------------------------------------+
|  ViewModel Layer                                |
|  WeatherViewModel.kt                            |
|  (manages UI state, user preferences, refresh)  |
+-------------------------------------------------+
         |  calls suspend functions
         v
+-------------------------------------------------+
|  Domain Layer                                   |
|  Repository interfaces                          |
|  Value types (Temperature, Pressure, etc.)      |
+-------------------------------------------------+
         |  implemented by
         v
+-------------------------------------------------+
|  Data Layer                                     |
|  WeatherRepositoryImpl  (Retrofit + Geocoder)   |
|  LocationRepositoryImpl (Fused Location)        |
|  OpenMeteoApiService    (HTTP API)              |
+-------------------------------------------------+
         |  wired by
         v
+-------------------------------------------------+
|  DI Layer (Hilt)                                |
|  NetworkModule, LocationModule, RepositoryModule|
+-------------------------------------------------+
```

**Data flows one way:** UI observes ViewModel state. ViewModel calls repositories. Repositories call the API. Results flow back up through Kotlin `Result<T>` types.

---

## 4. App Startup Flow

### 4.1 Application Init (`MaripanaTokanaApp.kt`)

When the process starts, Hilt initializes dependency injection and WorkManager schedules a periodic weather update for widgets (every 30 minutes, requires network).

### 4.2 Activity Launch (`MainActivity.kt`)

```
attachBaseContext()
  -> Read locale_index from SharedPreferences
  -> Create locale-specific Resources (for cold-start strings)
  -> Override getResources() to return localized Resources

onCreate()
  -> enableEdgeToEdge() (draws behind status/nav bars)
  -> Intercept back button (move to background, don't exit)
  -> setContent { MaripanaTokanaTheme { WeatherScreen() } }
```

### 4.3 Screen Init (`WeatherScreen.kt`)

```
Compose starts
  -> Collect ViewModel state flows (uiState, locale, font, units)
  -> Build localized context via ContextWrapper (for in-app language switching)
  -> Request location permissions (Accompanist)
  -> When permissions granted -> viewModel.fetchWeather()
  -> Register lifecycle observer for auto-refresh on resume
```

### 4.4 Weather Fetch (`WeatherViewModel.kt`)

The fetch uses a **two-step location strategy:**

```
Step 1: getLastLocation() -- instant, cached GPS
  -> If available: fetch weather immediately, show data
  -> Save coordinates to SharedPreferences

Step 2: getFreshLocation() -- balanced power accuracy
  -> If moved >5km from Step 1: re-fetch weather
  -> If Step 1 failed: this is the primary attempt
  -> On failure (and no cached data): show error state
```

This gives the user instant results from cached GPS while a fresh location loads in the background.

---

## 5. Domain Models

All numeric models are **inline value classes** -- zero-overhead wrappers that store one canonical unit and derive the other.

### `Temperature.kt`

Stores Celsius. Derives Fahrenheit via `celsius * 9/5 + 32`.

```kotlin
val t = Temperature.fromCelsius(20.0)
t.displayCelsius()              // "20°C"
t.displayFahrenheit()           // "68°F"
t.displayCelsius(decimals = 1)  // "20.0°C"
t.displayDual(true)             // ("20°C", "68°F") -- metric primary
t.displayDual(true, decimals=1) // ("20.0°C", "68.0°F") -- with decimals
```

Integer display uses `roundToInt()` to avoid negative zero (`-0`). The `decimals` parameter is used on the hero card (1 decimal) while all other locations use the default (integer). All formatting uses `Locale.US` to prevent digit script conversion (see [i18n section](#9-internationalization-i18n)).

### `WindSpeed.kt`, `Pressure.kt`, `Precipitation.kt`

Same pattern: store one unit (m/s, hPa, mm), derive the other (mph, inHg, inches).

### `WeatherData.kt`

The main container that holds everything the UI needs:

```kotlin
data class WeatherData(
    val temperature: Temperature,
    val feelsLike: Temperature,
    val tempMin: Temperature,
    val tempMax: Temperature,
    val weatherCode: Int,          // WMO code (0-99)
    val locationName: String,      // Reverse-geocoded city name
    val pressure: Pressure,
    val humidity: Int,             // Percentage
    val dewPoint: Temperature,     // Dew point temperature
    val windSpeed: WindSpeed,
    val windDeg: Int,              // Degrees (0-360)
    val windGust: WindSpeed?,
    val rain: Precipitation?,
    val snow: Precipitation?,
    val uvIndex: Double,
    val visibility: Int,           // Meters
    val sunrise: Long,             // Epoch seconds
    val sunset: Long,              // Epoch seconds
    val hourlyForecast: List<HourlyForecast>,  // Next 24 hours
    val dailyForecast: List<DailyForecast>,    // Next 10 days
    val timestamp: Long,           // When this data was fetched
)
```

### `HourlyForecast.kt` / `DailyForecast.kt`

Slim data classes for forecast entries with temperature, weather code, and precipitation probability. Precipitation probability is hidden in the UI when 0% (empty text preserves layout consistency).

---

## 6. Data Layer (API & Repositories)

### 6.1 Open-Meteo API (`OpenMeteoApiService.kt`)

A Retrofit interface with one endpoint:

```kotlin
@GET("v1/forecast")
suspend fun getForecast(
    @Query("latitude") latitude: Double,
    @Query("longitude") longitude: Double,
    @Query("current") current: String = "temperature_2m,apparent_temperature,...",
    @Query("hourly") hourly: String = "temperature_2m,weather_code,...",
    @Query("daily") daily: String = "temperature_2m_max,temperature_2m_min,...",
    @Query("timezone") timezone: String = "auto",
    @Query("wind_speed_unit") windSpeedUnit: String = "ms",
): OpenMeteoResponse
```

No API key. The default parameter strings request all the fields the app needs.

### 6.2 Response Parsing (`OpenMeteoResponse.kt` + `OpenMeteoMapper.kt`)

`OpenMeteoResponse` is a set of `@Serializable` data classes matching the JSON structure. `OpenMeteoMapper.kt` has a `toDomain()` extension function that converts API response objects into `WeatherData`.

### 6.3 Weather Codes (`WmoWeatherCode.kt`)

Maps WMO integer codes (0-99) to:
- `wmoDescriptionRes(code)` -- returns a `@StringRes Int` (resolved to localized text at display time)
- `wmoEmoji(code, isNight)` -- returns a Unicode emoji string (day/night variants for sun/moon). Hourly forecast uses per-day sunrise/sunset from `dailySunrise`/`dailySunset` lists to correctly determine day/night for hours spanning multiple days.

### 6.4 Weather Repository (`WeatherRepositoryImpl.kt`)

Fetches weather via `WeatherSourceSelector.current().getForecast(lat, lon)` and reverse-geocodes via `GeocodingSourceSelector.current().reverseGeocode(...)` — both pluggable at runtime through Settings (see [§13](#13-settings--pluggable-data-sources)). Falls back to formatted coordinates ("12.34, 56.78") if geocoding throws. Also fetches alerts from the 8 sources gated by their individual toggles (see [§14](#14-weather-alerts)).

**Location Name Refinement:**
To ensure a clean UI, the location name is refined by:
1.  **Splitting by separators**: Taking only the first part before commas, semicolons, or dashes (e.g., "Paris" from "Paris, France").
2.  **Subtext Extraction**: The region and country are extracted into a separate `locationSubtext` field displayed on a smaller second line.
3.  **DMS Formatting**: Coordinates are formatted into Degrees, Minutes, and Seconds (DMS) format (e.g., `48°51'24"N`) and displayed on two lines when toggled.

Returns `Result<WeatherData>`.

### 6.5 Location Repository (`LocationRepositoryImpl.kt`)

Delegates to the flavor-provided `LocationProvider` (Fused Location on `standard`, `LocationManager` on `fdroid` — see [§15](#15-build-flavors)):
- `getLastLocation()` -- instant cached location
- `getFreshLocation()` -- active GPS fetch with balanced power accuracy

Both return `Result<Pair<Double, Double>>`.

---

## 7. Dependency Injection

Hilt wires most of the graph via explicit modules in `di/`, plus flavor-specific modules, plus plain `@Inject constructor` classes that need no module at all.

### `di/NetworkModule.kt` (main)
Provides singletons: `Json` config, `OkHttpClient` (with logging), `Retrofit`, `OpenMeteoApiService`.

### `di/CommonLocationModule.kt` (main)
Binds `LocationRepositoryImpl` to the `LocationRepository` interface. The concrete `LocationProvider` it delegates to comes from the flavor's own `di/LocationModule.kt` (`app/src/standard/` or `app/src/fdroid/`), which also binds the flavor's `PermissionHandler`.

### `di/RepositoryModule.kt` (main)
Binds `WeatherRepositoryImpl` to the `WeatherRepository` interface.

### No module needed
`AppSettingsRepository`, `WeatherSourceSelector`, `GeocodingSourceSelector`, and each individual source (`OpenMeteoWeatherSource`, `PirateWeatherSource`, `SystemGeocoderSource`, `NominatimGeocodingSource`) are plain `@Singleton @Inject constructor` classes — Hilt provides them without a `@Provides`/`@Binds` declaration (see [§13](#13-settings--pluggable-data-sources)).

**How it connects:** The `WeatherViewModel` constructor declares its dependencies, and Hilt automatically provides them:

```kotlin
@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val weatherRepository: WeatherRepository,
    private val locationRepository: LocationRepository,
    private val settingsRepository: AppSettingsRepository,
    @ApplicationContext private val appContext: Context,
) : ViewModel()
```

---

## 8. UI Layer

### 8.1 State Management (`WeatherUiState.kt` + `WeatherViewModel.kt`)

The ViewModel exposes several `StateFlow`s that the UI observes:

| Flow | Type | Purpose |
|------|------|---------|
| `uiState` | `WeatherUiState` | PermissionRequired, Loading, Success(data), Error(resId) |
| `isRefreshing` | `Boolean` | Pull-to-refresh indicator |
| `metricPrimary` | `Boolean` | Which unit system is shown first |
| `fontIndex` | `Int` | Selected font pairing (0-21) |
| `localeIndex` | `Int` | Selected language (0-7) |

User actions (toggle units, cycle font, cycle language, refresh) are ViewModel methods that update state and persist preferences to `SharedPreferences("widget_prefs")`.

### 8.2 Main Screen (`WeatherScreen.kt` + `WeatherContent.kt`)

`WeatherContent.kt` is the largest file (~1,460 lines) and holds most of the composables; `WeatherScreen.kt` (~450 lines) is the top-level entry point.

**Top-level: `WeatherScreen()`**
- Collects all ViewModel state
- Creates localized context (ContextWrapper) for in-app language switching
- Determines layout direction (LTR/RTL) based on locale
- Wraps everything in `CompositionLocalProvider` for locale, fonts, and layout direction
- Handles permission request flow
- Registers lifecycle observer for auto-refresh on resume

**Screen states:**
- `PermissionRequired` -- Shows title + message in app language, with system language subtitle if different. "Grant Permission" button.
- `Loading` -- Centered spinner.
- `Success` -- Pull-to-refresh wrapping the main content.
- `Error` -- Error message + retry button.

**WeatherContent composable** (the main scrollable UI):
- **Fixed header**: City/region name (two lines, Expert-Mode Edit icon when active), date, "Updated" time, Settings gear icon
- **Alert banner** (`WeatherAlertBanner`): rendered above the hero card when `data.alerts` is non-empty
- **Scrollable middle** (`.weight(1f).verticalScroll()`):
  - Hero card: weather icon, temperature, feels-like, description, precipitation, high/low temps, wind with cardinal direction, "© Orinasa Njarasoa" watermark, share button (top-left) that captures the card as PNG and opens Android share sheet via FileProvider
  - Three collapsible sections: Hourly Forecast (expanded by default), 10-Day Forecast, Current Conditions
- **Fixed footer**: Font icon + name (left), credits/hash (center), language flag (right)

**Key sub-composables:**
- `DualUnitText` -- Shows primary value bold + secondary value dimmer. Clickable to toggle units.
- `CollapsibleSection` -- Animated expand/collapse with arrow icon. Share button appears when expanded, captures section content as PNG via `rememberGraphicsLayer()`.
- `HourlyForecastRow` -- Horizontal scrolling row of hourly cards.
- `DailyForecastList` -- Vertical list of daily forecast rows with up/down arrows for hi/lo.
- `DetailsContent` -- Full-width cards for high/low temp, wind/gust, sunrise/sunset, plus paired detail cards for temperature/precipitation, pressure/humidity, UV/visibility. Detail card rows use fixed height.
- `DetailCard` -- Single stat card with title, value, optional secondary value and subtitle.

### 8.3 Theming (`ui/theme/`)

- `Theme.kt` -- Material3 theme with dark/light support and dynamic colors (Android 12+).
- `Color.kt` -- Color palette.
- `Type.kt` -- Typography defaults.
- `AppFonts.kt` -- Font pairing system (see [Font System](#10-font-system)).

---

## 9. Internationalization (i18n)

### 9.1 Supported Languages

| Index | Tag | Flag | Native Digits |
|-------|-----|------|---------------|
| 0 | mg | Malagasy flag | -- |
| 1 | ar | Saudi flag | Eastern Arabic (U+0660) |
| 2 | en | UK flag | -- |
| 3 | es | Spain flag | -- |
| 4 | fr | France flag | -- |
| 5 | hi | India flag | Devanagari (U+0966) |
| 6 | ne | Nepal flag | Devanagari (U+0966) |
| 7 | zh | China flag | -- |

Default on first run: Malagasy (index 0).

### 9.2 String Resources

All user-facing text is in `res/values/strings.xml` (English) with translations in `res/values-{locale}/strings.xml`. This includes weather descriptions, UI labels, error messages, and widget text. About 77 strings and 2 string-arrays (cardinal directions, UV labels).

### 9.3 Locale Switching (Two Layers)

**Layer 1 -- Cold start** (`MainActivity.kt`):
`attachBaseContext()` reads the saved locale preference and overrides `getResources()` to return locale-specific resources. This ensures correct strings from the very first frame.

**Layer 2 -- Runtime cycling** (`WeatherScreen.kt`):
When the user taps the flag to change language, a `ContextWrapper` with the new locale is created and provided via `CompositionLocalProvider(LocalContext provides ...)`. This updates all `stringResource()` calls without recreating the Activity (which would cause a visible flash).

### 9.4 Native Digit Rendering

Hindi, Arabic, and Nepali have their own digit characters. The approach:

1. **All formatting uses `Locale.US`** -- `"%.0f".format(Locale.US, value)` always produces ASCII digits (0-9). This prevents inconsistent rendering where some APIs honor locale digit scripts and others don't. The "Updated" time also uses `SimpleDateFormat` with `Locale.US` (not `DateFormat.getTimeFormat(baseContext)`) because `attachBaseContext` overrides the activity locale — if the app locale is Hindi, `baseContext` produces Devanagari digits that `localizeDigits()` can't normalize back to ASCII.

2. **Character replacement at display time** -- `SupportedLocale.localizeDigits(s)` replaces ASCII 0-9 with native digits when the locale has them:

```kotlin
data class SupportedLocale(val tag: String, val flag: String, val nativeZero: Char? = null) {
    fun localizeDigits(s: String): String {
        val z = nativeZero ?: return s   // No native digits, return as-is
        return buildString(s.length) {
            for (c in s) {
                append(if (c in '0'..'9') z + (c - '0') else c)
            }
        }
    }
}
```

This is 100% consistent because it's a simple character map, not dependent on platform formatting APIs.

### 9.5 RTL Support

Arabic is right-to-left. `WeatherScreen` explicitly provides `LocalLayoutDirection` based on the selected locale using `TextUtilsCompat.getLayoutDirectionFromLocale()`. The footer icon row is forced LTR so the font icon stays left and language flag stays right regardless of text direction.

### 9.6 Permission Screen Dual Language

When the app locale differs from the phone's system locale, the permission screen shows each string in both languages -- the app locale in normal size and the system locale in smaller, dimmer text below. This uses `Locale.getDefault()` to create a system-locale context (not `baseContext`, which returns app-locale strings due to the `getResources()` override in MainActivity).

---

## 10. Font System

### 10.1 Font Pairings (`AppFonts.kt`)

Each pairing has a **display** font (for headings, numbers, temperatures) and a **body** font (for labels, descriptions, weather text):

| # | Name | Display | Body |
|---|------|---------|------|
| 0 | Default | System | System |
| 1 | Orbitron + Outfit | Orbitron | Outfit |
| 2 | Rajdhani + Inter | Rajdhani | Inter |
| 3 | Oxanium + Nunito | Oxanium | Nunito |
| 4 | Space Grotesk + DM Sans | Space Grotesk | DM Sans |
| 5 | Sora + Source Sans | Sora | Source Sans 3 |
| 6 | Manrope + Rubik | Manrope | Rubik |
| 7 | Josefin Sans + Lato | Josefin Sans | Lato |
| 8 | Cormorant + Fira Sans | Cormorant Garamond | Fira Sans |
| 9 | Playfair + Work Sans | Playfair Display | Work Sans |
| 10 | Quicksand + Nunito Sans | Quicksand | Nunito Sans |
| 11 | Comfortaa + Karla | Comfortaa | Karla |
| 12 | Baloo 2 + Poppins | Baloo 2 | Poppins |
| 13 | Exo 2 + Barlow | Exo 2 | Barlow |
| 14 | Michroma + Saira | Michroma | Saira |
| 15 | Jost + Atkinson | Jost | Atkinson Hyperlegible |
| 16 | Roboto + Fira Code | System | Fira Code |
| 17 | Montserrat + Open Sans | Montserrat | Open Sans |
| 18 | Space Grotesk + Space Mono | Space Grotesk | Space Mono |
| 19 | Plus Jakarta Sans + Inter | Plus Jakarta Sans | Inter |
| 20 | Archivo + Archivo Narrow | Archivo | Archivo Narrow |
| 21 | Roboto + Lora | System | Lora |

Pairings 16–20 set `bodyFontFeatures = "tnum"` for tabular (aligned) numerals.

All fonts are open source (Google Fonts, OFL licensed). Font files are in `res/font/` as TTF.

### 10.2 CompositionLocals

Fonts are distributed through the composable tree via two CompositionLocals:

```kotlin
val LocalDisplayFont = compositionLocalOf<FontFamily> { FontFamily.Default }
val LocalBodyFont = compositionLocalOf<FontFamily> { FontFamily.Default }
```

Any composable can read `LocalDisplayFont.current` or `LocalBodyFont.current` to use the selected fonts without passing them as parameters.

### 10.3 Font Glyph Stripping

Some bundled fonts contain Devanagari or Arabic digit glyphs. On certain devices (e.g. OnePlus), Compose finds these glyphs in the custom font but renders them incorrectly, showing western numerals instead of native script. When the glyphs are **absent**, Compose falls back to system fonts (Noto), which render correctly.

Fix: Devanagari digit codepoints (U+0966-U+096F) are stripped from Rajdhani, Baloo 2, and Poppins. Arabic digit codepoints (U+0660-U+0669) are stripped from Rubik. This is done using Python's `fontTools` library during development, not at build time.

---

## 11. Widgets

### 11.1 Overview

Two Glance-based home screen widgets:

- **4x1 (`WeatherWidget`)** -- Compact: city, temperature (dual unit), weather description, refresh time.
- **4x2 (`WeatherWidgetLarge`)** -- Expanded: adds feels-like, humidity, wind (all dual unit), plus a 3-day forecast row at the bottom (day name, weather emoji, hi/lo temperatures).

Both use a pre-composited `widget_background.png` (deep navy with Blue Marble texture) because Glance doesn't handle transparency compositing well.

### 11.2 Widget Data Flow

Widgets can't use Hilt (they run in a BroadcastReceiver context). `WidgetWeatherFetcher` is a standalone object that builds its own Retrofit client:

```
WeatherUpdateWorker (every 30 min via WorkManager)
  -> WidgetWeatherFetcher.fetch(context)
     -> Try GPS lastLocation
     -> Fall back to SharedPreferences cached coordinates
     -> Call Open-Meteo API
     -> Return WeatherData (or null on failure)
  -> WeatherWidget().updateAll(context)
  -> WeatherWidgetLarge().updateAll(context)
```

The main app saves location coordinates to `SharedPreferences("widget_prefs")` so widgets can use them even if background location access is denied.

### 11.3 Widget Metadata (`res/xml/`)

`weather_widget_info.xml` and `weather_widget_large_info.xml` define widget dimensions, resize behavior, update frequency, and initial loading layout.

---

## 12. Expert Mode

Expert Mode gates the Settings screen and lets testers override GPS location without physically moving the device. It replaced the earlier "Dev Mode" (hidden long-press activation) — activation is now an explicit toggle in Settings.

### 12.1 Activation & Lifecycle
- **Activation**: Toggle "Expert mode" on in the Settings screen (`ui/settings/SettingsScreen.kt`).
- **Expiration**: Automatically expires 12 hours after being enabled.
- **Deactivation**: Toggle it back off in Settings; this immediately clears any location override.

### 12.2 Location Overrides
While Expert Mode is active, an Edit (pencil) icon appears next to the location name, opening `LocationOverrideDialog`.
- **Search**: Supports city names, zip codes, or direct `lat,lon` input.
- **My Location**: A dedicated icon next to the search field allows quickly resetting to the real device location.
- **Persistence**: Overridden coordinates and names are stored in SharedPreferences (`dev_override_lat`, `dev_override_lon`, `dev_override_name`) and prioritized over GPS data in `WeatherViewModel`.

---

## 13. Settings & Pluggable Data Sources

`ui/settings/SettingsScreen.kt` + `SettingsViewModel.kt` present a full-screen Compose destination, gated behind Expert Mode (see [§12](#12-expert-mode)), backed by `AppSettingsRepository` (`data/settings/`) — a `@Singleton` SharedPreferences wrapper exposing a `StateFlow<AppSettings>` that both the ViewModel and Settings screen collect.

**Weather source** — `WeatherSource` enum: `OPEN_METEO` (default, no key) or `PIRATE_WEATHER` (requires an API key, entered inline with a "Test" flow that validates before saving). `WeatherSourceSelector` (`data/source/`) reads the current setting and returns the matching `WeatherDataSource` implementation (`OpenMeteoWeatherSource` / `PirateWeatherSource`); `WeatherRepositoryImpl` calls `sourceSelector.current().getForecast(...)` rather than a hardcoded API.

**Geocoding source** — `GeocodingSource` enum: `SYSTEM_GEOCODER` or `NOMINATIM`. `GeocodingSourceSelector` picks between `SystemGeocoderSource` and `NominatimGeocodingSource`. Default differs per build flavor (see [§15](#15-build-flavors)): F-Droid defaults to Nominatim, Standard defaults to the system geocoder — set via a flavor-specific `DefaultSettings` object.

**Alert toggles** — a master switch plus one per-source checkbox (see [§14](#14-weather-alerts)), all fields on the `AppSettings` data class.

None of this needs a dedicated Hilt module: `AppSettingsRepository`, the selectors, and each source implementation are plain `@Singleton @Inject constructor` classes — Hilt wires them without an explicit `@Provides`/`@Binds`.

## 14. Weather Alerts

Eight alert sources, each individually toggleable in Settings: NWS (US), GDACS (global), MeteoAlarm (Europe), JMA (Japan), ECCC (Canada), BOM (Australia), NHC (hurricanes), WMO SWIC (global). `WeatherRepositoryImpl.fetchAlerts()` gates the whole call behind the master `alertsEnabled` flag, then skips each source individually per its own toggle. `coveredByRegional` suppresses GDACS + WMO SWIC when a country-specific source already covers the location.

`WeatherAlertBanner` renders the merged, deduplicated list. Alert text is only run through string resources for `source == "derived"` entries — every other source's title/description is upstream plain text, not an i18n key.

**Cross-platform sync rule**: this file (`WeatherRepositoryImpl.kt`) is the canonical implementation. When alert parsing differs from `web/src/lib/api/alerts/`, Android wins — severity mapping, field names, source tag strings (WMO SWIC is `"wmoswic"`, no underscore), and deduplication must match on both platforms.

## 15. Build Flavors

Two flavors, differing only in location/permission plumbing — everything else (UI, domain, data sources, alerts) is shared in `src/main/`:

| | `standard` | `fdroid` |
|---|---|---|
| Location | `PlayServicesLocationProvider` (Fused Location) | `NativeLocationProvider` (`LocationManager`, GPS + network) |
| Permissions UI | Accompanist Permissions | Simplified, no Accompanist dependency |
| Default geocoding source | System Geocoder | Nominatim |
| Play Services dependency | Yes | No |
| Build command | `assembleStandardRelease` | `assembleFdroidRelease` |

Flavor-specific code (`LocationProvider`, `PermissionHandler`, `WidgetWeatherFetcher`, DI's `LocationModule`, `DefaultSettings`) lives in `app/src/standard/` and `app/src/fdroid/`. Widgets can't use the flavor's Hilt-provided location client directly since they run in a `BroadcastReceiver` context — see [§11.2](#112-widget-data-flow).

---

## 16. Build Configuration

### 16.1 Version Catalog (`gradle/libs.versions.toml`)

All dependency versions are centralized here. Dependencies are referenced in `build.gradle.kts` as `libs.something` (e.g. `libs.retrofit`, `libs.hilt.android`).

### 16.2 App Build (`app/build.gradle.kts`)

Key aspects:
- **Kotlin plugin is implicit**: AGP 9 auto-applies kotlin-android, so `id("org.jetbrains.kotlin.android")` must NOT be listed explicitly (causes an "extension already registered" conflict)
- **BuildConfig fields**: `GIT_HASH` (from `git rev-parse --short HEAD`) and `BUILD_TIME` (formatted timestamp) are injected at compile time. Footer appends `-d` suffix to the hash on debug builds via `BuildConfig.DEBUG`.
- **Build features**: Compose and BuildConfig generation enabled

### 16.3 Gradle Properties

```properties
android.disallowKotlinSourceSets=false  # Required for KSP + AGP 9 (built-in Kotlin)
org.gradle.jvmargs=-Xmx4096m -XX:+HeapDumpOnOutOfMemoryError  # Prevent OOM during DEX merging
```

---

## 17. Signing & Release

### 17.1 Debug Builds

Automatically signed with the debug keystore at `~/.android/debug.keystore`. No configuration needed.

### 17.2 Release Builds

Signing credentials are stored in two gitignored files:

- `release.keystore` -- the Java keystore file containing the private key
- `keystore.properties` -- passwords and alias:
  ```
  storeFile=release.keystore
  storePassword=...
  keyAlias=maripanatokana
  keyPassword=...
  ```

`app/build.gradle.kts` conditionally reads these and configures a `release` signing config. If the files don't exist (e.g. on CI without secrets), the release build still compiles but won't be signed.

### 17.3 Build Commands

```bash
./gradlew assembleDebug     # Debug APK (auto-signed)
./gradlew assembleRelease   # Release APK (requires keystore)
./gradlew bundleRelease     # Release AAB for Play Store
```

---

## 18. Key Design Decisions

### Why inline value classes for units?
`Temperature`, `WindSpeed`, `Pressure`, and `Precipitation` are `@JvmInline value class` types. At runtime they're just `Double` values with zero overhead, but at compile time they prevent mixing up units (you can't accidentally pass a `Pressure` where a `Temperature` is expected).

### Why two-step location?
`getLastLocation()` returns instantly from cache but may be stale. `getFreshLocation()` is accurate but takes seconds. By showing cached data first and refreshing if the user has moved significantly (>5km), the app feels instant while staying accurate.

### Why not use Locale extensions for native digits?
Android's `String.format()` with locale extensions like `"hi-u-nu-deva"` is inconsistent -- it works for `String.format()` but `SimpleDateFormat` ignores it, and `String` interpolation bypasses it entirely. The character-replacement approach (`localizeDigits()`) is simple and 100% reliable.

### Why strip font glyphs?
Certain fonts (Rajdhani, Baloo 2, Poppins, Rubik) include Devanagari or Arabic digit glyphs. On some devices (OnePlus), Compose finds these glyphs but renders them as western numerals. Removing them forces Compose to fall back to system fonts (Noto), which always render correctly.

### Why ContextWrapper instead of Activity.recreate()?
Calling `activity.recreate()` to change locale causes a visible flash (screen goes black briefly). The ContextWrapper approach swaps resources in-place via `CompositionLocalProvider`, so locale changes are seamless.

### Feature Parity (Android & Web)
The app exists as both a native Android app and a SvelteKit PWA. To ensure a consistent user experience across platforms, features and UI enhancements must be kept in sync as much as feasible. This includes:
- **Alert UI**: Headline display, issue time, source links, and numeric severity indicators.
- **I18n**: Shared string keys and localization logic.
- **Design Language**: Consistent colors, typography, and layout across the native and web implementations.

### Why a standalone widget fetcher?
Widgets run in a `BroadcastReceiver` context where Hilt isn't available. `WidgetWeatherFetcher` builds its own Retrofit instance and location client. It also falls back to SharedPreferences coordinates saved by the main app, so widgets work even without background location permission.

### Why WorkManager for widgets but not the app?
The app auto-refreshes on resume if data is >30 minutes old -- no background scheduling needed. Widgets can't detect "coming into view," so they need periodic WorkManager updates. The 30-minute interval balances freshness vs battery usage.

### Why SharedPreferences instead of DataStore?
The app stores simple key-value preferences (locale index, font index, metric preference, cached coordinates). SharedPreferences is simpler, synchronous for reads, and sufficient for this use case. All keys live in a single file: `"widget_prefs"`.

### Why edge-to-edge?
The app draws behind the status bar and navigation bar with the Blue Marble background image, creating an immersive feel. `statusBarsPadding()` and `navigationBarsPadding()` ensure content doesn't overlap system UI elements.
