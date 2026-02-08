# Project: maripanaTokana (Weather App)
**Goal:** Android weather app and widgets featuring simultaneous Metric/Imperial display.

**Package:** `orinasa.njarasoa.maripanatokana`

## Tech Stack
- **Language:** Kotlin 2.0.21
- **UI:** Jetpack Compose (BOM 2024.09.00) for app, Jetpack Glance 1.1.1 for widgets
- **Build:** AGP 9.0.0 (Kotlin Android plugin is implicit — do NOT add explicitly)
- **DI:** Hilt 2.59 + KSP 2.0.21-1.0.28
- **Network:** Retrofit 2.11.0 + Kotlinx Serialization
- **Weather Source:** Open-Meteo API (no key required)
- **Location:** Google Play Services FusedLocationProvider
- **Background:** WorkManager for periodic widget updates (30 min)
- **Architecture:** MVVM, Clean Architecture

## Core Features
- **Dual Units with Toggle:** Every measurement shows both metric and imperial. An icon button (ruler) swaps which is primary (bold/large) vs secondary (smaller/dimmer). Preference stored in `SharedPreferences("widget_prefs", "metric_primary")`.
- **Font Cycling:** 4 bundled font pairings (Default, Orbitron+Outfit, Rajdhani+Inter, Oxanium+Nunito) cycled via icon button. Uses `CompositionLocal` (`LocalDisplayFont`, `LocalBodyFont`). Glance widgets cannot use custom fonts.
- **Hero Card:** Emoji+description (upper-left), temperature (upper-right), feels like (lower-left), precipitation (lower-right).
- **Current Conditions:** Collapsible section with detail cards: Min/Max Temp, Wind/Wind Gust, Pressure/Humidity, UV Index/Visibility, Sunrise/Sunset. Cards in each row are equally sized via `IntrinsicSize.Min` + `fillMaxHeight()`.
- **Forecasts:** Hourly (24h horizontal LazyRow) and weekly (7-day vertical list), both collapsible.
- **Two-Step Location:** `lastLocation` (instant cached) renders immediately; `getFreshLocation(BALANCED_POWER_ACCURACY)` silently re-fetches weather if user moved >5 km.
- **Widgets:** 4x1 and 4x2, both via `WidgetWeatherFetcher` (standalone Retrofit, no Hilt). Show "Today in {city}" + refresh time. WorkManager updates with network constraint. 4x2 widget shows dual units for feels like, wind, and min/max.
- **Back Button:** `singleTop` launch mode + `moveTaskToBack(true)` — hides app instead of destroying.
- **Phone-only:** NOT compatible with TV, Wear, Auto (enforced via manifest features).
- **Visual Style:** Dark theme, Blue Marble background at 0.12 alpha, translucent cards (`Color(0xFF2A1FA5)`).

## Build Notes
- `compileSdk` uses AGP 9 syntax: `compileSdk { version = release(36) }`
- Hilt 2.59 + AGP 9 requires `android.disallowKotlinSourceSets=false` in gradle.properties
- Widget location uses SharedPreferences fallback (background context lacks location permission)
- Widgets must be removed and re-added from home screen to trigger `provideGlance()` after install

## Developer Context
- Owner is an experienced C#, Java, C++ developer.
- Prefer concise, technically accurate code.
- Avoid redundant explanations; focus on implementation details and logic.
- Do not suggest or link to video tutorials.
