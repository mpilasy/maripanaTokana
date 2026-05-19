## 2024-05-18 - [Add aria-labels to buttons]
**Learning:** In MaripanaTokana web project, localized strings for aria labels (like `android_only.cd_share`, `android_only.cd_cycle_mode`, `android_only.cd_refresh`) are available in `shared/i18n/locales/` despite the `android_only` namespace, as proven by existing uses in `Controls.svelte`.
**Action:** Always prefer localized string references via `$_()` over hardcoded strings for ARIA attributes if they exist in shared i18n, even if the namespace name suggests they might be platform-specific.
