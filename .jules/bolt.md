## 2025-02-28 - Date Formatting Optimization
**Learning:** Instantiating `Intl.DateTimeFormat` and executing string-based date comparisons via `toLocaleDateString()` inside utility functions can create massive performance bottlenecks (e.g., 20,000ms for 10k iterations).
**Action:** Always cache `Intl.DateTimeFormat` instances in module-level variables (e.g., Maps keyed by locale) or Svelte derived runes. Use fast numeric comparisons via native `Date` getters (getFullYear, getMonth, getDate) for date equivalency checks rather than strings.

## 2023-10-27 - Add spatial bounding box to GDACS distance filter
**What:** Implemented a latitude/longitude degree bounding box check before iterating over the Haversine trigonometric distance formula for GDACS alert coordinates.
**Why:** The GDACS dataset contains potentially tens of thousands of global weather features. The Haversine distance formula requires computationally expensive math (sine, cosine, atan2). Adding a fast O(1) mathematical boundaries check skips this heavy math for 99% of out-of-range coordinates.
**Impact:** Android calculation time for 10M iterations decreased from ~1679ms to 191ms. TypeScript execution decreased from 1585ms to 1017ms. CPU cycles and trigonometric operations were greatly reduced.
**Measurement:** Used temporary benchmarking scripts in both Kotlin (`WeatherRepositoryImplBenchmarkTest.kt`) and TypeScript (`web/bench_gdacs.ts`).

## 2025-05-21 - Collection Optimization
**Learning:** Combining lists with `+` followed by `.distinctBy {}` (e.g., `(list1 + list2 + list3).distinctBy { it.key }`) creates unnecessary intermediate list collections and adds overhead, especially for long lists or when doing this repeatedly.
**Action:** Instead, pre-allocate an `ArrayList` and use a `HashSet` to deduplicate items by key in a single pass while iterating. This approach avoids intermediate collections and is significantly faster.

## 2026-06-04 - Fast Date Parsing
**Learning:** Using `new Date(isoString)` or `SimpleDateFormat.parse` on large arrays (e.g. hourly weather data) causes significant performance overhead.
**Action:** Implement fast string extraction using `substring`, `parseInt`/`toInt`, and `Date.UTC` / `Calendar(UTC)` to bypass expensive parser evaluation when the ISO format is strict and predictable.

## 2023-11-20 - Collection Mapping Optimization
**Learning:** Chaining array methods like `slice().map().filter()` in JavaScript, or `map {}.distinctBy {}` in Kotlin, creates multiple intermediate allocations. When processing large arrays like hourly forecasts, this incurs significant memory and CPU overhead (e.g., executing in ~65ms per 10k loops).
**Action:** Replace chained collection transformations with single-pass `for` loops. Pre-allocate collection sizes (e.g., `ArrayList(size)`) and use `Set`/`HashSet` for manual O(1) deduplication during the loop to bypass intermediate array allocations, reducing execution time by ~50%.
