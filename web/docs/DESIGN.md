# Design Document — maripana Tokana PWA

This document explains the architecture, data flow, and key patterns of the **maripana Tokana** weather PWA. It mirrors the native Android app's features and design language.

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
| Geocoding | **Nominatim** / Open-Meteo Geocoding API | Reverse geocoding & location search |
| Alerts | 8 official sources (NWS, GDACS, MeteoAlarm, JMA, ECCC, BOM, NHC, WMO SWIC) | Proxied server-side (`routes/api/alerts/`) where CORS is missing |
| Server | **Node.js** (via adapter-node) | Serves the built app directly |

---

## 2. Project Structure

```
web/
├── src/
│   ├── app.html                          # HTML shell
│   ├── app.d.ts                          # Global TypeScript declarations
│   ├── hooks.server.ts                   # /svelte → / redirect
│   ├── service-worker.ts                 # PWA offline caching
│   ├── routes/                           # URL-mapped pages
│   │   ├── +layout.svelte                # Root layout: fonts, RTL, i18n init
│   │   ├── +page.svelte                  # Home page: mounts WeatherScreen
│   │   └── api/alerts/                   # CORS proxy endpoints (meteoalarm, bom, nhc, wmoswic, eccc)
│   └── lib/                              # All app code ($lib alias)
│       ├── api/                          # Network layer
│       │   ├── openMeteo.ts              # Open-Meteo client & mapper
│       │   ├── openMeteoAirQuality.ts    # AQI, Pollen & 48h AQI forecast client
│       │   ├── pirateWeather.ts          # Pirate Weather client
│       │   ├── geocodingSearch.ts        # Location search client
│       │   ├── wmoWeatherCode.ts         # WMO code → emoji + description lookup
│       │   └── alerts/                   # 8 official alert source fetchers + shared.ts
│       ├── domain/                       # Domain models & value objects
│       │   ├── weatherData.ts            # WeatherData, HourlyForecast, DailyForecast, HourlyAirQuality
│       │   ├── temperature.ts            # Temperature (°C ↔ °F)
│       │   ├── pressure.ts              # Pressure (hPa ↔ inHg)
│       │   ├── windSpeed.ts             # WindSpeed (m/s ↔ mph)
│       │   ├── precipitation.ts          # Precipitation (mm ↔ in)
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
│       ├── components/                   # UI components (WeatherScreen, HeroCard,
│       │                                  # SavedLocationsDialog, AirQualityChart,
│       │                                  # UvChart, PollenTierBadge, etc.)
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
│  - Hourly forecast    │         │  - Pollen (CAMS)      │
│  - 7-day forecast     │         │  - 48h AQI forecast   │
└───────────┬───────────┘         └───────────┬───────────┘
            │                                 │
            └────────────────┬────────────────┘
                             │
                             ▼
               ┌───────────────────────────┐
               │  fetchAlerts() (8 sources)│
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
        ├── Header (location name, heart favorite toggle, search trigger, date)
        ├── SavedLocationsDialog (search, list of favorites)
        ├── WeatherAlertBanner (merged 8-source alerts with individual expanders)
        ├── HeroCard (emoji, temp, feels-like, precip, share button)
        ├── CurrentConditions (2-column detail grid: temp, wind, pressure, AQI badge, UV badge, etc.)
        ├── CollapsibleSection "Hourly Forecast"
        │   └── HourlyForecast (horizontal scroll)
        ├── CollapsibleSection "Air Quality Forecast"
        │   └── AirQualityChart (48h trend line chart with colored AQI dots)
        ├── CollapsibleSection "UV Forecast"
        │   └── UvChart (7-day UV trend line chart with severity color badges)
        ├── CollapsibleSection "7-Day Forecast"
        │   └── DailyForecast (horizontal scroll + matching 7-day trend chart)
        └── Footer (font cycle / credits / language cycle)
```

### Layout Rules
- **Section Order**: Current Conditions displays above Hourly Forecast (v1.2.13).
- **Single-Card Expansion**: Expanding one card automatically collapses any other open card (v1.2.14).
- **Location Switch Reset**: Switching locations automatically collapses all open cards (v1.2.14).

---

## 9. Internationalization

Supports 8 languages (mg, ar, en, es, fr, hi, ne, zh). `svelte-i18n` handles reactive translations (`$_('key')`). Native digits for Arabic, Hindi, and Nepali are rendered via `localizeDigits()` character replacement at display time. RTL direction (`<html dir="rtl">`) is enforced when Arabic is selected.

---

## 10. Font System

22 font pairings in `src/lib/fonts.ts`. Styled via `--font-display`, `--font-body`, and `--font-features` CSS variables. Google Fonts loaded dynamically in `+layout.svelte`.

---

## 11. Service Worker & Offline

`src/service-worker.ts` uses three caches: `app-v{hash}` (NetworkFirst app shell), `api-cache` (NetworkFirst API responses), and `font-cache` (CacheFirst web fonts).

---

## 12. Advanced Mode

Renamed from "Expert Mode" in v1.2.15. Managed via `stores/advancedMode.ts`:
- 12-hour session timeout.
- Manual location search/override.
- Unlocks Settings screen for switching weather providers and toggling individual alert sources.

---

## 13. Settings & Pluggable Data Sources

`SettingsScreen.svelte` provides controls for Weather Source (Open-Meteo vs Pirate Weather with API key validation) and individual toggles for all 8 official alert sources.

---

## 14. Weather Alerts

8 official alert sources (NWS, GDACS, MeteoAlarm, JMA, ECCC, BOM, NHC, WMO SWIC). CORS-blocked upstreams are proxied via `src/routes/api/alerts/*/+server.ts`. Derived/algorithmic alerts were removed in v1.2.1. Multiple alerts display with expandable details per alert.

---

## 15. Screenshot Sharing

`src/lib/share.ts` uses `html2canvas` to capture branded PNG images (header + card content + copyright watermark) for Web Share API or direct download.

---

## 16. Deployment

Containerized via Docker (`node:22-alpine` multi-stage build running `node build/index.js`). Host port mapped via `docker-compose.yml` (default 3080).

---

## 17. Key Patterns & Decisions

- **Single-Card Accordion**: Only one section expands at a time to optimize screen space.
- **Shared i18n Locales**: Symlinked from `shared/i18n/locales/` for 100% parity with Android.
- **Single-Chunk Bundle**: All JS compiled into one chunk (`manualChunks: () => 'app'`).
- **No Derived Alerts**: Only official meteorological agency alerts are displayed.
