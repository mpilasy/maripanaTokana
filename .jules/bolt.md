## 2025-02-28 - Date Formatting Optimization
**Learning:** Instantiating `Intl.DateTimeFormat` and executing string-based date comparisons via `toLocaleDateString()` inside utility functions can create massive performance bottlenecks (e.g., 20,000ms for 10k iterations).
**Action:** Always cache `Intl.DateTimeFormat` instances in module-level variables (e.g., Maps keyed by locale) or Svelte derived runes. Use fast numeric comparisons via native `Date` getters (getFullYear, getMonth, getDate) for date equivalency checks rather than strings.

## 2024-05-18 - Caching `SimpleDateFormat` Across Coroutine Suspension Points
**What:** Cached `SimpleDateFormat` instances using `ThreadLocal` in a `companion object` to avoid redundant object instantiations in `WeatherRepositoryImpl.kt`.
**Why:** Repeatedly instantiating `SimpleDateFormat` for each async repository call causes high CPU and memory overhead.
**Impact:** Significantly faster parsing times (over 2x faster).
**Measurement:** A custom benchmark running 100,000 iterations showed instantiation times dropping from ~568ms to ~206ms.
**Learning:** When using `ThreadLocal` in coroutines, strictly fetch the value using `.get()` *after* any suspension point. If `ThreadLocal.get()` is held across a suspension point and the coroutine resumes on another thread, the value will refer to the wrong thread's context, leading to subtle concurrency bugs.
