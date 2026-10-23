# maripànaTokana

**maripànaTokana** (Malagasy for "a single thermometer") is a weather application available for Android and as a [PWA (Web)](./web). A hosted instance of the Web version is available at **[maripana.njarasoa.org](https://maripana.njarasoa.org)** — no install needed. It shows current conditions, hourly forecasts, 7-day forecasts with trend charts, air quality and UV forecast trends, and multi-location favorites. It always displays both metric and imperial units side by side, and supports 8 languages with 22 font pairings.

## Features

- **Cross-Platform Parity**: Features on the Android and Web versions are kept in sync to provide a consistent experience.
- **Multi-Location Favorites**: Header location search, favorite locations list (♥), and left/right swipe or arrow navigation between saved places.
- **Air Quality & UV Forecasts**: 48-hour AQI trend line chart with colored tier dots and 7-day UV trend line chart with severity tier badges.
- **7-Day Forecast Trend Chart**: Horizontal scrolling daily forecast cards paired with a matching 7-day temperature trend line chart.
- **Advanced Mode**: Toggle it on in Settings to unlock a 12-hour session with advanced options.
  - **Location Override**: Search for any city or enter specific coordinates to test weather in other regions.
  - **Quick Reset**: Toggle Advanced Mode off in Settings to immediately clear overrides and return to your actual location.
  - **Search History**: Recent overrides are cached for quick switching.
- **Enhanced Location Display**:
  - **Two-line Header**: Shows the city/locality on the first line and the region/country on a discreet second line.
  - **Smart Parsing**: Automatically cleans location names (e.g., "Paris" instead of "Paris, France") while preserving essential locality names.
  - **DMS Coordinates**: Tap the location to toggle GPS coordinates displayed in Degrees, Minutes, and Seconds (DMS) format across two lines.
- Real-time weather data from [Open-Meteo](https://open-meteo.com) (default, no key) or [Pirate Weather](https://pirateweather.net) (optional, API key)
- **Settings screen**: pluggable weather source, API key validation, per-source alert toggles, geocoding source
- **7 alert sources**: NWS (US), GDACS (global), MeteoAlarm (Europe), JMA (Japan), ECCC (Canada), BOM (Australia), NHC (hurricanes) — each individually toggleable
- GPS location with two-step strategy (instant cached + fresh background)
- **Dual-unit display**: every measurement shows both metric and imperial simultaneously
- **Tap to toggle**: tap any value to swap which unit is primary (bold/large) vs secondary (dimmer)
- **Single-card accordion**: opening one card collapses any other open card; switching locations collapses all cards
- **8 languages**: Malagasy, Arabic, English, Spanish, French, Hindi, Nepali, Chinese — cycled via flag button in footer
- **22 font pairings** including Roboto + Lora: cycled via font icon in footer
- Native digit rendering for Hindi, Arabic, and Nepali
- RTL support (Arabic)
- Two home screen widgets (4x1 compact, 4x2 with 3-day forecast)
- Auto-refresh on resume (if data >30 min old)
- Pull-to-refresh
- Edge-to-edge Blue Marble background
- Detailed weather information:
  - Temperature with 1 decimal on hero card (current, feels like, min/max)
  - Share button on hero card (captures card as PNG, shares via Android share sheet)
  - Pressure (hPa / inHg)
  - Humidity (%) with dew point (°C / °F)
  - Wind speed and direction with cardinal compass (m/s / mph)
  - Wind gusts (when available)
  - UV index
  - Precipitation (rain/snow in mm / inches)
  - Visibility (km / mi)
  - Sunrise/sunset times

## Platform Support

**Phone Only** — designed exclusively for Android phones with touchscreens. NOT compatible with Android TV, Wear OS, Android Auto, or tablets.

## Technical Stack

| Component | Version |
|-----------|---------|
| AGP | 9.2.1 |
| Kotlin | 2.2.10 |
| Compose BOM | 2024.09.00 |
| Glance | 1.1.1 |
| Hilt | 2.59 |
| Retrofit | 2.11.0 |
| Kotlinx Serialization | 1.7.3 |
| Play Services Location | 21.3.0 |
| WorkManager | 2.10.0 |
| Min SDK | 24 (Android 7.0) |
| Target SDK | 36 |

## Distribution

**F-Droid**: Download from F-Droid, the free and open-source Android app repository.

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
     alt="Get it on F-Droid"
     height="80">](https://f-droid.org/packages/orinasa.njarasoa.maripanatokana/)

**Obtainium**: Get each release as soon as it is on GitHub, without the F-Droid build delay.

[<img src="https://raw.githubusercontent.com/ImranR98/Obtainium/main/assets/graphics/badge_obtainium.png"
     alt="Get it on Obtainium"
     height="80">](https://apps.obtainium.imranr.dev/redirect?r=obtainium://add/https://github.com/mpilasy/maripanaTokana)

**Web**: No install needed — use the hosted PWA at [maripana.njarasoa.org](https://maripana.njarasoa.org).

The app is distributed in two flavors. Each [GitHub release](https://github.com/mpilasy/maripanaTokana/releases) has both APKs, and Obtainium asks which one to install:
- **F-Droid** (`maripanaTokana-vX.apk`): Uses native Android LocationManager (no Google Play Services dependency). This is the APK that F-Droid publishes.
- **Standard** (`maripanaTokana-standard-vX.apk`): Uses Google Play Services for enhanced location performance.

Both APKs have the same app ID and signing key, so a phone can have only one of them. To change flavors, install the other APK over the current one.

## Build and Run

```bash
# F-Droid flavor (no Google Play Services dependency)
./gradlew assembleFdroidDebug      # Debug APK
./gradlew assembleFdroidRelease    # Release APK for F-Droid (requires keystore)

# Standard flavor (with Google Play Services)
./gradlew assembleStandardDebug    # Debug APK
./gradlew assembleStandardRelease  # Release APK (requires keystore)
```

For detailed information about F-Droid deployment, location providers, and build configuration, see [docs/FDROID.md](docs/FDROID.md).

## Architecture

MVVM with Clean Architecture. Package: `orinasa.njarasoa.maripanatokana`

```
app/
├── data/
│   ├── location/        # LocationProvider (GPS)
│   ├── remote/          # API DTOs, Retrofit service, WMO code mapping
│   ├── repository/      # Repository implementations
│   ├── settings/        # AppSettingsRepository
│   └── source/          # Pluggable weather (Open-Meteo, Pirate Weather) and geocoding sources
├── di/                  # Hilt modules (Network, Location, Repository)
├── domain/
│   ├── model/           # Inline value classes (Temperature, Pressure, WindSpeed, Precipitation)
│   └── repository/      # Repository interfaces
├── ui/
│   ├── permission/      # Runtime location permission screen
│   ├── settings/        # SettingsScreen, SettingsViewModel
│   ├── theme/           # Compose theme, 22 font pairings, CompositionLocals
│   └── weather/         # WeatherScreen, WeatherViewModel, WeatherUiState
│       └── components/  # Charts, alert banner, location override dialog
└── widget/              # Glance widgets (4x1, 4x2), WorkManager updater
    └── theme/           # Widget-specific Glance theme
```

## Permissions

- `INTERNET` — fetch weather data (Open-Meteo or Pirate Weather) and alert sources
- `ACCESS_FINE_LOCATION` — precise GPS coordinates
- `ACCESS_COARSE_LOCATION` — fallback location

Location permissions are requested at runtime with a dual-language permission screen (app locale + system locale).

## License

(c) Orinasa Njarasoa

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. See [LICENSE](LICENSE).

Versions up to and including 1.5.24 were released under the MIT License.
