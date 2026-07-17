# Changelog

All notable changes to maripànaTokana are documented here, based on the curated
release notes in `fastlane/metadata/android/en-US/changelogs/`. The Android app
and Web PWA share one version number as of 1.2.1; before that, only Android was
versioned. Not every point release has recorded notes (some were build/CI-only
retriggers with no user-facing change) — those are omitted rather than guessed at.

## [1.2.6] - 2026-07-17
- Tap the AQI tier badge (not the number) to view full air quality details.

## [1.2.5] - 2026-07-17
- Added detailed Air Quality breakdown dialog displaying pollutant concentrations and AQI tiers.
- Introduced colored AQI category tier badges.

## [1.2.4] - 2026-07-17
- Added an Air Quality card (US AQI / EU AQI) to Current Conditions, powered by Open-Meteo.
- Collapsed alert banners with multiple alerts now show "N alerts" instead of just the first alert's title.
- Alert banner's source badge now opens the alert's detail link, even while collapsed.

## [1.2.3] - 2026-07-10
- Low/high temperature order swapped: low now shows on the left, high on the right (hero card, forecast list, widget).

## [1.2.2] - 2026-07-09
- Fixed ECCC (Environment Canada) alerts failing to load for all Canadian locations due to an outdated API endpoint.

## [1.2.1] - 2026-07-02
- Expert mode replaces Dev mode: toggle in Settings, no more hidden 7-tap gesture.
- In Expert mode, location can be changed manually, then reset to GPS with one tap (auto-resets after 12 hours).
- Removed derived (app-calculated) alerts; alerts now come only from official sources.
- MeteoAlarm alerts now filtered to your local subdivision, fixing e.g. Paris showing Corsica alerts.
- Fixed BOM alerts failing to load.
- Web alert parsing synced with Android for consistent results across platforms.

## [1.2.0] - 2026-06-29
- JMA alerts now show prefecture names in descriptions.
- BOM alerts: event type as title, affected area as description (was reversed).
- Fixed NWS/ECCC errors when outside US/Canada.
- Fixed alerts disappearing during background GPS refresh.
- Local time now shown below the update timestamp.
- Material 3 theme: updated colors and typography (Android).
- Web: fixed BOM, ECCC, JMA, and WMO SWIC alert reliability.
- Web: alerts clear immediately when switching location.

## [1.1.1] - 2026-06-26
- Fix JMA weather alerts: warning type now shown in title, area name shown as description.

## [1.1.0] - 2026-06-26
- Pluggable weather sources (Open-Meteo default, Pirate Weather optional).
- Eight weather alert sources (NWS, GDACS, MeteoAlarm, JMA, ECCC, BOM, NHC, WMO SWIC), each individually toggleable in Settings.
- Settings screen now includes geocoding and source configuration.

## [1.0.18] - 2026-05-15
- Technical release to enable F-Droid Reproducible Builds.

## [1.0.11] - 2026-05-06
- Fixed startup crash caused by R8 stripping WorkManager database classes.

## [1.0.6] - 2026-03-16
- Fixed crash in forecast lists caused by duplicate keys.
- Gradle upgrade.

## [1.0.4] - 2026-03-04
- Moved language and font selectors to the top for better accessibility.
- Fixed Docker build issues.

## [1.0.3] - 2026-03-02
- Fixed bug where today's forecast was always showing as fog.

## [1.0.2] - 2026-02-26
- Faster weather data loading with parallel fetching.
- Cached reverse geocoding for quicker startup.
- Optimized widget performance.
- Added accessibility labels to interactive elements.
- Code obfuscation enabled for release builds.

## [1.0] - 2026-02-13
- Initial release of maripànaTokana weather app.
- Current weather conditions with dual units.
- Hourly and 10-day forecasts.
- 8 language support.
- 22 font pairings.
- Two home screen widgets.
- Open-source with MIT License.
