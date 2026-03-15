# Gemini Configuration

## Mandates

- **Feature Parity**: The Android app and the Web app (PWA) must be kept in sync as much as feasible. Any new feature or UI enhancement implemented in one platform should be ported to the other to maintain a consistent experience.
- **Shared Resources**: I18n strings are shared via symlinks in `web/src/lib/i18n/locales`. Always ensure that new keys added to one platform are available for the other.
- **Domain Logic**: Core domain logic (like unit conversions and weather code mappings) should remain consistent across Kotlin and TypeScript implementations.
