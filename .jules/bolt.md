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

## 2025-05-22 - String Extraction Optimization
**Learning:** Chaining `.split(delimiter)[0]` to extract string segments allocates unnecessary intermediate `List` objects for the array of splits that get discarded.
**Action:** Replace `.split(delimiter)[0]` with `.substringBefore(delimiter)` to extract string segments, avoiding intermediate collection overhead and boosting performance.
