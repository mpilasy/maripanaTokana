## 2025-02-28 - Date Formatting Optimization
**Learning:** Instantiating `Intl.DateTimeFormat` and executing string-based date comparisons via `toLocaleDateString()` inside utility functions can create massive performance bottlenecks (e.g., 20,000ms for 10k iterations).
**Action:** Always cache `Intl.DateTimeFormat` instances in module-level variables (e.g., Maps keyed by locale) or Svelte derived runes. Use fast numeric comparisons via native `Date` getters (getFullYear, getMonth, getDate) for date equivalency checks rather than strings.
