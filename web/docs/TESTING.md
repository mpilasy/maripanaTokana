# Testing Instructions — maripànaTokana PWA

## Prerequisites

```bash
npm install
npm run build    # Verify clean build (no errors)
npm test         # Run the unit tests (vitest)
```

The build should complete with zero errors. One non-blocking warning is expected:
- Vite warns that some chunks are larger than 500 kB after minification.

### Unit Tests

Vitest runs the `src/**/*.test.ts` files in the Node environment. The settings are in `vite.config.ts`; there is no separate Vitest config file. The tests are next to the code they test:
- `src/lib/api/`: HTTP helper, Open-Meteo client and mapper, night weather, geocoding search, alert sources (`alerts/index`, `alerts/meteoAlarm`)
- `src/lib/domain/`: temperature, precipitation outlook, air quality, UV, Plus Codes, shared-location parser, region qualifier, script detection
- `src/lib/stores/`: weather, location, offline snapshot
- `src/lib/utils/`: dates
- `src/lib/server/`: the proxy cache

There are no end-to-end tests. CI (`.github/workflows/ci.yml`) runs `npm ci`, `npm run check`, `npm test` and `npm run build` on each push to `main` and each pull request.

---

## 1. Build Verification

```bash
npm run build
npm run check
```

- `npm run build` (vite build) produces a Node.js server in `build/` (adapter-node)
- `npm run check` runs `svelte-check` with no type errors
- `build/index.js` is the server entry point; run it with `node build/index.js`

---

## 2. Dev Server

```bash
npm run dev
```

- Open `http://localhost:5173` in Chrome/Firefox
- Browser should prompt for geolocation permission
- After granting, weather data should load and display

---

## 3. Geolocation

| Test | Expected |
|------|----------|
| First visit (no cached location) | Loading spinner, then browser geolocation prompt |
| Grant geolocation | Weather data loads and displays |
| Deny geolocation | Error screen with dual-language retry button |
| Subsequent visit (permission already granted) | Loads weather immediately (cached location used first) |
| Cached location available | Instant display from cache, then updates if position changed >5 km |

---

## 4. Weather Data Display & Saved Locations

- [ ] **Saved Locations & Navigation**
  - [ ] Location header displays city name and region subtext
  - [ ] Search icon opens the search (`SavedLocationsDialog`). You can search by city, postal code, coordinates (decimal or DMS), Plus Code or map link
  - [ ] A picked result shows as a preview for 12 hours. It is not saved
  - [ ] The empty heart (♡) shows only for a preview. Clicking it saves the place
  - [ ] Clicking the full heart (♥) removes a saved place
  - [ ] Holding the place name opens the rename dialog (`RenameLocationDialog`)
  - [ ] The pin icon (shown for a preview or a saved place) goes back to the current location
  - [ ] Swipe or tap arrow buttons to navigate between saved places
  - [ ] Holding an arrow opens the list of places
  - [ ] The grid icon opens the location details (`LocationDetails`): the place, DMS coordinates and Plus Code. Clicking a line opens OpenStreetMap; holding a line copies it
  - [ ] Opening `/?q=-18.8430,49.1535` (or a map link in `q`) opens that place as a preview

- [ ] **Free-Scroll Layout**
  - [ ] All cards sit in one column that scrolls up and down
  - [ ] All cards are always open (no accordion, no collapse or expand control)
  - [ ] Card order: alert banner (when alerts exist), hero card, Hourly Forecast, 10-Day Forecast, Current Conditions, Sun & Air, Activities
  - [ ] Header stays fixed at the top; footer stays fixed at the bottom

- [ ] **Hero Card**
  - [ ] Meteocons weather icon (day or night) and localized condition text
  - [ ] Temperature with 1 decimal, with the second unit below it
  - [ ] Low and high temperatures
  - [ ] Side column shows the rain or snow outlook when rain or snow is expected
  - [ ] US AQI and EU AQI show in the side column only when air quality is not good
  - [ ] 4 hour tiles show the hours +0, +4, +8 and +12
  - [ ] Share button and the "© Orinasa Njarasoa" line

- [ ] **Nowcast**
  - [ ] "Minutely Nowcast" shows in the Hourly Forecast card only when precipitation is expected in the next 2 hours (Open-Meteo source)
  - [ ] Nowcast is not shown when no precipitation is expected

- [ ] **Hourly Forecast**
  - [ ] Card shows 6 tabs: Conditions, Precipitation, Wind, Pressure, UV, Air Quality
  - [ ] Air Quality tab shows only when air quality data exists
  - [ ] Each tab shows a horizontal scrollable row of hourly cards and an overview chart below it

- [ ] **10-Day Forecast**
  - [ ] Card shows the same 6 tabs
  - [ ] Each tab shows a horizontal scrollable row of daily cards (10 days) and an overview chart below it

- [ ] **Current Conditions and Sun & Air (Cards)**
  - [ ] Both cards show tiles for their data (temperature, precipitation, wind and gust, pressure, humidity, sun, air quality, UV index, visibility)
  - [ ] Air quality tile opens the detailed breakdown with pollen tier badges
  - [ ] UV tile shows the severity tier

- [ ] **Activities**
  - [ ] Activities card shows activity indices

- [ ] **Help Page**
  - [ ] "Help" link in the footer opens the Help page
  - [ ] Help page shows sections with questions and answers
  - [ ] Back button returns to the weather screen

- [ ] **About Page**
  - [ ] "About" link in the footer opens the About page
  - [ ] About page shows the app name, version, license, Help link, source code link, "On the web" link, data source credits and device information
  - [ ] "Help" link on the About page opens the Help page
  - [ ] Back button returns to the weather screen

- [ ] **Install Link (iOS only)**
  - [ ] On iPhone or iPad, in a browser tab, the footer shows "Install" after About
  - [ ] "Install" opens a panel with two steps (Share, then Add to Home Screen) and a Close button
  - [ ] Help has an "Install" section with the same steps
  - [ ] On other systems, and in the app opened from the Home Screen, neither shows
  - [ ] To test without an iPhone, use an iPhone user agent and touch emulation in DevTools

- [ ] **Settings**
  - [ ] With Advanced Mode off, Settings shows the Advanced mode switch, its description, the geocoding information and the About row
  - [ ] With Advanced Mode on, it also shows the weather source (with the Pirate Weather key test) and the alerts: a master switch and the 7 sources (NWS, GDACS, MeteoAlarm, JMA, ECCC, BOM, NHC)

---

## 5. Screenshot Sharing

- [ ] Tap share button on the hero card → generates branded PNG with header + card
- [ ] Tap share button on any card (`SectionCard`) → generates branded PNG with header + card content
- [ ] On mobile: Web Share API sheet opens with the PNG
- [ ] On desktop: PNG downloads as `maripanatokana-weather.png`
- [ ] Screenshot has a footer with `maripànaTokana v<version>` and `https://maripana.njarasoa.org`

---

## 6. Dual Units & Toggle

- [ ] All temperature values show both °C and °F
- [ ] All wind values show both m/s and mph
- [ ] Pressure shows both hPa and inHg
- [ ] Visibility shows both km and mi
- [ ] Precipitation shows both mm and in
- [ ] **Tap any dual value** → primary and secondary swap (e.g., °F becomes bold, °C becomes dimmed)
- [ ] Toggle persists across page reload (stored in `localStorage`)

---

## 7. Internationalization (8 Languages)

Tap the flag emoji in the footer to cycle through languages:

| # | Flag | Language | Verify |
|---|------|----------|--------|
| 0 | (MG) | Malagasy | Default language, comma decimal separator |
| 1 | (SA) | Arabic | RTL layout, Eastern Arabic digits, Arabic decimal |
| 2 | (GB) | English | LTR, standard digits |
| 3 | (ES) | Spanish | LTR, comma decimal separator |
| 4 | (FR) | French | LTR, comma decimal separator |
| 5 | (IN) | Hindi | LTR, Devanagari digits |
| 6 | (NP) | Nepali | LTR, Devanagari digits |
| 7 | (CN) | Chinese | LTR, standard digits |

### Per-language checks:
- [ ] All UI labels change to the selected language
- [ ] Weather descriptions are localized
- [ ] Day/month names are localized (via `Intl.DateTimeFormat`)
- [ ] Cardinal directions are localized (N, S, E, W, etc.)
- [ ] UV severity labels are localized
- [ ] Digits are replaced for ar/hi/ne
- [ ] Decimal separators are correct per locale
- [ ] Language persists across reload

### Arabic (RTL) specific:
- [ ] `<html dir="rtl">` is set when Arabic is selected
- [ ] Layout mirrors (text right-aligned, scroll directions correct)
- [ ] Footer remains LTR (`dir="ltr"` attribute)
- [ ] Switching away from Arabic restores `dir="ltr"`

---

## 8. Dual-Language Error Screen

- [ ] Set app language to Malagasy (default)
- [ ] Deny geolocation or block network
- [ ] Error screen shows Malagasy text (primary) + English text (secondary, faded) if browser is English
- [ ] Retry button shows both languages
- [ ] Retry button is prominent (solid white, bold text)
- [ ] "Choose a place" and "Open settings" buttons show below it

---

## 9. Font Pairings (22 fonts)

Tap the font icon (Aa) in the footer to cycle through 22 font pairings.

- [ ] Font name displayed in footer updates on each tap
- [ ] Default (index 0) uses system fonts
- [ ] Fonts load from the app itself (check Network tab for `/fonts/*.css` requests; no requests to `fonts.googleapis.com`)
- [ ] Display font applies to: location name, card values
- [ ] Body font applies to: labels, descriptions
- [ ] Pairings 16-20 use tabular numbers (`font-feature-settings: "tnum"`)
- [ ] Font index persists across reload

---

## 10. Free-Scroll Layout

- [ ] Scroll the page: all cards appear in one column
- [ ] All cards are open on load; no card has a chevron or collapse control
- [ ] Switching location does not hide or collapse any card
- [ ] Each card has a share button next to its title

---

## 11. Pull-to-Refresh

- [ ] Scroll to top of the content area
- [ ] Touch and drag downward from the top
- [ ] Pull indicator appears
- [ ] Pull past threshold (~80px) → spinner becomes active
- [ ] Release → weather data refreshes
- [ ] During refresh, a small spinner shows at the top

---

## 12. Auto-Refresh on Tab Focus

1. Load the app and note the "Updated at HH:MM" time
2. Switch to another tab/app
3. Wait >30 minutes (or temporarily change `STALE_MS` in `src/lib/stores/weather.ts`)
4. Switch back to the app tab
5. [ ] Weather data refreshes automatically
6. [ ] "Updated at" time updates

---

## 13. PWA / Installability & Docker Testing

### Via npm preview (Svelte app only):
```bash
npm run build && npm run preview
```
Open `http://localhost:4173` in Chrome.

### Via Docker:
```bash
docker compose up -d --build
```

App is available at:
- `http://localhost:3080/` — maripànaTokana
- `http://localhost:3080/svelte` — Redirects to `/` (backwards compatibility)

### PWA Features:
- [ ] Service worker registers (check DevTools → Application → Service Workers)
- [ ] Manifest is detected (check DevTools → Application → Manifest)
- [ ] Chrome shows "Install app" option
- [ ] On iOS, the footer shows the Install link (see section 4)
- [ ] The page has `apple-mobile-web-app-title` = maripànaTokana and the 180x180 `apple-touch-icon`
- [ ] Theme color `#0E0B3D` applies to title bar
- [ ] All standard Svelte app tests pass (see sections 1-12 above)

### Performance Checks:
- [ ] Network tab shows gzip compression on JS/CSS/fonts
- [ ] Versioned assets have long cache headers (`max-age=31536000`)
- [ ] First visit: downloads all assets
- [ ] Repeat visit: browser checks HTML, reuses cached JS/CSS/fonts (minimal requests)
- [ ] DevTools → Coverage shows <50% unused JS per app (due to build optimization)

---

## 14. Offline Support

### Production build required.

1. Load the app, allow geolocation, wait for weather to display
2. Open DevTools → Application → Service Workers → check "Offline"
3. Reload the page
4. [ ] App shell loads from cache (background, layout visible)
5. [ ] Weather data loads from cache (last fetched data shown, with its real age)
6. Uncheck "Offline"
7. Pull-to-refresh or reload
8. [ ] Fresh data loads from network

---

## 15. Responsive Layout

Test at these viewport widths (DevTools → Device toolbar):

| Width | Device class |
|-------|-------------|
| 320px | Small phone (iPhone SE) |
| 375px | Standard phone (iPhone 12/13) |
| 414px | Large phone (iPhone 14 Plus) |
| 768px | Tablet portrait |

- [ ] Hero card scales without overflow
- [ ] Hourly forecast scrolls horizontally
- [ ] Daily forecast rows don't overflow
- [ ] Detail cards grid maintains 2-column layout
- [ ] Footer doesn't overlap content
- [ ] No horizontal scroll on the page body

---

## 16. Error Handling

- [ ] Deny geolocation → error screen with prominent "Retry" button
- [ ] Block network requests to `api.open-meteo.com` with no saved forecast → error screen with message
- [ ] Block them after a forecast was saved → the last forecast shows, with a "refresh failed" line
- [ ] Click "Retry" on error screen → retries the weather fetch
- [ ] If browser language differs from app language, error shows dual-language text

---

## 17. Edge Cases

- [ ] Reload the page rapidly — no duplicate API calls or state corruption
- [ ] Toggle units rapidly — values update correctly each time
- [ ] Cycle languages rapidly — all strings update, no missing translations
- [ ] Cycle fonts rapidly — no layout jank or loading errors
- [ ] Very long location names — truncated or wrapped gracefully
- [ ] Location with no wind gust data — gust tile shows `--`

---

## 18. Advanced Mode

- [ ] **Activation**
  - [ ] Toggling "Advanced mode" on in Settings shows the weather source and alert sections
  - [ ] When alert sources fail, the weather screen lists them
  - [ ] Advanced Mode stays on across reloads (check `advanced_mode_enabled` in localStorage)

- [ ] **Deactivation**
  - [ ] Toggling "Advanced mode" off hides the sections, clears a place preview and goes back to the current location
  - [ ] Clearing browser site data resets it

- [ ] **Place Preview (works without Advanced Mode)**
  - [ ] Search for a city (e.g., "Paris") → updates weather instantly
  - [ ] Enter `lat,lon` (e.g., `48.85,2.35`) → updates weather instantly
  - [ ] Two-line location header: City on line 1, Region/Country on line 2
  - [ ] A preview expires after 12 hours (check `advanced_override_set_time` in localStorage)
