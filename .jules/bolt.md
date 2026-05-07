## 2025-02-28 - Date Formatting Optimization
**Learning:** Instantiating `Intl.DateTimeFormat` and executing string-based date comparisons via `toLocaleDateString()` inside utility functions can create massive performance bottlenecks (e.g., 20,000ms for 10k iterations).
**Action:** Always cache `Intl.DateTimeFormat` instances in module-level variables (e.g., Maps keyed by locale) or Svelte derived runes. Use fast numeric comparisons via native `Date` getters (getFullYear, getMonth, getDate) for date equivalency checks rather than strings.

## 2024-05-08 - Optimized Date Parsing in API Mappers
**Learning:** Instantiating and executing `new Date(string)` or `SimpleDateFormat.parse` inside mapping loops (e.g., hourly/daily data spanning hundreds of items) incurs extremely high performance overhead compared to direct substring parsing.
**Action:** When working with large datasets where string date formats are strict and predictable (e.g., ISO formats from APIs like OpenMeteo), implement fast string extraction functions using `substring`, `parseInt`/`toInt`, and `Date.UTC` / `java.util.Calendar(UTC)` to bypass parser evaluation delays entirely.
