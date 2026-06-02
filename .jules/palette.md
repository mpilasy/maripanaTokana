## 2024-05-18 - [Add aria-labels to buttons]
**Learning:** In MaripanaTokana web project, localized strings for aria labels (like `android_only.cd_share`, `android_only.cd_cycle_mode`, `android_only.cd_refresh`) are available in `shared/i18n/locales/` despite the `android_only` namespace, as proven by existing uses in `Controls.svelte`.
**Action:** Always prefer localized string references via `$_()` over hardcoded strings for ARIA attributes if they exist in shared i18n, even if the namespace name suggests they might be platform-specific.

## 2026-06-02 - [Keyboard Focus Visibility]
**Learning:** Many interactive elements (like custom mode toggles and dialog list items) lacked `:focus-visible` styles, rendering them invisible to keyboard navigation. The project standardizes on `outline: 2px solid rgba(255, 255, 255, 0.5); outline-offset: 2px;` for dark mode focus states.
**Action:** When adding or converting `<button>` elements, always explicitly define `:focus-visible` to ensure keyboard accessibility.
