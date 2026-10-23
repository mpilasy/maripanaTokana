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
│   │   │   ├── data/
│   │   │   │   ├── location/
│   │   │   │   │   └── LocationProvider.kt      # Interface (flavor-implemented)
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
│   │   │   │   │   ├── WeatherRepositoryImpl.kt  # Weather, AQI, alerts & geocoding orchestrator
│   │   │   │   │   ├── LocationRepositoryImpl.kt # GPS location repository
│   │   │   │   │   └── WeatherCache.kt           # Cache of the last weather data
│   │   │   │   ├── settings/
│   │   │   │   │   └── AppSettingsRepository.kt # SharedPreferences settings repository
│   │   │   │   └── source/                      # Pluggable weather + geocoding sources
│   │   │   │       ├── WeatherDataSource.kt / WeatherSourceSelector.kt
│   │   │   │       ├── OpenMeteoWeatherSource.kt / PirateWeatherSource.kt
│   │   │   │       ├── GeocodingDataSource.kt / GeocodingSourceSelector.kt
│   │   │   │       └── SystemGeocoderSource.kt / NominatimGeocodingSource.kt
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
│   │   │   │   ├── CommonLocationModule.kt  # Hilt: Geocoder bindings
│   │   │   │   └── RepositoryModule.kt      # Hilt: Repository bindings
│   │   │   ├── ui/
│   │   │   │   ├── weather/
│   │   │   │   │   ├── WeatherScreen.kt     # Main UI host & dialog controller
│   │   │   │   │   ├── WeatherContent.kt    # Main scrollable weather layout
│   │   │   │   │   ├── WeatherViewModel.kt  # State, saved locations, refresh orchestration
│   │   │   │   │   ├── WeatherUiState.kt    # Sealed UI state
│   │   │   │   │   └── components/          # NowcastCard, ActivitiesCard, OverviewChart, WeatherIcon,
│   │   │   │   │                            # WeatherAlertBanner, SavedLocationsDialog, etc.
│   │   │   │   ├── settings/
│   │   │   │   │   ├── SettingsScreen.kt / SettingsViewModel.kt
│   │   │   │   │   └── HelpScreen.kt / AboutScreen.kt
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
│  Inline Value Classes (Temperature, AQI, etc.)  │
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
- **Saved Favorites**: Saved list of locations managed via `SavedLocationsDialog` (search, favorite heart icon ♥, reorder, delete).
- **Header Navigation**: Header shows current active location with search icon, heart favorite toggle icon, and left/right arrows or swipe gesture to cycle between Current Location and saved places.

### 4.2 Two-Step Location Strategy
When fetching weather for "Current Location":
1. `getLastLocation()` — returns instant cached GPS coordinates.
2. `getFreshLocation()` — fetches active GPS coordinates in background; if position moved >5 km, weather automatically re-fetches.

---

## 5. Domain Models

All core numeric measurement models are `@JvmInline value class` wrappers storing canonical units (Celsius, m/s, hPa, mm) and deriving display values on demand.

### `Temperature.kt`, `WindSpeed.kt`, `Pressure.kt`, `Precipitation.kt`
Prevents type mixing at compile time. `displayDual(metricPrimary)` returns a tuple of `(primary, secondary)` formatted strings.

### `AirQualityIndex.kt` & `HourlyAirQuality.kt`
- Calculates US AQI and European AQI standard tiers (Good, Moderate, Unhealthy for Sensitive Groups, Unhealthy, Very Unhealthy, Hazardous).
- Detailed pollutant concentrations: PM2.5, PM10, Nitrogen Dioxide (NO2), Ozone (O3), Sulfur Dioxide (SO2), Carbon Monoxide (CO).
- Pollen indices: Alder, Birch, Grass, Mugwort, Olive, and Ragweed pollen risk categories.
- `HourlyAirQuality`: hourly AQI forecast data. The Air quality tabs of both forecasts use it. `DailyAirQuality.kt` finds the worst AQI of each day from this data. A day with less than 6 hours of data has no value.

### `WeatherData.kt`
Holds the complete weather snapshot: current conditions, official weather alerts (from 7 sources), the hourly forecast (24 hours), the 15-minute precipitation nowcast (`minutelyForecast`), the daily forecast (10 days), the AQI index and hourly AQI forecast, the UV index, and sunrise/sunset times. `ActivityIndices.fromWeatherData()` computes the activity scores from this snapshot.

---

## 6. Data Layer (API & Repositories)

### 6.1 APIs
- **Weather Forecast (`OpenMeteoApiService.kt`)**: Requests current, hourly, 15-minute precipitation, and daily weather variables (`forecast_days=10`). The mapper keeps all 10 days. It keeps the next 24 hours of hourly data.
- **Air Quality & Pollen (`OpenMeteoAirQualityApiService.kt`)**: Requests current AQI, per-pollutant values, CAMS pollen variables, and the hourly AQI forecast (`forecast_days=5`, which feeds the 10-day forecast).
- **Reverse Geocoding**: System Geocoder (Standard flavor) or Nominatim API (F-Droid flavor). Place names are split into primary city name and region/country subtext. DMS coordinate display is available.

---

## 7. Dependency Injection

Hilt injects singletons and repositories via `di/NetworkModule.kt`, `di/CommonLocationModule.kt`, and `di/RepositoryModule.kt`. Flavor-specific modules in `app/src/standard/` and `app/src/fdroid/` bind the appropriate `LocationProvider` and `PermissionHandler`.

---

## 8. UI Layer & Free-Scroll Layout

All cards are always open. They sit in one scrolling column (`WeatherContent.kt`). The UI has no accordion and no snap paging.

### 8.1 Header Location Switcher
The top header provides instant location control:
- Location name + region subtext.
- Heart (♥) button to toggle saving the location to favorites.
- Search icon opening `SavedLocationsDialog`.
- Swipe gestures and arrow buttons to cycle between saved locations.

### 8.2 Card Order
`WeatherContent.kt` shows the cards in this order:
1. **Alert banner and hero card**: The banner shows official alerts when any exist. The hero card shows the weather icon and description, the temperature (one decimal in Celsius), the low and high, and a share button. A side column shows the rain or snow outlook. It also shows the US and EU AQI when air quality is not good. Under these, four hour tiles show the weather at +0, +4, +8, and +12 hours.
2. **Hourly forecast**: The nowcast card (`NowcastCard`) shows first, only when the source is Open-Meteo and rain or snow comes in the next 2 hours. Tabs follow: Conditions, Precipitation, Wind, Pressure, UV, and Air quality. Each tab shows a row of cards and an overview chart (`OverviewChart`). The Air quality tab shows only when air quality data exists.
3. **10-day forecast**: It has the same tabs as the hourly forecast.
4. **Current conditions**: Tiles for wind, pressure, humidity, and similar values.
5. **Sun & Air**: Tiles for UV, air quality, and sunrise and sunset.
6. **Activities** (`ActivitiesCard`): Ratings for running, laundry, and UV safety, from the current weather data.
7. **Footer**: The font name (tap to change the font), the Help and About links, the version, and the language flag (tap to change the language).

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

Numeric formatting uses `Locale.US` internally, applying character replacement at display time via `SupportedLocale.localizeDigits()` for 100% consistency.

---

## 10. Font System

Bundles 22 font pairings (system default + 21 Google Fonts pairings). Distributed through the composable tree via `LocalDisplayFont` and `LocalBodyFont` CompositionLocals. Tabular numbers (`tnum`) are enabled for numeric alignment.

---

## 11. Widgets

Two Glance home screen widgets:
- **4x1 (`WeatherWidget`)**: Compact view with city, dual-unit temperature, weather description, and refresh timestamp.
- **4x2 (`WeatherWidgetLarge`)**: Expanded view adding feels-like, humidity, wind, and a 3-day forecast row.

Updated every 30 minutes in background via `WorkManager` and `BaseWidgetWeatherFetcher` (standalone Retrofit fetcher using cached coordinates).

---

## 12. Advanced Mode

Renamed from "Expert Mode" in v1.2.15. Toggleable in Settings (`ui/settings/SettingsScreen.kt`):
- **12-Hour Session**: Automatically expires 12 hours after activation.
- **Manual Location Override**: Displays an edit icon next to the location header, enabling direct `lat,lon` or city override search, with a single-tap reset to real device GPS.
- **Pluggable Source Access**: Unlocks switching weather sources (Open-Meteo vs Pirate Weather) and individual alert provider toggles.

---

## 13. Settings & Pluggable Data Sources

Settings screen (`SettingsScreen.kt`) controls:
- **Weather Source**: Open-Meteo (default, keyless) or Pirate Weather (optional API key with inline test flow).
- **Geocoding Source**: System Geocoder vs Nominatim.
- **Alert Toggles**: Master alerts switch + individual source toggles for all 7 official providers.

---

## 14. Weather Alerts

Official alerts are fetched from 7 upstream sources: NWS (US), GDACS (global), MeteoAlarm (Europe), JMA (Japan), ECCC (Canada), BOM (Australia), and NHC (hurricanes).
- **Derived alerts removed**: Derived/algorithmic alerts were removed in v1.2.1 to prevent false alarms.
- **Multiple Alerts UI**: Multi-source alerts display merged source badges on the banner, with individual expand/collapse triggers per alert.

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

Release APKs require `release.keystore` and `keystore.properties` (gitignored). F-Droid builds are built from clean source tags and listed as `orinasa.njarasoa.maripanatokana`.

---

## 18. Key Design Decisions

- **Free-Scroll Layout**: All cards stay open in one scrolling column.
- **Tabbed Forecasts**: The hourly and 10-day forecasts each show a row of cards with an overview chart. Tabs switch the measurement.
- **Inline Value Classes**: Zero runtime allocation overhead with compile-time safety.
- **Dual Units**: Simultaneous metric & imperial display with instant global unit toggle.
- **No Derived Alerts**: Only official meteorological agencies supply alert data for maximum reliability.
