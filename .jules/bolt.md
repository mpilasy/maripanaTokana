## 2026-03-06 - [Intl.DateTimeFormat Instantiation Cost]
**Learning:** Instantiating `Intl.DateTimeFormat` is extremely slow in JavaScript (e.g. taking 4+ seconds for 10000 instantiations versus 10ms when cached and reused). In Svelte components like `DailyForecast.svelte`, doing this inside an `#each` block or utility function called per-item causes unnecessary overhead on every render.
**Action:** Always memoize `Intl.DateTimeFormat` instances using `$derived` or storing them outside the render loop if locale is static, so they are only recreated when the locale actually changes.

## 2026-03-06 - [Redundant Parsing & Object Instantiations on Remote Payloads]
**Learning:** The previous implementation mapped the complete list of 168 hours returned from OpenMeteo into 840+ domain objects and triggered Date formatting per element before applying `filter` and `take(24)`. Parsing Dates and instantiating large numbers of unutilized domain objects is highly detrimental to CPU and memory usage, especially inside background networking logic that's fired routinely.
**Action:** When filtering a large chronologically ordered response to slice a relevant time window, use `indexOfFirst` (or `findIndex`) to locate the relevant index. Bound the selection with `.mapNotNull` or slice operators to safely instantiate objects ONLY for the subset of data required for the UI context.
## 2026-03-19 - Optimize distinctBy code
**Learning:** Avoid using the Kotlin + list operator chained with .distinctBy {}. It allocates unnecessary temporary lists for intermediate sums and final results.
**Action:** Instead, manually allocate a combined ArrayList and use a HashSet to check for seen unique keys inside a loop to achieve zero intermediate allocations and significantly better performance.
