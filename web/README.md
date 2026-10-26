# maripànaTokana Web App

**maripànaTokana** (Malagasy for "a single thermometer") is a weather app. This folder holds its web version, built with SvelteKit. It has the same features as the [Android app](../README.md), except the widgets. It always shows metric and imperial units side by side, and it has 8 languages and 22 font choices.

A hosted copy is at **[maripana.njarasoa.org](https://maripana.njarasoa.org)**.

## Features

For the full list, see the [main README](../README.md#features). This list gives the web details.

- **Weather data** from [Open-Meteo](https://open-meteo.com) (default, no key) or [Pirate Weather](https://pirateweather.net) (your own API key). Air quality and pollen come from the Open-Meteo air-quality API.
- **One scrolling column**: header, alert banner, main card, hourly forecast (with the minutely nowcast, Open-Meteo only), 10-day forecast, Current conditions, Sun & Air, Activities, and the footer. All cards stay open.
- **Main card**: the temperature (°C with 1 decimal, °F as a whole number), today's low and high, the rain or snow outlook and the air quality (when it is not good), and 4 hour tiles (+0, +4, +8 and +12 hours).
- **Forecast tabs**: Conditions, Precipitation, Wind, Pressure, UV Index and Air Quality (when there is data). Each tab has a row of cards and a chart.
- **7 alert sources**: NWS (US), NHC (hurricanes), MeteoAlarm (Europe), JMA (Japan), ECCC (Canada), BOM (Australia) and GDACS (global). See [Alert Proxy Routes](#alert-proxy-routes).
- **Places**: the search icon in the header opens the search and the list of favorites. Swipe, or tap the arrows, to move between places. A searched place shows for 12 hours. The grid icon opens the location details: the place, the coordinates in degrees, minutes and seconds, and the Plus Code.
- **Shared places**: the web app takes a place from the `text`, `url`, `q`, `loc` or `title` URL parameter. The manifest `share_target` uses this, so on Android you can share a map link or coordinates to the installed web app.
- **Settings**: the Advanced Mode switch and the geocoding information. Advanced Mode stays on until you turn it off. It shows the weather-source choice with an API key test, a switch for each alert source, and which alert sources failed. When you turn it off, the app also goes back to your location.
- **Units, languages and fonts**: tap a value to put the other unit first. Tap the flag or the font name in the footer to change the language or the font. The fonts are self-hosted in `static/fonts`.
- **Share**: each card has a share button. `html2canvas` makes a PNG of the card, and the Web Share API shares it. If the browser cannot share files, the PNG is downloaded.
- **Refresh**: pull down, or tap the update time. The app also refreshes when the tab becomes visible and the data is more than 30 minutes old.
- **Offline**: the last forecast is saved in `localStorage` (`weatherSnapshot.ts`), and the service worker keeps the last API responses. Without a connection, the app shows them with their real age.
- **Help and About pages**, opened from the footer.
- **Install on iPhone and iPad**: iOS browsers never offer to install a web app. So on iOS, when the app is not open from the Home Screen, the footer has an Install link. It opens a panel with the two steps (Share, then Add to Home Screen), and Help has the same steps (`installHint.ts`, `InstallDialog.svelte`).
- **Error screen in two languages**: when the browser language is different from the app language, the error screen shows both.

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Framework | SvelteKit 2.70 and Svelte 5.57 (runes) |
| Language | TypeScript 5.9 |
| Build tool | Vite 7.3 |
| Adapter | `@sveltejs/adapter-node` 5.5 |
| Tests | Vitest 4.1 |
| i18n | `svelte-i18n` 4 |
| Screenshots | `html2canvas` 1.4 |
| Runtime | Node 24 |
| Geocoding | Open-Meteo geocoding first, Nominatim as fallback and for reverse geocoding |

## Build and Run

```bash
npm install          # Install dependencies
npm run dev          # Dev server at localhost:5173
npm run build        # Production build: a Node.js server in build/
npm run preview      # Preview the production build
npm run check        # Type-check with svelte-check
npm test             # Unit tests (Vitest)
```

The build makes a Node.js server (adapter-node). Run it with:

```bash
node build/index.js            # Port 3000
PORT=8080 node build/index.js  # Another port
```

The app reads no environment variables of its own. adapter-node reads `PORT`, `HOST` and `COMPRESS`.

CI runs `npm ci`, `npm run check`, `npm test` and `npm run build` on each push.

## Deployment (Docker)

The Dockerfile has two stages, both on `node:24-alpine`: one builds the app and one runs it. The build context is the repository root, because the build copies `shared/i18n/locales/` into the image in place of the `src/lib/i18n/locales` symlink. Run `docker compose` from the `web/` folder:

```bash
docker compose up -d --build             # Port 3080 (default)
PORT=8080 docker compose up -d --build   # Another port
```

The container `maripanaTokana.web` serves port 3000, mapped to host port `$PORT` (default 3080). It runs as the user `node`, with `COMPRESS=true` and a health check.

```
Dockerfile          # Two stages: build, then serve (node:24-alpine)
docker-compose.yml  # Container and port
.dockerignore       # Excludes node_modules, .git, build, .svelte-kit
```

### Alert Proxy Routes

Four alert sources do not send `Access-Control-Allow-Origin` headers, so the browser cannot call them. SvelteKit server routes call them instead:

| Route | Upstream |
|---|---|
| `/api/alerts/meteoalarm?country=XX` | feeds.meteoalarm.org |
| `/api/alerts/bom` | api.weather.bom.gov.au |
| `/api/alerts/nhc` | www.nhc.noaa.gov |
| `/api/alerts/eccc?bbox=...` | api.weather.gc.ca |

The server keeps each response for 5 minutes (`src/lib/server/proxy.ts`) and sends it with `Cache-Control: public, max-age=300`. If the upstream fails, the route returns status 502.

NWS, GDACS and JMA send CORS headers, so the browser calls them directly.

### Security

- `svelte.config.js` sets a Content Security Policy in report-only mode. Reports go to `/api/csp-report`.
- `src/hooks.server.ts` sets `X-Content-Type-Options`, `Referrer-Policy`, `Permissions-Policy` (location for this site only) and `X-Frame-Options: DENY`. It does not show server error details.

### Service Worker

`src/service-worker.ts` caches nothing at install. It caches at run time:
- Open-Meteo forecast and air-quality calls and Nominatim calls: network first, then the cache. Each cached response has its cache time, so the app can show the real age of the data.
- Pages and the app's own files: network first, then the cache.

It does not cache the geocoding search, Pirate Weather or alert calls.

## Structure

```
web/
├── src/
│   ├── lib/
│   │   ├── actions/          # Svelte actions (dragScroll, longPress)
│   │   ├── api/              # Open-Meteo, Pirate Weather and air-quality clients, geocoding, mapper, WMO codes
│   │   │   └── alerts/       # The 7 alert sources, index.ts (combines them) and shared.ts
│   │   ├── components/       # Svelte UI components
│   │   ├── domain/           # Value classes (Temperature, Pressure, ...), air quality, UV, activity indices
│   │   ├── i18n/             # Locale settings, localizeDigits(); locales/ links to shared/i18n/locales
│   │   ├── server/           # Server-only code for the alert proxy routes (proxy, cache)
│   │   ├── stores/           # Weather, preferences, location, saved places, Advanced Mode, offline snapshot
│   │   ├── utils/            # Date and chart path helpers
│   │   ├── credits.ts        # Credits on the About page
│   │   ├── deviceInfo.ts     # Device information on the About page
│   │   ├── fonts.ts          # The 22 font choices and their CSS files
│   │   ├── installHint.ts    # iOS Install link: when to show it
│   │   └── share.ts          # html2canvas capture, Web Share API or download
│   ├── routes/               # +page.svelte, +layout.svelte, +error.svelte
│   │   └── api/              # alerts/{meteoalarm,bom,nhc,eccc} proxies, csp-report
│   ├── hooks.server.ts       # Security headers
│   ├── service-worker.ts
│   └── app.html
├── static/                   # PWA manifest, icons, background, fonts
├── docs/                     # DESIGN.md and TESTING.md
├── svelte.config.js          # SvelteKit config (adapter-node, CSP)
├── vite.config.ts            # Vite and Vitest config
└── package.json
```

Unit tests are next to the code, as `*.test.ts` files.

## Internationalization

The texts are in `shared/i18n/locales/*.json`, which both platforms use. `src/lib/i18n/locales` is a symlink to that folder. When the web app loads a language, it adds the keys under `web_only` and drops the keys under `android_only` (`flattenForWeb` in `src/lib/i18n/index.ts`). After a change, run `node scripts/check_i18n_parity.js` from the repository root.

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

All number formatting uses ASCII digits. `localizeDigits()` changes them to native digits for display, as the Android app does.

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

Pairings 16 to 20 use `font-feature-settings: "tnum"` (tabular numbers) for the body font.

## Documentation

- [docs/DESIGN.md](docs/DESIGN.md): the design of the web app.
- [docs/TESTING.md](docs/TESTING.md): how to test the web app.

## License

GPL-3.0-or-later. (c) Orinasa Njarasoa. See [LICENSE](../LICENSE). Versions up to and including 1.5.24 were released under the MIT License.
