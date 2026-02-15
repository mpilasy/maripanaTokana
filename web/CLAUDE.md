# Project: maripana Tokana PWA (Weather App)
**Goal:** SvelteKit weather PWA — port of the Android app with simultaneous Metric/Imperial display.

## Shared Tech
- **Weather API:** [Open-Meteo](https://open-meteo.com) — free, no API key
- **Reverse Geocoding:** [Nominatim](https://nominatim.openstreetmap.org) — free, no API key
- **Screenshots:** `html2canvas` — captures DOM sections, composites onto branded canvas, shares via Web Share API
- **i18n:** 8 languages (mg, ar, en, es, fr, hi, ne, zh) with ~76 keys + 2 arrays (`cardinal_directions`, `uv_labels`). Default: Malagasy (mg, index 0).

## Project Layout
```
web/
├── shared/             # Framework-agnostic code (api, domain, i18n, fonts, share)
├── svelte/             # SvelteKit app
├── Dockerfile          # Multi-stage build
├── Caddyfile           # Path-based routing + gzip + caching
├── docker-compose.yml  # Container configuration
├── package.json        # Root scripts (build)
└── CLAUDE.md           # This file
```

### Shared Code (`shared/`)
Framework-agnostic TypeScript used by the Svelte app:
- **`api/`** — Open-Meteo fetch client, response types, mapper, WMO weather codes
- **`domain/`** — Value classes: Temperature, Pressure, WindSpeed, Precipitation, WeatherData
- **`i18n/`** — Locale definitions (`locales.ts`), `localizeDigits()`, 8 JSON translation files
- **`stores/location.ts`** — Geolocation utilities + Nominatim reverse geocoding
- **`fonts.ts`** — 22 FontPairing definitions + Google Fonts URLs
- **`share.ts`** — html2canvas capture + Web Share API / download fallback

Svelte imports shared code via `$shared/...` alias.

## Build / Docker
```bash
npm run build          # Builds Svelte app
docker compose up -d --build
```

## Deployment
```bash
docker compose up -d --build          # Default port 3080
PORT=8080 docker compose up -d --build  # Custom port via env
```
- **Dockerfile:** Multi-stage — builds Svelte app in `node:22-alpine`, serves via `caddy:alpine`. Uses root-level `html2canvas` symlink for shared code resolution.
- **Caddyfile:** Path-based routing with SPA fallback (`/svelte/*`), gzip compression, smart caching headers
- **docker-compose.yml:** Container `maripanaTokana.web`, port `${PORT:-3080}:80`, `restart: unless-stopped`
- App served at `/svelte` (CSS inlined, single JS bundle)
- `/` → redirects to `/svelte/`
- Designed to sit behind a reverse proxy (e.g., Nginx Proxy Manager) that handles TLS

### Performance Features
- **Gzip compression**: All text assets automatically compressed
- **Aggressive caching**: Versioned assets (hash in filename) cached for 1 year; HTML/service-worker always revalidated

## Core Features

- **Dual Units with Toggle:** Every measurement shows both metric and imperial. Tap any value to swap primary (bold/large) vs secondary (smaller/dimmer). Preference stored in `localStorage`.
- **Font Cycling:** 22 font pairings loaded from Google Fonts. Cycled via font icon in footer. Default (index 0) uses system-ui.
- **In-App Language Cycling:** 8 languages cycled via flag button in footer. Locale index stored in `localStorage`.
- **Native Digit Rendering:** `localizeDigits(s, locale)` replaces ASCII 0-9 with native digits for ar (U+0660), hi/ne (U+0966). Also replaces decimal separator for mg/es/fr (`,`) and ar (`٫`). No `toLocaleString()` used.
- **RTL Support:** `document.documentElement.dir = 'rtl'` when locale is `ar`, `ltr` otherwise. Footer forced LTR via `dir="ltr"`.
- **Two-Step Location:** Cached coords from localStorage -> fresh `navigator.geolocation` -> re-fetch if moved >0.045 deg (~5 km).
- **Auto-refresh:** `visibilitychange` event triggers refresh if data >30 min old.
- **Pull-to-Refresh:** Touch event handling on scroll container (touchstart/touchmove/touchend). Also click "Last updated" timestamp to refresh.
- **Current Conditions Grid:** 3 full-width merged cards at top (High/Low, Wind/Gust, Sunrise/Sunset) followed by 2-column detail cards (Temperature, Precipitation, Pressure, Humidity, UV, Visibility).
- **Screenshot Sharing:** `html2canvas` captures header and content, composites onto branded canvas with `#0E0B3D` background and copyright watermark. Uses `navigator.share({ files })` with PNG download fallback. Share buttons on HeroCard and each CollapsibleSection.
- **Service Worker:** NetworkFirst for same-origin requests, CacheFirst for Google Fonts. `skipWaiting()` + `clients.claim()`. No precaching — cache populates naturally.
- **Collapsible Sections:** Animated expand/collapse with chevron rotation. Share button next to section title.
- **Dual-Language Error Screen:** When the browser language differs from the app language, error screen shows a faded secondary translation.

## Visual Style
- Dark theme, Blue Marble background at 12% opacity
- Translucent cards (base: `rgba(42, 31, 165, 0.6)`, hero: `rgba(42, 31, 165, 0.8)`)
- Theme color: `#0E0B3D`
- Edge-to-edge with safe area padding (`env(safe-area-inset-top)`)

## Key Android->Web Mappings
| Android | Web |
|---------|-----|
| `SharedPreferences` | `localStorage` |
| `AnimatedVisibility(expandVertically)` | CSS/JS slide transitions |
| `LazyRow` | `display: flex; overflow-x: auto; scroll-snap-type` |
| `GraphicsLayer.toImageBitmap()` | `html2canvas` + canvas compositing |
| `Intent.ACTION_SEND` | `navigator.share()` with download fallback |
| `FusedLocationProvider` | `navigator.geolocation` |
| `android.location.Geocoder` | Nominatim reverse geocoding API |
| `Lifecycle.ON_RESUME` | `document.visibilitychange` |
| `R.string.xxx` | i18n key lookup (framework-specific) |

## Developer Context
- Owner is an experienced C#, Java, C++ developer.
- Prefer concise, technically accurate code.
- Avoid redundant explanations; focus on implementation details and logic.
- Port from Android source at `../` — same logic, different platform APIs.
