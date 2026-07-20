# Air Quality Forecast — Design Doc (not yet implemented)

## Context

The current Air Quality feature (added earlier) only shows *current* conditions. Open-Meteo's
air-quality API also supports hourly forecasts — up to 7 days (168 hours) of `us_aqi`,
`european_aqi`, and per-pollutant values, returned in the same request that already provides
`current` data. There is **no daily aggregate** for air quality (the API rejects `us_aqi_max`
etc.; the only daily air-quality parameter it supports is `uv_index_max`), so a daily AQI summary
is not viable without client-side aggregation of hourly values — out of scope here.

## Confirmed scope

1. **Placement**: a new standalone "Air Quality Forecast" collapsible section on the main screen,
   alongside the existing "Hourly Forecast" and "10-Day Forecast" sections — not embedded in the
   existing Air Quality detail dialog (opened by tapping the AQI tier badge).
2. **Presentation**: a **line graph**, not a row of value cards. 48-hour window (not 24h — longer
   than the temperature hourly row, since AQI changes more slowly and a 2-day trend is more
   informative than 1 day). **Each data point (dot) is color-coded by its AQI tier** — Good/
   Moderate/Unhealthy/Very Unhealthy/Hazardous — using the exact same AirNow color scale already
   defined for `AqiTierBadge`/`UvTierBadge` (green `#00E400`, yellow `#FFFF00`, red `#FF0000`,
   purple `#8F3F97`, maroon `#7E0023`). No per-pollutant breakdown in this chart — that stays
   exclusive to the existing detail dialog.
3. Both Android and Web, per the project's standing feature-parity rule.

## Key architectural decision: one API call, not two

Extend the *existing* `OpenMeteoAirQualityApiService.getAirQuality()` call (Android) /
`fetchAirQuality()` (Web) to also request `hourly=us_aqi,european_aqi` and `forecast_days=3`,
rather than adding a second network call. Open-Meteo returns `current` and `hourly` blocks in one
response for one request — splitting this into two calls would just double latency/battery/rate-
limit cost for no benefit, and mirrors why the main weather endpoint already combines
`current`+`hourly`+`daily` in one call. `forecast_days=3` safely covers "now → now+48h" with a
full day of buffer regardless of what hour "now" falls on or which timezone the location is in
(mirrors the original 24h-window plan's `forecast_days=2` for the same one-day-buffer reasoning,
scaled up for the wider 48h window).

**Timezone note**: the AQI hourly request doesn't need a `timezone` param — omit it, so Open-Meteo
returns UTC timestamps; parse with a fixed UTC offset of 0. Time-axis labels drawn on the chart
should use `WeatherData.utcOffsetSeconds` from the main forecast call (the real location offset)
to convert, exactly as `TemperatureChart`/`HourlyForecastRow` already do. Flag as an assumption to
verify live when implementing: confirm AQI-hourly UTC times land on the same wall-clock hours as
the weather-hourly times.

## 1. Android domain model — new file

`app/src/main/java/orinasa/njarasoa/maripanatokana/domain/model/HourlyAirQuality.kt`:
```kotlin
data class HourlyAirQuality(
    val time: Long,       // epoch millis, absolute (UTC-parsed)
    val usValue: Int,
    val europeanValue: Int,
) {
    fun tier(standard: AqiStandard): AqiTier =
        AirQualityIndex.tierFor(if (standard == AqiStandard.EUROPEAN) europeanValue else usValue, standard)
}
```
Deliberately minimal — no pollutants, no `primaryStandard` stored per-hour (standard is
location-based, computed once, passed down). Reuses `AirQualityIndex.tierFor()` — no new
threshold logic.

## 2. Android API layer

Edit `app/src/main/java/orinasa/njarasoa/maripanatokana/data/remote/OpenMeteoAirQualityApiService.kt`:
- Add `@Query("hourly") hourly: String = "us_aqi,european_aqi"` and
  `@Query("forecast_days") forecastDays: Int = 3` to `getAirQuality()`.
- Add `OpenMeteoAirQualityHourly` response class (`time: List<String>`, `usAqi: List<Int?>`,
  `europeanAqi: List<Int?>`), attach as a nullable `hourly` field on
  `OpenMeteoAirQualityResponse`.

## 3. Android mapper changes

In the same file, extend `toDomain(countryCode: String?)` to also return the hourly list — e.g.
wrap in a small `AirQualityResult(current: AirQualityIndex?, hourly: List<HourlyAirQuality>)`.
Apply the same `startIndex = first index >= now` slicing pattern already used in
`OpenMeteoMapper.kt:66-84` for the main hourly weather forecast, but with
`endIndex = startIndex + 48` (48-hour window, not 24).

**Verified**: `OpenMeteoMapper.kt`'s `parseIsoDateTime(iso, utcOffsetSeconds)` (line 13) and
`parseIsoDate` (line 30) are currently `private fun`. Change to `internal fun` and call with
`utcOffsetSeconds = 0` for the UTC-default AQI hourly block, rather than duplicating the parser.

## 4. `WeatherData.kt` field + wiring

Add `val hourlyAirQuality: List<HourlyAirQuality> = emptyList()` next to
`airQuality: AirQualityIndex? = null` (currently line 33).

In `OpenMeteoWeatherSource.kt` (lines 46-47), thread the new hourly list through the same
`.copy(...)` call that already sets `airQuality`.

## 5. Android chart composable — new file

`app/src/main/java/orinasa/njarasoa/maripanatokana/ui/weather/components/AirQualityChart.kt` —
model this on **`DailyTemperatureChart.kt`**, not `TemperatureChart.kt`. Reasoning: this new chart
has no companion horizontally-scrolling card row driving its x-axis, so it should be the simpler,
fixed-width, evenly-spaced-points, no-scroll-sync style already used for the 10-day summary chart
(`x = (i.toFloat() / (pointsCount - 1)) * width`), not the scroll-synced viewport-overlay style
`TemperatureChart` needs to stay aligned with `HourlyForecastRow`'s scrollable cards.

Design, adapted from `DailyTemperatureChart`'s structure:
- Y-axis: AQI value (primary standard for the location — `usValue` or `europeanValue` per
  `AirQualityIndex.primaryStandard`), padded-min/max range exactly like the existing charts (not
  pinned to 0 — AQI values are typically clustered in a narrow band, and forcing a 0 baseline
  would flatten the visual trend the same way it would for temperature).
- X-axis: 48 hourly points, evenly spaced across the full chart width. Add midnight/noon vertical
  gridlines + time labels, reusing `TemperatureChart`'s labeling approach (`00:00`/`12:00` text
  drawn directly on the canvas) rather than `DailyTemperatureChart`'s Monday-only gridlines, since
  an hourly chart needs more frequent time anchors than a 10-day one.
- Line: a single smooth cubic-bezier line (same construction as both existing charts) connecting
  all 48 points, drawn in a neutral/white stroke — the connecting line's color is NOT tier-coded,
  only the dots are (per confirmed scope), since a per-segment gradient between two different tier
  colors would look messier than requested and wasn't asked for.
- Dots: each dot's fill color is `AqiTierBadge`'s existing `colorsFor(tier).background` for that
  hour's `tier(primaryStandard)`. **Requires changing `colorsFor` in `AqiTierBadge.kt` (currently
  `private fun`, line 21) to `internal fun`** so this new file can call it — do not duplicate the
  five hex color constants a second time.
- No per-point numeric labels (48 points would be too dense) and no tap/hover interactivity —
  purely visual, consistent with how `TemperatureChart`/`DailyTemperatureChart` are non-interactive
  today. The color conveys tier; vertical position conveys relative value via the padded-range
  y-axis. Flag as a scope decision to revisit later if exact per-hour numbers turn out to be
  wanted (e.g. via a future tap-to-see-tooltip enhancement, deliberately out of scope now).

**Placement note**: put this new file in `ui/weather/components/` alongside `AqiTierBadge.kt`,
`AirQualityDetailDialog.kt`, `UvTierBadge.kt` — not inline in `WeatherContent.kt`, and not next to
`TemperatureChart.kt`/`DailyTemperatureChart.kt` (those are generic chart utilities with no AQI
awareness; this one is AQI-domain-specific and belongs with the other AQI components).

## 6. Android wiring into `WeatherContent.kt`

Insert directly after the existing Hourly Forecast `CollapsibleSection` block (currently ends
~line 510) and before the 10-Day Forecast block:
```kotlin
if (data.hourlyAirQuality.isNotEmpty()) {
    CollapsibleSection(title = stringResource(R.string.section_air_quality_forecast), headerGraphicsLayer = headerGraphicsLayer) {
        AirQualityChart(
            forecasts = data.hourlyAirQuality,
            primaryStandard = data.airQuality?.primaryStandard ?: AqiStandard.US,
            utcOffsetSeconds = data.utcOffsetSeconds,
            modifier = Modifier.fillMaxWidth().height(140.sd(scale)),
        )
    }
    Spacer(modifier = Modifier.height(24.sd(scale)))
}
```
`CollapsibleSection`'s `initialExpanded` defaults to `false` (verified at
`WeatherContent.kt:675`) — intentionally omit it here so this new section starts collapsed,
unlike Hourly Forecast's `initialExpanded = true`, since it's a secondary section that shouldn't
compete for above-the-fold space.

Placement rationale: groups the "next N hours, time-series" sections together (Hourly Forecast,
then Air Quality Forecast) before the reader moves to the longer 10-day horizon.

## 7. Web mirror

- `web/src/lib/api/openMeteoAirQuality.ts`: add `hourly: 'us_aqi,european_aqi'` and
  `forecast_days: '3'` to `fetchAirQuality()`'s `URLSearchParams`; extend
  `OpenMeteoAirQualityResponse` type with an optional `hourly` block; add
  `mapToHourlyAirQuality(response, nowMillis = Date.now())` using the same
  slice-by-startIndex pattern as `openMeteoMapper.ts:42-47`, with a 48-hour window
  (`startIndex + 48`) instead of 24.
- **Verified**: `openMeteoMapper.ts`'s `parseIsoDateTime`/`parseIsoDate` (lines 8, 17) are plain
  (non-exported) functions — add `export` rather than duplicating, call with
  `utcOffsetSeconds = 0`.
- New type `HourlyAirQuality` goes in `web/src/lib/domain/weatherData.ts` (next to
  `HourlyForecast`, not in `airQuality.ts`) since it's forecast-shaped data consumed alongside
  `hourlyForecast`. Add `hourlyAirQuality: HourlyAirQuality[]` to `WeatherData` (next to
  `airQuality` at line 74).
- `web/src/lib/stores/weather.ts` (`fetchAtLocation`, lines 51-60): call
  `mapToHourlyAirQuality` alongside the existing `mapToAirQuality`, merge into the returned data.
- **New shared color export**: add `export const AQI_TIER_COLORS: Record<AqiTier, string>` to
  `web/src/lib/domain/airQuality.ts` (the five hex values already hardcoded as CSS classes in
  `AqiTierBadge.svelte` — `#00E400`/`#FFFF00`/`#FF0000`/`#8F3F97`/`#7E0023`). The chart needs actual
  color *values* (for SVG `fill` attributes), not CSS classes, so this is a small new export
  rather than a duplication — `AqiTierBadge.svelte` can optionally switch to referencing the same
  map later, but that's not required for this feature.
- New component `web/src/lib/components/AirQualityChart.svelte`, modeled on
  **`DailyTemperatureChart.svelte`** (fixed-width, evenly-spaced `x = (i / (pointsCount-1)) * width`
  points, no scroll-sync viewport logic) rather than `TemperatureChart.svelte`. Same design as the
  Android composable in §5: smooth bezier line in a neutral color connecting 48 evenly-spaced
  points, each `<circle>` filled via `AQI_TIER_COLORS[point.tier]`, midnight/noon gridlines +
  labels adapted from `TemperatureChart.svelte`'s labeling (not `DailyTemperatureChart.svelte`'s
  Monday-only gridlines).
- **Verified** `WeatherScreen.svelte` structure: Hourly Forecast section at lines 290-303
  (`CollapsibleSection` + `<HourlyForecast>`), 10-Day at lines 304+ (`<DailyForecast>`). Insert
  the new section's `{#if data.hourlyAirQuality.length > 0}...{/if}` block between them, mirroring
  the Android insertion point. Omit `expanded={true}` (defaults collapsed), matching Android's
  `initialExpanded` omission.

## 8. i18n

Add `"section_air_quality_forecast"` to all 8 `shared/i18n/locales/*.json` files, next to
`section_hourly_forecast`/`section_this_week`. Draft translations (final linguistic review
recommended, especially `mg` — its existing section titles are terse, e.g. "Isan'ora"):

| locale | value |
|---|---|
| en | Air Quality Forecast |
| es | Pronóstico de calidad del aire |
| fr | Prévisions de qualité de l'air |
| ar | توقعات جودة الهواء |
| hi | वायु गुणवत्ता पूर्वानुमान |
| ne | वायु गुणस्तर पूर्वानुमान |
| zh | 空气质量预报 |
| mg | Vinavinan'ny hadin-drivotra *(flag for native review)* |

After adding all 8, run `node shared/i18n/generate-android-strings.js` to regenerate Android
`strings.xml` — never hand-edit those files. No changes needed to `aqi_tier_labels`.

## 9. Verification plan (for whoever implements this)

1. `./gradlew :app:compileFdroidDebugKotlin`
2. `cd web && npm run check`
3. Install debug APK on emulator; use expert-mode location override for one US location and one
   European location (e.g. `de`/`fr`).
4. For each: expand the new section, screenshot, confirm:
   - The line renders smoothly across all 48 points with no gaps (confirms the hourly slice
     didn't drop any nulls silently in a way that breaks continuity).
   - Dot colors visibly change tier (e.g. green → yellow) when the underlying AQI value crosses
     a `AirQualityIndex.tierFor` threshold — spot-check at least one transition against the raw
     API values if the location's forecast has any variation.
   - The primary standard (US vs EU value plotted) matches the existing current-AQI card's
     `primaryStandard` for that location.
   - The chart's leftmost point's hour matches "now" (within an hour), and the rightmost point is
     ~48 hours later.
5. Repeat on Web with its location-override equivalent.

## Critical files

- `app/src/main/java/orinasa/njarasoa/maripanatokana/data/remote/OpenMeteoAirQualityApiService.kt`
- `app/src/main/java/orinasa/njarasoa/maripanatokana/domain/model/WeatherData.kt`
- `app/src/main/java/orinasa/njarasoa/maripanatokana/data/source/OpenMeteoWeatherSource.kt`
- `app/src/main/java/orinasa/njarasoa/maripanatokana/ui/weather/WeatherContent.kt`
- `app/src/main/java/orinasa/njarasoa/maripanatokana/ui/weather/components/AqiTierBadge.kt` (visibility change only)
- `web/src/lib/api/openMeteoAirQuality.ts`
- `web/src/lib/stores/weather.ts`
- `web/src/lib/components/WeatherScreen.svelte`
- `web/src/lib/domain/airQuality.ts` (new `AQI_TIER_COLORS` export)
- `shared/i18n/locales/*.json` (all 8 files)
