# Competitor Feature Parity Plan — Google Weather & WeatherBug

Gap analysis against the two most common mainstream weather apps, and a phased plan to close
the gaps that are worth closing. Written the same way `AIR_QUALITY_FORECAST_PLAN.md` was: a
roadmap, not yet implemented. Each phase should get its own per-feature design doc (same style
as `AIR_QUALITY_FORECAST_PLAN.md`) before implementation starts.

## What we already have (verified against the codebase, not assumed)

- **Weather data**: Open-Meteo (default, keyless) + Pirate Weather (optional, user key),
  switchable in Expert Mode.
- **Alerts**: 8 official sources (NWS, GDACS, MeteoAlarm, JMA, ECCC, BOM, NHC, WMO SWIC), each
  individually toggleable, **plus algorithmically derived alerts** (thunderstorm, heavy
  rain/snow, high wind, extreme heat/cold, high UV) computed client-side from current
  conditions — this is a feature neither competitor has in this form.
- **Forecasts**: 24-hour hourly, 7-day daily. (Note: `AGENTS.md` says "10-day outlook" — that's
  stale; `OpenMeteoApiService.kt` requests `forecast_days=7`. Worth fixing separately.)
- **Air quality**: US AQI + European AQI (dual standard) with full per-pollutant breakdown and
  tiering — ahead of WeatherBug's "in-depth look," roughly at parity with Google's AQI card.
- **UV index**: current + daily max, tiered.
- **Sunrise/sunset**: current day + 7-day array.
- **Widgets** (Android only): 4x1 and 4x2 Glance widgets, 30-min WorkManager refresh.
- **Dual units, 22 fonts, 8 languages incl. RTL/native digits, screenshot sharing, Expert Mode
  location override** — no competitor has this combination; not part of the gap analysis below.

## Gap analysis

| Feature | Google Weather | WeatherBug | Us | Gap |
|---|---|---|---|---|
| Precipitation/storm radar map | Yes (Weather Map, 8h precip animation) | Yes (20+ map layers) | **None** | Large |
| Minute-by-minute precip nowcast | Yes (Google nowcast, ~12h) | Partial (via radar) | **None** | Large |
| Push / background severe-weather notifications | Yes | Yes (Spark lightning alerts) | **None** (in-app banner only) | Large |
| Multi-location / saved locations | Yes | Yes | **None** (single active location) | Large |
| Pollen index | Yes | Yes | **None** | Medium |
| Lightning tracking | No | Yes (Spark, signature feature) | **None** | Medium |
| Historical weather / "on this day" | No | Partial | **None** | Small |
| Moon phase | No (not prominent) | No (not prominent) | **None** | Small (low priority — neither competitor emphasizes it) |
| AI-generated natural-language summary | Yes (AI Weather Report) | No | **None** | Small/optional |
| Life/activity indices (running, drying, etc.) | Partial | No | **None** | Small |
| Weather cameras / crowdsourced PWS network | No | Yes (Earth Networks sensor network) | **None** | Not pursued (see below) |
| Air quality (depth) | Yes | Yes | **Yes, ahead** | None |
| UV index | Yes | Yes | **Yes** | None |
| Derived/algorithmic alerts | No | No | **Yes** | We're ahead |

## Constraints that shape the plan

1. **Keyless-by-default.** Every new data source must have a free, no-signup default path,
   matching the existing Open-Meteo/Nominatim pattern. Paid/keyed sources may only be added as
   optional (same pattern as Pirate Weather).
2. **F-Droid flavor has no Google Play Services.** No Google Maps SDK, no FCM. Any
   map-rendering or push-notification feature needs an implementation that works without
   proprietary blobs on the `fdroid` flavor (`app/src/fdroid/`).
3. **Feature parity mandate.** Every feature ships on both Android and Web before being
   considered done (`AGENTS.md` Mandate #1).
4. **i18n mandate.** New UI strings go in all 8 `shared/i18n/locales/*.json` files, regenerate
   Android strings via `node shared/i18n/generate-android-strings.js`.
5. **No backend server we control.** The web app is a stateless SvelteKit frontend + thin proxy
   routes for CORS-blocked alert feeds — no database, no user accounts. Multi-location and
   notifications need to be solved client-side/on-device, not via a server we'd have to run.

## Confirmed keyless data sources for the gaps

| Gap | Source | Notes |
|---|---|---|
| Radar map | [RainViewer](https://www.rainviewer.com/api.html) | Public tile API, no key, 1200+ radars, 5-min refresh, free for personal/community use. |
| Lightning | [Blitzortung.org](https://en.wikipedia.org/wiki/Blitzortung) | Free, keyless, community lightning network — check attribution/ToS before shipping. |
| Pollen | Open-Meteo Air Quality API | Already integrated (`OpenMeteoAirQualityApiService.kt`). Supports grass/birch/alder pollen via CAMS — **Europe + global coverage only**, no pollen data for most of the world. Must be shown as "unavailable in your region" gracefully elsewhere. |
| Minute nowcast | Open-Meteo `minutely_15` parameter | Native 15-min resolution only in North America (HRRR) and Central Europe (ICON-D2/AROME); interpolated from hourly elsewhere — so accuracy varies by region, same caveat as pollen. |
| Historical weather | Open-Meteo Historical Forecast/Archive API | Keyless, no new integration pattern needed. |

## Phased roadmap

### Phase 1 — cheap wins, no new architecture
1. **Multi-location / saved locations.** Biggest usability gap, zero new data sources needed
   (reuses existing weather/geocoding calls per location), pure client-side state
   (SharedPreferences / localStorage list instead of single value). Do this first — it's the
   most-requested table-stakes feature and everything else benefits from it existing.
2. **Pollen index.** Extend the existing `OpenMeteoAirQualityApiService` call (same pattern as
   `AIR_QUALITY_FORECAST_PLAN.md`'s hourly AQI extension) to also request pollen variables.
   Show inside the existing Air Quality detail dialog; hide gracefully outside CAMS coverage.
3. **Fix the stale "10-day" claim** — either bump `forecast_days` to 10 (small quota cost) or
   correct the docs/UI copy to say 7-day. Cheap, unblocks accurate parity claims later.

### Phase 2 — proactive alerts (safety-relevant, do before visual features)
4. **Background severe-weather notifications.** We already fetch alerts; the gap is surfacing
   them when the app isn't open. `standard` flavor can use a local notification triggered by the
   existing WorkManager widget-refresh job (no FCM needed — this is a pull-based check, not a
   push from a server, so it works identically on `fdroid`). Web: `Notification` API +
   service-worker periodic sync where supported, degrade silently elsewhere (PWA notification
   support is inconsistent across browsers — verify before committing to this as a hard
   requirement).

### Phase 3 — the big visual gap
5. **Precipitation radar map.** RainViewer tiles. This is the single largest architectural
   addition — neither codebase has any map/tile-rendering code today. Needs its own spike/design
   doc before implementation: Android tile rendering approach (avoid Google Maps SDK to keep
   `fdroid` flavor clean — evaluate a lightweight OSM-tile Compose canvas vs. a WebView), Web
   approach (MapLibre GL or Leaflet, both keyless, both work with `adapter-node`).
6. **Minute-by-minute nowcast.** Once the radar spike is done, this is comparatively small:
   consume `minutely_15` precipitation on the existing hourly-forecast API call, show as a short
   sparkline/banner ("rain starting in 12 min") for the covered regions.

### Phase 4 — differentiators, evaluate demand before committing
7. **Lightning tracking.** Blitzortung integration + a map layer (reuses Phase 3's map
   infrastructure). WeatherBug's signature feature, but also the most niche — build only if
   Phase 3's map groundwork makes it cheap, and check Blitzortung's terms before shipping.
8. **Historical weather / "on this day."** Small, isolated feature — new API call, no shared UI
   dependency. Good filler task, not urgent.
9. **Life/activity indices** (running conditions, laundry-drying, UV-safe outdoor time).
   Derivable entirely from data we already have (UV, precip probability, wind, humidity) via
   simple rule-based scoring — no new API. Cheap once someone defines the thresholds.
10. **AI-generated natural-language summary.** Explicitly lower priority: doing this "for real"
    means either calling an LLM (cost, privacy, and offline-mode conflicts with the app's
    no-backend philosophy) or a templated/rule-based sentence generator (cheap, keyless, fits
    the project's constraints, but is "AI" in name only). Recommend the rule-based approach if
    this is ever pursued, and only after Phase 1–3 land.

### Explicitly not pursuing
- **Crowdsourced personal-weather-station network** (WeatherBug/Earth Networks' core
  differentiator) — requires server infrastructure and a user community we don't have. Out of
  scope indefinitely.
- **Live weather camera feeds** — no free keyless source exists; skip.
- **Moon phase** — low priority; it's a pure client-side astronomical calculation (near-zero
  cost) so it can be slotted into any phase opportunistically, but neither competitor treats it
  as a headline feature either.

## Suggested sequencing

Multi-location (P1) → pollen + stale-forecast-length fix (P1) → background alert notifications
(P2) → radar map (P3) → minutely nowcast (P3) → lightning / historical / life-indices, in
whatever order demand suggests (P4). AI summary last, and only if still wanted after the rest
ships.

## Process for each feature

Mirror `AIR_QUALITY_FORECAST_PLAN.md`: before writing code, produce a per-feature design doc
covering the Android implementation (files, domain model changes, API layer, UI composables,
wiring point) and the Web mirror, plus the i18n keys needed across all 8 locales. Implement
Android first (per `CLAUDE.md`'s agent-usage guidance — read the source implementation, then
port), then Web in the same session/PR, never partially.
