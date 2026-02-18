# Agent Instructions for maripànaTokana

This document provides guidance for AI agents working on the **maripànaTokana** project, which consists of an Android weather app and a corresponding web version (PWA).

## Project Overview
**maripànaTokana** (Malagasy for "a single thermometer") features simultaneous Metric and Imperial units display, supporting 8 languages and multiple font pairings.

- **Android App**: Kotlin 2.0.21, Jetpack Compose (BOM 2024.09.00), Hilt 2.59, Retrofit 2.11.0, Glance 1.1.1 (Widgets), WorkManager 2.10.0.
- **Web App (PWA)**: TypeScript, SvelteKit v2 (Svelte 5 runes), svelte-i18n, html2canvas.

## Core Principles
- **Concise Code**: Prefer technically accurate, concise code. Avoid redundant explanations.
- **Shared Logic**: Both platforms share the same domain logic patterns and i18n resources (located in `shared/i18n/locales/`).
- **Unit Handling**: Use platform-appropriate value wrappers for unit safety:
  - Android: `@JvmInline value class` (e.g., `Temperature`, `WindSpeed`).
  - Web: Immutable classes with static factory methods.
- **Locale Neutrality**:
  - Always format numbers/times using ASCII-safe locales (`Locale.US` in Android, internal ASCII in Web).
  - Apply native digits (Arabic, Hindi, Nepali) via `localizeDigits()` at display time.
  - This ensures 100% consistency across different OS versions and locales.

## Directory Structure
- `app/`: Android application source code.
  - `src/main/`: Common Android logic and UI.
  - `src/fdroid/`: F-Droid flavor specific (Native `LocationManager`).
  - `src/standard/`: Standard flavor specific (Google Play Services `FusedLocationProvider`).
- `web/`: Web application source code (SvelteKit).
- `shared/`: Shared resources, primarily i18n JSON files.
- `docs/`: Comprehensive design, testing, and F-Droid documentation for Android.
- `web/docs/`: Design and testing documentation for the Web app.

## Android Development
### Build Commands
- `./gradlew assembleFdroidDebug`: Build the F-Droid flavor (debug).
- `./gradlew assembleStandardDebug`: Build the Standard flavor (debug).
- `./gradlew test`: Run unit tests.
- `./gradlew lint`: Run static analysis.

### Key Patterns
- **Build Flavors**: Used to separate Google Play Services dependencies from the F-Droid version.
- **Location Abstraction**: `LocationProvider` interface with flavor-specific implementations.
- **Runtime Locale Switching**: Uses `ContextWrapper` and `CompositionLocalProvider` to avoid Activity recreation flashes.
- **Font System**: 16 pairings provided via `LocalDisplayFont` and `LocalBodyFont` `CompositionLocals`.
- **F-Droid Compliance**: The `fdroid` flavor MUST NOT have Google Play Services dependencies.

## Web Development
### Build Commands
- `npm install` (in `web/`)
- `npm run dev`: Start dev server at `localhost:5173`.
- `npm run build`: Production build (inlines CSS into `index.html`).
- `npm run check`: Run `svelte-check` for type-checking.
- `docker compose up -d --build`: Deploy via Docker.

### Key Patterns
- **Svelte 5 Runes**: Use `$state`, `$derived`, `$effect`, and `$props`.
- **Single-file Build**: CSS is inlined into `index.html` post-build to reduce HTTP requests.
- **PWA**: Includes service worker for offline caching and a manifest for installability.
- **Screenshot Sharing**: Uses `html2canvas` for branded DOM-to-PNG capture.

## Documentation Reference
- `docs/DESIGN.md`: Detailed Android architecture and design decisions.
- `web/docs/DESIGN.md`: Detailed Web architecture.
- `docs/TESTING_GUIDE.md`: Android F-Droid testing procedures.
- `docs/FDROID.md`: Android F-Droid deployment details.

## Verification Checklist
1. **Consistency**: If changing shared logic or i18n, ensure it's reflected correctly on both Android and Web.
2. **Dual Units**: Verify that both Metric and Imperial units display correctly and that the toggle works across all UI components.
3. **i18n Source of Truth**: Use `shared/i18n/locales/` as the canonical source for translations. Run `node shared/i18n/generate-android-strings.js` after updates to sync Android.
4. **F-Droid Integrity**: Ensure no Google Play Services leak into the `fdroid` source sets or dependencies.
5. **Build Verification**: Verify successful builds for both Android flavors (`fdroid`, `standard`) and the web app.
