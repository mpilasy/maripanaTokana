# maripána Tokana PWA

**maripána Tokana** (Malagasy for "a single thermometer") is a Progressive Web App weather dashboard that shows current conditions, hourly forecasts, and a 10-day outlook. It always displays both metric and imperial units side by side, and supports 8 languages with 22 font pairings.

This is the web port of the [Android app](../), built with SvelteKit.

## Features

- Real-time weather data from [Open-Meteo](https://open-meteo.com) API (no key required)
- GPS location with two-step strategy (instant cached + fresh background)
- **Dual-unit display**: every measurement shows both metric and imperial simultaneously
- **Tap to toggle**: tap any value to swap which unit is primary (bold/large) vs secondary (dimmer)
- **8 languages**: Malagasy, Arabic, English, Spanish, French, Hindi, Nepali, Chinese — cycled via flag button in footer
- **22 font pairings** loaded from Google Fonts: cycled via font icon in footer
- Native digit rendering for Arabic, Hindi, and Nepali
- RTL support (Arabic)
- Auto-refresh when tab becomes visible (if data >30 min old)
- Pull-to-refresh via touch gestures
- Edge-to-edge Blue Marble background
- **Screenshot sharing**: capture any section as a branded PNG via `html2canvas` + Web Share API (with download fallback)
- **Installable PWA** with offline support (service worker with NetworkFirst caching)
- Collapsible sections with slide animation for hourly, daily, and conditions
- **Dual-language error screen**: shows browser language as secondary when different from app language
- **Single-file build**: CSS inlined into HTML, JS consolidated into one bundle via `manualChunks`
- Detailed weather information:
  - Temperature with 1 decimal on hero card (current, feels like, min/max)
  - Pressure (hPa / inHg)
  - Humidity (%) with dew point (°C / °F)
  - Wind speed and direction with cardinal compass (m/s / mph)
  - Wind gusts (when available)
  - UV index with severity label
  - Precipitation (rain/snow in mm / inches)
  - Visibility (km / mi)
  - Sunrise/sunset times
  - Hourly forecast (24h horizontal scroll)
  - 10-day daily forecast

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Framework | SvelteKit + Svelte 5 (runes) |
| Language | TypeScript |
| Adapter | `@sveltejs/adapter-static` |
| i18n | `svelte-i18n` |
| Screenshots | `html2canvas` |
| Weather API | Open-Meteo (free, no key) |
| Geocoding | Nominatim (free, no key) |

## Build & Run

```bash
npm install          # Install dependencies
npm run dev          # Dev server at localhost:5173
npm run build        # Production build to build/ (includes CSS inlining)
npm run preview      # Preview production build
npm run check        # Type-check with svelte-check
```

The build step runs `vite build` followed by `scripts/inline-assets.js`, which inlines CSS into `index.html` to reduce HTTP requests.

## Deployment (Docker)

Multi-stage Docker build: node builds the Svelte app, Caddy serves it.

```bash
docker compose up -d --build    # Build and run on port 3080 (default)
```

To use a custom port, create a `.env` file or pass it inline:

```bash
PORT=8080 docker compose up -d --build
```

The container (`maripanaTokana.web`) exposes port 80, mapped to host port `$PORT` (default 3080). The app is available at `/svelte`, and `/` redirects to `/svelte/`.

```
Dockerfile          # Multi-stage: node build → caddy serve
Caddyfile           # Path-based routing + SPA fallback + gzip compression
docker-compose.yml  # Container config (port)
.dockerignore       # Excludes node_modules, .git, build, .svelte-kit
```

### Performance
- **Gzip compression**: All text assets (JS, CSS, fonts) automatically compressed
- **Smart caching**: Versioned assets (hash in filename) cached for 1 year; HTML/service-worker always revalidated
- **Minimal repeat visits**: Only HTML/SW checked on return; JS/CSS served from cache if unchanged

## Architecture

SvelteKit app with framework-agnostic shared code.

```
web/
├── src/
│   ├── lib/
│   │   ├── api/              # Open-Meteo fetch client, types, mapper, WMO codes
│   │   ├── domain/           # Value classes: Temperature, Pressure, WindSpeed, Precipitation
│   │   ├── i18n/             # Locale config, localizeDigits(), 8 JSON translations (symlinked)
│   │   ├── stores/           # Svelte stores (weather, preferences, location)
│   │   ├── components/       # 9 Svelte UI components
│   │   ├── fonts.ts          # 22 FontPairing definitions + Google Fonts URLs
│   │   └── share.ts          # html2canvas capture + Web Share API / download fallback
│   ├── routes/               # +page.svelte, +layout.svelte
│   ├── service-worker.ts
│   └── app.html
├── static/                   # PWA manifest, icons, background
├── scripts/                  # Post-build CSS inlining
├── svelte.config.js          # base: '/svelte'
├── vite.config.ts            # Vite config (single-chunk bundling)
└── package.json              # Dependencies + build scripts
```

## Internationalization

| Language | Tag | Native Digits | Decimal Sep |
|----------|-----|---------------|-------------|
| Malagasy | mg | — | `,` |
| Arabic | ar | Eastern Arabic (٠١٢٣٤٥٦٧٨٩) | `٫` |
| English | en | — | `.` |
| Spanish | es | — | `,` |
| French | fr | — | `,` |
| Hindi | hi | Devanagari (०१२३४५६७८९) | `.` |
| Nepali | ne | Devanagari (०१२३४५६७८९) | `.` |
| Chinese | zh | — | `.` |

All numeric formatting uses ASCII digits internally. Native digits are applied via character replacement at display time (`localizeDigits()`) — same approach as the Android app.

## Font Pairings

| # | Name | Display Font | Body Font |
|---|------|-------------|-----------|
| 0 | Default | system-ui | system-ui |
| 1 | Orbitron + Outfit | Orbitron | Outfit |
| 2 | Rajdhani + Inter | Rajdhani | Inter |
| 3 | Oxanium + Nunito | Oxanium | Nunito |
| 4 | Space Grotesk + DM Sans | Space Grotesk | DM Sans |
| 5 | Sora + Source Sans | Sora | Source Sans 3 |
| 6 | Manrope + Rubik | Manrope | Rubik |
| 7 | Josefin Sans + Lato | Josefin Sans | Lato |
| 8 | Cormorant + Fira Sans | Cormorant Garamond | Fira Sans |
| 9 | Playfair + Work Sans | Playfair Display | Work Sans |
| 10 | Quicksand + Nunito Sans | Quicksand | Nunito Sans |
| 11 | Comfortaa + Karla | Comfortaa | Karla |
| 12 | Baloo 2 + Poppins | Baloo 2 | Poppins |
| 13 | Exo 2 + Barlow | Exo 2 | Barlow |
| 14 | Michroma + Saira | Michroma | Saira |
| 15 | Jost + Atkinson | Jost | Atkinson Hyperlegible |
| 16 | Roboto + Fira Code | system-ui | Fira Code |
| 17 | Montserrat + Open Sans | Montserrat | Open Sans |
| 18 | Space Grotesk + Space Mono | Space Grotesk | Space Mono |
| 19 | Plus Jakarta Sans + Inter | Plus Jakarta Sans | Inter |
| 20 | Archivo + Archivo Narrow | Archivo | Archivo Narrow |
| 21 | Roboto + Lora | system-ui | Lora |

Pairings 16–20 use `font-feature-settings: "tnum"` (tabular numbers) for the body font.

## License

MIT License — (c) Orinasa Njarasoa
