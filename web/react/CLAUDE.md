# React App (`/react` path)

**Framework:** React 19 with Vite build tool
**i18n:** `react-i18next` with same 8 locale JSON files
**Screenshots:** `html2canvas` for sharing
**Build optimization:** Vite with `minify: 'terser'`, `cssCodeSplit: false`, single bundle via `manualChunks`
**Base path:** `/react/` configured in vite.config.ts

## Build & Run
```bash
cd react
npm install          # Install dependencies
npm run dev          # Dev server at localhost:5174
npm run build        # Production build to react/dist/
npm run preview      # Preview production build
```

## Architecture
```
react/
├── src/
│   ├── App.tsx          # Root component
│   ├── main.tsx         # Entry point with service worker registration
│   ├── i18n.ts          # react-i18next configuration
│   ├── hooks/           # useWeather, usePreferences hooks
│   ├── components/      # React components (HeroCard, HourlyForecast, etc.)
│   └── App.css          # Styling
├── index.html           # HTML shell
├── vite.config.ts       # Vite config with base: '/react/'
└── package.json         # React dependencies
```

## Shared Code
Imports framework-agnostic code from `shared/` via `$lib` alias (configured in vite.config.ts and tsconfig.json to resolve to `../shared`).

## i18n Notes
- `returnObjects: true` is enabled in i18next config so `t()` can return arrays (needed for `cardinal_directions`, `uv_labels`)

## Performance
- Terser minification, no sourcemaps
- Single bundle via `manualChunks`, `cssCodeSplit: false`
