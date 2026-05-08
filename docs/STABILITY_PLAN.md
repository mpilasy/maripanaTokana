# Stability Plan

Known issues ranked by priority. Excludes Open-Meteo API and alerts URL failures
(those are external dependencies outside our control).

---

## P0 — ~~Fix before next release~~ DONE (v1.0.17)

### ~~1. R8 may silently break API deserialisation in release builds~~ ✓ FIXED

**Fixed in:** `app/proguard-rules.pro` (commit `6817ab8`)

Added explicit `-keep @kotlinx.serialization.Serializable class orinasa.njarasoa.maripanatokana.data.remote.** { *; }`
and `-keep class orinasa.njarasoa.maripanatokana.data.remote.**$$serializer { *; }`.
Verified in R8 mapping: all 10 `@Serializable` data classes survive with original
names. The kotlinx.serialization plugin ships companion/serializer() rules but does
not protect data class fields themselves; this closes that gap.

---

### ~~2. Location callbacks run on the main thread (ANR risk)~~ ✓ FIXED

**Fixed in:** `NativeLocationProvider.kt` (commit `6817ab8`)

Replaced `Looper.getMainLooper()` with a per-request `HandlerThread`. The thread is
started before registering providers and quit in `awaitClose` after each location
request completes or times out. Location callbacks no longer run on the UI thread.

---

## P1 — Fix in the next iteration

### 3. `spawnGpsCacheRefresh()` is not cancelled when `fetchJob` is cancelled

**Risk:** `spawnGpsCacheRefresh()` launches its own `viewModelScope` coroutine,
independent of `fetchJob`. Rapid `fetchWeather()` calls (e.g. permission grant +
immediate pull-to-refresh) cancel and restart `fetchJob` but leave multiple
background refreshes running in parallel. They race to write `cachedGpsWeatherData`
and `location_name` to SharedPrefs. Last writer wins; order is undefined.

**Where:** `WeatherViewModel.kt:249–263`, `fetchWeather()`, `refresh()`

**Fix:** Track the cache-refresh job separately (`private var cacheRefreshJob: Job?`)
and cancel it alongside `fetchJob` at the top of `fetchWeather()` and `refresh()`.

---

### 4. Screen rotation triggers an extra `fetchWeather()` call

**Risk:** On rotation the Activity recreates, `remember { mutableStateOf(checkGranted()) }`
reinitialises, and `LaunchedEffect(granted)` fires again with `granted = true`,
causing `fetchWeather()` to cancel the in-flight request and restart it from scratch.
Visible as a brief spinner flash and a wasted network round-trip on every rotation.

**Where:** `WeatherScreen.kt:81–88`, `FDroidPermissionHandler.rememberPermissionRequester()`

**Fix:** Only invoke `onGranted` if we weren't already in a Success or Loading state,
or gate the `LaunchedEffect` on a `rememberSaveable` flag so it only fires once per
install rather than once per composition.

---

### 5. Widget has no boot-time refresh

**Risk:** After a device reboot the widget shows the last cached temperature
indefinitely. It won't update until the user opens the main app or the next
scheduled widget update fires (which may be hours later depending on system
scheduling).

**Where:** `AndroidManifest.xml`, widget receiver.

**Fix:** Add `RECEIVE_BOOT_COMPLETED` permission and a `BroadcastReceiver` that
triggers a widget update on boot, or rely on `WorkManager` with a `BOOT_COMPLETED`
constraint.

---

## P2 — Fix when time allows

### 6. SharedPrefs writes use `apply()` — data lost on process kill

**Risk:** All SharedPrefs edits use `apply()` (async write). If the process is killed
immediately after saving a location or settings change (e.g. under memory pressure),
the write may not have flushed. On next launch the app uses stale or missing prefs.

**Where:** `WeatherViewModel.kt` — all `prefs.edit { ... }` blocks.

**Fix:** Switch critical writes (last known lat/lon, location name) to `commit()`
on a background dispatcher, or accept the risk and document it. Settings writes
(units, font, locale) are low-stakes and can stay as `apply()`.

---

### 7. Widget and main app share SharedPrefs without coordination

**Risk:** The widget and the main app both write to `widget_prefs`. If the widget
ever runs in a separate process (e.g. an OEM splits app widgets into a dedicated
process), concurrent writes are unsafe. Even in-process, a widget update that fires
mid-fetch can overwrite a location name the main app just wrote.

**Where:** `BaseWidgetWeatherFetcher.kt`, `WeatherViewModel.kt` — both use
`"widget_prefs"`.

**Fix:** Use a `ContentProvider` or `DataStore` with proper locking, or at minimum
document the single-process assumption in the widget code.

---

### 8. `movedSignificantly()` threshold is inaccurate at high latitudes

**Risk:** The threshold `dlat² + dlon² > 0.045²` treats degrees of latitude and
longitude as equivalent distances. At 60°N (Finland, Canada, Russia) 0.045° of
longitude ≈ 2.5 km, not 5 km, so users at high latitudes get weather re-fetched
twice as often as intended.

**Where:** `WeatherViewModel.kt:429–437`

**Fix:** Apply a `cos(lat)` correction to the longitude delta before comparing:
`dlon_km = dlon * cos(Math.toRadians(lat))`, then threshold on the corrected value.

---

## P3 — Known limitations, accept or document

### 9. `lat`/`lon` stored as `Float` in SharedPrefs

Float has ~7 significant decimal digits. For coordinates this introduces up to
~10 m of error. Harmless for weather data but technically imprecise.
**Fix:** Use `Double.toBits()` + `putLong()`, or switch to DataStore with a proper
schema. Low priority.

---

### 10. OkHttp requests are not cancelled when `withTimeoutOrNull` fires

When the 15s outer timeout cancels the coroutine, any in-flight OkHttp call keeps
running until the server responds or the connection drops. No crash results — the
response is simply discarded — but it wastes battery and bandwidth.
**Fix:** Pass the coroutine's `Job` to an OkHttp `Interceptor` that calls
`call.cancel()` when the job is cancelled.

---

### 11. Custom font may not cover Devanagari or Arabic glyphs

Hindi, Nepali, and Arabic locales are supported in-app. If the selected custom font
lacks those Unicode blocks, Android falls back silently to the system font, producing
an inconsistent visual mix.
**Fix:** Either bundle a font that covers all supported scripts, or detect missing
glyphs at locale-switch time and fall back to system font intentionally.

---

### 12. No `SavedStateHandle` — state lost on process death

If Android kills the process while the app is backgrounded (e.g. Success state with
weather data loaded), returning to the app shows a fresh `PermissionRequired` →
`Loading` → `Success` cycle instead of restoring instantly. Not a crash, just a
visible re-fetch.
**Fix:** Persist the last `WeatherData` in `SavedStateHandle` or re-hydrate it from
SharedPrefs on ViewModel init rather than starting from `PermissionRequired`.

---

*Last updated: 2026-05-08 — P0 items resolved in v1.0.17*
