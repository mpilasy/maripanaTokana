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
│   ├── service-worker.ts                 # PWA offline caching
│   ├── routes/                           # URL-mapped pages
│   │   ├── +layout.svelte                # Root layout: fonts, RTL, i18n init
│   │   ├── +page.svelte                  # Home page: mounts WeatherScreen
│   │   ├── +error.svelte                 # Error page
│   │   └── api/                          # alerts/{meteoalarm,bom,nhc,eccc} CORS proxies, csp-report
│   └── lib/                              # All app code ($lib alias), abridged
│       ├── api/                          # Network layer
│       │   ├── openMeteo.ts              # Open-Meteo client & mapper
│       │   ├── openMeteoAirQuality.ts    # AQI, Pollen & hourly AQI forecast client
│       │   ├── pirateWeather.ts          # Pirate Weather client
│       │   ├── geocodingSearch.ts        # Location search client
│       │   ├── wmoWeatherCode.ts         # WMO code → i18n description key
│       │   ├── openMeteoMapper.ts / openMeteoTypes.ts  # Response mapping and types
│       │   ├── externalAlerts.ts         # Merges alert sources
│       │   └── alerts/                   # 7 official alert source fetchers + shared.ts
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
│       │   └── airQuality.ts            # AQI tiering, Pollen tiers, AQI_TIER_COLORS
│       ├── i18n/                         # Internationalization
│       │   ├── index.ts / locales.ts
│       │   └── locales/                  # 8 JSON files (symlinked to shared/i18n/locales)
│       ├── stores/
│       │   ├── weather.ts                # Weather state machine & fetch orchestration
│       │   ├── savedLocations.ts         # Multi-location state, favorites list, active selection
│       │   ├── preferences.ts            # Persisted user preferences (units, font, language)
│       │   ├── location.ts              # Geolocation & Nominatim reverse geocoding
│       │   └── advancedMode.ts          # Advanced Mode 12-hour session state
│       ├── actions/ utils/ server/       # Svelte actions, date/path helpers, server-only proxy helpers
│       ├── credits.ts / deviceInfo.ts    # About page data
│       ├── components/                   # Svelte UI components (see section 8)
│       ├── fonts.ts                      # 22 FontPairing definitions
│       └── share.ts                      # html2canvas capture & sharing
├── static/                               # Static assets
├── svelte.config.js / vite.config.ts
├── Dockerfile / docker-compose.yml
```

---

## 3. Build Pipeline

- `npm run dev`: Vite dev server at `localhost:5173`.
- `npm run build`: Compiles Svelte 5 components, bundles JS into a single chunk (`manualChunks: () => 'app'`), emits Node.js server to `build/`.
- `npm run check`: Type-checks with `svelte-check`.

---

## 4. Application Lifecycle & Saved Locations

### Multi-Location Management (`stores/savedLocations.ts`)
Users can save and navigate multiple locations (shipped v1.2.15):
- **Current Location**: Uses browser geolocation API (`navigator.geolocation`).
- **Saved Favorites**: Favorites list persisted in `localStorage`. `SavedLocationsDialog.svelte` handles place search, favorite toggling (♥), reordering, and deletion.
- **Header Switching**: Click location title or search icon to open search/favorites dialog. Header includes favorite heart icon and arrow buttons to cycle between saved places.

---

## 5. Data Flow

### Parallel Fetch Sequence
```
               ┌───────────────────────────┐
               │ Active Location (GPS/Fav) │
               └─────────────┬─────────────┘
                             │
            ┌────────────────┴────────────────┐
            ▼                                 ▼
┌───────────────────────┐         ┌───────────────────────┐
│  fetchWeather()       │         │  fetchAirQuality()    │
│  - Current conditions │         │  - US & EU AQI        │
│  - Minutely nowcast   │         │    (current)          │
│  - Hourly forecast    │         │  - Pollen (CAMS)      │
│  - 10-day forecast    │         │  - Hourly AQI forecast│
└───────────┬───────────┘         └───────────┬───────────┘
            │                                 │
            └────────────────┬────────────────┘
                             │
                             ▼
               ┌───────────────────────────┐
               │  fetchAlerts() (7 sources)│
               └─────────────┬─────────────┘
                             │
                             ▼
               ┌───────────────────────────┐
               │  weatherStore.set(...)    │
               └───────────────────────────┘
```

---

## 6. Domain Models

Immutable value objects (`Temperature`, `WindSpeed`, `Pressure`, `Precipitation`) encapsulate unit conversion. Each provides `displayDual(metricPrimary)` returning `[primary, secondary]` strings.

`airQuality.ts` encapsulates US/EU AQI thresholds, pollutant tiers, Pollen category boundaries (Grass, Birch, Alder), and `AQI_TIER_COLORS`.

---

## 7. State Management

Svelte stores drive application state:
- `stores/weather.ts`: Core weather state (`loading`, `success`, `error`), refresh logic.
- `stores/savedLocations.ts`: Saved favorites array, active location selection, dialog visibility.
- `stores/preferences.ts`: `metricPrimary`, `fontIndex`, `localeIndex` persisted in `localStorage`.
- `stores/advancedMode.ts`: Advanced mode 12-hour session toggle & manual coordinate override.

---

## 8. Component Architecture & Layout Rules

### Component Tree
```
+layout.svelte
└── +page.svelte
    └── WeatherScreen
        ├── Header (location name, region, local-time chip, "Updated" line; actions: search, favorite, current location, settings; location pager when places are saved)
        ├── LocationDetails, RenameLocationDialog, SavedLocationsDialog
        ├── WeatherAlertBanner (merged 7-source alerts, expandable)
        ├── HeroCard (temperature, low/high, rain or snow outlook, US and EU AQI, 4 hour tiles)
        ├── SectionCard "Hourly forecast"
        │   ├── NowcastCard (only when rain or snow is coming; Open-Meteo only)
        │   └── HourlyForecast (ForecastTabs: Conditions, Precipitation, Wind, Pressure, UV, Air quality; cards + OverviewChart)
        ├── SectionCard "10-day forecast"
        │   └── DailyForecast (same 6 tabs; cards + chart)
        ├── SectionCard "Current conditions"  (CurrentConditions part="now")
        ├── SectionCard "Sun & Air"           (CurrentConditions part="sky")
        ├── SectionCard "Activities"          (ActivitiesCard)
        ├── Footer (font name, Help and About links, version, language flag)
        ├── AirQualityDetailDialog
        ├── SettingsScreen (in SettingsPage shell)
        ├── AboutScreen (credits from `lib/credits.ts`)
        └── HelpScreen
```

### Layout Rules
- **Free scroll**: The page is one scrolling column. Every card is always open. There is no accordion and no collapsible section.
- **Air quality tab**: The Air quality tab shows only when air quality data exist.
- **Surfaces**: Use the CSS tokens in `+layout.svelte` (`--surface-section`, `--surface-tile`, `--surface-tile-highlight`, `--hairline`, `--hero-gradient`) — same values as Android `ui/theme/Color.kt`.

---

## 9. Internationalization

Supports 8 languages (mg, ar, en, es, fr, hi, ne, zh). `svelte-i18n` handles reactive translations (`$_('key')`). Native digits for Arabic, Hindi, and Nepali are rendered via `localizeDigits()` character replacement at display time. RTL direction (`<html dir="rtl">`) is enforced when Arabic is selected.

---

## 10. Font System

22 font pairings in `src/lib/fonts.ts`. Styled via `--font-display`, `--font-body`, and `--font-features` CSS variables. Each pairing points to a self-hosted CSS file in `static/fonts` (for example `/fonts/orbitron-outfit.css`). `+layout.svelte` links the CSS file of the selected pairing.

---

## 11. Service Worker & Offline

`src/service-worker.ts` uses two caches. `app-{version}` holds page navigations and app assets (NetworkFirst, offline fallback to cache). `api-cache` holds responses from `api.open-meteo.com`, `air-quality-api.open-meteo.com`, and `nominatim.openstreetmap.org` (NetworkFirst, stamped with `X-SW-Cached-At`). The worker does not precache. On activate, it deletes other caches.

---

## 12. Advanced Mode

Renamed from "Expert Mode" in v1.2.15. Managed via `stores/advancedMode.ts`:
- 12-hour session timeout.
- Manual location search/override.
- Unlocks Settings screen for switching weather providers and toggling individual alert sources.

---

## 13. Settings & Pluggable Data Sources

`SettingsScreen.svelte` provides controls for Weather Source (Open-Meteo vs Pirate Weather with API key validation) and individual toggles for all 7 official alert sources.

---

## 14. Weather Alerts

7 official alert sources (NWS, GDACS, MeteoAlarm, JMA, ECCC, BOM, NHC). CORS-blocked upstreams are proxied via `src/routes/api/alerts/*/+server.ts`. Derived/algorithmic alerts were removed in v1.2.1. Multiple alerts display with expandable details per alert.

---

## 15. Screenshot Sharing

`src/lib/share.ts` uses `html2canvas` to capture branded PNG images (header + card content + copyright watermark) for Web Share API or direct download.

---

## 16. Deployment

Containerized via Docker (`node:24-alpine` multi-stage build running `node build/index.js`). Host port mapped via `docker-compose.yml` (default 3080).

---

## 17. Key Patterns & Decisions

- **Shared i18n Locales**: Symlinked from `shared/i18n/locales/` for 100% parity with Android.
- **Single-Chunk Bundle**: All JS compiled into one chunk (`manualChunks: () => 'app'`).
- **No Derived Alerts**: Only official meteorological agency alerts are displayed.
