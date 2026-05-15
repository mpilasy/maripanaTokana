## 2025-02-28 - Date Formatting Optimization
**Learning:** Instantiating `Intl.DateTimeFormat` and executing string-based date comparisons via `toLocaleDateString()` inside utility functions can create massive performance bottlenecks (e.g., 20,000ms for 10k iterations).
**Action:** Always cache `Intl.DateTimeFormat` instances in module-level variables (e.g., Maps keyed by locale) or Svelte derived runes. Use fast numeric comparisons via native `Date` getters (getFullYear, getMonth, getDate) for date equivalency checks rather than strings.
## 2024-05-18 - OpenMeteo Date Parsing Optimization
**What:** Replaced repeated `SimpleDateFormat` instantiations with a custom `parseIsoDate` function utilizing `String.substring` and a `ThreadLocal<Calendar>` to bypass expensive formatting loops.
**Why:** Deserializing large hourly data sets using multiple dynamic `SimpleDateFormat` instances created massive performance bottlenecks and redundant memory allocations.
**Impact:** Significantly reduced CPU cycles on background refresh syncs.
**Measurement:** Benchmarked the mapping logic on arrays of size 168. Baseline runtime was 382ms. Replacing formatters with `ThreadLocal` dropped it to 227ms. Moving to purely manual string slicing with a single `Calendar` instantiation brought it down to 101ms (~73% reduction in CPU time).
