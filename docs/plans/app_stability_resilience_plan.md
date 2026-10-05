# App Stability & Resilience Plan

## Summary

Audit of the Android app (Kotlin/Compose, both flavors) and the web app (SvelteKit + adapter-node PWA and its server proxies). All findings were checked against the code on 2026-10-05. Network claims were checked with live requests. A list of rejected findings at the end prevents them from being raised again.

Paths are repo-relative. `A/` = `app/src/main/java/orinasa/njarasoa/maripanatokana/`.

**Baseline at time of audit**

- `cd web && npm run check`: 0 errors, 0 warnings.
- `cd web && npm audit`: high-severity advisories in `@sveltejs/kit` (≤2.70.3, installed 2.50.2), `devalue`, `vite`, `picomatch`, `postcss`, `nanoid`; moderate in `svelte`, `esbuild`.
- Android unit tests: 5 test files exist (`app/src/test/...`). They could not be run locally: only JDK 25 is installed, and toolchain auto-provisioning is disabled by design (`Unable to download toolchain matching ... languageVersion=21`).
- CI (`.github/workflows/`): `fdroid-build.yml` builds the release and `version-sync-check.yml` checks version strings. **No workflow runs unit tests, lint, `npm run check`, or i18n parity.**
- i18n: all 8 locales currently have the same 152 keys.

## Decisions (2026-10-05)

| Topic | Decision | Affects |
|---|---|---|
| WMO SWIC certificate mismatch | Scoped check: a WMO-only client that keeps chain validation and accepts a cert valid for `cyclone.wmo.int`. Same approach on the web proxy. | 1.11, 4.2 |
| Android cloud backup | Split and exclude: API key and live GPS coordinates move to a separate prefs file excluded from backup. Settings and saved locations stay backed up. | 4.1 |
| Web tests | Add `vitest` as a dev dependency and run it in CI. | 0.1, 5.3 |
| SvelteKit advisories | Upgrade within 2.x now and re-run `npm audit`. Decide on Kit 3 later based on what remains. | 0.2 |

## Priority overview

Effort: XS < 1h, S ≈ half day, M ≈ 1–2 days.

| Phase | ID | Item | Platform | Effort |
|---|---|---|---|---|
| 0 Safety net | 0.1 | CI runs no tests, lint or type-check | Both | S |
| | 0.2 | Vulnerable web dependencies (Kit runs on the server) | Web | S–M |
| | 0.3 | Document the JDK 21 requirement for local tests | Android | XS |
| 1 Correctness | 1.1 | Swallowed `CancellationException` lets cancelled fetches overwrite state | Android | M |
| | 1.2 | `isRefreshing` can stick at `true` | Android | XS |
| | 1.3 | Alerts from the previous location attached to the current one | Both | S |
| | 1.4 | Overlapping web fetches resolve out of order | Web | S |
| | 1.5 | Web: cached-location failure aborts the fresh-location path | Web | XS |
| | 1.6 | Android: stuck Loading when cached-location weather fails | Android | S |
| | 1.7 | Share intent re-applied on every Activity recreation | Android | XS |
| | 1.8 | Hyphenated place names truncated ("Saint-Denis" → "Saint") | Both | XS |
| | 1.9 | Coordinate search: no range check, integers rejected | Both | XS |
| | 1.10 | `null` inside Open-Meteo arrays kills the whole payload | Android | M |
| | 1.11 | WMO SWIC TLS mismatch: alerts never load on Android | Android | S |
| | 1.12 | MeteoAlarm / WMO skipped when Geocoder is unavailable | Android | M |
| | 1.13 | Web proxies corrupt multi-byte UTF-8 | Web | XS |
| | 1.14 | `NativeLocationProvider` leaks listeners and a thread on its error path | Android (fdroid) | XS |
| | 1.15 | `$effect` interval leak in WeatherScreen | Web | XS |
| | 1.16 | Web a11y labels read raw `android_only.*` keys | Web + i18n | S |
| 2 Resilience | 2.1 | Android app has no offline cache (offline cold start shows error) | Android | M |
| | 2.2 | Refresh failures are invisible when data is on screen | Both | S |
| | 2.3 | Only two generic error messages | Both | S |
| | 2.4 | Alert source failures are silent; no logging at all | Both | S |
| | 2.5 | No fetch timeouts on web client / proxies | Web | S |
| | 2.6 | Service worker: no offline shell, caches error responses | Web | M |
| | 2.7 | No retry for transient network failures | Both | S |
| | 2.8 | Stale location fixes used without age check | Android | S |
| 3 Web server | 3.1 | Proxies hit upstream on every request (no caching) | Web | S |
| | 3.2 | Two Nominatim calls per load; usage-policy risk | Web (+Android) | S |
| | 3.3 | Inconsistent proxy failure handling | Web | XS |
| | 3.4 | No security headers / CSP | Web | S |
| | 3.5 | No custom error page / server error hook | Web | XS |
| | 3.6 | Docker runs as root; stale Caddyfile | Web | S |
| 4 Privacy & security | 4.1 | Cloud backup includes GPS coordinates and API key | Android | S |
| | 4.2 | WMO web proxy disables all certificate validation | Web | XS |
| | 4.3 | Google Fonts leaks user IP to Google | Web | S |
| | 4.4 | Short-link resolver reads unbounded body | Android | XS |
| 5 Polish | 5.1 | Share bitmaps: redundant copies, fixed filename | Android | XS |
| | 5.2 | Charts have no accessibility semantics | Both | S |
| | 5.3 | Tests to add alongside fixes | Both | ongoing |
| | 5.4 | Large files / duplicated fetch logic | Both | opportunistic |

Suggested order: Phase 0 first (cheap, and catches regressions from everything after it). Then 1.1–1.9 and 1.13–1.16 (small, user-visible bugs). Then 2.1–2.3 (the biggest "feels professional" gains). The remaining items can go in any order.

---

## Phase 0 — Safety net

**Status (2026-10-05): done, uncommitted.**
- `.github/workflows/ci.yml` added, with three jobs: i18n parity, Android unit tests plus lint (lint is non-blocking), and web check/test/build.
- `scripts/check_i18n_parity.js` added.
- Web dependencies upgraded within their majors: Kit 2.70.3, Svelte 5.57.1, Vite 7.3.6. No high-severity advisories remain. Still open: `cookie` (low), which needs Kit 3, and `esbuild` (moderate, dev server only, via `svelte-i18n`).
- `vitest` added, with `location.test.ts` (3 tests).
- JDK 21 note added to `AGENTS.md`.
- Android unit tests have not been run yet (no local JDK 21). The first CI run is the first real signal.

### 0.1 CI runs no checks on code changes
- **Where:** `.github/workflows/` contains only the release build and the version-sync check.
- **Fix:** add a workflow on push/PR with three jobs:
  - Android: JDK 21 via `actions/setup-java`, `node shared/i18n/generate-android-strings.js`, `./gradlew testFdroidDebugUnitTest testStandardDebugUnitTest lintFdroidDebug`.
  - Web: `npm ci && npm run check && npm test && npm run build` (copy `shared/i18n/locales` as the Dockerfile does if the symlink doesn't resolve in CI).
  - i18n: a small script that fails if any locale is missing a key present in another.
- **Constraint:** do not touch Gradle toolchain/foojay settings (see `docs/FDROID.md`).

### 0.2 Vulnerable web dependencies
- **Where:** `web/package.json`. `@sveltejs/kit` is a devDependency but is bundled into the Node server (`build/index.js`), so its advisories apply at runtime.
- **Decision: upgrade within 2.x now, decide on Kit 3 later.** Upgrade Kit, Svelte, Vite and svelte-check within their current majors (`npm update`), then `npm audit`. Record any advisories that remain, and decide on the Kit 3 migration based on that list. Verify with `npm run check && npm run build` and a manual smoke test of every `/api/alerts/*` route.

### 0.3 Local Android tests need JDK 21
- **Fix:** add one line to `AGENTS.md`/`README.md`: install a JDK 21 (e.g. Temurin) and point `org.gradle.java.installations.paths` (in `~/.gradle/gradle.properties`, not the repo) at it. Auto-provisioning stays disabled.

---

## Phase 1 — Correctness bugs

**Status (2026-10-05):** all items except 1.11 are done (commits `268611c`, `08821af`). 1.11 was resolved by removing WMO SWIC.

### 1.1 Swallowed cancellation overwrites newer state
- **Where:** 44 `catch (e: Exception)` / `catch (_: Exception)` / `runCatching` sites across `A/` and the flavor source sets. Key path: `app/src/fdroid/.../data/location/NativeLocationProvider.kt` `getFreshLocation()` catches `Exception`, which includes the `CancellationException` that `requestLocationUpdate()` deliberately rethrows.
- **Failure:** the user switches to a saved location while GPS acquisition is running. `fetchJob` is cancelled, the location provider converts the cancellation into `Result.failure`, and `doFetch`'s `onFailure` (`A/ui/weather/WeatherViewModel.kt:551-554`) sets `Error(error_get_location)` *after* the new location's data was shown, if no cached location was used.
- **Fix:** in every `catch (Exception)` inside a suspend function, rethrow `CancellationException` first (or catch narrower types). Start with the location providers (both flavors), `WeatherRepositoryImpl`, the data sources and `WeatherViewModel`. Replace `runCatching` in suspend code with an explicit try/catch.

### 1.2 `isRefreshing` can stay `true`
- **Where:** `A/ui/weather/WeatherViewModel.kt:434/461` (`fetchWeather`) and `:469/495` (`refresh`).
- **Failure:** `handleSharedText` (`:227`) cancels `fetchJob` mid-refresh. The reset at the end of the job never runs, so the pull-to-refresh spinner stays visible.
- **Fix:** move `_isRefreshing.value = false` into `finally`. (Web already does this in `web/src/lib/stores/weather.ts`.)

### 1.3 Alerts attached to the wrong location
- **Android:** `A/ui/weather/WeatherViewModel.kt:402-418`. Alerts are written into whatever `Success` state is current when they arrive. Both branches at `:406-409` are identical.
- **Web:** `web/src/lib/stores/weather.ts` `fetchAlertsForData` has the same unconditional write. `setWeatherData` carries alerts over when `locationName` matches, so two different places with the same name share alerts.
- **Fix:** keep a reference to the alert job and cancel it on each new fetch (Android). Tag each alert request with its lat/lon and drop the result if the displayed data is for different coordinates (both platforms). Compare coordinates, not names, in `setWeatherData`. Collapse the duplicate branch.

### 1.4 Overlapping web fetches resolve out of order
- **Where:** `web/src/lib/stores/weather.ts` `doFetchWeather`. It has no cancellation or ordering guard. Android cancels `fetchJob`; web has no equivalent.
- **Failure:** the user switches location while a refresh is in flight. The older request finishes last and overwrites the new location's weather.
- **Fix:** a module-level `fetchGeneration` counter. Capture it at the start, and ignore results (and errors) if it changed. Optionally pass an `AbortController` to the underlying fetches (pairs with 2.5).

### 1.5 Web: cached-location failure skips the fresh location
- **Where:** `web/src/lib/stores/weather.ts`, `await cachedFetchPromise` inside the single `try`.
- **Failure:** the weather request for the cached coordinates fails (e.g. rate limit). The whole function jumps to `catch` and never tries the fresh GPS position, which might have succeeded.
- **Fix:** catch the cached-fetch failure separately and continue to step 2.

### 1.6 Android: stuck Loading when cached-location weather fails
- **Where:** `A/ui/weather/WeatherViewModel.kt:507-562`.
- **Failure:** `usedCached` is set when a last-known location exists, *before* its weather loads. If that weather request fails and the fresh fix is within 5 km, step 2 skips its fetch. Nothing sets a state for 45 s, then the screen shows `error_get_location` (the wrong message).
- **Fix:** set the flag only after cached weather is displayed (rename to `cachedWeatherDisplayed`). Use `error_fetch_weather` for the timeout fallback when a location was obtained.
- **Audit findings (2026-10-05):** Race conditions remain in `doFetch()` where fresh GPS failure triggers `refreshFailed` or flashes an error screen over valid cached weather (A.3).

### 1.7 Shared location re-applied on recreation
- **Where:** `A/MainActivity.kt:53`. `handleSharedIntent(intent)` runs in every `onCreate`. The manifest declares no `configChanges`, so rotation, dark-mode toggle and process restore all recreate the Activity with the original share intent.
- **Failure:** the user shares a location, then switches to another saved location and rotates the phone. The app jumps back to the shared location.
- **Fix:** call it only when `savedInstanceState == null` (`onNewIntent` already covers later shares).

### 1.8 Hyphenated place names truncated
- **Where:** `A/ui/weather/WeatherViewModel.kt:444` and `:478`, `A/data/source/SystemGeocoderSource.kt:27`, `web/src/lib/stores/location.ts:74`. All split on `-`.
- **Failure:** "Saint-Denis" → "Saint", "Aix-en-Provence" → "Aix", "Stratford-upon-Avon" → "Stratford".
- **Fix:** split only on `,`, `;` and `" - "` (hyphen with spaces). Add unit tests with these names. Fix both platforms in the same change.

### 1.9 Coordinate search input
- **Where:** `A/ui/weather/WeatherViewModel.kt` `searchLocation` (regex `^(-?\d+\.\d+)\s*,\s*(-?\d+\.\d+)$`); check the web equivalent in `SavedLocationsDialog.svelte` / `geocodingSearch.ts`.
- **Problem:** `"95.0, 10.0"` is accepted and leads to an API 400 and a generic error. `"-18, 47"` (no decimals) is not recognized as coordinates.
- **Fix:** accept optional decimals and require lat ∈ [-90, 90] and lon ∈ [-180, 180]. Mirror on web.

### 1.10 Nulls in Open-Meteo responses
- **Where:** `A/data/remote/OpenMeteoResponse.kt:23-65`, `A/data/remote/OpenMeteoMapper.kt:88-95`.
- **Problem:** scalar fields in `current` and the element types of `hourly`/`daily` lists are non-nullable. `coerceInputValues` only helps for fields that have defaults. A `null` element inside a list (e.g. `precipitation_probability` beyond a model's horizon, `uv_index_max`) fails the whole response.
- **Fix:** make secondary metrics nullable (`List<Double?>`, `Double? = null`) and show `--` in the UI. Keep temperature and weather code strict. `BaseWidgetWeatherFetcher` decodes the same type, so the widget is covered by the same change. Check the web mapper handles `null` the same way.
- **Low priority:** `A/domain/model/Temperature.kt:16-22` throws on NaN via `roundToInt()`. JSON can't carry NaN, so only derived values could trigger it. Add a guard returning `"--"` and mirror it in `web/src/lib/domain/temperature.ts` (currently renders `"NaN°C"`).

### 1.11 WMO SWIC never loads on Android
- **Where:** `A/di/NetworkModule.kt:201-209`; the error is swallowed in `A/data/repository/WeatherRepositoryImpl.kt:246-254`.
- **Problem:** `severe.worldweather.wmo.int` serves a certificate for `CN=cyclone.wmo.int` (curl: `subjectAltName does not match hostname`). Alternative hosts `cyclone.wmo.int` and `severeweather.wmo.int` return 404 for `/json/PH.json`.
- **Update (2026-10-05): the endpoint itself is gone.** With TLS fixed (web proxy, round 1), `severe.worldweather.wmo.int/json/{CC}.json` returns 404 for every country tested, so WMO SWIC is dead on **both** platforms regardless of the certificate. SWIC now lives at `severeweather.wmo.int` (valid certificate). It exposes `json/wmo_all.json`, which is ~800 KB, ~1900 CAP items worldwide, and has no geometry: each item has `event`, `headline`, `areaDesc`, `sent`, `expires`, member id `mid`, severity `s`, urgency `u`, certainty `c`, and `capURL`. It also exposes `json/wmo_member.json` (member id → country). The source therefore needs a rewrite, and the TLS work below no longer applies. **Needs a new decision:** (a) rewrite on web only, with the server proxy downloading `wmo_all.json`, caching it 10 min, filtering by country via `mid`, and Android calling the proxy (this breaks the "Android has no server dependency" property); (b) rewrite on both, with Android downloading 800 KB per refresh, which is heavy on mobile data; (c) remove the WMO SWIC source on both platforms.
- **Decision (2026-10-05): remove WMO SWIC on both platforms.** Delete the source, its toggle, the API service, the web proxy and client, and its i18n keys. Update `CLAUDE.md`/`AGENTS.md` (7 alert sources) and any store or README text that mentions it. Countries without a regional source fall back to GDACS.
- **Original decision (superseded): scoped check.** Add a dedicated OkHttp client used only by `provideWmoSwicApiService`. Its `HostnameVerifier` accepts `severe.worldweather.wmo.int` only when the peer certificate is valid for `cyclone.wmo.int` (delegate to the default verifier with that name). Chain validation stays on, and no other client changes. Add a comment linking to this item so the workaround can be removed once WMO fixes the certificate. Web gets the same approach (4.2).

### 1.12 Country-gated alerts skipped without Geocoder
- **Where:** `A/data/repository/WeatherRepositoryImpl.kt:93-97`, `:180`, `:248`.
- **Problem:** the country code comes only from `android.location.Geocoder`. Bounding boxes already cover US/CA/AU/JP, but MeteoAlarm and WMO SWIC need `countryCode` and are skipped when there is no geocoder backend (some de-Googled devices). GDACS still runs.
- **Fix:** if Geocoder returns null or throws, reverse-geocode with the existing `NominatimGeocodingSource` (country plus subdivision for MeteoAlarm). Cache per rounded coordinate (see 3.2).

### 1.13 Web proxies corrupt UTF-8
- **Where:** `web/src/routes/api/alerts/bom/+server.ts`, `wmoswic/+server.ts`. They concatenate `Buffer` chunks with `raw += chunk` without `res.setEncoding('utf8')`.
- **Failure:** a multi-byte character split across two TCP chunks becomes `U+FFFD`, which shows up as garbled text in alert titles and descriptions.
- **Fix:** call `res.setEncoding('utf8')`, and treat non-2xx `res.statusCode` as a failure.

### 1.14 Location listener leak on error path
- **Where:** `app/src/fdroid/.../data/location/NativeLocationProvider.kt` inside `callbackFlow`, `catch (e: Exception) { close(e); return@callbackFlow }`.
- **Problem:** returning before `awaitClose` skips its cleanup. Providers registered earlier in the loop keep delivering updates, and the `HandlerThread` is never quit.
- **Fix:** on that path, call `removeUpdates(locationListener)` and `handlerThread.quitSafely()` before `close(e)`, or restructure so `awaitClose` always runs.
- **Also:** `app/src/fdroid/.../widget/WidgetWeatherFetcher.kt` catches only `SecurityException` around `getLastKnownLocation`. Any other exception skips the saved-coordinates fallback. Catch `Exception` there and fall through to prefs.

### 1.15 Interval leak in WeatherScreen
- **Where:** `web/src/lib/components/WeatherScreen.svelte:107-123`.
- **Problem:** `$effect` starts a `setInterval` without returning a cleanup. Unmounting during loading leaks it, and a repeated `loading` emission stacks a second interval.
- **Fix:** create the interval inside the `loading` branch and `return () => clearInterval(id)`. Drop the module-level `acquiringInterval` variable.

### 1.16 Web a11y labels read raw i18n keys
- **Where:** `web/src/lib/i18n/index.ts:9` drops `android_only`, but 14 `$_('android_only.cd_*')` calls remain in `Controls.svelte`, `HeroCard.svelte`, `CollapsibleSection.svelte`, `HourlyForecast.svelte`, `DailyForecast.svelte`, `DualUnitText.svelte`, `WeatherScreen.svelte`.
- **Fix:** move the `cd_*` keys used on web to top level in all 8 `shared/i18n/locales/*.json`, update the Android references, regenerate with `node shared/i18n/generate-android-strings.js`, and switch web calls to `$_('cd_share')` etc.

---

## Phase 2 — Resilience, offline & error UX

**Status (2026-10-05):**
- **Done** (`67ff726`): 2.1, 2.2, 2.3, 2.5, 2.6, 2.7.
- **2.8 partial:** Android ignores fixes older than 24 h, but the `last_known_location` note isn't shown yet. That needs `LocationProvider` to return the fix age.
- **2.4 done** (`96a3236`): per-source alert status (shown in advanced mode) and debug logging.

### 2.1 Android app has no offline cache
- **Problem:** `WeatherRepositoryImpl.getWeather` always hits the network. Launching the app offline (or in a dead zone) shows the error screen, even though the widget already keeps a cached forecast (`A/widget/BaseWidgetWeatherFetcher.kt:60-74`).
- **Fix:** persist the last successful `WeatherData` per location key (GPS / saved id / preview), plus the timestamp. On launch, show it immediately with a "data from HH:MM" label, then refresh. A JSON file in `filesDir` is enough; no database needed. Web gets this partly through the service worker API cache; make it explicit there too (localStorage snapshot of the last success).
- **Audit findings (2026-10-05):** Implementation has two critical defects: web snapshot prototype restoration fails under minification (A.1), and Android cache file writes have a concurrent race condition on shared `tmp.json` (A.2).

### 2.2 Silent refresh failures
- **Where:** Android `fetchWeather`/`refresh`/`fetchForSavedLocation` `onFailure` branches only act when nothing is on screen. Web `doFetchWeather` `catch` does the same.
- **Problem:** after a failed refresh the user sees old data with no hint that it is stale.
- **Fix:** keep the data and show a non-blocking indicator ("Couldn't refresh · data from 14:05") in the header area, plus a retry action. New i18n keys go in all 8 locales.

### 2.3 Error messages don't say what went wrong
- **Problem:** only `error_fetch_weather` and `error_get_location` exist. Offline, timeout, rate-limited (429), server error, and invalid Pirate Weather key (401/403) all look identical.
- **Fix:** map exceptions to a small error enum in the repository/data source (`Offline`, `Timeout`, `RateLimited`, `Server`, `InvalidApiKey`, `Location`), with one message each and a suitable action (retry / open settings / open location settings). Mirror on web.

### 2.4 Silent alert sources, no logging
- **Problem:** every alert source failure becomes `emptyList()`, so "no alerts" is indistinguishable from "source unreachable". There are zero `Log.` calls in the Android code, so failures leave no trace even in debug builds.
- **Fix:** have each source return a status (ok / failed / skipped) along with its alerts. In advanced mode or settings, show sources that failed ("MeteoAlarm unavailable"). Add a tiny debug-only logging helper (no-op in release; no third-party crash reporting, to keep F-Droid anti-feature free) and use it in catch blocks. Same on web with `console.warn`.

### 2.5 No fetch timeouts on web
- **Where:** every `fetch` in `web/src/lib/api/**` and `web/src/routes/api/alerts/**`; no `AbortSignal` in `web/src`. The `node:https` call in `wmoswic/+server.ts` has no timeout (`bom` has one).
- **Fix:** `signal: AbortSignal.timeout(10_000)` for client fetches, 8 s for proxies (below the client timeout). Add `req.setTimeout` to WMO.

### 2.6 Service worker offline behavior
- **Where:** `web/src/service-worker.ts`.
- **Problems:**
  1. `/` is server-rendered, so it is never in `APP_ASSETS`. The fallback `base + '/index.html'` does not exist under adapter-node, so offline navigation returns the 503 "Offline" response.
  2. Responses are cached regardless of status, so a 429/500 can later be served as data.
  3. `air-quality-api.open-meteo.com` is not handled.
- **Fix:** for `request.mode === 'navigate'`, cache successful responses and fall back to the cached `/`. `cache.put` only when `response.ok`. Add the air-quality host to the NetworkFirst branch.
- **Verify:** load once online, go offline in DevTools, reload. The page should render with the last data.

### 2.7 No retry for transient failures
- **Problem:** a single dropped packet or 503 on the weather request shows an error.
- **Fix:** one automatic retry with ~1 s backoff for `IOException`/5xx on the forecast request only (an OkHttp interceptor scoped to the weather clients; a small wrapper on web). Do not retry alert sources, which are already best-effort.

### 2.8 Location age not checked
- **Where:** `NativeLocationProvider.getStaleFallbackLocation()` and `getLastLocation()`; check the Play Services provider too.
- **Problem:** a fix from days ago (e.g. before a flight) can be used without any indication that it is old.
- **Fix:** ignore fallback fixes older than a threshold (e.g. 24 h) or pass the fix age up and show "approximate / last known location".

---

## Phase 3 — Web server & deployment

**Status (2026-10-05): done** (`f2949d5`). The CSP ships as report-only, with violations logged by `/api/csp-report`. Switch `reportOnly` to `directives` in `svelte.config.js` once a browser session shows no violations.

### 3.1 Proxies hit upstream on every request
- **Where:** all `web/src/routes/api/alerts/*/+server.ts`. Every client load re-downloads the full BOM national warnings list, the NHC storms list, MeteoAlarm country feeds, and so on.
- **Fix:** a small in-memory TTL cache (5–10 min) keyed by URL in a shared helper, plus `Cache-Control: public, max-age=300` on responses. This cuts upstream load, rate-limit risk, and latency.

### 3.2 Nominatim usage
- **Where:** `web/src/lib/stores/location.ts` `reverseGeocode` and `web/src/lib/api/alerts/shared.ts` `getLocationInfo`. These are two concurrent reverse lookups per load. The Nominatim policy allows at most 1 req/s and bans heavy users. Browsers silently drop the custom `User-Agent` header (forbidden header), so requests identify only by Referer.
- **Fix:** a single reverse call with `addressdetails=1` serving both the name and the country/subdivision, cached per coordinate rounded to ~1 km. Apply the same cache on Android when Nominatim is the geocoder (and for 1.12).

### 3.3 Inconsistent proxy failures
- **Where:** `nhc/+server.ts` has no try/catch, so a network error becomes a 500. `meteoalarm` throws the upstream status. `bom`, `eccc` and `wmoswic` return an empty payload.
- **Fix:** pick one convention (recommended: 502 with an empty body; client sources already treat non-OK as no alerts) and apply it everywhere. This also feeds the source status in 2.4.

### 3.4 No security headers
- **Where:** `web/src/hooks.server.ts` only handles a redirect.
- **Fix:** configure `kit.csp` in `svelte.config.js` (`connect-src` limited to the API hosts actually used; `font-src`/`style-src` per 4.3). Add `X-Content-Type-Options: nosniff`, `Referrer-Policy: strict-origin-when-cross-origin`, and `Permissions-Policy: geolocation=(self)` in `handle`.

### 3.5 No error page / server error hook
- **Fix:** add `src/routes/+error.svelte` (localized, with reload) and a `handleError` hook in `hooks.server.ts` that logs the error.

### 3.6 Container and proxy config
- **Where:** `web/Dockerfile` runs as root on port 80, with no `HEALTHCHECK` and no `NODE_ENV=production`. `web/Caddyfile` describes the old static deployment (`root * /srv/app`, `try_files {path} /index.html`), which contradicts adapter-node.
- **Fix:** `USER node`, `PORT=3000`, `NODE_ENV=production`, and a `HEALTHCHECK` (`wget -qO- localhost:3000/`). Map the port in `docker-compose.yml`. Delete the Caddyfile, or rewrite it as a `reverse_proxy` to the Node server if Caddy is still used. Remove the empty untracked `src/routes/api/alerts/meteofrance/` and `metoffice/` directories.

---

## Phase 4 — Privacy & security

**Status (2026-10-05): done.**
- 4.1: sensitive keys moved to `private_prefs` with a one-time migration. That file and `weather_cache/` are excluded from backup and device transfer.
- 4.2: moot, since WMO SWIC was removed.
- 4.3: fonts self-hosted under `web/static/fonts`, OFL licenses included.
- 4.4: the resolver follows https redirects only and caps the body at 256 KB.

### 4.1 Cloud backup includes location and API key
- **Where:** `app/src/main/AndroidManifest.xml` has `allowBackup="true"`. `res/xml/backup_rules.xml` and `data_extraction_rules.xml` are unmodified templates, so everything is backed up. The single `widget_prefs` file holds GPS coordinates (`lat`, `lon`, `last_render_*`), saved locations, and `settings_weather_api_key`.
- **Decision: split and exclude.**
  - Move `settings_weather_api_key`, `lat`, `lon`, `last_render_lat`, `last_render_lon` and the cached widget response to a new prefs file (e.g. `private_prefs`).
  - Exclude that file in both `backup_rules.xml` (`<exclude domain="sharedpref" path="private_prefs.xml"/>`) and `data_extraction_rules.xml` (`cloud-backup` and `device-transfer`).
  - Add a one-time migration on startup that copies existing values from `widget_prefs` and removes them there.
  - Update every reader: `AppSettingsRepository`, `WeatherViewModel`, both `WidgetWeatherFetcher` flavors, `BaseWidgetWeatherFetcher`.
  - Settings, saved locations, language, units and font stay in `widget_prefs` and remain backed up.

### 4.2 WMO web proxy disables certificate validation
- **Where:** `web/src/routes/api/alerts/wmoswic/+server.ts` uses `rejectUnauthorized: false`.
- **Problem:** this disables chain validation entirely, not just the hostname check, so anyone on the server's network path can inject alerts.
- **Fix:** keep `rejectUnauthorized: true` and pass a `checkServerIdentity` that validates against `cyclone.wmo.int` (`tls.checkServerIdentity('cyclone.wmo.int', cert)`). Same pattern as 1.11(a).

### 4.3 Google Fonts on web
- **Where:** `web/src/lib/fonts.ts` loads every pairing from `fonts.googleapis.com`.
- **Problem:** every visitor's IP goes to Google (a GDPR concern), which doesn't match the app's privacy positioning; Android bundles its fonts in `res/font`.
- **Fix:** self-host the WOFF2 files under `web/static/fonts/` with local `@font-face`. This also simplifies the CSP in 3.4 and the service worker font branch.

### 4.4 Short-link resolver
- **Where:** `A/data/location/SharedLocationParser.kt` `resolveUrlRedirect`. It follows redirects to any host and reads the full response body (`readText()`) into memory.
- **Fix:** only follow `https` redirects, and cap the body read (e.g. 256 KB).

---

## Phase 5 — Polish, tests, maintainability

**Status (2026-10-05):**
- **5.1 done:** no redundant bitmap copies, unique share filenames, cleanup of files older than 1 h.
- **5.2 done** for the temperature, daily temperature, UV and air-quality charts on both platforms. The nowcast chart has no summary yet, though the nowcast text next to it carries the same information.
- **5.3:** tests landed with each fix (Android unit tests; 22 vitest tests on web).
- **5.4:** opportunistic, not started.
- **Also done:** web shows `--` for missing humidity, wind direction, cloud cover, UV and visibility, matching Android.

### 5.1 Share bitmaps
- **Where:** `A/ui/weather/WeatherContent.kt:1748-1760`, `:1765`.
- **Problem:** `combineBitmaps` copies both inputs, so 5 ARGB_8888 bitmaps are alive at once. Output always goes to `shared_images/weather.png`, so a second share can overwrite a file still being read.
- **Fix:** draw the inputs directly (copy only if a source uses `HARDWARE` config). Use a timestamped filename and delete older files in `shared_images/` first.

### 5.2 Chart accessibility
- **Problem:** Canvas charts (`TemperatureChart`, `DailyTemperatureChart`, `DailyUvChart`, `AirQualityChart` on both platforms) expose nothing to TalkBack or screen readers.
- **Fix:** a `semantics { contentDescription = ... }` summary on Android ("Next 24 h: high 24°, low 15°, rain from 14:00") and `role="img"` with `aria-label` on web.

### 5.3 Tests to add with the fixes
- Android (`app/src/test/...`):
  - mapper with null list elements (1.10)
  - name splitting (1.8)
  - coordinate parsing (1.9)
  - ViewModel cancellation, stuck-loading and alert-desync scenarios with `kotlinx-coroutines-test` (1.1, 1.2, 1.3, 1.6)
  - alert parsers against recorded fixture payloads (MeteoAlarm Atom, BOM, ECCC, JMA, NHC)
- Web (**decision: add `vitest`** as a dev dependency, with an `npm test` script run in the 0.1 CI job):
  - fetch generation guard (1.4)
  - name splitting and coordinate parsing (1.8, 1.9)
  - proxy helpers: UTF-8 decoding, timeout, TTL cache (1.13, 2.5, 3.1)
  - service worker URL/status gating (2.6)

### 5.4 Maintainability (opportunistic only)
- `A/ui/weather/WeatherContent.kt` (1839 lines) and `web/src/lib/components/WeatherScreen.svelte` (903 lines) are hard to review.
- `WeatherViewModel.fetchWeather` and `refresh` duplicate ~30 lines.
- Extract pieces only when a fix above touches them; no standalone refactor.

---

## Post-Implementation Adversarial Audit (2026-10-05)

Deep audit of commits `88d3de8` through `f746b7d` and working tree changes. Verified with live code analysis and production build artifact inspection.

**Verification of this audit (2026-10-05).** Each claim was re-checked against the code and the production bundle.

| Item | Verdict | Action |
|---|---|---|
| A.1 | Confirmed, critical: the built client bundle minifies `Temperature` to `class ie`, so no `__class` tag is written | Fixed |
| A.2 | Confirmed: one shared `tmp.json` for all keys | Fixed |
| A.3 | Confirmed on both platforms | Fixed |
| A.4 | Confirmed; medium rather than high, since Play Services times the request out itself | Fixed |
| A.5 | Confirmed but rare: missing providers already return false from `isProviderEnabled`; predates this branch | Deferred |
| A.6 | Confirmed; predates this branch | Fixed |
| A.7 | Low: on API 26+ bitmap pixel memory is freed with the object | Deferred |
| A.8 | Partly: `"%.1f".format` uses the device locale rather than the app locale; native digits are missing for ar, hi and ne | Fixed |
| A.9 | True, cosmetic | Deferred |
| A.10 | True, low | Deferred |
| A.11 | True, low | Deferred |

A.1 was reproduced end to end against the production build in headless Chromium. With the pre-fix build, the offline reload threw `TypeError: e.data.temperature.displayDualMixed is not a function`; with the fix it rendered the snapshot with no exceptions.

Emulator testing (commit `7ad4d2b`) separately found and fixed three bugs:
- Settings switches didn't update in release builds, because the prefs listener was garbage-collected after R8.
- NWS was queried outside the US.
- The share-sheet preview was denied access to the image.

### A.1 [CRITICAL] Web: Production minification breaks `weatherSnapshot` prototype reconstruction
- **Where:** `web/src/lib/stores/weatherSnapshot.ts:16-34`.
- **Problem:** `replacer` uses `(original as object).constructor?.name` to index `CLASSES`. In production builds (`npm run build`), Vite/esbuild minifies class names (e.g. `Temperature` $\to$ `ie`, `Pressure` $\to$ `Qr`). At runtime, `original.constructor.name` is `"Qr"`, while `CLASSES` keys are strings (`"Pressure"`). `CLASSES[name]` returns `undefined`, so `__class` is never saved. On reload, `loadSnapshot` deserializes plain untyped objects without prototype methods.
- **Failure:** On startup, `doFetchWeather` displays the cached snapshot. Components calling methods like `data.temperature.displayDual()`, `displayCelsius()`, or `primaryFirst()` throw `TypeError: data.temperature.displayDual is not a function`, crashing the entire app on load. Vitest failed to catch this because test runners do not minify class names.
- **Fix:** Serialize domain values with explicit schema tags or restore domain value class instances via static factory methods (e.g. `Temperature.fromCelsius(...)`) in `loadSnapshot()` instead of relying on `constructor.name`.

### A.2 [CRITICAL] Android: Concurrent cache writes corrupt `WeatherCache`
- **Where:** `A/data/repository/WeatherCache.kt:24-33`.
- **Problem:** `save(key, data)` hardcodes `val tmp = File(dir, "tmp.json")` without file locking or synchronization.
- **Failure:** `WeatherViewModel.publish()` dispatches `weatherCache.save()` on `Dispatchers.IO`. If two background fetches complete concurrently (e.g., GPS background refresh via `spawnGpsCacheRefresh()` and a user switching saved locations), both coroutines write to `tmp.json` simultaneously. One write overwrites the other, causing `tmp.renameTo(file(key))` to fail or rename Location B's data into Location A's cache file.
- **Fix:** Use unique temporary files per key (e.g. `File(dir, "$key.tmp")`) or `File.createTempFile("weather", ".tmp", dir)` and synchronize cache writes.

### A.3 [HIGH] Android & Web: `doFetch` race condition marks successful weather as `refreshFailed`
- **Where:** `A/ui/weather/WeatherViewModel.kt:520-574` and `web/src/lib/stores/weather.ts:207-256`.
- **Problem:**
  - Android: Step 1 (cached location) and Step 2 (fresh GPS) run in parallel coroutines. If Step 1 succeeds and displays weather, but Step 2's fresh GPS fix fails or times out later, Step 2 calls `onWeatherFailure()` which sets `_refreshFailed.value = true` because `_uiState.value` is already `Success`. If Step 2 fails fast before Step 1 finishes weather network request, it flashes `WeatherUiState.Error(R.string.error_get_location)` before Step 1 overwrites it with `Success`.
  - Web: Step 1 renders cached-location weather, then `const fresh = await freshLocationPromise` rejects on GPS timeout or permission refusal. The outer `catch` block catches the rejection while `current.kind === 'success'`, setting `refreshFailed = true`.
- **Failure:** A user indoors or in poor satellite reception sees a persistent "Couldn't refresh" error banner even though fresh weather for their location was just fetched successfully.
- **Fix:** Fresh GPS failure should not mark the fetch as failed when cached-location weather has just been displayed successfully. Only flag `refreshFailed` if both cached and fresh attempts fail, or if a manual user refresh fails.

### A.4 [HIGH] Android: Leaked `CancellationTokenSource` in `PlayServicesLocationProvider`
- **Where:** `app/src/standard/.../data/location/PlayServicesLocationProvider.kt:47-52`.
- **Problem:** `CancellationTokenSource().token` is passed inline to `getCurrentLocation(...)` inside `withTimeoutOrNull(10_000L)`.
- **Failure:** When `withTimeoutOrNull` expires, the coroutine cancels, but `cts.cancel()` is never called. Play Services continues tracking GPS in the background until its internal timeout expires, draining battery.
- **Fix:** Enclose `getCurrentLocation` in `try { ... } finally { cts.cancel() }`.

### A.5 [HIGH] Android (F-Droid): Exception in `NativeLocationProvider` aborts multi-provider fallback
- **Where:** `app/src/fdroid/.../data/location/NativeLocationProvider.kt:138-144`.
- **Problem:** Inside the provider registration loop (`GPS_PROVIDER`, `NETWORK_PROVIDER`, `PASSIVE_PROVIDER`), the `catch (e: Exception)` block calls `locationManager.removeUpdates(...)`, `handlerThread.quitSafely()`, `close(e)`, and `return@callbackFlow`.
- **Failure:** If `GPS_PROVIDER` throws `IllegalArgumentException` or any runtime error on customized or restricted devices, the loop immediately terminates, completely preventing `NETWORK_PROVIDER` and `PASSIVE_PROVIDER` from registering.
- **Fix:** Catch exceptions per provider, log, and continue to the next provider instead of terminating the flow.

### A.6 [MEDIUM] Android: `OpenMeteoWeatherSource` bypasses `CountryResolver`
- **Where:** `A/data/source/OpenMeteoWeatherSource.kt:42-52`.
- **Problem:** `WeatherRepositoryImpl` uses `CountryResolver` (Geocoder with Nominatim fallback), but `OpenMeteoWeatherSource` still calls `Geocoder(context, Locale.US).getFromLocation(...)` directly for AQI standard selection (`US_AQI` vs `European AQI`).
- **Failure:** On de-Googled devices without a system geocoder, AQI standard detection always fails and defaults to US AQI, even in European countries.
- **Fix:** Inject and use `CountryResolver` in `OpenMeteoWeatherSource`.

### A.7 [MEDIUM] Android: Share card bitmap allocated in `combineBitmaps` is never recycled
- **Where:** `A/ui/weather/WeatherContent.kt:1772-1821`.
- **Problem:** `combineBitmaps` creates a composite ARGB_8888 bitmap. `shareCardBitmap` compresses it to PNG but never calls `recycle()`.
- **Failure:** Multiple consecutive shares accumulate large uncompressed bitmaps (5–10MB each) in native memory until GC runs.
- **Fix:** Call `bitmap.recycle()` after `compress()`.

### A.8 [MEDIUM] Chart accessibility summaries violate `Locale.US` and lack digit localization
- **Where:** `A/ui/weather/components/DailyUvChart.kt:74`, `AirQualityChart.kt:99`, `TemperatureChart.kt:93-95`, and `web/src/lib/components/DailyUvChart.svelte:39`.
- **Problem:**
  - `DailyUvChart.kt` uses `"%.1f".format(maxUv)` without `Locale.US`.
  - Android chart descriptions are passed to `stringResource` without `localizeDigits()`, reading ASCII digits to TalkBack in Arabic, Hindi, and Nepali.
  - Web `DailyUvChart.svelte` uses `toFixed(1)` (producing ASCII decimal points in French/Spanish/Malagasy) and does not call `loc()`.
- **Fix:** Enforce `Locale.US` in format strings and apply `localizeDigits()` / `loc()` to all numeric values before passing to summary strings.

### A.9 [MEDIUM] UI Inconsistency: `refresh_failed` display parity
- **Where:** `A/ui/weather/WeatherContent.kt:356-370` vs `web/src/lib/components/HeroCard.svelte:52-54`.
- **Problem:** Android displays `refresh_failed` as an additional line under `updated_time`, while Web replaces `updated_time` inside the refresh button.
- **Fix:** Align visual presentation across both platforms.

### A.10 [LOW] Web: Unauthenticated `/api/csp-report` logging endpoint lacks rate-limiting
- **Where:** `web/src/routes/api/csp-report/+server.ts:5-14`.
- **Problem:** Public unauthenticated endpoint logs parsed JSON directly to stdout with `console.warn`.
- **Fix:** Add basic throttling / rate limiting to avoid log flooding.

### A.11 [LOW] Tooling: `check_i18n_parity.js` blind to array length mismatches
- **Where:** `scripts/check_i18n_parity.js:12-18`.
- **Problem:** Arrays like `cardinal_directions` and `uv_labels` are treated as leaf values without checking element count across locales.
- **Fix:** Validate that array keys have identical lengths across all locales.

---

## Rejected findings (verified false — do not re-raise)

- **GDACS needs a web proxy for CORS:** GDACS returns `Access-Control-Allow-Origin: *`.
- **GDACS polygon geometry crashes Android:** the live feed returns only `Point` geometries, and a parse failure would only empty the GDACS list.
- **`handleSharedText` permanently blocks refresh:** `isResolvingSharedLocation` is reset on every path (`WeatherViewModel.kt:233, 245, 253`).
- **`Float.MIN_VALUE` / float precision for coordinates:** works as a sentinel; float precision (~1 m) is far below the 5 km threshold.
- **WorkManager retries drain battery offline:** the periodic work has a `NetworkType.CONNECTED` constraint.
- **Widget shows "refreshed just now" for cached data:** widgets render `data.timestamp`, the cached time. (`last_refresh` written in `WeatherUpdateWorker.kt:23` is never read and can be removed.)
- **Open-Meteo parallel daily arrays may differ in length:** the API returns equal-length arrays.
- **Thread race on `usedCached` flags:** both coroutines run on the Main dispatcher. The real bug there is 1.6.
- **i18n keys missing between locales:** all 8 locales currently have the same 152 keys (0.1 adds a CI guard).
- **"Single return" / "no break/continue" rules:** not project rules (not in AGENTS.md).
