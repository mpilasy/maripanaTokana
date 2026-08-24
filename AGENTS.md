# maripànaTokana — Project Guide for AI Agents

> **This is the canonical context file.** All vendor-specific files (`CLAUDE.md`, `GEMINI.md`, `.cursorrules`, `.github/copilot-instructions.md`) reference this document. Read this first.

## What This Project Is

A cross-platform weather app (Android + Web PWA) that shows current conditions, hourly forecasts, 7-day forecasts with trend charts, air quality and UV forecast trends, and multi-location favorites. Every measurement displays both metric and imperial units simultaneously. Supports 8 languages with native digit rendering and 22 font pairings.

- **Package:** `orinasa.njarasoa.maripanatokana`
- **License:** MIT
- **Owner:** Experienced C#, Java, C++ developer. Prefer concise, technically accurate code. Skip redundant explanations.

---

## Project Structure

```
maripanaTokana/
├── app/                          # Android (Kotlin + Jetpack Compose)
│   ├── src/main/                 # Shared source code
│   ├── src/fdroid/               # F-Droid flavor (no Google Play Services)
│   └── src/standard/             # Standard flavor (with Google Play Services)
├── web/                          # SvelteKit PWA (Svelte 5 + TypeScript)
│   ├── src/lib/                  # App code (api, domain, i18n, stores, components)
│   ├── src/routes/               # Page routing
│   └── src/service-worker.ts     # Offline caching
├── shared/                       # Shared i18n translations (canonical source)
│   └── i18n/locales/             # 8 JSON files (symlinked into web/)
├── metadata/                     # F-Droid YAML metadata
├── fastlane/                     # F-Droid store listings (8 languages)
├── docs/                         # Android-specific documentation
│   ├── DESIGN.md                 # Android architecture & design guide
│   ├── FDROID.md                 # F-Droid build config & deployment (CRITICAL)
│   ├── TESTING_GUIDE.md          # Android testing checklist
│   └── SCREENSHOTS_README.md     # Screenshot capture guide
├── web/docs/                     # Web-specific documentation
│   ├── DESIGN.md                 # Web architecture & design guide
│   └── TESTING.md                # Web testing checklist
├── gradle/libs.versions.toml     # Centralized dependency versions
├── build.gradle.kts              # Root Gradle config
├── app/build.gradle.kts          # App module Gradle config
├── settings.gradle.kts           # Gradle settings
└── gradle.properties             # Gradle/Android build properties
```

---

## Tech Stack

### Android

| Component | Version | Notes |
|-----------|---------|-------|
| AGP | 9.2.1 | |
| Kotlin | 2.2.10 | Compose compiler plugin implicit via AGP 9 |
| Compose BOM | 2024.09.00 | Material 3 |
| Glance | 1.1.1 | Home screen widgets |
| Hilt | 2.59 | DI via KSP (not kapt) |
| Retrofit | 2.11.0 | + Kotlinx Serialization (no Gson) |
| WorkManager | 2.10.0 | Widget background refresh (30 min) |
| Min SDK | 24 | Android 7.0+ |
| Target SDK | 36 | Android 16 |

### Web

| Component | Version | Notes |
|-----------|---------|-------|
| SvelteKit | 2.x | Node.js adapter (`adapter-node`) |
| Svelte | 5.x | Runes (`$state`, `$derived`, `$effect`, `$props`) |
| TypeScript | 5.x | Strict mode |
| svelte-i18n | 4.x | `$_('key')` + `$json('key')` syntax |
| html2canvas | 1.4.1 | Screenshot capture |
| Node.js | 22 Alpine | Production server (`node build/index.js`) |

### Shared

| Component | Notes |
|-----------|-------|
| Open-Meteo API | Weather data — free, no API key (default) |
| Pirate Weather | Weather data — requires API key (optional, user-configured) |
| Nominatim | Reverse geocoding — free, no API key |
| i18n | 8 JSON files in `shared/i18n/locales/` |

---

## Architecture

### Android: MVVM + Clean Architecture

```
UI (Compose) → ViewModel (StateFlow) → Repository → API (Retrofit)
                    ↑ Hilt DI injects everything
```

- **Data layer:** `data/remote/` (API services, response models, mappers), `data/repository/` (implementations), `data/location/` (LocationProvider interface)
- **Domain layer:** `domain/model/` (inline value classes: Temperature, WindSpeed, Pressure, Precipitation), `domain/repository/` (interfaces)
- **UI layer:** `ui/weather/` (WeatherScreen, WeatherContent, WeatherViewModel, WeatherUiState), `ui/theme/` (fonts, colors, typography)
- **Widget layer:** `widget/` (Glance widgets, WorkManager updater, standalone weather fetcher)
- **DI:** `di/` (NetworkModule, LocationModule, RepositoryModule)

### Web: SvelteKit + Stores

```
Components (.svelte) → Stores (writable/derived) → API (fetch) → Open-Meteo / Pirate Weather
                                                  ↘ Server routes (/api/alerts/*) → proxied alert feeds
```

- **API:** `lib/api/` (openMeteo, pirateWeather, WMO codes; `alerts/` subdirectory with one file per source)
- **Domain:** `lib/domain/` (Temperature, Pressure, WindSpeed, Precipitation — same logic as Kotlin)
- **Stores:** `lib/stores/` (weather, preferences, location, devMode)
- **Components:** `lib/components/` (WeatherScreen, HeroCard, HourlyForecast, DailyForecast, CurrentConditions, SettingsScreen, CollapsibleSection, etc.)
- **i18n:** `lib/i18n/` (svelte-i18n setup, locale config, `localizeDigits()`)
- **Server routes:** `routes/api/alerts/` (SvelteKit `+server.ts` proxy endpoints for MeteoAlarm, BOM, NHC, WMO SWIC, ECCC — needed because these feeds don't set `Access-Control-Allow-Origin`)

### Android ↔ Web Mapping

| Android | Web |
|---------|-----|
| `SharedPreferences` | `localStorage` |
| `StateFlow` + `collectAsState()` | Svelte writable stores + `$store` |
| `AnimatedVisibility` | CSS transitions / `transition:slide` |
| `LazyRow` | `display: flex; overflow-x: auto; scroll-snap-type` |
| `GraphicsLayer.toImageBitmap()` | `html2canvas` + canvas compositing |
| `Intent.ACTION_SEND` | `navigator.share()` + download fallback |
| `FusedLocationProvider` | `navigator.geolocation` |
| `android.location.Geocoder` | Nominatim API |
| `Lifecycle.ON_RESUME` | `document.visibilitychange` |
| `R.string.xxx` | `$_('key')` via svelte-i18n |

---

## Build Commands

### Android

```bash
./gradlew assembleFdroidRelease     # F-Droid flavor (no Play Services)
./gradlew assembleStandardRelease   # Standard flavor (with Play Services)
```

### Web

```bash
cd web
npm install
npm run dev                          # Dev server at localhost:5173
npm run build                        # Production build (includes CSS inlining)
npm run check                        # Type-check
docker compose up -d --build         # Docker deploy (port 3080)
```

### i18n

```bash
node shared/i18n/generate-android-strings.js   # JSON → Android XML strings
```

---

## Core Features (Both Platforms)

- **Dual Units with Toggle:** Every measurement shows both metric and imperial. Tap any value to swap primary (bold/large) vs secondary (smaller/dimmer). Preference persisted.
- **Multi-Location Favorites:** Header location search, favorite places (♥), and left/right swipe or arrow navigation between saved locations.
- **Air Quality & UV Forecasts:** 48-hour AQI trend line chart with color-coded tier dots and 7-day UV trend line chart.
- **Single-Card Accordion:** Opening one section collapses any other that was open; switching locations collapses all cards.
- **Font Cycling:** 22 bundled font pairings cycled via footer icon. Index 0 = system default.
- **Language Cycling:** 8 languages (mg, ar, en, es, fr, hi, ne, zh) cycled via footer flag button. Default: Malagasy (mg, index 0).
- **Native Digit Rendering:** Arabic (Eastern Arabic ٠-٩), Hindi/Nepali (Devanagari ०-९). All numeric formatting uses ASCII internally; native digits applied via character replacement at display time.
- **RTL Support:** Full RTL layout for Arabic. Footer icon row forced LTR.
- **Two-Step Location:** Cached location renders immediately; fresh GPS silently re-fetches if user moved >5 km.
- **Auto-Refresh:** Re-fetches weather on app resume if data >30 min old.
- **Pull-to-Refresh:** Swipe gesture to manually refresh.
- **Screenshot Sharing:** Capture hero card or any section as branded PNG with watermark.
- **Advanced Mode:** Toggle in Settings → 12-hour session unlocking location override, pluggable weather/geocoding sources, and per-alert-source toggles. DMS coordinate display.
- **Dual-Language Error Screen:** Shows secondary translation when app locale differs from system/browser locale.
- **Widgets (Android only):** 4x1 compact and 4x2 with 3-day forecast. WorkManager background updates.

---

## Internationalization

| Language | Tag | Native Digits | Decimal Sep | RTL |
|----------|-----|---------------|-------------|-----|
| Malagasy | mg | — | `,` | No |
| Arabic | ar | Eastern Arabic (٠-٩) | `٫` | **Yes** |
| English | en | — | `.` | No |
| Spanish | es | — | `,` | No |
| French | fr | — | `,` | No |
| Hindi | hi | Devanagari (०-९) | `.` | No |
| Nepali | ne | Devanagari (०-९) | `.` | No |
| Chinese | zh | — | `.` | No |

- Translations: `shared/i18n/locales/*.json` (~76 keys + 2 arrays: `cardinal_directions`, `uv_labels`)
- Android strings: `app/src/main/res/values-{locale}/strings.xml` (generated from JSON)
- Web: Symlinked from `shared/i18n/locales/` into `web/src/lib/i18n/locales/`

---

## Critical Rules & Gotchas

### Android Build (Kotlin/Gradle)

- **DO NOT add `id("org.jetbrains.kotlin.android")` to app plugins** — AGP 9 + compose plugin already register the `kotlin` extension. Adding it causes "Cannot add extension with name 'kotlin'" error.
- **`kotlin { jvmToolchain(21) }` MUST be in `app/build.gradle.kts`** — overrides Kotlin 2.x's default JetBrains JDK vendor. Without it, F-Droid builds fail.
- **DO NOT add foojay-resolver to `settings.gradle.kts`** — F-Droid's security scanner blocks it.
- **`auto-provisioning=disabled` MUST stay in `gradle.properties`** — F-Droid doesn't allow JDK downloads.
- **`gradle/gradle-daemon-jvm.properties`** — must NOT have `toolchainVendor` or `toolchainUrl` lines.
- **R8 keep rules in `app/proguard-rules.pro` MUST be preserved** — Room/WorkManager `_Impl` classes are instantiated via reflection. Without keep rules, R8 strips them and the app crashes at startup.
- **See `docs/FDROID.md` for the complete F-Droid build configuration guide** with diagnostic checklist and failure history.
- Use `Locale.US` for all numeric/time formatting to prevent native digit rendering in format strings.
- Do NOT use `AppCompatActivity` — causes `Theme.AppCompat` requirement. Use `ContextWrapper` for locale.
- Do NOT use `createConfigurationContext()` as `LocalContext` — triggers Gboard keyboard toast.
- `fontFeatureSettings` must be passed via `style = TextStyle(fontFeatureSettings = ...)`, not as a direct `Text()` parameter.
- Temperature uses `roundToInt()` for display to avoid `-0`.

### Web (Svelte 5)

- **`{@const}`** must be immediate child of block constructs (`{#each}`, `{#if}`), not nested inside elements. Use `$derived()` instead.
- **`bind:this`** variable must be declared with `$state()`.
- **Props:** Use `interface Props` + `$props()` (not `export let`).
- **Children:** Use `Snippet` type + `{@render children()}` (not `<slot />`).
- **Effects:** `$effect()` replaces `afterUpdate` and reactive statements.
- No `toLocaleString()` for digits — use `localizeDigits()` character replacement.

### Both Platforms

- Do NOT commit `keystore.properties`, `release.keystore`, `.env`, or credentials.
- Phone-only: NOT compatible with TV, Wear, Auto.
- WMO codes map to emoji + i18n description keys (keep in sync across platforms).
- Domain value classes (Temperature, Pressure, WindSpeed, Precipitation) must have identical conversion logic in both Kotlin and TypeScript.

---

## Mandates

1. **Feature Parity:** Android and Web must stay in sync. New features implemented in one platform should be ported to the other.
2. **Shared Resources:** i18n strings are shared via `shared/i18n/locales/`. New keys added on one platform must be available on the other.
3. **Domain Logic Consistency:** Unit conversions, weather code mappings, and display logic must be identical across Kotlin and TypeScript.
4. **No Co-Authored-By:** Do NOT add `Co-Authored-By: Claude` or any AI attribution to commit messages.

---

## Build Flavors (Android)

| Flavor | Location Provider | Play Services | Built with |
|--------|------------------|---------------|------------|
| `standard` | `FusedLocationProviderClient` | Yes | `assembleStandardRelease` |
| `fdroid` | Native `LocationManager` | No | `assembleFdroidRelease` |

Flavor-specific code lives in `app/src/standard/` and `app/src/fdroid/` (LocationProvider, PermissionHandler, WidgetWeatherFetcher, DI module).

---

## Visual Style (Both Platforms)

- Dark theme
- Blue Marble background at 12% opacity
- Translucent cards (base: `rgba(42, 31, 165, 0.6)`, hero: `rgba(42, 31, 165, 0.8)`)
- Theme color: `#0E0B3D`
- Edge-to-edge with safe area padding

---

## Further Reading

| Document | What it covers |
|----------|----------------|
| `docs/DESIGN.md` | Android architecture, domain models, DI, widgets, fonts, developer mode |
| `docs/FDROID.md` | **F-Droid build config, JDK toolchain, diagnostic checklist, failure history** |
| `docs/TESTING_GUIDE.md` | Android testing checklist (functional, performance, accessibility) |
| `docs/SCREENSHOTS_README.md` | F-Droid screenshot capture guide |
| `web/docs/DESIGN.md` | Web architecture, stores, components, service worker, deployment |
| `web/docs/TESTING.md` | Web testing checklist (PWA, offline, responsive) |
| `web/README.md` | Web build, deployment, i18n tables, font pairings table |
| `README.md` | Project overview, features, tech stack |
