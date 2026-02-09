# Project: maripànaTokana (Weather App)
**Goal:** Android weather app and widgets featuring simultaneous Metric/Imperial display.

**Package:** `orinasa.njarasoa.maripanatokana`

## Tech Stack
- **Language:** Kotlin 2.0.21
- **UI:** Jetpack Compose (BOM 2024.09.00) for app, Jetpack Glance 1.1.1 for widgets
- **Build:** AGP 9.0.0 (Kotlin Android plugin is implicit — do NOT add explicitly)
- **DI:** Hilt 2.59 + KSP 2.0.21-1.0.28
- **Network:** Retrofit 2.11.0 + Kotlinx Serialization (no API key required)
- **Location:** Google Play Services FusedLocationProvider
- **Background:** WorkManager for periodic widget updates (30 min)
- **Architecture:** MVVM, Clean Architecture

## Core Features
- **Dual Units with Toggle:** Every measurement shows both metric and imperial. Tap any value (`DualUnitText`) to swap which is primary (bold/large) vs secondary (smaller/dimmer). Preference stored in `SharedPreferences("widget_prefs", "metric_primary")`.
- **Font Cycling:** 16 bundled font pairings (Default + 15 custom) cycled via font icon in footer. Uses `CompositionLocal` (`LocalDisplayFont`, `LocalBodyFont`). Glance widgets cannot use custom fonts.
- **In-App Language Cycling:** 8 languages (mg, ar, en, es, fr, hi, ne, zh) cycled via flag emoji button in footer. Uses `ContextWrapper` overriding only `getResources()` to swap locale without activity recreation or keyboard toasts. Locale index stored in `SharedPreferences("widget_prefs", "locale_index")`. Default: Malagasy (mg, index 0).
- **i18n:** All strings extracted to `res/values/strings.xml` (~73 strings + 2 string-arrays). Translations in `res/values-{locale}/strings.xml`. `wmoDescriptionRes()` returns `@StringRes Int`. `WeatherUiState.Error` holds `@StringRes Int`. All numeric `.format()` calls use `Locale.US` to prevent Devanagari/Arabic digit rendering. Native digits applied via `SupportedLocale.localizeDigits()` character replacement at display time.
- **Hero Card:** Emoji+description (upper-left), temperature with 1 decimal (upper-right), feels like (lower-left), precipitation (lower-right), "© Orinasa Njarasoa" watermark (bottom-center). Share icon (top-left) captures card via `rememberGraphicsLayer()` and shares PNG through FileProvider.
- **Current Conditions:** Collapsible section with detail cards: Min/Max Temp, Wind/Wind Gust, Pressure/Humidity, UV Index/Visibility, Sunrise/Sunset. Cards in each row are equally sized via `IntrinsicSize.Min` + `fillMaxHeight()`.
- **Forecasts:** Hourly (24h horizontal LazyRow) and weekly (7-day vertical list), both collapsible.
- **Two-Step Location:** `lastLocation` (instant cached) renders immediately; `getFreshLocation(BALANCED_POWER_ACCURACY)` silently re-fetches weather if user moved >5 km.
- **Widgets:** 4x1 ("Now in {city}") and 4x2 ("Today in {city}"), both via `WidgetWeatherFetcher` (standalone Retrofit, no Hilt). WorkManager updates with network constraint. 4x2 widget shows dual units for feels like, wind, and min/max, plus 3-day forecast.
- **Footer:** "Weather data by Open-Meteo" link + "Hash: {commit} • {build time}" via `BuildConfig.GIT_HASH` and `BuildConfig.BUILD_TIME`.
- **Back Button:** `singleTop` launch mode + `moveTaskToBack(true)` — hides app instead of destroying.
- **Auto-refresh:** `refreshIfStale()` on `ON_RESUME` if data >30 min old.
- **Phone-only:** NOT compatible with TV, Wear, Auto (enforced via manifest features).
- **Visual Style:** Dark theme, Blue Marble background at 0.12 alpha, translucent cards (`Color(0xFF2A1FA5)`). Edge-to-edge with `statusBarsPadding()`.
- **RTL Support:** Arabic layout direction explicitly provided via `LocalLayoutDirection`. Footer icon row forced LTR.
- **Permission Screen:** Dual-language when app locale differs from system locale.

## Build Notes
- `compileSdk` uses AGP 9 syntax: `compileSdk { version = release(36) }`
- Hilt 2.59 + AGP 9 requires `android.disallowKotlinSourceSets=false` in gradle.properties
- `BuildConfig.GIT_HASH` set via `providers.exec` in build.gradle.kts
- `BuildConfig.BUILD_TIME` set via `SimpleDateFormat` at top of build.gradle.kts
- Widget location uses SharedPreferences fallback (background context lacks location permission)
- Widgets must be removed and re-added from home screen to trigger `provideGlance()` after install
- Do NOT use `AppCompatActivity` just for locale switching — causes `Theme.AppCompat` requirement. Use `ContextWrapper` approach instead.
- Do NOT use `createConfigurationContext()` directly as `LocalContext` — triggers Gboard keyboard toast.
- Always use `Locale.US` for numeric/time formatting to avoid locale-sensitive digit rendering.
- Temperature uses `roundToInt()` for integer display to avoid `-0`. Hero card uses `decimals = 1`.

## Developer Context
- Owner is an experienced C#, Java, C++ developer.
- Prefer concise, technically accurate code.
- Avoid redundant explanations; focus on implementation details and logic.
