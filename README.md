# maripànaTokana

Android weather app displaying current weather with dual metric/imperial units.

## Features

- Real-time weather data from Open-Meteo API (no key required)
- GPS location detection
- Dual-unit display (Metric / Imperial) for all measurements
- **Unit toggle**: tap the °C/°F button to swap which unit system is emphasized; both units always visible
- Primary unit shown bold/large, secondary shown smaller/dimmer underneath
- Unit preference persisted across app and widgets via SharedPreferences
- Cardinal wind direction display (e.g. "NNW (340°)")
- "Last refreshed" timestamp below the date
- Custom app icon: Blue Marble with thermometer overlay
- Dark gradient UI optimized for readability
- Detailed weather information:
  - Temperature (current, feels like, min/max)
  - Pressure (hPa / inHg)
  - Humidity (%)
  - Wind speed and direction with cardinal compass (m/s / mph)
  - Wind gusts (when available)
  - Precipitation (rain/snow in mm / inches)
  - Visibility (km / mi)
  - Sunrise/sunset times

## Platform Support

**Phone Only** - This app is designed exclusively for Android phones with touchscreens. It is NOT compatible with:
- Android TV
- Wear OS
- Android Auto
- Tablets (not optimized)

## Technical Stack

- **AGP**: 9.0.0
- **Kotlin**: 2.0.21
- **Compose BOM**: 2024.09.00
- **Hilt**: 2.59
- **Retrofit**: 2.11.0
- **Kotlinx Serialization**: 1.7.3
- **Glance**: 1.1.1 (for widget support)
- **Play Services Location**: 21.3.0
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 36

## Setup

### Build and Run

```bash
./gradlew :app:installDebug
```

## Architecture

The app follows Clean Architecture principles with clear separation of concerns:

```
app/
├── data/
│   ├── remote/          # API DTOs and services
│   └── repository/      # Repository implementations
├── di/                  # Hilt dependency injection modules
├── domain/
│   ├── model/           # Value objects (Temperature, Pressure, etc.)
│   └── repository/      # Repository interfaces
├── ui/
│   ├── theme/           # Compose theme
│   └── weather/         # Weather screen + ViewModel
└── widget/              # Glance app widget
```

### Value Objects

All measurements are stored in canonical metric units and converted to imperial on demand:
- `Temperature`: Celsius → Fahrenheit
- `Pressure`: hPa → inHg
- `WindSpeed`: m/s → mph
- `Precipitation`: mm → inches

Each provides `displayDual()` for a combined string and `displayDual(metricPrimary: Boolean)` returning a `Pair<String, String>` of (primary, secondary) for emphasis-styled rendering.

## Permissions

The app requires:
- `INTERNET` - To fetch weather data from OpenWeatherMap API
- `ACCESS_FINE_LOCATION` - To get precise GPS coordinates
- `ACCESS_COARSE_LOCATION` - Fallback location method

Location permissions are requested at runtime when the app launches.

## Known Issues

- Hilt 2.59 with AGP 9.0.0 requires `android.disallowKotlinSourceSets=false` in gradle.properties (experimental setting)

## License

[Your license here]