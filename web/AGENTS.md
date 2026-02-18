# Agent Instructions for maripànaTokana PWA

This document provides guidance for AI agents working specifically on the **maripànaTokana** Web PWA.

## Project Overview
A SvelteKit-based weather dashboard featuring simultaneous Metric and Imperial units display. Ported from the Android app, it shares the same domain logic patterns and i18n resources.

- **Tech Stack**: SvelteKit v2 (Svelte 5 runes), TypeScript, svelte-i18n, html2canvas, Caddy (Docker).
- **Core Goal**: Provide a fast, installable, and localized weather experience on the web.

## Core Principles
- **Svelte 5 Runes**: Strictly use `$state`, `$derived`, `$effect`, and `$props`. Avoid legacy Svelte 4 syntax.
- **Unit Safety**: Use the immutable domain classes (e.g., `Temperature.ts`) for all weather measurements to ensure consistent unit conversion and display.
- **Locale Neutrality**:
  - Perform all internal number/time formatting in ASCII.
  - Apply native digits (ar, hi, ne) via `localizeDigits()` at display time.
  - Replaces decimal separators per locale (`,` for mg/es/fr, `٫` for ar).
- **Concise Implementation**: Prefer concise, technically accurate Svelte components and logic.

## Directory Structure
- `src/lib/api/`: Open-Meteo client, types, and WMO code mappings.
- `src/lib/domain/`: Immutable value classes for unit-safe measurements.
- `src/lib/i18n/`: svelte-i18n setup. Locales are symlinked from `../shared/i18n/locales/`.
- `src/lib/stores/`: Svelte stores for weather state, location, and user preferences.
- `src/lib/components/`: Svelte UI components (HeroCard, Forecasts, etc.).
- `scripts/`: Post-build scripts, notably `inline-assets.js` for CSS inlining.

## Build and Development
- `npm install`: Install dependencies.
- `npm run dev`: Start dev server at `localhost:5173`.
- `npm run build`: Production build. Generates a static site and inlines CSS into `index.html`.
- `npm run check`: Run `svelte-check` for type-checking.
- `docker compose up -d --build`: Build and serve via Caddy.

## Key Patterns
- **Two-Step Location**: Uses cached `localStorage` coordinates first, then refreshes via `navigator.geolocation` if the user moved >5 km.
- **Single-Chunk Bundling**: Vite is configured to bundle all JS into a single chunk for performance on mobile.
- **CSS Inlining**: CSS is inlined into `index.html` post-build to eliminate a round-trip HTTP request.
- **Screenshot Sharing**: Uses `html2canvas` to capture DOM sections and share them via the Web Share API (with download fallback).

## Documentation Reference
- `docs/DESIGN.md`: Comprehensive design document explaining the architecture and key decisions.
- `docs/TESTING.md`: Detailed manual testing checklist.

## Verification Checklist
1. **Svelte 5 Compliance**: Ensure all new state and logic use Svelte 5 runes.
2. **i18n Consistency**: If modifying translations, remember they are shared with the Android app in the `../shared/` directory.
3. **Unit Toggle**: Verify that toggling `metricPrimary` updates all dual-unit displays across the app.
4. **Digit Localization**: Ensure `localizeDigits()` is applied to all user-facing numbers.
5. **Static Build**: After changes, verify that `npm run build` still produces a functional static site.
