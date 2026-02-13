# Angular App (`/ng` path)

**Framework:** Angular 19 with CLI build
**i18n:** Custom translation service with same 8 locale JSON files
**Screenshots:** `html2canvas` for sharing
**Build optimization:** Angular 19 esbuild application builder (AOT by default)
**Base href:** `/ng/` configured in angular.json

## Build & Run
```bash
cd angular
npm install          # Install dependencies
npm run start        # Dev server (ng serve)
npm run build        # Production build to angular/dist/browser/
npm run watch        # Build in watch mode
```

## Architecture
```
angular/
├── src/
│   ├── app/
│   │   ├── app.component.ts     # Root component
│   │   ├── components/          # Angular components
│   │   ├── services/            # Weather, i18n, preferences services
│   │   ├── pipes/               # Custom pipes
│   │   └── app.css
│   ├── main.ts                  # Entry point with service worker registration
│   ├── index.html               # HTML shell
│   └── styles.css               # Global styling
├── angular.json                 # Angular config with baseHref: '/ng/'
└── package.json                 # Angular dependencies
```

## Shared Code
Imports framework-agnostic code from `shared/` via `$lib` alias (configured in tsconfig.json to resolve to `../shared`).

## Angular 19 Notes
- Uses `application` builder (esbuild-based), NOT the legacy `browser` builder (webpack)
- `vendorChunk`, `buildOptimizer`, `aot` options are NOT supported — they are implicit
- Output goes to `dist/browser/` (not `dist/`)
- Requires explicit `"polyfills": ["zone.js"]` in angular.json
- Custom `I18nService` provides `t(key)` for strings and `tArray(key)` for array translations (e.g., `cardinal_directions`, `uv_labels`)

## Performance
- esbuild compilation (AOT by default)
- No sourcemaps
