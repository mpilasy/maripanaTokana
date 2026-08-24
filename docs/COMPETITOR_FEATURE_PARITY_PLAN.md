# Competitor Feature Parity Plan — Google Weather & WeatherBug

Gap analysis against the two most common mainstream weather apps, tracking shipped features and the remaining roadmap.

## What We Have (Verified against current v1.2.x codebase)

- **Weather data**: Open-Meteo (default, keyless) + Pirate Weather (optional, user key), switchable in Advanced Mode.
- **Alerts**: 8 official sources (NWS, GDACS, MeteoAlarm, JMA, ECCC, BOM, NHC, WMO SWIC), each individually toggleable. (Derived/algorithmic alerts were removed in v1.2.1 to prevent false alarms).
- **Forecasts**: 24-hour hourly, 7-day daily with horizontal scrolling and matching trend chart.
- **Air quality**: US AQI + European AQI (dual standard) with full per-pollutant breakdown dialog, category tier badges, and a 48-hour AQI trend forecast chart.
- **Pollen index**: Grass, birch, and alder pollen counts with risk tier badges inside the Air Quality detail dialog.
- **UV index**: Live reading in Current Conditions, category tier badge, and a 7-day UV trend forecast card.
- **Multi-location / Saved locations**: Header search, favorite places (♥), and swipe/arrow navigation between Current Location and saved locations.
- **Sunrise/sunset**: Current day + daily forecast array.
- **Widgets** (Android only): 4x1 and 4x2 Glance widgets, 30-min WorkManager refresh.
- **Dual units, 22 fonts, 8 languages incl. RTL/native digits, screenshot sharing, Advanced Mode location override** — no competitor has this combination.

## Gap Analysis & Status

| Feature | Google Weather | WeatherBug | Us | Status |
|---|---|---|---|---|
| Multi-location / saved locations | Yes | Yes | **Yes** | **Completed (v1.2.15)** |
| Air quality forecast (48h trend) | Yes | Yes | **Yes** | **Completed (v1.2.12)** |
| Pollen index | Yes | Yes | **Yes** (CAMS region) | **Completed (v1.2.5)** |
| UV forecast (7-day trend) | Yes | Yes | **Yes** | **Completed (v1.2.12)** |
| Official severe-weather alerts | Yes | Yes | **Yes** (8 sources) | **Completed (v1.1.0)** |
| Precipitation/storm radar map | Yes (Weather Map, 8h precip animation) | Yes (20+ map layers) | **None** | Open (Phase 3) |
| Minute-by-minute precip nowcast | Yes (Google nowcast, ~12h) | Partial (via radar) | **None** | Open (Phase 3) |
| Push / background severe-weather notifications | Yes | Yes (Spark lightning alerts) | **None** (in-app banner only) | Open (Phase 2) |
| Lightning tracking | No | Yes (Spark, signature feature) | **None** | Open (Phase 4) |
| Historical weather / "on this day" | No | Partial | **None** | Open (Phase 4) |
| Moon phase | No (not prominent) | No (not prominent) | **None** | Low priority |
| AI-generated natural-language summary | Yes (AI Weather Report) | No | **None** | Optional |
| Life/activity indices (running, drying, etc.) | Partial | No | **None** | Open (Phase 4) |

## Constraints

1. **Keyless-by-default.** Every new data source must have a free, no-signup default path, matching the existing Open-Meteo/Nominatim pattern. Paid/keyed sources may only be added as optional.
2. **F-Droid flavor has no Google Play Services.** No Google Maps SDK, no FCM. Any map-rendering or push-notification feature needs an implementation that works without proprietary blobs on `app/src/fdroid/`.
3. **Feature parity mandate.** Every feature ships on both Android and Web before being considered done (`AGENTS.md` Mandate #1).
4. **i18n mandate.** New UI strings go in all 8 `shared/i18n/locales/*.json` files, regenerate Android strings via `node shared/i18n/generate-android-strings.js`.
5. **No backend server we control.** The web app is a stateless SvelteKit frontend + thin proxy routes for CORS-blocked alert feeds — no database, no user accounts.

## Phased Roadmap

### Phase 1 — Quick Wins & Usability (COMPLETED)
1. **Multi-location / saved locations** — **Done (v1.2.15)**: Header location search, favorite locations list, swipe/arrow switching between saved places.
2. **Pollen index** — **Done (v1.2.5)**: Integrated into Air Quality breakdown dialog for CAMS coverage areas.
3. **AQI & UV Forecast cards** — **Done (v1.2.12)**: 48-hour AQI trend chart and 7-day UV trend chart.
4. **7-Day Forecast standardization** — **Done (v1.2.12/v1.2.13)**: 7-day forecast with horizontal scrolling and matching trend line chart.

### Phase 2 — Proactive Notifications (Safety-Relevant)
5. **Background severe-weather notifications**: Surface official alerts when the app is backgrounded.
   - Android: Local notifications via WorkManager widget-refresh job (works identically on `fdroid` and `standard`).
   - Web: `Notification` API + service-worker periodic sync where supported.

### Phase 3 — Visual Map & Nowcast
6. **Precipitation radar map**: RainViewer tile API integration (keyless, free tile server). Requires map renderer evaluation (lightweight canvas/OSM approach to keep `fdroid` clean, MapLibre GL for Web).
7. **Minute-by-minute nowcast**: `minutely_15` precipitation from Open-Meteo as a short sparkline/banner for covered regions.

### Phase 4 — Differentiators & Extras
8. **Lightning tracking**: Blitzortung integration + map layer.
9. **Historical weather / "on this day"**: Open-Meteo Historical Archive API call.
10. **Life/activity indices**: Derived client-side indices (outdoor running quality, laundry drying time, UV exposure limits).
11. **Natural-language summary**: Rule-based template generator for daily highlights.

## Sequencing Summary

- **Completed**: Saved Locations, Pollen Index, AQI Forecast, UV Forecast, 7-Day Trend Chart, Official Alerts (v1.2.x).
- **Next Up**: Background Alert Notifications (Phase 2) → Precip Radar Map & Nowcast (Phase 3) → Lightning / Historical / Life Indices (Phase 4).
