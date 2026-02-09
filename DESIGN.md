# maripànaTokana - Design & Implementation Guide

**maripànaTokana** (Malagasy for "a single thermometer") is a phone-only Android weather app that shows current conditions, hourly forecasts, and a 7-day outlook. What makes it standout is that it shows both standard units at all times. 
It can be surfaced to the homescreen via one of two home screen widget options.

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
12. [Build Configuration](#12-build-configuration)
13. [Signing & Release](#13-signing--release)
14. [Key Design Decisions](#14-key-design-decisions)

---

## 1. Technology Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Language | Kotlin | 2.0.21 |
| Build system | Gradle + AGP | 9.1.0 / 9.0.0 |
| UI framework | Jetpack Compose | BOM 2024.09.00 |
| Widget framework | Glance | 1.1.1 |
| Dependency injection | Hilt | 2.59 |
| Networking | Retrofit + OkHttp | 2.11.0 / 4.12.0 |
| Serialization | Kotlinx Serialization | 1.7.3 |
| Location | Google Play Services | 21.3.0 |
| Background work | WorkManager | 2.10.0 |
| Permissions | Accompanist Permissions | 0.36.0 |
| Min SDK | Android 7.0 (API 24) | |
| Target SDK | Android 15 (API 36) | |

**Weather API:** [Open-Meteo](https://open-meteo.com) -- free, no API key required.

---

## 2. Project Structure

```
maripanaTokana/
+-- app/
|   +-- build.gradle.kts                    # App build config, signing, dependencies
|   +-- src/main/
|       +-- AndroidManifest.xml             # Permissions, activities, widget receivers
|       +-- java/orinasa/njarasoa/maripanatokana/
|       |   +-- MaripanaTokanaApp.kt        # Application class (Hilt + WorkManager init)
|       |   +-- MainActivity.kt             # Entry point, locale setup, Compose host
|       |   +-- data/
|       |   |   +-- remote/
|       |   |   |   +-- OpenMeteoApiService.kt   # Retrofit API interface
|       |   |   |   +-- OpenMeteoResponse.kt     # JSON response data classes
|       |   |   |   +-- OpenMeteoMapper.kt       # API response -> domain model
|       |   |   |   +-- WmoWeatherCode.kt        # Weather code -> emoji/description
|       |   |   +-- repository/
|       |   |       +-- WeatherRepositoryImpl.kt  # Fetches weather + geocodes city
|       |   |       +-- LocationRepositoryImpl.kt # GPS location provider
|       |   +-- domain/
|       |   |   +-- model/
|       |   |   |   +-- WeatherData.kt       # Main weather data container
|       |   |   |   +-- Temperature.kt       # Celsius/Fahrenheit value type
|       |   |   |   +-- WindSpeed.kt         # m/s and mph value type
|       |   |   |   +-- Pressure.kt          # hPa and inHg value type
|       |   |   |   +-- Precipitation.kt     # mm and inches value type
|       |   |   |   +-- HourlyForecast.kt    # Single hour forecast entry
|       |   |   |   +-- DailyForecast.kt     # Single day forecast entry
|       |   |   +-- repository/
|       |   |       +-- WeatherRepository.kt     # Interface
|       |   |       +-- LocationRepository.kt    # Interface
|       |   +-- di/
|       |   |   +-- NetworkModule.kt         # Hilt: Retrofit, OkHttp, JSON
|       |   |   +-- LocationModule.kt        # Hilt: Fused location, Geocoder
|       |   |   +-- RepositoryModule.kt      # Hilt: binds repo implementations
|       |   +-- ui/
|       |   |   +-- weather/
|       |   |   |   +-- WeatherScreen.kt     # Main UI (all composables)
|       |   |   |   +-- WeatherViewModel.kt  # State management, user actions
|       |   |   |   +-- WeatherUiState.kt    # Sealed UI state
|       |   |   +-- theme/
|       |   |       +-- AppFonts.kt          # 16 font pairings + CompositionLocals
|       |   |       +-- Theme.kt             # Material3 theme
|       |   |       +-- Color.kt             # Color definitions
|       |   |       +-- Type.kt              # Typography
|       |   +-- widget/
|       |       +-- WeatherWidget.kt             # 4x1 widget (Glance)
|       |       +-- WeatherWidgetLarge.kt        # 4x2 widget (Glance)
|       |       +-- WeatherWidgetReceiver.kt     # 4x1 broadcast receiver
|       |       +-- WeatherWidgetLargeReceiver.kt # 4x2 broadcast receiver
|       |       +-- WidgetWeatherFetcher.kt      # Standalone weather fetcher
|       |       +-- WeatherUpdateWorker.kt       # Background periodic updater
|       |       +-- theme/WidgetTheme.kt         # Widget color palette
|       +-- res/
|           +-- drawable/                    # Background images, icons
|           +-- font/                        # TTF files (font families x 2 weights)
|           +-- layout/                      # widget_loading.xml (placeholder)
|           +-- mipmap-*/                    # App launcher icons
|           +-- values/                      # strings.xml, colors.xml, themes.xml
|           +-- values-ar/strings.xml        # Arabic translations
|           +-- values-es/strings.xml        # Spanish translations
|           +-- values-fr/strings.xml        # French translations
|           +-- values-hi/strings.xml        # Hindi translations
|           +-- values-mg/strings.xml        # Malagasy translations
|           +-- values-ne/strings.xml        # Nepali translations
|           +-- values-zh/strings.xml        # Chinese translations
|           +-- xml/                         # Widget metadata, backup rules
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
    val dailyForecast: List<DailyForecast>,    // Next 7 days
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
- `wmoEmoji(code)` -- returns a Unicode emoji string

### 6.4 Weather Repository (`WeatherRepositoryImpl.kt`)

Calls the API and reverse-geocodes the city name using Android's built-in `Geocoder`. Falls back to formatted coordinates ("12.34, 56.78") if geocoding fails. Returns `Result<WeatherData>`.

### 6.5 Location Repository (`LocationRepositoryImpl.kt`)

Wraps Google's Fused Location Provider:
- `getLastLocation()` -- instant cached location
- `getFreshLocation()` -- active GPS fetch with balanced power accuracy

Both return `Result<Pair<Double, Double>>`.

---

## 7. Dependency Injection

Hilt wires everything together via three modules in the `di/` package:

### `NetworkModule.kt`
Provides singletons: `Json` config, `OkHttpClient` (with logging), `Retrofit`, `OpenMeteoApiService`.

### `LocationModule.kt`
Provides: `FusedLocationProviderClient`, `Geocoder`. Binds `LocationRepositoryImpl` to `LocationRepository` interface.

### `RepositoryModule.kt`
Binds `WeatherRepositoryImpl` to `WeatherRepository` interface.

**How it connects:** The `WeatherViewModel` constructor declares its dependencies, and Hilt automatically provides them:

```kotlin
@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val weatherRepository: WeatherRepository,   // from RepositoryModule
    private val locationRepository: LocationRepository,  // from LocationModule
    @ApplicationContext private val appContext: Context,  // built-in Hilt
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
| `fontIndex` | `Int` | Selected font pairing (0-15) |
| `localeIndex` | `Int` | Selected language (0-7) |

User actions (toggle units, cycle font, cycle language, refresh) are ViewModel methods that update state and persist preferences to `SharedPreferences("widget_prefs")`.

### 8.2 Main Screen (`WeatherScreen.kt`)

This is the largest file (~885 lines). It contains all the composable functions:

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
- **Fixed header**: City name, date, "Updated" time
- **Scrollable middle** (`.weight(1f).verticalScroll()`):
  - Hero card: weather icon, temperature, feels-like, description, precipitation, "© Orinasa Njarasoa" watermark, share button (top-left) that captures the card as PNG and opens Android share sheet via FileProvider
  - Three collapsible sections: Hourly Forecast (expanded by default), This Week, Current Conditions
- **Fixed footer**: Font icon + name (left), credits/hash (center), language flag (right)

**Key sub-composables:**
- `DualUnitText` -- Shows primary value bold + secondary value dimmer. Clickable to toggle units.
- `CollapsibleSection` -- Animated expand/collapse with arrow icon. Share button appears when expanded, captures section content as PNG via `rememberGraphicsLayer()`.
- `HourlyForecastRow` -- Horizontal scrolling row of hourly cards.
- `DailyForecastList` -- Vertical list of daily forecast rows with up/down arrows for hi/lo.
- `DetailsContent` -- Grid of detail cards (wind, pressure, humidity, UV, visibility, sunrise/sunset).
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

All user-facing text is in `res/values/strings.xml` (English) with translations in `res/values-{locale}/strings.xml`. This includes weather descriptions, UI labels, error messages, and widget text. About 73 strings and 2 string-arrays (cardinal directions, UV labels).

### 9.3 Locale Switching (Two Layers)

**Layer 1 -- Cold start** (`MainActivity.kt`):
`attachBaseContext()` reads the saved locale preference and overrides `getResources()` to return locale-specific resources. This ensures correct strings from the very first frame.

**Layer 2 -- Runtime cycling** (`WeatherScreen.kt`):
When the user taps the flag to change language, a `ContextWrapper` with the new locale is created and provided via `CompositionLocalProvider(LocalContext provides ...)`. This updates all `stringResource()` calls without recreating the Activity (which would cause a visible flash).

### 9.4 Native Digit Rendering

Hindi, Arabic, and Nepali have their own digit characters. The approach:

1. **All formatting uses `Locale.US`** -- `"%.0f".format(Locale.US, value)` always produces ASCII digits (0-9). This prevents inconsistent rendering where some APIs honor locale digit scripts and others don't.

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

## 12. Build Configuration

### 12.1 Version Catalog (`gradle/libs.versions.toml`)

All dependency versions are centralized here. Dependencies are referenced in `build.gradle.kts` as `libs.something` (e.g. `libs.retrofit`, `libs.hilt.android`).

### 12.2 App Build (`app/build.gradle.kts`)

Key aspects:
- **AGP 9 syntax**: `compileSdk { version = release(36) }` instead of `compileSdk = 36`
- **Kotlin plugin is implicit**: AGP 9 auto-applies kotlin-android, so it's not listed explicitly
- **BuildConfig fields**: `GIT_HASH` (from `git rev-parse --short HEAD`) and `BUILD_TIME` (formatted timestamp) are injected at compile time
- **Build features**: Compose and BuildConfig generation enabled

### 12.3 Gradle Properties

```properties
android.disallowKotlinSourceSets=false  # Required for Hilt 2.59 + AGP 9
```

---

## 13. Signing & Release

### 13.1 Debug Builds

Automatically signed with the debug keystore at `~/.android/debug.keystore`. No configuration needed.

### 13.2 Release Builds

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

### 13.3 Build Commands

```bash
./gradlew assembleDebug     # Debug APK (auto-signed)
./gradlew assembleRelease   # Release APK (requires keystore)
./gradlew bundleRelease     # Release AAB for Play Store
```

---

## 14. Key Design Decisions

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

### Why a standalone widget fetcher?
Widgets run in a `BroadcastReceiver` context where Hilt isn't available. `WidgetWeatherFetcher` builds its own Retrofit instance and location client. It also falls back to SharedPreferences coordinates saved by the main app, so widgets work even without background location permission.

### Why WorkManager for widgets but not the app?
The app auto-refreshes on resume if data is >30 minutes old -- no background scheduling needed. Widgets can't detect "coming into view," so they need periodic WorkManager updates. The 30-minute interval balances freshness vs battery usage.

### Why SharedPreferences instead of DataStore?
The app stores simple key-value preferences (locale index, font index, metric preference, cached coordinates). SharedPreferences is simpler, synchronous for reads, and sufficient for this use case. All keys live in a single file: `"widget_prefs"`.

### Why edge-to-edge?
The app draws behind the status bar and navigation bar with the Blue Marble background image, creating an immersive feel. `statusBarsPadding()` and `navigationBarsPadding()` ensure content doesn't overlap system UI elements.
