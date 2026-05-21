## 2025-02-28 - Date Formatting Optimization
**Learning:** Instantiating `Intl.DateTimeFormat` and executing string-based date comparisons via `toLocaleDateString()` inside utility functions can create massive performance bottlenecks (e.g., 20,000ms for 10k iterations).
**Action:** Always cache `Intl.DateTimeFormat` instances in module-level variables (e.g., Maps keyed by locale) or Svelte derived runes. Use fast numeric comparisons via native `Date` getters (getFullYear, getMonth, getDate) for date equivalency checks rather than strings.

## 2023-10-27 - Add spatial bounding box to GDACS distance filter
**What:** Implemented a latitude/longitude degree bounding box check before iterating over the Haversine trigonometric distance formula for GDACS alert coordinates.
**Why:** The GDACS dataset contains potentially tens of thousands of global weather features. The Haversine distance formula requires computationally expensive math (sine, cosine, atan2). Adding a fast O(1) mathematical boundaries check skips this heavy math for 99% of out-of-range coordinates.
**Impact:** Android calculation time for 10M iterations decreased from ~1679ms to 191ms. TypeScript execution decreased from 1585ms to 1017ms. CPU cycles and trigonometric operations were greatly reduced.
**Measurement:** Used temporary benchmarking scripts in both Kotlin (`WeatherRepositoryImplBenchmarkTest.kt`) and TypeScript (`web/bench_gdacs.ts`).

## 2025-05-14 - Collection Allocation Optimization
**Learning:** Chaining operators like `+` to combine lists and `.distinctBy {}` for deduplication in Kotlin creates hidden performance penalties by allocating intermediate ArrayLists and LinkedHashMaps.
**Action:** When combining and deduplicating large collections in performance-critical code paths, manually allocate a single `ArrayList` with exact capacity and use a `HashSet` to deduplicate keys in a single pass to avoid unnecessary garbage collection overhead.
