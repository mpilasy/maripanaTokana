# Stability Plan

Known issues ranked by priority, tracked against the codebase. Excludes external API downtime (Open-Meteo, upstreams).

---

## P0 — Resolved

### 1. R8 silently breaking API deserialization in release builds — FIXED
**Fixed in:** `app/proguard-rules.pro` (v1.0.17 / v1.0.18).
Added explicit keep rules for `@Serializable` classes and Room/WorkManager database implementations (`WorkDatabase_Impl`).

### 2. Location callbacks on main thread (ANR risk) — FIXED
**Fixed in:** `NativeLocationProvider.kt` (v1.0.17).
Replaced `Looper.getMainLooper()` with a dedicated `HandlerThread`.

---

## P1 — Prioritized Items

### 3. Rapid `fetchWeather()` calls coroutine cancellation
**Risk:** Rapid weather fetches (e.g. permission grant + simultaneous pull-to-refresh) cancel `fetchJob` but leave concurrent tasks running. Last writer wins on location/prefs writes.
**Where:** `WeatherViewModel.kt`
**Mitigation:** `fetchJob` manages the primary fetch pipeline, and card/location states collapse cleanly on location change (v1.2.14).

### 4. Screen rotation / composition re-triggers
**Risk:** Activity recreation or permission state changes could trigger duplicate `fetchWeather()` network calls.
**Where:** `WeatherScreen.kt`, permission handlers.
**Fix:** Gate `onGranted` callbacks on current `WeatherUiState` so loading or success states don't re-trigger unnecessary round-trips.

### 5. Widget boot-time refresh
**Risk:** After device reboot, widget shows last cached temperature until next WorkManager periodic update (30 min).
**Where:** `AndroidManifest.xml`, `WeatherWidgetReceiver`.
**Fix:** Maintain WorkManager `BOOT_COMPLETED` constraint or explicit boot receiver trigger.

---

## P2 — Secondary Enhancements

### 6. SharedPreferences writes use `apply()` — potential loss on force kill
**Risk:** Async `apply()` writes for critical location state could be lost if process is killed mid-write.
**Where:** `WeatherViewModel.kt`, `AppSettingsRepository.kt`.
**Fix:** Use `commit()` or background dispatcher for critical saved location list writes (`persistSavedLocations`).

### 7. Widget and main app SharedPrefs coordination
**Risk:** Widget and main app read/write `"widget_prefs"`. Concurrent writes across processes could conflict.
**Where:** `BaseWidgetWeatherFetcher.kt`, `WeatherViewModel.kt`.
**Fix:** Single-process assumption is currently enforced; document and keep keys partitioned.

### 8. `movedSignificantly()` distance check at extreme latitudes
**Risk:** Simple `dlat² + dlon² > 0.045²` treats degree deltas as isometric. At high latitudes (60°N+), 0.045° longitude is ~2.5 km rather than 5 km.
**Where:** `WeatherViewModel.kt:movedSignificantly()`, `web/src/lib/stores/location.ts:movedSignificantly()`.
**Fix:** Add `cos(lat)` correction for longitude delta calculation on both platforms.

---

## P3 — Documented Limitations

### 9. Coordinate precision in SharedPreferences
Stored as Float in SharedPrefs (~10m precision error). Harmless for weather data lookup.

### 10. In-flight OkHttp call cancellation on timeout
Outer coroutine timeout cancels the job, but network socket closes naturally. Negligible overhead.

### 11. Saved state handle re-hydration
If process is killed in background, app re-fetches fresh weather on launch rather than restoring dead state memory.

---

*Last updated: August 2026*
