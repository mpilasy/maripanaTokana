# Design Document — maripana Tokana PWA

This document explains the architecture, data flow, and key patterns of the **maripana Tokana** weather PWA for developers who may not be familiar with Svelte or SvelteKit. It maps concepts to more widely known frameworks where helpful.

For the Android app's design documentation, see [`docs/DESIGN.md`](../../docs/DESIGN.md).

---

## Table of Contents

1. [Technology Overview](#1-technology-overview)
2. [Project Structure](#2-project-structure)
3. [Build Pipeline](#3-build-pipeline)
4. [Application Lifecycle](#4-application-lifecycle)
5. [Data Flow](#5-data-flow)
6. [Domain Models](#6-domain-models)
7. [State Management](#7-state-management)
8. [Component Architecture](#8-component-architecture)
9. [Internationalization](#9-internationalization)
10. [Font System](#10-font-system)
11. [Service Worker & Offline](#11-service-worker--offline)
12. [Developer Mode](#12-developer-mode)
13. [Screenshot Sharing](#13-screenshot-sharing)
14. [Deployment](#14-deployment)
15. [Key Patterns & Decisions](#15-key-patterns--decisions)

---

## 1. Technology Overview

| Layer | Technology | Role |
|-------|-----------|------|
| Framework | **SvelteKit** (v2) + **Svelte 5** | UI framework + routing + build tooling |
| Language | **TypeScript** | Type safety throughout |
| Build | **Vite** | Dev server + production bundler |
| Hosting | **adapter-static** | Generates a fully static site (no server) |
| i18n | **svelte-i18n** | Runtime internationalization (8 languages) |
| Screenshots | **html2canvas** | DOM-to-canvas capture for sharing |
| Weather API | **Open-Meteo** | Free weather data (no API key) |
| Geocoding | **Nominatim** (OpenStreetMap) | Reverse geocoding (coordinates to place name) |
| Server | **Caddy** (in Docker) | Static file serving with SPA fallback |

### Why Svelte?

Svelte compiles components to efficient vanilla JavaScript at build time — there's no virtual DOM diffing at runtime. This results in:
- Smaller bundles (~15-25 KB)
- Faster runtime performance (direct DOM updates)
- Less boilerplate (reactivity is built into the language)

### Svelte Concepts for Non-Svelte Developers

| Svelte Concept | Equivalent In Other Frameworks | What It Does |
|----------------|---------------------|--------------|
| `.svelte` file | `.jsx` / `.vue` file | Single-file component with `<script>`, HTML template, and `<style>` |
| `$state(value)` | `useState(value)` | Declares reactive state that triggers re-renders when changed |
| `$derived(expr)` | `useMemo(() => expr)` | Computed value that auto-updates when dependencies change |
| `$effect(() => {...})` | `useEffect(() => {...})` | Side effect that runs when reactive dependencies change |
| `$props()` | `props` / `defineProps()` | Declares component input properties |
| `{#if ...}` / `{:else}` | Ternary in JSX / `v-if` | Conditional rendering in templates |
| `{#each items as item}` | `.map()` in JSX / `v-for` | List rendering |
| `$store` (with `$` prefix) | `useSelector()` / `computed()` | Auto-subscribes to a reactive store and gets its current value |
| `bind:this={el}` | `useRef()` / `ref` | Gets a reference to a DOM element |
| `transition:slide` | CSS transitions / `<Transition>` | Built-in animated enter/exit transitions |
| `onMount(() => {...})` | `useEffect(() => {...}, [])` | Runs once when component first appears in the DOM |
| `<svelte:head>` | `<Helmet>` / `useHead()` | Injects elements into `<head>` (title, meta, link tags) |

### SvelteKit Concepts

SvelteKit is a full application framework built on top of Svelte:

| SvelteKit Concept | Next.js Equivalent | What It Does |
|-------------------|-------------------|--------------|
| `src/routes/+page.svelte` | `app/page.tsx` | Page component for a URL route |
| `src/routes/+layout.svelte` | `app/layout.tsx` | Shared layout wrapping all pages |
| `adapter-static` | `output: 'export'` | Generates static HTML (no server needed) |
| `$lib/` alias | `@/lib/` | Import alias for `src/lib/` directory (Svelte-specific code) |
| `$lib/` sub-paths | — | All app code under `src/lib/` (api, domain, i18n, stores, etc.) |
| `src/service-worker.ts` | Custom setup | Built-in service worker support with build manifest |

---

## 2. Project Structure

```
web/
├── src/
│   ├── app.html                          # HTML shell (viewport, PWA meta, fonts preconnect)
│   ├── app.d.ts                          # Global TypeScript declarations
│   ├── service-worker.ts                 # PWA offline caching (3 cache strategies)
│   ├── routes/                           # URL-mapped pages
│   │   ├── +layout.svelte                # Root layout: fonts, RTL, i18n init, auto-refresh
│   │   └── +page.svelte                  # Home page: mounts WeatherScreen
│   └── lib/                              # All app code (imported via $lib)
│       ├── api/                          # Network layer
│       │   ├── openMeteo.ts              # API client: fetchWeather(lat, lon)
│       │   ├── openMeteoTypes.ts         # Response type definitions
│       │   ├── openMeteoMapper.ts        # API response → domain model conversion
│       │   └── wmoWeatherCode.ts         # WMO code → emoji + i18n key lookup
│       ├── domain/                       # Business logic / value objects
│       │   ├── weatherData.ts            # WeatherData, HourlyForecast, DailyForecast
│       │   ├── temperature.ts            # Temperature (°C ↔ °F) with displayDual()
│       │   ├── pressure.ts              # Pressure (hPa ↔ inHg) with displayDual()
│       │   ├── windSpeed.ts             # WindSpeed (m/s ↔ mph) with displayDual()
│       │   └── precipitation.ts          # Precipitation (mm ↔ in) with displayDual()
│       ├── i18n/                         # Internationalization
│       │   ├── index.ts                  # svelte-i18n setup + locale registration
│       │   ├── locales.ts                # Locale config + localizeDigits()
│       │   └── locales/                  # 8 JSON files (symlinked to shared/i18n/locales)
│       ├── stores/
│       │   ├── weather.ts                # Weather state machine + fetch orchestration
│       │   ├── preferences.ts            # Persisted user preferences (units, font, language)
│       │   └── location.ts              # Geolocation + Nominatim reverse geocoding
│       ├── components/                   # UI components (9 .svelte files)
│       ├── fonts.ts                      # 22 FontPairing definitions + Google Fonts URLs
│       └── share.ts                      # html2canvas capture + Web Share API / download
├── static/                               # Files copied as-is to build output
├── scripts/
│   └── inline-assets.js                  # Post-build: inlines CSS into HTML
├── svelte.config.js                      # SvelteKit config (static adapter)
├── vite.config.ts                        # Vite config (single-chunk bundling)
├── package.json                          # Dependencies + build scripts
├── tsconfig.json
├── Dockerfile                            # Multi-stage: node build → caddy serve
├── Caddyfile                             # SPA routing + service worker headers
└── docker-compose.yml                    # Container config (port 3080)
```

**Total**: ~42 source files, ~2,900 lines of code.

---

## 3. Build Pipeline

### Development

```
npm run dev  →  Vite dev server at localhost:5173
                (hot module replacement, no service worker)
```

### Production

```
npm run build  →  Step 1: vite build
                          ↓
                  Compiles .svelte → JS, bundles into single chunk,
                  generates service-worker.js, copies static/
                          ↓
                  Step 2: node scripts/inline-assets.js
                          ↓
                  Finds <link href="*.css"> in index.html,
                  reads CSS file contents, replaces with <style> tag,
                  deletes the now-unused CSS file
                          ↓
                  Output: build/ directory
                  - index.html (CSS inlined, JS referenced)
                  - _app/immutable/entry/*.js (single app bundle)
                  - service-worker.js
                  - manifest.json, icons, background image
```

### Why CSS Inlining?

The app targets mobile users on potentially slow connections. Inlining CSS into the HTML eliminates one round-trip HTTP request. JavaScript stays as a separate file because ES modules loaded from `data:` URIs cannot resolve imports to other modules (a browser limitation discovered during development).

### Bundle Strategy

Vite is configured with `manualChunks: () => 'app'` which forces all JavaScript into a single bundle file. Without this, Vite would split the code into ~20 separate chunks. For a small app like this, the overhead of multiple HTTP requests outweighs any lazy-loading benefit.

---

## 4. Application Lifecycle

### Startup Sequence

```
Browser loads index.html
    │
    ├── Service worker registers (if production build)
    │
    ├── +layout.svelte initializes:
    │   ├── Read locale_index from localStorage
    │   ├── Initialize svelte-i18n with saved or detected language
    │   ├── Set <html dir="rtl"> if Arabic, "ltr" otherwise
    │   ├── Set <html lang="mg|ar|en|...">
    │   ├── Load Google Fonts URL for current font pairing
    │   └── Attach visibilitychange listener (auto-refresh when tab refocused)
    │
    ├── +page.svelte waits for i18n ready, then mounts WeatherScreen
    │
    └── WeatherScreen.svelte onMount:
        └── doFetchWeather()
            ├── Check localStorage for cached GPS coordinates
            │   ├── If cached: fetch weather immediately (instant result)
            │   └── Display data while getting fresh location
            ├── Request fresh GPS position from browser
            ├── Cache new coordinates in localStorage
            ├── If moved >5 km from cached position: re-fetch weather
            └── Set weatherState to success/error
```

### State Machine

WeatherScreen renders based on three possible states:

```
                    ┌──────────┐
    App starts ───→ │ loading  │ ──→ Spinner
                    └──────────┘
                       │    │
                success│    │catch
                       ▼    ▼
               ┌──────────┐  ┌─────────┐
               │ success  │  │  error  │ ──→ Error message + Retry button
               └──────────┘  └─────────┘
                    │              │
                    │  retry click │
                    │    ┌─────────┘
                    ▼    ▼
               ┌──────────┐
               │ loading  │  (or isRefreshing if we already have data)
               └──────────┘
```

If we already have weather data and the user refreshes (pull-to-refresh or tab refocus), we show the existing data with a small spinner overlay rather than replacing everything with a loading screen.

---

## 5. Data Flow

### Fetch Sequence

```
┌──────────┐     ┌──────────────┐     ┌─────────────────┐
│ Browser  │────→│ location.ts  │────→│ Geolocation API │
│ GPS API  │     │ getPosition()│     │ (browser native) │
└──────────┘     └──────────────┘     └─────────────────┘
                        │
              lat, lon  │
                        ▼
          ┌─────────────────────────┐
          │ Parallel fetch:         │
          │ ┌─────────────────────┐ │
          │ │ openMeteo.ts        │ │  GET api.open-meteo.com/v1/forecast
          │ │ fetchWeather(lat,lon)│ │  ?latitude=...&longitude=...
          │ └─────────────────────┘ │  &current=temperature_2m,wind_speed_10m,...
          │ ┌─────────────────────┐ │  &hourly=temperature_2m,...
          │ │ location.ts         │ │  &daily=temperature_2m_max,...
          │ │ reverseGeocode()    │ │  &forecast_days=10
          │ └─────────────────────┘ │
          │   GET nominatim/reverse │
          └─────────────────────────┘
                        │
        OpenMeteoResponse + locationName
                        │
                        ▼
          ┌─────────────────────────┐
          │ openMeteoMapper.ts      │
          │ mapToWeatherData()      │
          │                         │
          │ - Parse ISO timestamps  │
          │ - Filter hourly (next   │
          │   24h only)             │
          │ - Create Temperature,   │
          │   Pressure, WindSpeed,  │
          │   Precipitation objects │
          └─────────────────────────┘
                        │
               WeatherData object
                        │
                        ▼
          ┌─────────────────────────┐
          │ weatherState store      │
          │ set({ kind: 'success',  │
          │        data: ... })     │
          └─────────────────────────┘
                        │
          Svelte reactivity propagates
                        │
       ┌────────────────┼───────────────┐
       ▼                ▼               ▼
   HeroCard    HourlyForecast    CurrentConditions
   DailyForecast    Footer    (all re-render)
```

### API Parameters

The app fetches 15 current-weather fields, 3 hourly fields (for 10 days, filtered to next 24h), and 6 daily fields (10-day forecast). Wind speed is requested in m/s; all conversions to imperial happen client-side in the domain model classes.

---

## 6. Domain Models

The domain layer consists of immutable value classes that encapsulate unit conversion. Each class stores the metric value internally and computes the imperial equivalent on demand.

### Pattern: Dual-Unit Display

Every measurement class follows the same pattern:

```typescript
class Temperature {
    private constructor(readonly celsius: number) {}

    get fahrenheit(): number {
        return this.celsius * 9 / 5 + 32;
    }

    // Returns [primaryString, secondaryString] based on user preference
    displayDual(metricPrimary: boolean): [string, string] {
        return metricPrimary
            ? [this.displayCelsius(), this.displayFahrenheit()]
            : [this.displayFahrenheit(), this.displayCelsius()];
    }

    static fromCelsius(c: number): Temperature {
        return new Temperature(c);
    }
}
```

The `displayDual()` method returns a tuple of `[primary, secondary]` strings. The UI renders the primary value large and bold, and the secondary value smaller and dimmed. When the user taps any value, the `metricPrimary` preference flips, and all `displayDual()` calls across the entire app reactively swap their outputs.

### Domain Model Graph

```
WeatherData
├── locationName: string
├── timestamp: number (epoch ms)
├── temperature: Temperature          ←── celsius ↔ fahrenheit
├── feelsLike: Temperature
├── tempMin: Temperature
├── tempMax: Temperature
├── pressure: Pressure                ←── hPa ↔ inHg
├── humidity: number (%)
├── dewPoint: Temperature
├── windSpeed: WindSpeed              ←── m/s ↔ mph
├── windDeg: number (0-360°)
├── windGust: WindSpeed | null
├── rain: Precipitation | null        ←── mm ↔ inches
├── snow: Precipitation | null
├── weatherCode: number (WMO 0-99)
├── isDay: boolean
├── uvIndex: number
├── visibility: number (meters)
├── sunrise: number (epoch seconds)
├── sunset: number (epoch seconds)
├── dailySunrise: number[] (epoch ms per day)
├── dailySunset: number[] (epoch ms per day)
├── hourlyForecast: HourlyForecast[]
│   └── { time, temperature, weatherCode, precipProbability }
└── dailyForecast: DailyForecast[]
    └── { date, tempMax, tempMin, weatherCode, precipProbability }
```

### WMO Weather Codes

The World Meteorological Organization defines standard numeric codes for weather conditions. `wmoWeatherCode.ts` maps these to:
- **Emoji**: `wmoEmoji(code, isNight)` — returns a weather emoji with day/night variants (e.g., code 0 returns `☀️` during day, `🌙` at night)
- **i18n key**: `wmoDescriptionKey(code)` — returns a translation key like `"wmo_clear_sky"` that resolves to "Clear sky" (en), "Lanitra manga" (mg), etc.

---

## 7. State Management

### Svelte Stores (the short version)

A Svelte store is a reactive container. Think of it like an observable or a signal:

```typescript
import { writable } from 'svelte/store';

// Create a store with initial value
const count = writable(0);

// Update it
count.set(5);
count.update(n => n + 1);

// In a .svelte component, prefix with $ to auto-subscribe:
// <p>{$count}</p>
// This automatically re-renders when count changes.
```

### Svelte Store Files

#### `stores/weather.ts` — Weather State Machine

```typescript
type WeatherState =
    | { kind: 'loading' }
    | { kind: 'success'; data: WeatherData }
    | { kind: 'error'; message: string };
```

The `doFetchWeather()` function implements a two-stage location strategy:
1. **Instant**: If we have cached GPS coordinates in `localStorage`, fetch weather at that location immediately and display it.
2. **Fresh**: Request fresh GPS from the browser (may take 1-15 seconds). If the new position is more than ~5 km from the cached one, re-fetch weather at the new location.

This means the user sees data almost instantly on repeat visits, even before the GPS lock.

`refreshIfStale()` checks if the last fetch was more than 30 minutes ago and triggers a new fetch. It's called on the `visibilitychange` browser event (when the user switches back to the app's tab).

#### `stores/preferences.ts` — Persisted User Preferences

Three values, all persisted to `localStorage`:

| Store | Type | Default | Cycled By |
|-------|------|---------|-----------|
| `metricPrimary` | `boolean` | `true` | Tapping any dual-unit value |
| `fontIndex` | `number` (0-21) | `0` | Tapping "Aa" in footer |
| `localeIndex` | `number` (0-7) | `0` | Tapping flag emoji in footer |

The `persistedWritable<T>(key, default)` helper creates a Svelte writable store that reads its initial value from `localStorage` and writes back on every change.

### `stores/location.ts` — Geolocation Utilities

Utility functions (not a Svelte store):

- `getPosition()`: Wraps `navigator.geolocation.getCurrentPosition()` in a Promise with 15-second timeout.
- `reverseGeocode(lat, lon)`: Calls the Nominatim API to convert coordinates to a human-readable place name.

**Location Name Refinement:**
To ensure a clean UI, the location name is refined by:
1.  **Splitting by separators**: Taking only the first part before commas, semicolons, or dashes (e.g., "Paris" from "Paris, France").
2.  **Subtext Extraction**: The region and country are extracted into a separate `subtext` field displayed on a smaller second line.
3.  **DMS Formatting**: Coordinates are formatted into Degrees, Minutes, and Seconds (DMS) format (e.g., `48°51'24"N`) and displayed on two lines when toggled.

- `getCachedLocation()` / `cacheLocation()`: Read/write last known coordinates to `localStorage`.
- `movedSignificantly()`: Returns `true` if lat or lon changed by more than 0.045 degrees (~5 km).

---

## 8. Component Architecture

### Component Tree

```
+layout.svelte                  (root: fonts, RTL, i18n, auto-refresh)
└── +page.svelte                (i18n loading gate)
    └── WeatherScreen           (state switch, pull-to-refresh, error screen)
        ├── [loading] → Spinner
        ├── [error]   → Error message + dual-language retry button
        └── [success] →
            ├── Header (location name, date, updated time)
            ├── HeroCard (emoji, temp, feels-like, precip, share btn)
            ├── CollapsibleSection "Hourly" (expanded by default)
            │   └── HourlyForecast (horizontal scrolling cards)
            ├── CollapsibleSection "This Week" (collapsed by default)
            │   └── DailyForecast (10-day vertical list)
            ├── CollapsibleSection "Conditions" (collapsed by default)
            │   └── CurrentConditions (2-column detail card grid)
            │       └── DetailCard × 10 (each with DualUnitText)
            └── Footer (font cycle / credits / language cycle)
```

### Key Components Explained

#### WeatherScreen.svelte (388 lines) — The Orchestrator

This is the root component that:
1. Calls `doFetchWeather()` on mount
2. Switches between loading/success/error states
3. Handles pull-to-refresh via touch events (`touchstart/move/end`)
4. Renders the fixed header (location + date) and fixed footer
5. Contains the scrollable content area between them
6. Shows a dual-language error screen when geolocation or API fails

**Dual-language error screen**: When an error occurs, the primary message is shown in the app's selected language. If the browser's system language differs from the app language, a secondary message is shown in the browser language at reduced opacity. This helps users who may not read the app's default language (Malagasy).

#### HeroCard.svelte (184 lines) — Main Weather Display

Displays the current weather prominently:
- Weather emoji (day/night variant based on sunrise/sunset)
- Localized weather description
- Large temperature with dual units
- "Feels like" temperature
- Current precipitation (rain/snow amount, or "No precip")
- Share button (top-right corner)
- Copyright watermark

#### CollapsibleSection.svelte (107 lines) — Animated Sections

Wraps content in an expandable/collapsible container:
- Click the header to toggle
- Content slides in/out with a 300ms animation (Svelte's `transition:slide`)
- Chevron rotates via CSS transition
- Share button appears next to the title when expanded

#### DualUnitText.svelte (48 lines) — Unit Toggle

Renders two lines of text (primary bold, secondary dimmed). When clicked, it calls `toggleUnits()` which flips `metricPrimary`, causing every `DualUnitText` in the app to swap its primary/secondary values simultaneously.

#### HourlyForecast.svelte (99 lines) — Horizontal Scroll

A flexbox row with `overflow-x: auto` and `scroll-snap-type: x mandatory` — the CSS equivalent of Android's `LazyRow`. Each card shows time, emoji, temperature, and precipitation probability.

The day/night emoji logic checks each hour's timestamp against the `dailySunrise` and `dailySunset` arrays to determine whether to show a sun or moon variant.

#### CurrentConditions.svelte (199 lines) — Detail Grid

A 2-column CSS grid displaying 10 detail cards:
- Min/Max temperature
- Wind speed with cardinal compass direction (N, NNE, NE, etc.)
- Wind gust (empty placeholder if unavailable)
- Atmospheric pressure
- Humidity with dew point
- UV index with severity label (Low → Extreme)
- Visibility
- Sunrise/sunset times

Cardinal directions and UV labels are translated strings stored as arrays in each locale JSON file.

---

## 9. Internationalization

### 8 Supported Languages

| Index | Tag | Language | Special Features |
|-------|-----|----------|-----------------|
| 0 | mg | Malagasy | Comma decimal separator |
| 1 | ar | Arabic | RTL layout, Eastern Arabic numerals (٠١٢), Arabic decimal (٫) |
| 2 | en | English | (none) |
| 3 | es | Spanish | Comma decimal separator |
| 4 | fr | French | Comma decimal separator |
| 5 | hi | Hindi | Devanagari numerals (०१२) |
| 6 | ne | Nepali | Devanagari numerals (०१२) |
| 7 | zh | Chinese | (none) |

### How It Works

**svelte-i18n** provides a reactive `$_('key')` function that returns the translated string for the current locale. When the locale changes, all `$_()` calls across the app re-evaluate automatically.

Each locale is a JSON file with ~66 keys covering:
- Weather descriptions (32 WMO conditions)
- UI labels (section headers, detail card titles)
- Error messages
- Cardinal directions (16-element array: N, NNE, NE, ...)
- UV severity labels (5-element array: Low → Extreme)

### Native Digit Rendering

For Arabic, Hindi, and Nepali, numeric digits are replaced with native script characters at display time:

```typescript
function localizeDigits(s: string, locale: SupportedLocale): string {
    let result = s;
    // Replace decimal separator if locale specifies one
    if (locale.decimalSep) {
        result = result.replace('.', locale.decimalSep);
    }
    // Replace ASCII digits with native script digits
    if (locale.nativeZero) {
        result = result.replace(/[0-9]/g, d =>
            String.fromCodePoint(locale.nativeZero! + parseInt(d))
        );
    }
    return result;
}
```

This approach (character replacement at display time) mirrors the Android app's implementation and keeps all internal number formatting in ASCII.

### RTL Support

When Arabic is selected:
- `<html dir="rtl">` is set, causing the entire page layout to mirror
- The footer forces `dir="ltr"` to keep credits and controls in their expected positions
- When switching away from Arabic, `dir="ltr"` is restored

---

## 10. Font System

22 font pairings are defined in `src/lib/fonts.ts`. Each pairing specifies:

```typescript
interface FontPairing {
    name: string;             // Display name (e.g., "Orbitron + Outfit")
    displayFamily: string;    // CSS font-family for headings
    bodyFamily: string;       // CSS font-family for body text
    bodyFontFeatures?: string; // e.g., '"tnum"' for tabular numbers
    googleFontsUrl?: string;  // Google Fonts CSS URL to load
}
```

Pairing 0 uses `system-ui` (no network request). Pairings 1-21 load from Google Fonts. Pairings 16-20 enable tabular numbers (`font-feature-settings: "tnum"`) for aligned numeric columns.

The current font is applied via CSS custom properties:
- `--font-display`: Used by location name, temperature values, section titles
- `--font-body`: Used by descriptions, labels, card text
- `--font-features`: Applied to body font elements

Font loading happens in `+layout.svelte` via a `<link>` tag injected into `<head>`. When the user cycles fonts, the old link is replaced with the new one.

---

## 11. Service Worker & Offline

### Caching Strategies

The service worker (`src/service-worker.ts`) uses three named caches with different strategies:

| Cache Name | Strategy | Used For | Rationale |
|-----------|----------|----------|-----------|
| `app-v{hash}` | **NetworkFirst** | App shell (HTML, JS) | Always try to get latest; serve cached if offline |
| `api-cache` | **NetworkFirst** | Open-Meteo + Nominatim responses | Fresh data preferred; cached data as offline fallback |
| `font-cache` | **CacheFirst** | Google Fonts CSS + font files | Fonts rarely change; save bandwidth |

**NetworkFirst** means: try the network first; if it fails, serve from cache. Each successful network response is also written to cache for future offline use.

**CacheFirst** means: if it's in cache, serve immediately without hitting the network. Only fetch from network if not cached yet.

### Install & Activate

```
Install:  skipWaiting()          — Activate immediately (don't wait for old tabs to close)
Activate: clients.claim()        — Take control of all open tabs
          Delete old version caches
```

There is no precaching (pre-downloading all assets on install). The cache populates naturally as the user navigates. This avoids downloading dozens of files on first visit.

### Offline Fallback

If the app shell request fails and there's no cache hit, the service worker tries to serve `/index.html` from cache (SPA fallback). If even that fails, it returns a `503 Service Unavailable` response.

---

## 12. Screenshot Sharing

`src/lib/share.ts` implements branded screenshot capture:

1. **Capture**: Uses `html2canvas` (dynamically imported to avoid SSR issues) to render the header element and content element to separate canvases at 2x resolution.

2. **Composite**: Creates a new canvas with:
   - Brand-colored background (`#0E0B3D`)
   - 32px padding on all sides
   - Header canvas on top
   - 16px gap
   - Content canvas below
   - Copyright watermark at the bottom: "© Orinasa Njarasoa • maripanaTokana"

3. **Share**: Converts the composite canvas to a PNG blob, then:
   - **Mobile** (Web Share API available): Opens the native share sheet with the image
   - **Desktop** (fallback): Downloads the PNG as `maripanatokana-weather.png`

Share buttons appear on the HeroCard and on each CollapsibleSection header (only when expanded).

---

## 13. Deployment

### Docker (Production)

The Docker setup builds and serves the Svelte app:

```dockerfile
# Stage 1: Build
FROM node:22-alpine
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .

RUN npm run build

# Stage 2: Serve
FROM caddy:alpine
COPY Caddyfile /etc/caddy/Caddyfile
COPY --from=build /app/build /srv/app
EXPOSE 80
```

### Routing Configuration

The Caddyfile implements path-based routing with optimization:

**Compression**: Gzip enabled for all text assets (JS, CSS, fonts)

**Routing**:
- `/` → Svelte app (SPA fallback)
- `/svelte` → 301 redirect to `/` (backwards compatibility)

**Caching**:
- **Versioned assets** (regex: `.*\.[a-f0-9]{8}\.(js|css|woff2|ttf)$`): `max-age=31536000, immutable` (1 year)
  - Vite hashes filenames; unchanged assets stay cached
- **HTML files**: `no-cache, public, must-revalidate` (browser always checks, serves cached if unchanged)
- **Service worker**: `no-cache, no-store, must-revalidate` (always fetch fresh)

**Result**: On repeat visits, browser only downloads HTML (metadata check), reuses all cached JS/CSS/fonts if unchanged.

### Docker Compose

```bash
docker compose up -d --build                       # Default port 3080
PORT=8080 docker compose up -d --build             # Custom port
```

The container runs Caddy on port 80, mapped to the host port.

---

## 14. Key Patterns & Decisions

### Why No Permission Screen?

Early versions had a dedicated permission screen before showing weather data. This was removed because:
- The browser already shows its own geolocation permission prompt
- An extra screen just delays the user from seeing weather data
- If permission is denied, the error screen handles it with a clear retry button

### Two-Stage Location Strategy

Instead of blocking on a fresh GPS fix every time:
1. Read cached coordinates from `localStorage` (instant)
2. Fetch weather at cached location (fast API call)
3. Request fresh GPS in background
4. Only re-fetch if user moved significantly (~5 km)

This provides near-instant weather display on repeat visits.

### Dual-Language Error Screen

The error screen shows messages in both the app language and the browser's system language (if different). This is critical because the default language is Malagasy — a user whose browser is set to English will see both languages, ensuring they can understand the error.

### Single-Chunk Bundle

Vite normally splits code into many small chunks for lazy loading. For this app (~45 KB total), the overhead of multiple HTTP requests outweighs any benefit. All code is bundled into one file via `manualChunks: () => 'app'`.

### CSS Inlining

A post-build script (`scripts/inline-assets.js`) reads the generated CSS file and inlines it as a `<style>` tag in `index.html`. This eliminates one HTTP request. JS is not inlined because ES modules loaded from `data:` URIs cannot resolve relative imports (a browser security restriction).

### No Precaching
Early versions had the service worker precache all assets on install, which caused dozens of simultaneous downloads. The current approach lets caches fill naturally via NetworkFirst — assets are cached as they're requested, and served from cache when offline.

---

## 12. Developer Mode

Developer Mode allows testing the application in different geographic locations.

### 12.1 Activation & Lifecycle
- **Activation**: Long-press the location name in the header.
- **Expiration**: Dev Mode automatically expires after 4 hours (`dev_mode_expiration` in localStorage).
- **Deactivation**: Double-tap the "DEV" badge to immediately clear overrides and restore the actual GPS location.

### 12.2 Location Overrides
When active, the user can search for a new location via the `LocationOverrideDialog`.
- **Search**: Supports city names or direct `lat,lon` input via Open-Meteo Geocoding API.
- **My Location**: A dedicated icon next to the search field allows quickly resetting to the real device GPS.
- **Persistence**: Overridden coordinates and names are stored in localStorage and prioritized over browser GPS data in `weather` store.

---

## 13. Screenshot Sharing
Temperature, Pressure, WindSpeed, and Precipitation are immutable classes with private constructors and static factory methods. This ensures unit conversions are always consistent and prevents accidental mutation. The `displayDual()` method encapsulates the metric/imperial toggle logic in one place.

---

## Appendix: File Sizes

| Category | Files | Lines |
|----------|-------|-------|
| Components | 9 | ~1,280 |
| Stores | 2 | ~120 |
| API | 4 | ~217 |
| Domain | 5 | ~177 |
| i18n | 9 | ~535 |
| Fonts | 1 | ~169 |
| Share | 1 | ~73 |
| Routes | 2 | ~136 |
| Service Worker | 1 | ~85 |
| Config | 5 | ~80 |
| Scripts | 1 | ~42 |
| **Total** | **~42** | **~2,900** |
