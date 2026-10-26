# Design Document — maripànaTokana PWA

This document explains the architecture, data flow, and key patterns of the **maripànaTokana** weather PWA. It mirrors the native Android app's features and design language.

For the Android app's design documentation, see [`docs/DESIGN.md`](../../docs/DESIGN.md).

---

## Table of Contents

1. [Technology Overview](#1-technology-overview)
2. [Project Structure](#2-project-structure)
3. [Build Pipeline](#3-build-pipeline)
4. [Application Lifecycle & Saved Locations](#4-application-lifecycle--saved-locations)
5. [Data Flow](#5-data-flow)
6. [Domain Models](#6-domain-models)
7. [State Management](#7-state-management)
8. [Component Architecture & Layout Rules](#8-component-architecture--layout-rules)
9. [Internationalization](#9-internationalization)
10. [Font System](#10-font-system)
11. [Service Worker & Offline](#11-service-worker--offline)
12. [Advanced Mode](#12-advanced-mode)
13. [Settings & Pluggable Data Sources](#13-settings--pluggable-data-sources)
14. [Weather Alerts](#14-weather-alerts)
15. [Screenshot Sharing](#15-screenshot-sharing)
16. [Deployment](#16-deployment)
17. [Key Patterns & Decisions](#17-key-patterns--decisions)

---

## 1. Technology Overview

| Layer | Technology | Role |
|-------|-----------|------|
| Framework | **SvelteKit** (v2) + **Svelte 5** | UI framework + routing + build tooling |
| Language | **TypeScript** | Type safety throughout |
| Build | **Vite** | Dev server + production bundler |
| Hosting | **adapter-node** | Builds a standalone Node.js server (`build/index.js`) |
| i18n | **svelte-i18n** | Runtime internationalization (8 languages) |
| Screenshots | **html2canvas** | DOM-to-canvas capture for sharing |
| Weather API | **Open-Meteo** (default) / **Pirate Weather** (optional) | Weather data |
| Geocoding | **Open-Meteo Geocoding API** first, **Nominatim** as fallback | Location search; Nominatim also does reverse geocoding |
| Alerts | 7 official sources (NWS, GDACS, MeteoAlarm, JMA, ECCC, BOM, NHC) | Proxied server-side (`routes/api/alerts/`) where CORS is missing |
| Server | **Node.js** (via adapter-node) | Serves the built app directly |

---

## 2. Project Structure

```
web/
├── src/
│   ├── app.html                          # HTML shell
│   ├── app.d.ts                          # Global TypeScript declarations
│   ├── hooks.server.ts                   # /svelte → / redirect, security headers
│   ├── service-worker/index.ts           # PWA offline caching
│   ├── routes/                           # URL-mapped pages
│   │   ├── +layout.svelte                # Root layout: fonts, RTL, i18n init
│   │   ├── +page.svelte                  # Home page: mounts WeatherScreen
│   │   ├── +error.svelte                 # Error page
│   │   └── api/                          # alerts/{meteoalarm,bom,nhc,eccc} CORS proxies, csp-report
│   └── lib/                              # All app code ($lib alias), abridged
│       ├── api/                          # Network layer
│       │   ├── openMeteo.ts              # Open-Meteo client, reads the X-SW-Cached-At header
│       │   ├── openMeteoAirQuality.ts    # AQI, Pollen & hourly AQI forecast client
│       │   ├── pirateWeather.ts          # Pirate Weather client
│       │   ├── geocodingSearch.ts        # Location search client
│       │   ├── wmoWeatherCode.ts         # WMO code → i18n description key
│       │   ├── openMeteoMapper.ts / openMeteoTypes.ts  # Response mapping and types
│       │   ├── http.ts / nightWeather.ts  # Fetch helper with timeout; night icons for the daily cards
│       │   ├── externalAlerts.ts         # Merges alert sources
│       │   └── alerts/                   # 7 official alert source fetchers, index.ts (combines them) + shared.ts
│       ├── domain/                       # Domain models & value objects
│       │   ├── weatherData.ts            # WeatherData, HourlyForecast, DailyForecast, HourlyAirQuality
│       │   ├── temperature.ts            # Temperature (°C ↔ °F)
│       │   ├── pressure.ts              # Pressure (hPa ↔ inHg)
│       │   ├── windSpeed.ts             # WindSpeed (m/s ↔ mph)
│       │   ├── precipitation.ts          # Precipitation (mm ↔ in)
│       │   ├── precipOutlook.ts          # Rain or snow outlook and nowcast check
│       │   ├── activityIndices.ts        # Activity ratings
│       │   ├── uv.ts                     # UV levels
│       │   ├── plusCode.ts               # Plus Code search
│       │   ├── sharedLocationParser.ts   # Coordinates and map links in shared text
│       │   ├── script.ts / regionQualifier.ts # Latin letters, place-name cleanup
│       │   ├── dailyAirQuality.ts        # Worst AQI of each day
│       │   └── airQuality.ts            # AQI tiering, Pollen tiers, AQI_TIER_COLORS
│       ├── i18n/                         # Internationalization
│       │   ├── index.ts / locales.ts
│       │   └── locales/                  # 8 JSON files (symlinked to shared/i18n/locales)
│       ├── stores/
│       │   ├── weather.ts                # Weather state machine & fetch orchestration
│       │   ├── savedLocations.ts         # Multi-location state, favorites list, active selection
│       │   ├── preferences.ts            # Persisted user preferences (units, font, language, weather source, API key, alert switches)
│       │   ├── location.ts              # Geolocation & Nominatim reverse geocoding
│       │   ├── weatherSnapshot.ts       # Last weather data per place, for offline use
│       │   └── advancedMode.ts          # Advanced Mode switch (stays on until turned off)
│       ├── actions/ utils/ server/       # Svelte actions (longPress, dragScroll), date/path helpers, server-only proxy and TTL cache
│       ├── credits.ts / deviceInfo.ts    # About page data
│       ├── components/                   # Svelte UI components (see section 8)
│       ├── fonts.ts                      # 22 FontPairing definitions
│       ├── installHint.ts                # When to show the iOS Install link
│       └── share.ts                      # html2canvas capture & sharing
├── static/                               # Static assets
├── vite.config.ts
├── Dockerfile / docker-compose.yml
```

---

## 3. Build Pipeline

- `npm run dev`: Vite dev server at `localhost:5173`.
- `npm run build`: Compiles Svelte 5 components, bundles JS into a single chunk (`output.bundleStrategy: 'single'`), emits Node.js server to `build/`.
- `npm run check`: Type-checks with `svelte-check`.
- `npm test`: Runs the Vitest unit tests (`src/**/*.test.ts`).

The Docker build context is the repository root. The Dockerfile copies `shared/i18n/locales/` over the `src/lib/i18n/locales` symlink.

---

## 4. Application Lifecycle & Saved Locations

### Multi-Location Management (`stores/savedLocations.ts`)
Users can save and navigate multiple locations (shipped v1.2.15):
- **Current Location**: Uses browser geolocation API (`navigator.geolocation`).
- **Search and preview**: `SavedLocationsDialog.svelte` is the place search. A picked result shows as a preview for 12 hours (`locationOverride` in `stores/savedLocations.ts`). When no weather is on screen, a picked result is saved directly.
- **Saved Favorites**: Favorites list persisted in `localStorage`. The empty heart in the header saves a preview, and the full heart removes a saved place. Holding the place name renames it (`RenameLocationDialog`). There is no reorder.
- **Header Switching**: Only the search icon opens the search. The heart and the pin (back to the current location) show only for a saved place or a preview; the gear is always there. Arrow buttons, dots and a swipe (more than 80 px, not on the forecast strips) cycle between places. Holding an arrow opens the list of places.
- **Shared places**: the manifest `share_target` and the `text`, `url`, `title`, `q` and `loc` URL parameters open a place as a preview (`WeatherScreen.svelte`, `sharedLocationParser.ts`). Plus Codes are also accepted.

---

## 5. Data Flow

### Fetch Sequence
```
               ┌───────────────────────────┐
               │ Active Location (GPS/Fav) │
               └─────────────┬─────────────┘
                             │  saved snapshot shows at once
                             ▼
  in parallel: weather, air quality (Open-Meteo only),
               reverse geocoding, country lookup
                             │
                             ▼
               ┌───────────────────────────┐
               │  weatherState set         │
               └─────────────┬─────────────┘
                             │  no wait
                             ▼
               ┌───────────────────────────┐
               │  alerts (7 sources),      │
               │  merged in when they come │
               └───────────────────────────┘
```

If the service worker answers with an old cached response, the app marks the data as stale and shows the "refresh failed" line with the offline reason. The app refreshes when the tab becomes visible and the data is more than 30 minutes old. It fetches the location again when it moves more than 0.045° (about 5 km). Timeouts: geolocation 15 s, Nominatim 10 s.

---

## 6. Domain Models

Immutable value objects (`Temperature`, `WindSpeed`, `Pressure`, `Precipitation`) encapsulate unit conversion. Each provides `displayDual(metricPrimary)` returning `[primary, secondary]` strings.

`airQuality.ts` encapsulates US/EU AQI thresholds, pollutant tiers, one shared pollen scale for the 6 pollen types (`pollenTierFor`), and `AQI_TIER_COLORS`. The primary standard (EU or US) depends on the country.

---

## 7. State Management

Svelte stores drive application state:
- `stores/weather.ts`: Core weather state (`loading`, `success`, `error`), refresh logic.
- `stores/savedLocations.ts`: Saved favorites array, active location selection, dialog visibility.
- `stores/preferences.ts`: `metricPrimary`, `fontIndex`, `localeIndex`, the weather source, the API key and the alert switches, persisted in `localStorage`.
- `stores/advancedMode.ts`: the Advanced Mode switch (`advanced_mode_enabled`). It has no time limit.
- `stores/weatherSnapshot.ts`: the last weather data for each place (`weather_snapshot_v2`), for offline use.

---

## 8. Component Architecture & Layout Rules

### Component Tree
```
+layout.svelte
└── +page.svelte
    └── WeatherScreen
        ├── header, inline in WeatherScreen (location name, region, Latin-letters toggle, local-time chip, "Updated" line, refresh-failed line; actions: search, favorite, current location, settings; location pager when places are saved)
        ├── LocationDetails, RenameLocationDialog, SavedLocationsDialog
        ├── WeatherAlertBanner (merged 7-source alerts, expandable)
        ├── HeroCard (temperature, low/high, rain or snow outlook, US and EU AQI, 4 hour tiles)
        ├── SectionCard "Hourly forecast"
        │   ├── NowcastCard (only when rain or snow is coming; Open-Meteo only)
        │   └── HourlyForecast (ForecastTabs: Conditions, Precipitation, Wind, Pressure, UV, Air quality; cards + TemperatureChart for Conditions, OverviewChart for the others)
        ├── SectionCard "10-day forecast"
        │   └── DailyForecast (same 6 tabs; cards + DailyTemperatureChart or OverviewChart)
        ├── SectionCard "Current conditions"  (CurrentConditions part="now")
        ├── SectionCard "Sun & Air"           (CurrentConditions part="sky")
        ├── SectionCard "Activities"          (ActivitiesCard)
        ├── Footer (font name, Help and About links, iOS-only Install link, version, language flag)
        ├── InstallDialog (iOS only: Share, then Add to Home Screen)
        ├── AirQualityDetailDialog
        ├── SettingsScreen (in SettingsPage shell)
        ├── AboutScreen (credits from `lib/credits.ts`)
        └── HelpScreen
```

### Layout Rules
- **Free scroll**: The page is one scrolling column. Every card is always open. There is no accordion and no collapsible section.
- **Air quality tab**: The Air quality tab shows only when air quality data exist.
- **Surfaces**: Use the CSS tokens in `+layout.svelte` (`--surface-section`, `--surface-tile`, `--surface-tile-highlight`, `--hairline`, `--hero-gradient`) — same values as Android `ui/theme/Color.kt`. The hero gradient is diagonal (`160deg`) on the web and vertical on Android.

---

## 9. Internationalization

Supports 8 languages (mg, ar, en, es, fr, hi, ne, zh). `svelte-i18n` handles reactive translations (`$_('key')`). Native digits for Arabic, Hindi, and Nepali are rendered via `localizeDigits()` character replacement at display time. RTL direction (`<html dir="rtl">`) is enforced when Arabic is selected.

---

## 10. Font System

22 font pairings in `src/lib/fonts.ts`. Styled via `--font-display`, `--font-body`, and `--font-features` CSS variables. Each pairing points to a self-hosted CSS file in `static/fonts` (for example `/fonts/orbitron-outfit.css`). `+layout.svelte` links the CSS file of the selected pairing.

---

## 11. Service Worker & Offline

`src/service-worker/index.ts` uses two caches. `app-{version}` holds page navigations and app assets (NetworkFirst, offline fallback to cache). `api-cache` holds responses from `api.open-meteo.com`, `air-quality-api.open-meteo.com`, and `nominatim.openstreetmap.org` (NetworkFirst, stamped with `X-SW-Cached-At`). The worker does not precache. On activate, it deletes other caches.

---

## 12. Advanced Mode

Renamed from "Expert Mode" in v1.2.15. Managed via `stores/advancedMode.ts`. It stays on until the user turns it off. It:
- Shows the weather-source and alert sections in Settings.
- Shows the alert sources that failed, on the main screen.
- When turned off, clears a place preview and restores the GPS weather.

Search and place previews do not need Advanced Mode. The 12-hour limit applies to a preview.

---

## 13. Settings & Pluggable Data Sources

`SettingsScreen.svelte` always shows the Advanced mode switch, the geocoding information and the About row. With Advanced Mode on, it also provides controls for Weather Source (Open-Meteo vs Pirate Weather with API key validation), a master alerts switch and individual toggles for all 7 official alert sources.

---

## 14. Weather Alerts

7 official alert sources (NWS, GDACS, MeteoAlarm, JMA, ECCC, BOM, NHC). CORS-blocked upstreams are proxied via `src/routes/api/alerts/*/+server.ts`. The proxies keep each response for 5 minutes (`lib/server/proxy.ts`, `ttlCache.ts`). Each source is used only for its countries, and GDACS is skipped where a regional source covers the place (`api/alerts/index.ts`). Derived/algorithmic alerts were removed in v1.2.1. Multiple alerts display with expandable details per alert.

---

## 15. Screenshot Sharing

`src/lib/share.ts` uses `html2canvas` to capture PNG images (header + card content + a footer with `maripànaTokana v{version}` and the site address) for Web Share API or direct download. The PNG has the date and time in its EXIF data.

---

## 16. Deployment

Containerized via Docker (`node:24-alpine` multi-stage build running `node build/index.js`). Host port mapped via `docker-compose.yml` (default 3080).

---

## 17. Key Patterns & Decisions

- **Shared i18n Locales**: Symlinked from `shared/i18n/locales/` for 100% parity with Android. `flattenForWeb` adds the `web_only` keys and drops the `android_only` keys. The fallback language is `mg`; on the first run, the browser language is used.
- **Security**: a report-only Content Security Policy with reports to `/api/csp-report` (`vite.config.ts`), and security headers in `hooks.server.ts`.
- **Single-Chunk Bundle**: All JS compiled into one chunk (`output.bundleStrategy: 'single'`).
- **No Derived Alerts**: Only official meteorological agency alerts are displayed.
