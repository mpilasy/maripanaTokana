## 2026-03-06 - [Intl.DateTimeFormat Instantiation Cost]
**Learning:** Instantiating `Intl.DateTimeFormat` is extremely slow in JavaScript (e.g. taking 4+ seconds for 10000 instantiations versus 10ms when cached and reused). In Svelte components like `DailyForecast.svelte`, doing this inside an `#each` block or utility function called per-item causes unnecessary overhead on every render.
**Action:** Always memoize `Intl.DateTimeFormat` instances using `$derived` or storing them outside the render loop if locale is static, so they are only recreated when the locale actually changes.
