# Bolt's Journal

## 2026-02-27 - Intl.DateTimeFormat caching pattern in SvelteKit
**Learning:** This codebase creates `Intl.DateTimeFormat` instances inside format helper functions that are called in `{#each}` loops. With 10 daily forecast items, that's 20 constructor calls per render. Svelte 5's `$derived` rune is the perfect tool to cache these — it automatically re-creates when the tracked dependency (localeTag) changes.
**Action:** When profiling Svelte components, always check for `new Intl.DateTimeFormat()` or `new Intl.NumberFormat()` inside functions called from `{#each}` blocks. Cache them with `$derived` tied to the locale prop.

## 2026-02-27 - Environment: no GitHub API access for PR creation
**Learning:** The git proxy at `127.0.0.1:33272` only supports git operations (push/fetch/clone). GitHub API calls for PR creation require separate authentication that isn't available in this environment.
**Action:** Push the branch and note the PR must be created manually or via a different flow.
