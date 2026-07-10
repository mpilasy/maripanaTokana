# Gemini CLI Instructions

Read [AGENTS.md](./AGENTS.md) for the full project context — architecture, tech stack, build commands, critical rules, and mandates.

For F-Droid build issues, read [docs/FDROID.md](./docs/FDROID.md) before touching any Gradle config.

## Key Rules

- Do NOT add `Co-Authored-By` or any AI attribution to commit messages.
- Do NOT add `id("org.jetbrains.kotlin.android")` to `app/build.gradle.kts` — causes extension conflict.
- Do NOT add foojay-resolver to `settings.gradle.kts` — F-Droid blocks it.
- `kotlin { jvmToolchain(21) }` must stay in `app/build.gradle.kts`.
- `auto-provisioning=disabled` must stay in `gradle.properties`.
- Feature parity: keep Android and Web in sync.

## Model Routing

Use the right model for the job to save cost and context:

- **Flash** (e.g., `gemini-1.5-flash`): Documentation, i18n/translation updates, string resource changes, README edits, simple renames, formatting fixes, lint cleanup.
- **Pro** (e.g., `gemini-1.5-pro`, `gemini-2.0-flash`): Standard feature implementation, bug fixes, refactoring, test writing, build config changes, most day-to-day coding tasks.
- **Ultra/High-Reasoning** (e.g., `gemini-2.0-pro`): Architecture decisions, cross-platform porting (Android ↔ Web), debugging complex build failures (F-Droid CI, Gradle toolchain issues), multi-file refactors touching both platforms, and anything requiring deep reasoning about side effects.

## Agent Usage

- Use `codebase_investigator` for deep architectural analysis or system-wide searches rather than multiple sequential `grep_search` calls.
- Use the `generalist` sub-agent for batch refactoring or high-volume output tasks (like fixing lint errors across multiple files).
- Run independent tasks in parallel (e.g., researching Android and Web implementation) to save turns.
- Use `is_background: true` with `run_shell_command` for long-running builds (`./gradlew assembleFdroidRelease`, `cd web && npm run build`) so you can continue analysis.

## Tool Preferences

- Use `replace` or `write_file` for targeted file modifications.
- Use `grep_search` for content discovery, utilizing its parallel search capabilities.
- Use `glob` for identifying file structures and patterns.
- For Gradle/build changes, always dry-run first: `run_shell_command(command: "./gradlew :app:assembleFdroidRelease --dry-run")`.
- For web changes, always type-check: `run_shell_command(command: "cd web && npm run check")`.

## Project-Specific Patterns

- **Android strings**: Never edit `res/values-*/strings.xml` directly. Edit `shared/i18n/locales/*.json` then run `node shared/i18n/generate-android-strings.js`.
- **Domain model changes**: If you change a value class (Temperature, Pressure, etc.) in Kotlin, you must also update the TypeScript equivalent in `web/src/lib/domain/` and vice versa.
- **New i18n keys**: Add to all 8 JSON files in `shared/i18n/locales/`. Missing keys will show raw key strings at runtime.
- **Widget changes**: Widgets use standalone Retrofit (no Hilt). Test widget code paths separately from main app code.
- **F-Droid metadata**: The local copy is in `metadata/orinasa.njarasoa.maripanatokana.yml`. The app is already merged into `fdroid/fdroiddata` (inclusion MR `!33362` merged 2026-06-18) — do NOT push to the old fork branch, it's dead. F-Droid's bot auto-detects new tags and opens/merges its own MR (~daily, see `docs/FDROID.md`).
