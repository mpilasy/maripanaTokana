# Testing Instructions — maripána Tokana PWA

## Prerequisites

```bash
npm install
npm run build    # Verify clean build (no errors)
```

The build should complete with zero errors. One non-blocking warning is expected:
- `state_referenced_locally` in `CollapsibleSection.svelte` — intentional behavior.

---

## 1. Build Verification

```bash
npm run build
npm run check
```

- `npm run build` produces output in `build/` directory (runs vite build + CSS inlining)
- `npm run check` runs `svelte-check` with no type errors
- The `build/` directory should contain `index.html` (with CSS inlined), JS bundles, and static assets
- `index.html` should contain a `<style>` tag (CSS inlined by `scripts/inline-assets.js`)

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

## 4. Weather Data Display

After granting location, verify the following sections render:

### Hero Card
- [ ] Weather emoji (day/night appropriate)
- [ ] Weather description text (localized)
- [ ] Current temperature in large text (e.g., `24.3°C`)
- [ ] Secondary unit below primary (e.g., `75.7°F` dimmed)
- [ ] "Feels like" label with dual-unit temperature
- [ ] Precipitation display (rain/snow emoji + amount, or "no precipitation")
- [ ] Share button (top-right corner)
- [ ] "(c) Orinasa Njarasoa" watermark at bottom

### Hourly Forecast (collapsible, expanded by default)
- [ ] Horizontal scrollable row of hourly cards
- [ ] Each card: time (HH:mm), weather emoji, temperature, precip probability
- [ ] Scroll snap works (cards snap into position)
- [ ] Day/night emoji changes correctly based on sunrise/sunset
- [ ] Share button next to section title (visible when expanded)

### Daily Forecast (collapsible, collapsed by default)
- [ ] Click section header to expand
- [ ] 10 rows with: day name, date, weather emoji + description, precip %, hi/lo temps
- [ ] Day names localized to current language
- [ ] Share button next to section title (visible when expanded)

### Current Conditions (collapsible, collapsed by default)
- [ ] Min/Max temperature cards
- [ ] Wind speed + direction (cardinal compass) and wind gust (if available)
- [ ] Pressure (hPa primary if metric)
- [ ] Humidity (%) + dew point
- [ ] UV index with severity label (Low/Moderate/High/Very High/Extreme)
- [ ] Visibility (km/mi)
- [ ] Sunrise/sunset times
- [ ] Share button next to section title (visible when expanded)

---

## 5. Screenshot Sharing

- [ ] Tap share button on HeroCard → generates branded PNG with header + card
- [ ] Tap share button on any CollapsibleSection → generates branded PNG with header + section content
- [ ] On mobile: Web Share API sheet opens with the PNG
- [ ] On desktop: PNG downloads as `maripanatokana-weather.png`
- [ ] Screenshot includes copyright watermark at bottom

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

---

## 9. Font Pairings (22 fonts)

Tap the font icon (Aa) in the footer to cycle through 22 font pairings.

- [ ] Font name displayed in footer updates on each tap
- [ ] Default (index 0) uses system fonts
- [ ] Google Fonts load correctly (check Network tab for `fonts.googleapis.com` requests)
- [ ] Display font applies to: location name, card values
- [ ] Body font applies to: labels, descriptions
- [ ] Pairings 16-20 use tabular numbers (`font-feature-settings: "tnum"`)
- [ ] Font index persists across reload

---

## 10. Collapsible Sections

- [ ] Hourly Forecast: expanded by default on load
- [ ] Daily Forecast: collapsed by default
- [ ] Current Conditions: collapsed by default
- [ ] Click header → section expands/collapses with slide animation (~300ms)
- [ ] Chevron rotates on expand/collapse
- [ ] Share button appears next to title when expanded

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
3. Wait >30 minutes (or temporarily change `STALE_MS` in `weather.ts`)
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
- `http://localhost:3080/svelte` — Svelte app (maripána Tokana)
- `http://localhost:3080/` — Redirects to `/svelte/`

### Svelte App (`/svelte`) — PWA Features:
- [ ] Service worker registers (check DevTools → Application → Service Workers)
- [ ] Manifest is detected (check DevTools → Application → Manifest)
- [ ] Chrome shows "Install app" option
- [ ] Theme color `#0E0B3D` applies to title bar
- [ ] All standard Svelte app tests pass (see sections 1-12 above)

### Performance Checks:
- [ ] Network tab shows gzip compression on JS/CSS/fonts
- [ ] Versioned assets have long cache headers (`max-age=31536000`)
- [ ] HTML files have `no-cache` headers (not cached)
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
5. [ ] Weather data loads from cache (last fetched data shown)
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
- [ ] Block network requests to `api.open-meteo.com` → error screen with message
- [ ] Click "Retry" on error screen → retries the weather fetch
- [ ] If browser language differs from app language, error shows dual-language text

---

## 17. Edge Cases

- [ ] Reload the page rapidly — no duplicate API calls or state corruption
- [ ] Toggle units rapidly — values update correctly each time
- [ ] Cycle languages rapidly — all strings update, no missing translations
- [ ] Cycle fonts rapidly — no layout jank or loading errors
- [ ] Very long location names — truncated or wrapped gracefully
- [ ] Location with no wind gust data — gust card replaced with empty placeholder
