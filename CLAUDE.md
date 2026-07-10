# Claude Code Instructions

Read [AGENTS.md](./AGENTS.md) for the full project context — architecture, tech stack, build commands, critical rules, and mandates.

For F-Droid build issues, read [docs/FDROID.md](./docs/FDROID.md) before touching any Gradle config.

## Key Rules

- Do NOT add `Co-Authored-By: Claude` or any AI attribution to commit messages.
- Do NOT add `id("org.jetbrains.kotlin.android")` to `app/build.gradle.kts` — causes extension conflict.
- Do NOT add foojay-resolver to `settings.gradle.kts` — F-Droid blocks it.
- `kotlin { jvmToolchain(21) }` must stay in `app/build.gradle.kts`.
- `auto-provisioning=disabled` must stay in `gradle.properties`.
- Feature parity: keep Android and Web in sync.

## Model Routing

Use the right model for the job to save cost and context:

- **Haiku** (`model: haiku`): Documentation, i18n/translation updates, string resource changes, README edits, simple renames, formatting fixes, lint cleanup.
- **Sonnet** (`model: sonnet`): Standard feature implementation, bug fixes, refactoring, test writing, build config changes, most day-to-day coding tasks.
- **Opus** (`model: opus`): Architecture decisions, cross-platform porting (Android ↔ Web), debugging complex build failures (F-Droid CI, Gradle toolchain issues), multi-file refactors touching both platforms, and anything requiring deep reasoning about side effects.

## Agent Usage

- Use **Explore subagents** for codebase searches rather than multiple sequential Grep calls. This project has two platforms (Kotlin + Svelte) so searches often span both.
- Use **parallel subagents** when making changes to both Android and Web for the same feature — they're independent codebases with no compile-time dependencies.
- Use **background agents** for running builds (`./gradlew assembleFdroidRelease`, `cd web && npm run build`) while continuing other work.
- When porting a feature between platforms, read the source implementation first with an Explore agent, then implement in the target platform. Don't guess at the existing behavior.

## Tool Preferences

- Use `Edit` for file modifications, never `sed`/`awk` via Bash.
- Use `Grep` for content search, never `grep`/`rg` via Bash.
- Use `Glob` for file search, never `find`/`ls` via Bash.
- For Gradle/build changes, always dry-run first: `./gradlew :app:assembleFdroidRelease --dry-run`.
- For web changes, type-check after: `cd web && npm run check`.

## Project-Specific Patterns

- **Android strings**: Never edit `res/values-*/strings.xml` directly. Edit `shared/i18n/locales/*.json` then run `node shared/i18n/generate-android-strings.js`.
- **Domain model changes**: If you change a value class (Temperature, Pressure, etc.) in Kotlin, you must also update the TypeScript equivalent in `web/src/lib/domain/` and vice versa.
- **New i18n keys**: Add to all 8 JSON files in `shared/i18n/locales/`. Missing keys will show raw key strings at runtime.
- **Array i18n keys** (e.g. `cardinal_directions`): Use `$json('key')` not `$_('key')` — `$_` only handles strings, `$json` returns the raw JSON value.
- **Alert text in UI**: Only derive alert titles/descriptions through `$_()` for `source === 'derived'` alerts — external alert text (NWS, GDACS, etc.) is plain text, not i18n keys.
- **Widget changes**: Widgets use standalone Retrofit (no Hilt). Test widget code paths separately from main app code.
- **Weather sources**: `WeatherSource` enum has `OPEN_METEO` and `PIRATE_WEATHER` only — OpenWeatherMap was removed. Do not re-add it.
- **Alert sources**: 8 sources (NWS, GDACS, MeteoAlarm, JMA, ECCC, BOM, NHC, WMO SWIC). Each has an individual toggle in `AppSettings`. `coveredByRegional` suppresses GDACS + WMO SWIC when a country-specific source applies.
- **Web alert sources**: Sources without CORS (MeteoAlarm, BOM, NHC, WMO SWIC, ECCC) are proxied through SvelteKit server routes at `src/routes/api/alerts/`. Sources with CORS (NWS, GDACS, JMA) are called directly from the browser.
- **Alert parser sync rule**: Android (`WeatherRepositoryImpl.kt`) is the canonical alert implementation. When alert parsing logic differs between platforms, Android wins. Keep `web/src/lib/api/alerts/` in sync: severity mapping, field names (title/description/headline/link/time), source tag strings, and deduplication logic must match. WMO SWIC source tag is `"wmoswic"` (no underscore) on both platforms. MeteoAlarm "Moderate" → watch (not warning). ECCC hits the `weather-alerts` OGC API collection (not `alerts` — that ID 404s) and uses `alert_type`/`alert_name_en`/`alert_text_en`/`publication_datetime` fields; `alert_type` is directly `"warning"`/`"watch"`/`"statement"` (map non-`"warning"` to watch).
- **Web adapter**: The web app uses `@sveltejs/adapter-node`. Build produces a Node.js server at `build/index.js`. Run with `node build/index.js` (PORT env var, default 3000). Do NOT switch back to adapter-static.
- **F-Droid metadata**: The local copy is in `metadata/orinasa.njarasoa.maripanatokana.yml`. The app is already merged into `fdroid/fdroiddata` (inclusion MR `!33362` merged 2026-06-18) — do NOT push to the old fork branch, it's dead. F-Droid's bot auto-detects new tags and opens/merges its own MR (~daily, see `docs/FDROID.md`).
- **New releases**: Bump `versionCode` + `versionName` in `app/build.gradle.kts`, add build entry to `metadata/orinasa.njarasoa.maripanatokana.yml`, add changelogs to `fastlane/metadata/android/*/changelogs/{versionCode}.txt`, add a matching entry to the top of `CHANGELOG.md`, tag `v{version}`, push tag.
