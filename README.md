# maripànaTokana

**maripànaTokana** (Malagasy for "a single thermometer") is a weather app for Android, and a [web app (PWA)](./web) with the same features. A hosted copy of the web app is at **[maripana.njarasoa.org](https://maripana.njarasoa.org)**, with no install needed. Every value shows in metric and imperial units side by side. The app has 8 languages and 22 font pairs.

<p align="center">
  <img src="fastlane/metadata/android/mg/images/phoneScreenshots/1.png" width="24%" alt="Main card, in Malagasy">
  <img src="fastlane/metadata/android/ne/images/phoneScreenshots/2.png" width="24%" alt="Hourly and 10-day forecasts, in Nepali">
  <img src="fastlane/metadata/android/es/images/phoneScreenshots/3.png" width="24%" alt="Current conditions, in Spanish">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/4.png" width="24%" alt="Sun and air, and activities, in English">
</p>

## Features

- **Same features on Android and web.**
- **Main card**: the condition, the temperature with 1 decimal in both units, the low and high, rain or snow when it is coming, air quality (US and EU index) when it is not good, and 4 hour tiles 4 hours apart. For a place in another time zone, the hours also show the phone's time.
- **Free-scroll layout**: the main card, then cards that are always open, in one scrolling column:
  - **Hourly and 10-day forecasts** with the same tabs: Conditions, Precipitation, Wind, Pressure, UV and Air quality. Each tab has a row of cards and an overview chart.
  - **Minutely nowcast** for the next 2 hours, only when rain or snow is coming (Open-Meteo).
  - **Current conditions** and **Sun & Air** tiles: temperature, precipitation, wind compass, gusts, pressure and air-quality dials, humidity with dew point, sun arc, UV dial, visibility.
  - **Activities**: ratings for running, drying laundry and sun protection.
- **Places**:
  - Search by city, postal code, coordinates or Plus Code. A searched place shows for 12 hours.
  - Favorites: save, rename and remove places. Swipe left or right, or use the arrows, to move between them.
  - A two-line header: the place, then the region and country. The grid icon opens a card with the coordinates (degrees, minutes and seconds) and the full Plus Code.
- **Weather alerts** from 7 sources: NWS (US), NHC (hurricanes), MeteoAlarm (Europe), JMA (Japan), ECCC (Canada), BOM (Australia) and GDACS (global). Each source has its own switch in Settings.
- **Weather data** from [Open-Meteo](https://open-meteo.com) (default, no key) or [Pirate Weather](https://pirateweather.net) (optional, your own API key).
- **Units**: every value shows metric and imperial units. Tap a value to put the other unit first.
- **8 languages**: Malagasy, Arabic, English, Spanish, French, Hindi, Nepali and Chinese. Tap the flag in the footer to change. Local digits for Arabic, Hindi and Nepali, and right-to-left layout for Arabic.
- **22 font pairs**: tap the font name in the footer to change.
- **Share**: each card has a share button that shares the card as an image.
- **Help and About pages**: 11 questions and answers, the data sources, and what the system reports about the device.
- **Refresh**: pull down, or tap the update time. The app also refreshes on resume when the data is more than 30 minutes old.
- **Two home-screen widgets**: 4x1 (current weather) and 4x2 (current weather and a 3-day forecast).
- **Advanced Mode** (Settings): for 12 hours, override the location with any city or coordinates, for tests.
- Blue Marble background, edge to edge.

## Platform Support

**Phone Only** — designed exclusively for Android phones with touchscreens. NOT compatible with Android TV, Wear OS, Android Auto, or tablets.

## Technical Stack

| Component | Version |
|-----------|---------|
| AGP | 9.2.1 |
| Kotlin | 2.2.21 |
| Compose BOM | 2026.06.01 |
| Glance | 1.2.0 |
| Hilt | 2.60.1 |
| Retrofit | 2.11.0 |
| Kotlinx Serialization | 1.7.3 |
| Play Services Location | 21.4.0 |
| WorkManager | 2.12.0 |
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
│       └── components/  # Charts, tiles, alert banner, nowcast, dialogs
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
