## 2023-10-25 - Optimize String splitting with substringBefore
**Learning:** Chained `.split(delimiter)[0]` calls in Kotlin create unnecessary intermediate List allocations. In tight loops or frequent UI updates (like WeatherViewModel state refreshes), this can cause minor GC pressure.
**Action:** Use `.substringBefore(delimiter)` when only the first part of a delimited string is needed. It avoids List allocations entirely and is often more readable.
