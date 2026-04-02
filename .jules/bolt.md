## 2026-03-06 - [Intl.DateTimeFormat Instantiation Cost]
**Learning:** Instantiating `Intl.DateTimeFormat` is extremely slow in JavaScript (e.g. taking 4+ seconds for 10000 instantiations versus 10ms when cached and reused). In Svelte components like `DailyForecast.svelte`, doing this inside an `#each` block or utility function called per-item causes unnecessary overhead on every render.
**Action:** Always memoize `Intl.DateTimeFormat` instances using `$derived` or storing them outside the render loop if locale is static, so they are only recreated when the locale actually changes.

## 2026-03-06 - [Redundant Parsing & Object Instantiations on Remote Payloads]
**Learning:** The previous implementation mapped the complete list of 168 hours returned from OpenMeteo into 840+ domain objects and triggered Date formatting per element before applying `filter` and `take(24)`. Parsing Dates and instantiating large numbers of unutilized domain objects is highly detrimental to CPU and memory usage, especially inside background networking logic that's fired routinely.
**Action:** When filtering a large chronologically ordered response to slice a relevant time window, use `indexOfFirst` (or `findIndex`) to locate the relevant index. Bound the selection with `.mapNotNull` or slice operators to safely instantiate objects ONLY for the subset of data required for the UI context.

## 2026-03-19 - Fast Date Parsing in WeatherRepository
**Learning:** Re-instantiating `SimpleDateFormat` inside a `.map` loop introduces significant performance overhead, especially over a large number of items. In `WeatherRepositoryImpl`, allocating the formatter per loop iteration resulted in a parsing time of ~90ms for 1000 dates, while hoisting it out reduced the time to ~8ms. Additionally, when using `io.mockk` to test ViewModels (like `WeatherViewModelTest`), all new repository methods called during init or refresh (like `fetchAlerts`) must be explicitly mocked to prevent test crashes.
**Action:** When parsing lists of dates using `.map`, `.forEach`, or similar sequence operators, always initialize formatting objects (like `SimpleDateFormat`) before the loop. Ensure that test mocks are updated whenever repository interfaces receive new dependencies or functions.

## 2026-03-19 - [Avoid List allocations in string extraction]
**Learning:** Chaining `.split(delimiter)[0]` calls (e.g. `rawName.split(",")[0].split(";")[0]...`) causes the JVM to allocate intermediate `List` instances and scan the entire string multiple times. This introduces unnecessary memory pressure during frequent operations like location resolution in Android's `WeatherRepositoryImpl.kt` and `WeatherViewModel.kt`.
**Action:** Replace chained `.split(delimiter)[0]` logic with chained `.substringBefore(delimiter)` calls. This extracts the exact substring required up to the first delimiter and avoids allocating intermediate array objects, improving memory footprint and speed.
