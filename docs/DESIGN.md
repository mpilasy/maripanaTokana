# maripànaTokana - Design & Implementation Guide

**maripànaTokana** (Malagasy for "a single thermometer") is a phone-only Android weather app. It shows current conditions, an hourly forecast, a 10-day forecast, and saved places. Both forecasts have the same tabs: Conditions, Precipitation, Wind, Pressure, UV, and Air quality. Each tab shows a row of cards and a trend chart. The app also has Help and About pages. Every measurement shows metric and imperial units together.

It can be surfaced to the home screen via 4x1 and 4x2 Glance home screen widgets.

There is also a web PWA version built with SvelteKit. See [`web/docs/DESIGN.md`](../web/docs/DESIGN.md) for web architecture documentation.

---

## Table of Contents

1. [Technology Stack](#1-technology-stack)
2. [Project Structure](#2-project-structure)
3. [Architecture Overview](#3-architecture-overview)
4. [App Startup & Location Flow](#4-app-startup--location-flow)
5. [Domain Models](#5-domain-models)
6. [Data Layer (API & Repositories)](#6-data-layer-api--repositories)
7. [Dependency Injection](#7-dependency-injection)
8. [UI Layer & Free-Scroll Layout](#8-ui-layer--free-scroll-layout)
9. [Internationalization (i18n)](#9-internationalization-i18n)
10. [Font System](#10-font-system)
11. [Widgets](#11-widgets)
12. [Advanced Mode](#12-advanced-mode)
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
| Language | Kotlin | 2.2.21 |
| Build system | Gradle + AGP | 9.2.1 |
| UI framework | Jetpack Compose | BOM 2026.06.01 |
| Widget framework | Glance | 1.2.0 |
| Dependency injection | Hilt | 2.60.1 |
| Networking | Retrofit + OkHttp | 2.11.0 / 4.12.0 |
| Serialization | Kotlinx Serialization | 1.7.3 |
| Location | Google Play Services (Standard) / LocationManager (F-Droid) | 21.4.0 |
| Background work | WorkManager | 2.12.0 |
| Permissions | Accompanist Permissions (Standard) | 0.37.3 |
| Min SDK | Android 7.0 (API 24) | |
| Target SDK | Android 16 (API 36) | |

**Weather API:** [Open-Meteo](https://open-meteo.com) (default, no API key) and [Pirate Weather](https://pirateweather.net) (optional, the user enters their own API key).

---

## 2. Project Structure

The tree below is abridged. It shows the main files only.

```
maripanaTokana/
├── app/
│   ├── build.gradle.kts                    # App build config, signing, dependencies
│   ├── src/main/
│   │   ├── AndroidManifest.xml             # Permissions, activities, widget receivers
│   │   ├── java/orinasa/njarasoa/maripanatokana/
│   │   │   ├── MaripanaTokanaApp.kt        # Application class (Hilt + WorkManager init)
│   │   │   ├── MainActivity.kt             # Entry point, locale setup, Compose host
│   │   │   ├── PrefsMigration.kt           # Moves sensitive settings to non-backed-up prefs
│   │   │   ├── data/
│   │   │   │   ├── location/
│   │   │   │   │   ├── LocationProvider.kt      # Interface (flavor-implemented)
│   │   │   │   │   ├── CoordinateParser.kt / PlusCode.kt # Coordinate and Plus Code parsing
│   │   │   │   │   ├── SharedLocationParser.kt  # geo: links, map links and text shared to the app
│   │   │   │   │   └── PlaceName.kt / RegionQualifier.kt / Script.kt # Place-name cleanup, Latin letters
│   │   │   │   ├── remote/
│   │   │   │   │   ├── OpenMeteoApiService.kt   # Weather Retrofit interface
│   │   │   │   │   ├── OpenMeteoAirQualityApiService.kt # AQI & Pollen Retrofit interface
│   │   │   │   │   ├── OpenMeteoResponse.kt     # Weather JSON response data classes
│   │   │   │   │   ├── OpenMeteoMapper.kt       # Weather API response -> domain model
│   │   │   │   │   ├── PirateWeatherApiService.kt / PirateWeatherMapper.kt # Pirate Weather API + mapper
│   │   │   │   │   ├── NominatimApiService.kt   # Nominatim geocoding API
│   │   │   │   │   ├── NwsApiService.kt / GdacsApiService.kt / MeteoAlarmApiService.kt
│   │   │   │   │   ├── JmaApiService.kt / EcccApiService.kt / BomApiService.kt / NhcApiService.kt # Alert APIs
│   │   │   │   │   └── WmoWeatherCode.kt        # Weather code -> description string resource
│   │   │   │   ├── repository/
│   │   │   │   │   ├── WeatherRepositoryImpl.kt  # Weather, alerts & geocoding orchestrator
│   │   │   │   │   ├── LocationRepositoryImpl.kt # GPS location repository
│   │   │   │   │   ├── CountryResolver.kt        # Country of a place, for the alert sources
│   │   │   │   │   └── WeatherCache.kt           # Last weather data, one JSON file per place (gps, saved id, preview)
│   │   │   │   ├── settings/
│   │   │   │   │   └── AppSettingsRepository.kt # SharedPreferences settings repository
│   │   │   │   └── source/                      # Pluggable weather + geocoding sources
│   │   │   │       ├── WeatherDataSource.kt / WeatherSourceSelector.kt
│   │   │   │       ├── OpenMeteoWeatherSource.kt / PirateWeatherSource.kt
│   │   │   │       ├── GeocodingDataSource.kt / GeocodingSourceSelector.kt
│   │   │   │       ├── SystemGeocoderSource.kt / NominatimGeocodingSource.kt
│   │   │   │       └── FetchErrors.kt           # Reasons shown when a refresh fails
│   │   │   ├── domain/
│   │   │   │   ├── model/
│   │   │   │   │   ├── WeatherData.kt       # Main weather data container
│   │   │   │   │   ├── Temperature.kt       # Celsius/Fahrenheit value class
│   │   │   │   │   ├── WindSpeed.kt         # m/s and mph value class
│   │   │   │   │   ├── Pressure.kt          # hPa and inHg value class
│   │   │   │   │   ├── Precipitation.kt     # mm and inches value class
│   │   │   │   │   ├── AirQualityIndex.kt   # AQI tiers, pollutant breakdown, pollen
│   │   │   │   │   ├── HourlyAirQuality.kt  # Hourly AQI forecast entry
│   │   │   │   │   ├── DailyAirQuality.kt   # Worst AQI of each day, from the hourly data
│   │   │   │   │   ├── HourlyForecast.kt    # Single hour forecast entry
│   │   │   │   │   ├── MinutelyForecast.kt  # 15-minute precipitation entry (nowcast)
│   │   │   │   │   ├── DailyForecast.kt     # Single day forecast entry
│   │   │   │   │   ├── ActivityIndices.kt   # Activity scores from the weather data
│   │   │   │   │   ├── WeatherAlert.kt      # Official weather alert
│   │   │   │   │   ├── SavedLocation.kt     # Saved place
│   │   │   │   │   └── AppSettings.kt       # User settings
│   │   │   │   └── repository/
│   │   │   │       ├── WeatherRepository.kt     # Interface
│   │   │   │       └── LocationRepository.kt    # Interface
│   │   │   ├── di/
│   │   │   │   ├── NetworkModule.kt         # Hilt: Retrofit, OkHttp, JSON
│   │   │   │   ├── CommonLocationModule.kt  # Hilt: LocationRepository binding
│   │   │   │   └── RepositoryModule.kt      # Hilt: Repository bindings
│   │   │   ├── ui/
│   │   │   │   ├── weather/
│   │   │   │   │   ├── WeatherScreen.kt     # Main UI host & dialog controller
│   │   │   │   │   ├── WeatherContent.kt    # Main scrollable weather layout
│   │   │   │   │   ├── WeatherViewModel.kt  # State, saved locations, refresh orchestration
│   │   │   │   │   ├── WeatherUiState.kt    # Sealed UI state
│   │   │   │   │   └── components/          # NowcastCard, ActivitiesCard, OverviewChart, TemperatureChart,
│   │   │   │   │                            # TileGraphics (tabs, gauges, sun arc), WeatherIcon,
│   │   │   │   │                            # WeatherAlertBanner, SavedLocationsDialog, RenameLocationDialog,
│   │   │   │   │                            # AirQualityDetailDialog, etc.
│   │   │   │   ├── settings/
│   │   │   │   │   ├── SettingsScreen.kt / SettingsViewModel.kt
│   │   │   │   │   └── HelpScreen.kt / AboutScreen.kt / DeviceInfo.kt
│   │   │   │   ├── permission/
│   │   │   │   │   └── PermissionHandler.kt # Interface (flavor-implemented)
│   │   │   │   └── theme/
│   │   │   │       ├── AppFonts.kt          # 22 font pairings
│   │   │   │       ├── Theme.kt             # Material3 theme
│   │   │   │       └── Color.kt / Type.kt
│   │   │   └── widget/
│   │   │       ├── WeatherWidget.kt             # 4x1 widget (Glance)
│   │   │       ├── WeatherWidgetLarge.kt        # 4x2 widget (Glance)
│   │   │       ├── WeatherWidgetReceiver.kt     # 4x1 receiver
│   │   │       ├── WeatherWidgetLargeReceiver.kt # 4x2 receiver
│   │   │       ├── BaseWidgetWeatherFetcher.kt  # Standalone weather fetcher (no Hilt)
│   │   │       └── WeatherUpdateWorker.kt       # Background periodic worker
│   │   └── res/
│   │       ├── drawable/ / font/ / layout/ / values/
│   │       └── values-{ar,es,fr,hi,mg,ne,zh}/strings.xml  # Generated translations
│   ├── src/standard/                        # Standard flavor (Play Services, Accompanist)
│   └── src/fdroid/                          # F-Droid flavor (Native LocationManager, no Play Services)
│                                            # Each flavor has: LocationModule, location provider,
│                                            # PermissionHandler, WidgetWeatherFetcher, DefaultSettings
├── gradle/libs.versions.toml
├── build.gradle.kts / settings.gradle.kts / gradle.properties
```

---

## 3. Architecture Overview

The app follows **MVVM** (Model-View-ViewModel) with Clean Architecture principles:

```
┌─────────────────────────────────────────────────┐
│  UI Layer                                       │
│  WeatherScreen.kt / WeatherContent.kt (Compose) │
│  WeatherWidget*.kt (Glance)                     │
└─────────────────────────────────────────────────┘
         │  observes StateFlow          ▲  calls
         ▼                              │  actions
┌─────────────────────────────────────────────────┐
│  ViewModel Layer                                │
│  WeatherViewModel.kt                            │
│  (UI state, saved locations list, refresh)      │
└─────────────────────────────────────────────────┘
         │  calls suspend functions
         ▼
┌─────────────────────────────────────────────────┐
│  Domain Layer                                   │
│  Repository interfaces                          │
│  Value classes (Temperature, Pressure, ...)      │
└─────────────────────────────────────────────────┘
         │  implemented by
         ▼
┌─────────────────────────────────────────────────┐
│  Data Layer                                     │
│  WeatherRepositoryImpl (Retrofit + Geocoders)   │
│  LocationRepositoryImpl (Fused / Native)        │
│  OpenMeteoApiService / OpenMeteoAirQualityApiService   │
└─────────────────────────────────────────────────┘
         │  wired by
         ▼
┌─────────────────────────────────────────────────┐
│  DI Layer (Hilt)                                │
│  NetworkModule, CommonLocationModule,           │
│  RepositoryModule (+ flavor LocationModule)     │
└─────────────────────────────────────────────────┘
```

Data flows strictly one way: UI observes ViewModel `StateFlow`s, ViewModel invokes repository functions, and repositories query APIs or local caches.

---

## 4. App Startup & Location Flow

### 4.1 Saved Locations & Current Location Navigation
The app supports multi-location browsing (shipped v1.2.15):
- **Current Location**: Uses GPS location provider (cached + fresh two-step fix).
- **Search and preview**: `SavedLocationsDialog` is the place search. A picked result shows as a preview for 12 hours. When no weather is on screen (for example, without location access), a picked result is saved directly.
- **Saved Favorites**: the empty heart in the header saves a preview. The full heart removes a saved place. Holding the place name renames it (`RenameLocationDialog`). There is no reorder.
- **Header Navigation**: the header actions are search, then heart and pin (only for a saved place or a preview; the pin goes back to the current location), then the settings gear. Under the header, a pager row has arrows and dots. Swipe, or tap the arrows, to cycle between Current Location and saved places. Holding an arrow opens a list of all places.

### 4.2 Two-Step Location Strategy
When fetching weather for "Current Location":
1. `getLastLocation()` — returns instant cached GPS coordinates.
2. `getFreshLocation()` — fetches active GPS coordinates in background; if position moved >5 km, or if step 1 gave nothing, weather re-fetches.

The whole fetch has a 45-second timeout. The last cached weather for the place shows at once, before the fetch.

---

## 5. Domain Models

The four measurement models (`Temperature`, `WindSpeed`, `Pressure`, `Precipitation`) are `@JvmInline value class` wrappers storing canonical units (Celsius, m/s, hPa, mm) and deriving display values on demand.

### `Temperature.kt`, `WindSpeed.kt`, `Pressure.kt`, `Precipitation.kt`
Prevents type mixing at compile time. `displayDual(metricPrimary)` returns a tuple of `(primary, secondary)` formatted strings.

### `AirQualityIndex.kt` & `HourlyAirQuality.kt`
- `AirQualityIndex` is a serializable data class. It holds the US AQI and the European AQI from Open-Meteo (`us_aqi`, `european_aqi`) and maps them to 5 tiers: Good, Moderate, Unhealthy, Very Unhealthy and Hazardous. The app does not calculate the index. The primary standard (EU or US) depends on the country.
- Detailed pollutant concentrations: PM2.5, PM10, Nitrogen Dioxide (NO2), Ozone (O3), Sulfur Dioxide (SO2), Carbon Monoxide (CO), ammonia and dust.
- Pollen indices: Alder, Birch, Grass, Mugwort, Olive, and Ragweed pollen risk categories.
- `HourlyAirQuality`: hourly AQI forecast data. The Air quality tabs of both forecasts use it. `DailyAirQuality.kt` finds the worst AQI of each day from this data. A future day with less than 6 hours of data has no value. Today needs only 1 hour.

### `WeatherData.kt`
Holds the complete weather snapshot: current conditions, official weather alerts (from 7 sources), the hourly forecast (24 hours), the 15-minute precipitation nowcast (`minutelyForecast`), the daily forecast (10 days), the AQI index and hourly AQI forecast, the UV index, and sunrise/sunset times. `ActivityIndices.fromWeatherData()` computes the activity scores from this snapshot.

---

## 6. Data Layer (API & Repositories)

### 6.1 APIs
- **Weather Forecast (`OpenMeteoApiService.kt`)**: Requests current, hourly, 15-minute precipitation, and daily weather variables (`forecast_days=10`). The mapper keeps all 10 days. It keeps the next 24 hours of hourly data.
- **Air Quality & Pollen (`OpenMeteoAirQualityApiService.kt`)**: Requests current AQI, per-pollutant values, CAMS pollen variables, and the hourly AQI forecast (`forecast_days=5`, which feeds the 10-day forecast).
- **Geocoding**: the user picks the System Geocoder or Nominatim in Settings (Advanced Mode). The default is the System Geocoder in the standard flavor and Nominatim in the F-Droid flavor. Place search uses the Open-Meteo Geocoding API first, with Nominatim as fallback. Place names are split into primary city name and region/country subtext. The location details card shows the coordinates in DMS and decimal, and the Plus Code.
- **Pirate Weather** has no air quality, so the Air quality tabs do not show with that source.

---

## 7. Dependency Injection

Hilt injects singletons and repositories via `di/NetworkModule.kt`, `di/CommonLocationModule.kt` (binds `LocationRepository`), and `di/RepositoryModule.kt`. Flavor-specific `LocationModule.kt` files in `app/src/standard/` and `app/src/fdroid/` bind the appropriate `LocationProvider`, and their `LocationBindingModule` binds the `PermissionHandler`.

---

## 8. UI Layer & Free-Scroll Layout

All cards are always open. They sit in one scrolling column (`WeatherContent.kt`). The UI has no accordion and no snap paging. The header and the pager row are fixed at the top, and the column scrolls under them with a fade at its top edge. The footer is fixed at the bottom. Sizes scale with the screen width (`LocalScale`: 1, 0.8 or 0.7).

The background is a navy gradient (`#0E0B3D` to `#1A1565`) with the Blue Marble image at 12% opacity. The cards use glass surfaces (`ui/theme/Color.kt`) on a dark Material 3 scheme.

### 8.1 Header Location Switcher
The top header provides instant location control:
- Location name + region subtext. An icon switches a name in another script to Latin letters.
- For a place in another time zone, a chip shows its local time.
- The grid icon opens the location details card: place, DMS and decimal coordinates, Plus Code. Tap a line to open a map; hold a line to copy it.
- The "Updated" line: tap it to refresh. When a refresh fails, a red line gives the reason.
- Search, heart, pin and settings actions, and the pager row (see 4.1).

### 8.2 States
- **Loading**: "Where are we?" cycles through the 8 languages, with a seconds counter.
- **Permission and error screens**: the text shows in the app language and, when different, in the phone language. They offer "Choose a place" and "Open Settings".
- **Refresh**: pull to refresh, tap the update time, or come back to the app after 30 minutes (`refreshIfStale` on resume).

### 8.3 Shared Places
The app takes `geo:` links, map links (Google, OpenStreetMap, Apple) and coordinates shared as text (`ACTION_SEND`, `SharedLocationParser`). They open as a preview.

### 8.4 Card Order
`WeatherContent.kt` shows the cards in this order:
1. **Alert banner and hero card**: The banner shows official alerts when any exist. The hero card shows the weather icon and description, the temperature (one decimal in Celsius), the low and high, and a share button. A side column shows the rain or snow outlook. It also shows the US and EU AQI when air quality is not good. Under these, four hour tiles show the weather at +0, +4, +8, and +12 hours.
2. **Hourly forecast**: The nowcast card (`NowcastCard`) shows first, only when the source is Open-Meteo and rain or snow comes in the next 2 hours. Tabs follow: Conditions, Precipitation, Wind, Pressure, UV, and Air quality. Each tab shows a row of cards and a chart: `TemperatureChart` (hourly) or `DailyTemperatureChart` (daily) for Conditions, and `OverviewChart` for the other tabs. The Air quality tab shows only when air quality data exists.
3. **10-day forecast**: It has the same tabs as the hourly forecast.
4. **Current conditions**: 6 tiles: temperature with feels-like, precipitation with cloud cover, wind with compass, gust, pressure with gauge, and humidity with dew point.
5. **Sun & Air**: sun arc (sunrise and sunset), air quality with a dial, UV with a dial, and visibility. The air-quality tile opens `AirQualityDetailDialog` (pollutants and pollen).
6. **Activities** (`ActivitiesCard`): Ratings for running, laundry, and UV safety, from the current weather data.

The **footer** is outside the scrolling column, and always left-to-right. It has the display font name (tap to change the font), the Help and About links, the version line (`v{version} • #{git hash} • {build time}`), and the language flag (tap to change the language).

Each card has a share button. The shared image is the header and the card, with a footer that has the app version and the web address (`combineBitmaps`).

## 9. Internationalization (i18n)

Supports 8 languages:
- **Index 0**: Malagasy (`mg`, default on first run)
- **Index 1**: Arabic (`ar`, RTL layout, Eastern Arabic digits `٠-٩`, Arabic decimal `٫`)
- **Index 2**: English (`en`)
- **Index 3**: Spanish (`es`)
- **Index 4**: French (`fr`)
- **Index 5**: Hindi (`hi`, Devanagari digits `०-९`)
- **Index 6**: Nepali (`ne`, Devanagari digits `०-९`)
- **Index 7**: Chinese (`zh`)

Numeric formatting uses `Locale.US` internally, applying character replacement at display time via `SupportedLocale.localizeDigits()` for 100% consistency. It also changes the decimal point to the language's separator: a comma for French, Spanish and Malagasy, and `٫` for Arabic.

The texts come from `shared/i18n/locales/*.json`. `shared/i18n/generate-android-strings.js` writes all `res/values*/strings.xml` files, including the English `values/`. The in-app language switch uses a localized `ContextWrapper` (`WeatherScreen.kt`) and `MainActivity.attachBaseContext`.

---

## 10. Font System

Bundles 22 font pairings (system default + 21 Google Fonts pairings). Distributed through the composable tree via `LocalDisplayFont` and `LocalBodyFont` CompositionLocals. Tabular numbers (`tnum`) are enabled for 5 pairings (indexes 16 to 20).

---

## 11. Widgets

Two Glance home screen widgets:
- **4x1 (`WeatherWidget`)**: Compact view with city, dual-unit temperature, weather description, and refresh timestamp.
- **4x2 (`WeatherWidgetLarge`)**: Expanded view adding feels-like, humidity, wind, and a 3-day forecast row.

Updated every 30 minutes in background via `WorkManager` and `BaseWidgetWeatherFetcher` (standalone Retrofit fetcher, no Hilt). The flavor `WidgetWeatherFetcher` tries a fresh location fix first, then the saved coordinates. The widgets always use Open-Meteo and the device location, not the active favorite or the chosen weather source. Tapping the widget opens the app; tapping the time refreshes it (`RefreshActionCallback`).

The widgets have an alert badge, but the widget data has no alerts (`alerts = emptyList()`), so the badge does not show.

---

## 12. Advanced Mode

Renamed from "Expert Mode" in v1.2.15. A switch in Settings (`ui/settings/SettingsScreen.kt`), saved as `settings_advanced_mode`. It stays on until the user turns it off. It:
- Shows the Weather source, Alerts and Location (geocoder) sections in Settings.
- Shows the alert sources that failed, on the main screen.
- When turned off, clears a place preview and fetches the weather again.

Search and place previews do not need Advanced Mode. The 12-hour limit applies to a preview.

---

## 13. Settings & Pluggable Data Sources

Settings screen (`SettingsScreen.kt`) always shows the Advanced mode switch and an About row. With Advanced Mode on, it also controls:
- **Weather Source**: Open-Meteo (default, keyless) or Pirate Weather (optional API key with inline test flow).
- **Geocoding Source**: System Geocoder vs Nominatim.
- **Alert Toggles**: Master alerts switch + individual source toggles for all 7 official providers.

Settings, Help and About share the `SettingsPage` layout. Help has three sections (Places and favorites; Language, font and units; Weather data). About has the license, the source code and web links, the credits for data, imagery, fonts and libraries, and the device information (`DeviceInfo.kt`).

---

## 14. Weather Alerts

Official alerts are fetched from 7 upstream sources: NWS (US), GDACS (global), MeteoAlarm (Europe), JMA (Japan), ECCC (Canada), BOM (Australia), and NHC (hurricanes).
- **Derived alerts removed**: Derived/algorithmic alerts were removed in v1.2.1 to prevent false alarms.
- **Multiple Alerts UI**: Multi-source alerts display merged source badges on the banner, with individual expand/collapse triggers per alert. The banner is collapsed by default: orange for a watch, red for a warning or emergency. Source badges link to the alert.
- **Coverage**: each source is used only for its countries (with bounding boxes as fallback). GDACS is skipped where a regional source covers the place.
- **Loading**: alerts load after the weather shows. Results for other coordinates are dropped.

---

## 15. Build Flavors

| | `standard` | `fdroid` |
|---|---|---|
| Location Provider | `PlayServicesLocationProvider` | `NativeLocationProvider` |
| Permissions UI | Accompanist Permissions | Simplified native dialogs |
| Default Geocoder | System Geocoder | Nominatim |
| Play Services Blob | Included | **Excluded** |
| Build command | `./gradlew assembleStandardRelease` | `./gradlew assembleFdroidRelease` |

---

## 16. Build Configuration

Centralized in `gradle/libs.versions.toml`. Uses `jvmToolchain(21)` in `app/build.gradle.kts` for vendor-agnostic JDK 21 compatibility on F-Droid CI. `auto-provisioning=disabled` remains set in `gradle.properties`.

---

## 17. Signing & Release

Release APKs are signed when `keystore.properties` exists or the `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD` environment variables are set. Otherwise they are unsigned. The keystore and `keystore.properties` are gitignored. F-Droid builds are built from clean source tags and listed as `orinasa.njarasoa.maripanatokana`.

---

## 18. Key Design Decisions

- **Free-Scroll Layout**: All cards stay open in one scrolling column.
- **Tabbed Forecasts**: The hourly and 10-day forecasts each show a row of cards with an overview chart. Tabs switch the measurement.
- **Inline Value Classes**: Zero runtime allocation overhead with compile-time safety.
- **Dual Units**: Simultaneous metric & imperial display with instant global unit toggle.
- **No Derived Alerts**: Only official meteorological agencies supply alert data for maximum reliability.
