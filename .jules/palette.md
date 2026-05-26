## 2024-05-18 - [Add aria-labels to buttons]
**Learning:** In MaripanaTokana web project, localized strings for aria labels (like `android_only.cd_share`, `android_only.cd_cycle_mode`, `android_only.cd_refresh`) are available in `shared/i18n/locales/` despite the `android_only` namespace, as proven by existing uses in `Controls.svelte`.
**Action:** Always prefer localized string references via `$_()` over hardcoded strings for ARIA attributes if they exist in shared i18n, even if the namespace name suggests they might be platform-specific.

## 2026-05-26 - [Module-Level IDs for Svelte 5 Accessibility]
**Learning:** When creating disclosure widgets (e.g., Collapsible Sections, Alert Banners) in Svelte 5 that require dynamic IDs for `aria-controls`, generating the ID in the instance script (`let contentId = 'content-' + Math.random()`) causes SSR hydration mismatch warnings. A module-level counter (`<script module lang="ts"> let idCounter = 0; </script>`) combined with instance incrementing (`let contentId = 'content-' + idCounter++;`) safely ensures stable, unique IDs across both server and client rendering.
**Action:** Always use `<script module>` with an incrementing counter when generating IDs for ARIA mappings to maintain accessible connections without introducing Svelte compiler/hydration errors.
