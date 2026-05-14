## 2025-02-28 - Date Formatting Optimization
**Learning:** Instantiating `Intl.DateTimeFormat` and executing string-based date comparisons via `toLocaleDateString()` inside utility functions can create massive performance bottlenecks (e.g., 20,000ms for 10k iterations).
**Action:** Always cache `Intl.DateTimeFormat` instances in module-level variables (e.g., Maps keyed by locale) or Svelte derived runes. Use fast numeric comparisons via native `Date` getters (getFullYear, getMonth, getDate) for date equivalency checks rather than strings.

## 2025-05-14 - Collection Allocation Optimization
**Learning:** Chaining operators like `+` to combine lists and `.distinctBy {}` for deduplication in Kotlin creates hidden performance penalties by allocating intermediate ArrayLists and LinkedHashMaps.
**Action:** When combining and deduplicating large collections in performance-critical code paths, manually allocate a single `ArrayList` with exact capacity and use a `HashSet` to deduplicate keys in a single pass to avoid unnecessary garbage collection overhead.
