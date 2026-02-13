# maripànaTokana

**maripànaTokana** (Malagasy for "a single thermometer") is a phone-only Android weather app that shows current conditions, hourly forecasts, and a 10-day outlook. It always displays both metric and imperial units side by side, and supports 8 languages with 16 font pairings.

## Features

- Real-time weather data from [Open-Meteo](https://open-meteo.com) API (no key required)
- GPS location with two-step strategy (instant cached + fresh background)
- **Dual-unit display**: every measurement shows both metric and imperial simultaneously
- **Tap to toggle**: tap any value to swap which unit is primary (bold/large) vs secondary (dimmer)
- **8 languages**: Malagasy, Arabic, English, Spanish, French, Hindi, Nepali, Chinese — cycled via flag button in footer
- **16 font pairings** including Roboto + Lora: cycled via font icon in footer
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
| AGP | 9.0.0 |
| Kotlin | 2.0.21 |
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

The app is distributed in two flavors:
- **F-Droid**: Uses native Android LocationManager (no Google Play Services dependency)
- **Standard**: Uses Google Play Services for enhanced location performance (Google Play, side-load)

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
│   ├── remote/          # API DTOs, Retrofit service, WMO code mapping
│   └── repository/      # Repository implementations
├── di/                  # Hilt modules (Network, Location, Repository)
├── domain/
│   ├── model/           # Inline value classes (Temperature, Pressure, WindSpeed, Precipitation)
│   └── repository/      # Repository interfaces
├── ui/
│   ├── theme/           # Compose theme, 16 font pairings, CompositionLocals
│   └── weather/         # WeatherScreen, WeatherViewModel, WeatherUiState
└── widget/              # Glance widgets (4x1, 4x2), WorkManager updater
```

## Permissions

- `INTERNET` — fetch weather data from Open-Meteo API
- `ACCESS_FINE_LOCATION` — precise GPS coordinates
- `ACCESS_COARSE_LOCATION` — fallback location

Location permissions are requested at runtime with a dual-language permission screen (app locale + system locale).

## License

(c) Orinasa Njarasoa
