# maripànaTokana - Design & Implementation Guide

**maripànaTokana** (Malagasy for "a single thermometer") is an Android weather app for phones and for Android TV. One APK has both: the phone screens start from `MainActivity`, and the TV screens start from `TvActivity` (a TV launcher starts it). Wear OS and Android Auto are not supported.

The phone app shows current conditions, an hourly forecast, a 10-day forecast, the sun, moon and sea, and saved places. Both forecasts have the same tabs (Conditions, Heat index and Wind chill when they apply, Precipitation, Wind, Pressure, Humidity, UV, and Air quality). Each tab shows a row of cards and a trend chart. A chart view shows up to 240 hours, or the days, in stacked panels. The app also has Help and About pages. Every measurement shows metric and imperial units together.

The phone app also has two Glance home-screen widgets, 4x1 and 4x2.

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
| Language | Kotlin | 2.4.21 |
| Build system | Gradle 9.8.2, AGP 9.4.1 | |
| UI framework | Jetpack Compose | BOM 2026.09.00 |
| Widget framework | Glance | 1.2.0 |
| Dependency injection | Hilt | 2.60.1 |
| Networking | Retrofit + OkHttp | 2.11.0 / 4.12.0 |
| Serialization | Kotlinx Serialization | 1.12.0 |
| Location | Google Play Services (Standard) / LocationManager (F-Droid) | 21.4.0 |
| Background work | WorkManager | 2.12.0 |
| Permissions | Accompanist Permissions (Standard) | 0.37.3 |
| Min SDK | Android 7.0 (API 24) | |
| Compile SDK | API 37 | |
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
│   │   │   │   │   ├── RateLimitedNominatimApiService.kt # 1 request per second, reverse results kept
│   │   │   │   │   ├── NwsApiService.kt / GdacsApiService.kt / MeteoAlarmApiService.kt
│   │   │   │   │   ├── JmaApiService.kt / EcccApiService.kt / BomApiService.kt / NhcApiService.kt # Alert APIs
│   │   │   │   │   └── WmoWeatherCode.kt        # Weather code -> description string resource
│   │   │   │   ├── repository/
│   │   │   │   │   ├── WeatherRepositoryImpl.kt  # Weather, alerts & geocoding orchestrator
│   │   │   │   │   ├── LocationRepositoryImpl.kt # GPS location repository
│   │   │   │   │   ├── CountryResolver.kt        # Country of a place, for the alert sources
│   │   │   │   │   ├── ExpiringValue.kt          # One value kept for a time (GDACS feed)
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
│   │   │   │   │   ├── ChartHour.kt         # One hour of the chart view (up to 240 hours)
│   │   │   │   │   ├── ThermalIndices.kt    # Heat index and wind chill
│   │   │   │   │   ├── Normals.kt / ForecastChanges.kt # Normal highs and lows; what changed since yesterday
│   │   │   │   │   ├── Moon.kt / Seasons.kt # Moon phase and times; solstices and equinoxes, declination
│   │   │   │   │   ├── SeaConditions.kt     # Waves, swell, sea temperature and tides
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
│   │   │   │   ├── mobile/                  # Phone screens only
│   │   │   │   │   ├── weather/
│   │   │   │   │   │   ├── WeatherScreen.kt     # Main UI host & dialog controller
│   │   │   │   │   │   ├── WeatherContent.kt    # Main scrollable weather layout
│   │   │   │   │   │   ├── WeatherViewModel.kt  # State, saved locations, refresh orchestration
│   │   │   │   │   │   ├── WeatherUiState.kt    # Sealed UI state
│   │   │   │   │   │   ├── chart/               # ChartViewScreen (chart view)
│   │   │   │   │   │   └── components/          # NowcastCard, ActivitiesCard, OverviewChart, TemperatureChart,
│   │   │   │   │   │                            # DailyTemperatureChart, TileGraphics (tabs, gauges, sun arc),
│   │   │   │   │   │                            # tier badges, WeatherAlertBanner, SavedLocationsDialog,
│   │   │   │   │   │                            # RenameLocationDialog, AirQualityDetailDialog, etc.
│   │   │   │   │   ├── settings/
│   │   │   │   │   │   ├── SettingsScreen.kt / SettingsViewModel.kt
│   │   │   │   │   │   └── HelpScreen.kt / AboutScreen.kt
│   │   │   │   │   └── permission/
│   │   │   │   │       └── PermissionHandler.kt # Interface (flavor-implemented)
│   │   │   │   ├── common/                  # UI code that the phone and the TV share
│   │   │   │   │   ├── Locales.kt           # SupportedLocale, supportedLocales
│   │   │   │   │   ├── Format.kt            # latitudeText
│   │   │   │   │   ├── components/          # WeatherIcon, MoonPhaseIcon, TierColors (colorsFor), LocalizedDialog
│   │   │   │   │   └── about/               # Credits, AppQrCodes, DeviceInfo
│   │   │   │   ├── chart/                   # ChartViewMath (chart math), ChartStyle (colours): phone and TV charts
│   │   │   │   ├── tv/
│   │   │   │   │   ├── TvActivity.kt        # TV entry point and home screen (D-pad cards)
│   │   │   │   │   ├── TvChart.kt           # Hourly and daily charts
│   │   │   │   │   ├── TvDetails.kt         # Details, air quality and alerts screens
│   │   │   │   │   ├── TvPlaces.kt          # Search, device location and saved places
│   │   │   │   │   ├── TvSettings.kt        # Units, language, Help and About
│   │   │   │   │   └── TvDream.kt           # Screensaver (DreamService)
│   │   │   │   └── theme/
│   │   │   │       ├── AppFonts.kt          # 22 font pairings
│   │   │   │       ├── Theme.kt             # Material3 theme
│   │   │   │       └── Color.kt / Type.kt
│   │   │   └── widget/
│   │   │       ├── WeatherWidget.kt             # 4x1 widget (Glance)
│   │   │       ├── WeatherWidgetLarge.kt        # 4x2 widget (Glance)
│   │   │       ├── WeatherWidgetReceiver.kt     # 4x1 receiver
│   │   │       ├── WeatherWidgetLargeReceiver.kt # 4x2 receiver
│   │   │       ├── BaseWidgetWeatherFetcher.kt  # Standalone forecast fetcher; alerts via Hilt entry point
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
│  ui/tv/* (Compose for TV), WeatherWidget*.kt    │
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

Both return a `LocationFix` (coordinates and the fix time). A fix is old when it is more than 30 minutes old, or when the fresh request failed and the provider used a cached fix (`fallback`). When the shown weather is for the device location and its fix is old, the header shows a small line under the place: "Using last known location · 14:05" (the fix time, in the format of the "Updated" line). The TV home shows the same line under the place name. Saved places and previews do not show it.

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
- **Sea (`OpenMeteoMarineApiService.kt`)**: waves, swell, sea temperature and the sea level for the tides. An inland place gets no sea data, and the Sea card does not show.
- **Normals (`OpenMeteoArchiveApiService.kt`)**: past weather (ERA5) for the normal highs and lows. `NormalsCache.kt` keeps them for each place for 30 days. `ForecastSnapshotStore.kt` keeps old forecasts to find what changed since yesterday.
- **Geocoding**: the user picks the System Geocoder or Nominatim in Settings (Advanced Mode). The default is the System Geocoder in the standard flavor and Nominatim in the F-Droid flavor. Place search uses the Open-Meteo Geocoding API first, with Nominatim as fallback. Place names are split into primary city name and region/country subtext. The location details card shows the coordinates in DMS and decimal, and the Plus Code.
- **Pirate Weather** has no air quality, so the Air quality tabs do not show with that source.
- **Network**: all services share one OkHttp client with a 10 MB HTTP cache (`cacheDir/http`) and a 40-second call timeout. The forecast and the place name load at the same time.
- **Nominatim policy**: `RateLimitedNominatimApiService` sends one request at a time, at most one per second, and keeps the last 32 reverse results by coordinates and language. One refresh asks for the same place up to 3 times (name, Latin-letter name, country for alerts).

---

## 7. Dependency Injection

Hilt injects singletons and repositories via `di/NetworkModule.kt`, `di/CommonLocationModule.kt` (binds `LocationRepository`), and `di/RepositoryModule.kt`. Flavor-specific `LocationModule.kt` files in `app/src/standard/` and `app/src/fdroid/` bind the appropriate `LocationProvider`, and their `LocationBindingModule` binds the `PermissionHandler`.

---

## 8. UI Layer & Free-Scroll Layout

This section describes the phone screens. The chart view is in 8.5 and the TV screens are in 8.6.

All cards are always open. They sit in one scrolling column (`WeatherContent.kt`). The UI has no accordion and no snap paging. The header and the pager row are fixed at the top, and the column scrolls under them with a fade at its top edge. The footer is fixed at the bottom. Sizes scale with the screen width (`LocalScale`: 1, 0.8 or 0.7).

The background is a navy gradient (`#0E0B3D` to `#1A1565`) with the Blue Marble image at 12% opacity. The cards use glass surfaces (`ui/theme/Color.kt`) on a dark Material 3 scheme.

### 8.1 Header Location Switcher
The top header provides instant location control:
- Location name + region subtext. An icon switches a name in another script to Latin letters.
- For a place in another time zone, a chip shows its local time.
- The grid icon opens the location details card: place, DMS and decimal coordinates, Plus Code. Tap a line to open a map; hold a line to copy it.
- The "Updated" line: tap it to refresh. When a refresh fails, a red line gives the reason.
- Search, heart, pin and settings actions, and the pager row (see 4.1).
- After a few seconds without scrolling, the header folds to the name line (`IdleChrome`). Its actions are then refresh, and search (at the current location) or the pin (at a saved place or a preview); settings is not there. A scroll opens the header again.

### 8.2 States
- **Loading**: "Where are we?" cycles through the 8 languages, with a seconds counter.
- **Permission and error screens**: the text shows in the app language and, when different, in the phone language. They offer "Choose a place" and "Open Settings".
- **Refresh**: pull to refresh, tap the update time, or come back to the app after 30 minutes (`refreshIfStale` on resume).

### 8.3 Shared Places
The app takes `geo:` links, map links (Google, OpenStreetMap, Apple) and coordinates shared as text (`ACTION_SEND`, `SharedLocationParser`). They open as a preview.

### 8.4 Card Order
`WeatherContent.kt` shows the cards in this order:
1. **Alert banner and hero card**: The banner shows official alerts when any exist. The hero card shows the weather icon and description, the temperature (one decimal in Celsius), the low and high, and a share button. A side column shows the rain or snow outlook (`PrecipOutlook`, web `precipOutlook`): rain or snow now comes from the nowcast value that covers the current time, as on the nowcast card; a start or stop inside the nowcast has its time to the minute; past the nowcast, the hourly weather codes decide. It also shows the US and EU AQI when air quality is not good. Under these, four hour tiles show the weather at +0, +4, +8, and +12 hours.
2. **Hourly forecast**: The title row has a button that opens the chart view (8.5). The nowcast card (`NowcastCard`) shows first, only when the source is Open-Meteo and rain or snow comes in the next 2 hours. Tabs follow (`ForecastTabs`): Conditions, Heat index and Wind chill (each only when an hour has it), Precipitation, Wind, Pressure, Humidity, UV, and Air quality. Each tab shows a row of cards that scrolls sideways, with a chart that scrolls with it (`ChartScrub`): `TemperatureChart` (hourly) or `DailyTemperatureChart` (daily) for Conditions, and `OverviewChart` for the other tabs. The Air quality tab shows only when air quality data exists. The hourly list has the next 24 hours.
3. **10-day forecast**: The title row has a button that opens the chart view in the daily mode (8.5). It has the same tabs as the hourly forecast. A block on top (`ForecastChangesBlock`) lists up to 4 changes since yesterday's forecast. The cards and the daily temperature chart show the normal highs and lows.
4. **Current conditions**: 6 tiles: temperature with feels-like, precipitation with cloud cover, wind with compass, gust, pressure with gauge, and humidity with dew point.
5. **Sun & Air**: the sun tile (arc with the length of the day, sunrise and sunset, and the noon sun from overhead today and in a year), the moon tile (arc, phase, rise and set, next new or full moon), the declination tile (dial from 23° S to 23° N, days to the next solstice or equinox), air quality with a dial, UV with a dial, cloud cover, and visibility. The air-quality tile opens `AirQualityDetailDialog` (pollutants and pollen).
6. **Sea** (only when the place has sea data): waves, swell, sea temperature, and the next high and low tides, with a note that the values are estimates.
7. **Activities** (`ActivitiesCard`): Ratings for running, laundry, UV safety, skin comfort, car washing and driving (`ActivityIndices`, web `activityIndices.ts`). Skin comfort: the dew point (below 5, -2 and -10 °C) or a humidity below 35 or 25 %. Car washing: rain or snow now, frost, or a wet day (50 % or 1 mm) today or tomorrow is poor; a wet day after tomorrow, or 30 % tomorrow, is fair. Driving: from the WMO code (freezing rain, heavy snow or hail; snow, fog or heavy rain; light rain), the visibility (200 m, 1 km, 5 km), the gusts (25, 17, 12 m/s) and rain at 1 °C or less. The other ratings use the current weather.

The hero card also shows the largest change of the temperature in the next 6 hours, when it is over 5 °C (9 °F) whatever unit shows first (`TemperatureTrend`), and, when the high differs from the 10-year normal by 2 °C or more, today's high against the normal (`HighVsNormal`). The four hour tiles show the temperature in colour and the chance of rain.

### 8.5 Chart View
`ui/mobile/weather/chart/ChartViewScreen.kt`. The chart button in the hourly forecast card opens it in the hourly mode, and the one in the 10-day forecast card opens it in the daily mode (`ChartViewMode`); Back closes it. A switch at the top (Hourly | Daily) changes the mode. The hourly mode shows up to 240 hours (10 days) in stacked panels that scroll sideways together, and opens at the current hour. The daily mode shows one point for each day of the daily forecast in the same panels, all in the width: the high and low, the daily totals, highest and mean values, and the highest hourly gust and air quality of each day (`chartDays`, `chartDayMaxIndex`); a tap selects a day. The panels, from top to bottom, are a time axis with weather icons, temperature (with dew point, heat index and wind chill), precipitation (with the chance of rain), wind (with gusts and direction arrows), pressure, humidity, and UV index with the air quality as a dashed line on its own scale (only when an hour has a UV index or air quality). A card at the top shows the values of the selected hour or day in both units; a tap changes the units. One finger scrolls the hours, and a tap moves the hour under it to the selection line. The data comes from `WeatherData.chartHourly` (`ChartHour`). `ui/chart/ChartStyle.kt` holds the colours and line styles that the main-page charts and the TV charts also use. The web has the same view in `ChartView.svelte`.

### 8.6 Android TV
`TvActivity` (declared in the manifest with the `LEANBACK_LAUNCHER` category and no touchscreen requirement) shows the weather of one place with screens made for the D-pad. A card has a white border when it has focus; OK opens it; Back returns to the card that had focus. The TV screens use the phone's data layer and preferences (saved places, units, language) but not `WeatherContent`.
- **Home** (`TvActivity.kt`): a left panel with the place (and "Using last known location · time" when the device fix is old), the worst alert, the weather icon, temperature, feels-like, the time and the update time, an air-quality button, and Details, Settings and About buttons. On the right, one row of hourly cards and one row of daily cards. OK on an hour or a day opens its chart. The forecast times are the place's. The update time and the fix time are the TV's own, as on the phone. For a place in another time zone (`isRemoteTimezone`), the panel shows the place's time with a clock (with the weekday when the date differs), a TV icon marks the TV's own times, and the hourly cards and the screensaver hours also show the TV's time.
- **Charts** (`TvChart.kt`): the hourly chart and the daily chart. Left and right move the selected hour or day; up and down move a whole day in the hourly chart; OK shows the next value. Related values share one plot: temperature with dew point, heat index and wind chill (the daily chart also has the low); precipitation with the chance of rain; wind with gusts; UV index with the air quality as a dashed line on its own scale; the humidity; and, in the hourly chart, the pressure. The cards above the plot show the values of the selected hour or day in both units.
- **Details** (`TvDetails.kt`): the place with the time and the update time, as on the home panel. A first row of two wide tiles: the temperature (with the heat index or wind chill when one applies, as on the phone) and the precipitation (with the chance of precipitation in the next hour). Then two rows of six: wind, gust, pressure, humidity and dew point, UV, visibility; air quality, sunrise, sunset, moon, the noon sun, and the declination. There is no cloud cover tile: it did not fit. The same file has the air-quality screen (both standards, pollutants, pollen) and the alerts screen (full text).
- **Places** (`TvPlaces.kt`): search, device location and the saved places, with the same storage as the phone.
- **Settings** (`TvSettings.kt`): units, language, Help, and About. The weather source choice and the Advanced Mode options are not on the TV.
- **Screensaver** (`TvDream.kt`): a `DreamService` that shows the time, the weather of the TV app's place, the worst alert and the next hours. The content drifts to avoid screen burn-in. Phones can choose it too.

The **footer** is outside the scrolling column, and always left-to-right. It has the display font name (tap to change the font), the Help and About links, the version line (`v{version} • #{git hash} • {build time}`), and the language flag (tap to change the language).

Each card has a share button. The shared image is the header and the card, with a footer that has the app version and the web address (`combineBitmaps`).

## 9. Internationalization (i18n)

Supports 8 languages:
- **Index 0**: Malagasy (`mg`)
- **Index 1**: Arabic (`ar`, RTL layout, Eastern Arabic digits `٠-٩`, Arabic decimal `٫`)
- **Index 2**: English (`en`)
- **Index 3**: Spanish (`es`)
- **Index 4**: French (`fr`)
- **Index 5**: Hindi (`hi`, Devanagari digits `०-९`)
- **Index 6**: Nepali (`ne`, Devanagari digits `०-९`)
- **Index 7**: Chinese (`zh`)

On the first start after install, `DefaultLanguage.kt` selects the first device language that the app has, else English. An app that was installed before keeps its language (Malagasy if the user never changed it).

Numeric formatting uses `Locale.US` internally, applying character replacement at display time via `SupportedLocale.localizeDigits()` for 100% consistency. It also changes the decimal point to the language's separator: a comma for French, Spanish and Malagasy, and `٫` for Arabic.

Times follow the 24-hour or 12-hour clock of the device (`DateFormat.is24HourFormat`, whose default comes from the device region) or, on the web, of the browser region (`Intl` `hourCycle`). The helpers are `ui/common/ClockFormat.kt` (`uses24HourClock`, `clockPattern`, `hourLabelPattern`, `clockFormat`) and `web/src/lib/utils/date.ts` (`uses24HourClock`, `formatTimeAtLocation`, `formatHourAtLocation`). A time with minutes is "21:05" or "9:05 PM"; a full-hour label is "21:00" or "9 PM" (a place or device at a :30 or :45 offset keeps the minutes). The AM/PM marker is in the app language, without dots, because the digit helper changes dots to commas.

The texts come from `shared/i18n/locales/*.json`. `shared/i18n/generate-android-strings.js` writes all `res/values*/strings.xml` files, including the English `values/`. The in-app language switch uses a localized `ContextWrapper` (`WeatherScreen.kt`) and `MainActivity.attachBaseContext`.

---

## 10. Font System

Bundles 22 font pairings (system default + 21 Google Fonts pairings). Distributed through the composable tree via `LocalDisplayFont` and `LocalBodyFont` CompositionLocals. Tabular numbers (`tnum`) are enabled for 5 pairings (indexes 16 to 20).

---

## 11. Widgets

Two Glance home screen widgets:
- **4x1 (`WeatherWidget`)**: Compact view with city, dual-unit temperature, weather description, and refresh timestamp.
- **4x2 (`WeatherWidgetLarge`)**: Expanded view adding feels-like, humidity, wind, and a 3-day forecast row.

Updated every 30 minutes in background via `WorkManager` and `BaseWidgetWeatherFetcher` (standalone Retrofit fetcher, no Hilt). The same fetch loads the active alerts with the app's `WeatherRepository.fetchAlerts` (through the `WidgetEntryPoint` Hilt entry point, so the alert settings apply), and the widgets show their count as a badge. The flavor `WidgetWeatherFetcher` tries a fresh location fix first, then the saved coordinates. The widgets always use Open-Meteo and the device location, not the active favorite or the chosen weather source. Tapping the widget opens the app; tapping the time refreshes it: `RefreshActionCallback` queues a one-time `WeatherUpdateWorker` job, because the fetch (a location fix can take 20 s) is longer than a broadcast may run. The widget shows the place name that the app saved with its last GPS position only when the widget position is within about 5 km of it; else it looks up the name. The widget cache (`WidgetCache.kt`) also keeps the alerts.

---

## 12. Advanced Mode

Renamed from "Expert Mode" in v1.2.15. A switch in Settings (`ui/mobile/settings/SettingsScreen.kt`), saved as `settings_advanced_mode`. It stays on until the user turns it off. It:
- Shows the Weather source, Alerts and Location (geocoder) sections in Settings.
- Shows the alert sources that failed, on the main screen.
- When turned off, clears a place preview and fetches the weather again.

Search and place previews do not need Advanced Mode. The 12-hour limit applies to a preview.

---

## 13. Settings & Pluggable Data Sources

Settings screen (`SettingsScreen.kt`) always shows the Advanced mode switch and an About row. With Advanced Mode on, it also controls:
- **Weather Source**: Open-Meteo (default, keyless) or Pirate Weather (optional API key with inline test flow).
- **Weather Model** (Open-Meteo only): Automatic (default) or one model from `WEATHER_MODELS` in `WeatherModel.kt` (ECMWF IFS, ECMWF AIFS, GFS, ICON, Météo-France, UK Met Office, GEM, JMA). Each model has its number of forecast days.
- **Geocoding Source**: System Geocoder vs Nominatim.
- **Alert Toggles**: Master alerts switch + individual source toggles for all 7 official providers.

Settings, Help and About share the `SettingsPage` layout. Help has three sections (Places and favorites; Language, font and units; Weather data). About has the version, the hash and the UTC commit time (`BuildConfig.COMMIT_TIME`; on the web `__GIT_HASH__` and `__COMMIT_TIME__` from `vite.config.ts`), the license, the Help and Licenses links, the source code and web links, the F-Droid and Obtainium badges (a tap shows the QR code, for another phone to scan) and the GitHub releases badge (a tap opens it), and the device information (`DeviceInfo.kt`). The Licenses page (`LicensesScreen`) has the credits for data, imagery, fonts and libraries; a tap on a credit shows its licence text in the app (Android `R.raw`, web `static/licenses/`), or opens its licence page. The TV About is one list of full-width rows (as in the Fijerena app): the app with its version and build line, the copyright and licence, the four sharing links as buttons side by side (OK shows the QR code; F-Droid and Obtainium with their badge), this device (label and value in two columns), and a Licenses row last. The TV Licenses page has one row for each credit under its section; OK on a credit with a bundled text opens it, one card per paragraph so Up and Down scroll through it. Back returns focus to the row it came from.

---

## 14. Weather Alerts

Official alerts are fetched from 7 upstream sources: NWS (US), GDACS (global), MeteoAlarm (Europe), JMA (Japan), ECCC (Canada), BOM (Australia), and NHC (hurricanes).
- **Derived alerts removed**: Derived/algorithmic alerts were removed in v1.2.1 to prevent false alarms.
- **Multiple Alerts UI**: Multi-source alerts display merged source badges on the banner, with individual expand/collapse triggers per alert. The banner is collapsed by default: orange for a watch, red for a warning or emergency. Source badges link to the alert.
- **Coverage**: each source is used only for its countries (with bounding boxes as fallback). GDACS is skipped where a regional source covers the place. The GDACS feed lists events for the whole world, so one download is kept for 10 minutes.
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
- **Tabbed Forecasts**: The hourly and 10-day forecasts each show a row of cards with an overview chart. Tabs switch the measurement. The chart view shows all measurements at once.
- **One APK for phone and TV**: The TV screens are separate Compose screens in `ui/tv/`, with their own entry activity, and they share the data layer.
- **Inline Value Classes**: Zero runtime allocation overhead with compile-time safety.
- **Dual Units**: Simultaneous metric & imperial display with instant global unit toggle.
- **No Derived Alerts**: Only official meteorological agencies supply alert data for maximum reliability.
