# Svelte App (Primary Implementation)

**Framework:** SvelteKit with Svelte 5 runes (`$state`, `$derived`, `$effect`, `$props`), TypeScript
**Adapter:** `@sveltejs/adapter-static` (fully static, no server-side logic)
**i18n:** `svelte-i18n` — reactive `$_()` store, 8 JSON locale files in `shared/i18n/locales/`
**Styling:** Plain CSS with custom properties (`--font-display`, `--font-body`, `--font-features`), no CSS framework
**Build optimization:** `manualChunks` consolidates JS into one bundle; `scripts/inline-assets.js` inlines CSS into HTML
**Served at:** `/svelte` (base path configured in `svelte.config.js`)

## Build & Run
```bash
cd svelte
npm install          # Install dependencies
npm run dev          # Dev server at localhost:5173
npm run build        # Production build to build/ (vite build + CSS inlining)
npm run preview      # Preview production build
npm run check        # Type-check with svelte-check
```

## Architecture
```
svelte/
├── src/
│   ├── lib/
│   │   ├── i18n/index.ts    # svelte-i18n setup (re-exports from $shared/i18n/locales)
│   │   ├── stores/          # Svelte writable stores (preferences, weather state)
│   │   └── components/      # All Svelte components (9 files)
│   ├── routes/
│   │   ├── +page.svelte     # Single page — mounts WeatherScreen
│   │   └── +layout.svelte   # Root layout (font loading, RTL, i18n init)
│   ├── service-worker.ts    # PWA caching (NetworkFirst for app + API, CacheFirst for fonts)
│   └── app.html             # Shell HTML with manifest link + page title
├── scripts/
│   └── inline-assets.js     # Post-build: inlines CSS into index.html
├── static/
│   ├── manifest.json        # PWA manifest
│   └── ...                  # Icons, background image
├── svelte.config.js         # SvelteKit config (static adapter, base: '/svelte', $shared alias)
├── vite.config.ts           # Vite config (single-chunk bundling)
├── tsconfig.json
└── package.json             # Svelte dependencies
```

Shared code (api, domain, i18n locales, fonts, share, location) lives in `shared/` at the repo root and is imported via the `$shared` alias configured in `svelte.config.js`.

## Component Details
- **WeatherScreen.svelte:** State machine switch (`loading | success | error`), pull-to-refresh, `loc()` for digit localization, dual-language error screen with browser locale detection, auto-fetch on mount.
- **HeroCard.svelte:** Emoji+description (left), temperature with `DualUnitText` (right), feels-like (bottom-left), precipitation (bottom-right), share button (top-right), copyright watermark.
- **HourlyForecast.svelte:** Horizontal flex scroll with `scroll-snap-type`. `isNightForHour()` uses `dailySunrise`/`dailySunset` arrays for day/night emoji selection.
- **DailyForecast.svelte:** Vertical list with `Intl.DateTimeFormat` for localized day names. Precip probability shown only when >0%.
- **CurrentConditions.svelte:** CSS Grid (`1fr 1fr` columns, `1fr` auto-rows) of DetailCard pairs. Special humidity+dewpoint combined card. Cardinal direction from `$_('cardinal_directions')` array. UV label from `$_('uv_labels')` array.
- **CollapsibleSection.svelte:** Uses Svelte 5 `Snippet` for children. Share button next to title (visible when expanded). `let isExpanded = $state(expanded)` captures initial prop (intentional `state_referenced_locally` warning).

## Svelte 5 Specifics
- **`{@const}` placement:** Must be immediate child of block constructs (`{#each}`, `{#if}`, etc.), NOT nested inside elements. Use `$derived()` in script block as alternative.
- **`bind:this`:** Variable must be declared with `$state()` (e.g., `let el = $state<HTMLElement | null>(null)`).
- **Props:** Use `interface Props` + `$props()` (not `export let`).
- **Children:** Use `Snippet` type + `{@render children()}` (not `<slot />`).
- **Effects:** `$effect()` replaces `afterUpdate`/reactive statements.

## State Management
- `StateFlow` + `collectAsState()` → Svelte writable stores + `$store`
- `$_('key')` via svelte-i18n for all i18n lookups
- CSS custom properties for fonts set via `$effect` in `+layout.svelte`
- Collapsible sections use `transition:slide`

## Performance
- CSS inlined into HTML via post-build script
- Single JS bundle via `manualChunks`

## Known Issues
- **CollapsibleSection warning:** `state_referenced_locally` — intentional, captures initial prop value.
