# Changelog

All notable changes to maripànaTokana are documented here, based on the curated
release notes in `fastlane/metadata/android/en-US/changelogs/`. The Android app
and Web PWA share one version number as of 1.2.1; before that, only Android was
versioned. Not every point release has recorded notes (some were build/CI-only
retriggers with no user-facing change) — those are omitted rather than guessed at.

## [1.5.1] - 2026-10-19
- New look: Meteocons weather icons with day and night versions replace the emoji; neutral glass cards, purple only as an accent; forecast columns no longer boxed and tile sections without an outer card (Android and web).
- UV and air quality in the hourly and 10-day cards shown as a colour bar under the value, explained by the legend; other level badges are soft tints.
- Charts lighter: the visible part shown as a light band, fewer grid lines, labels kept inside the chart.
- The air-quality dial shows all five levels as coloured segments, with a dot at the reading's place within its level.
- The nowcast hides its sentence while it is already raining; activities use flat icons; the footer is quieter.
- Fixed: sunrise, sunset and the nowcast's times showed in the device's time zone instead of the place's.
- Web: a mouse can drag the sideways-scrolling forecast rows and tabs.

## [1.5.0] - 2026-10-19
- Redesigned main screen: a glance card on top (condition, temperature in both units, low/high, a one-line rain outlook for the next 24 hours, the next three hours), then always-open cards in one scrolling column (Android and web).
- Hourly and 10-day cards share the same tabs (Conditions, Wind, Precipitation, Pressure, UV, Air quality), each a row of cards with an overview chart below: lines for readings at a moment, bars for daily peaks and precipitation amounts.
- New data: hourly UV, daily mean pressure, daily air quality (air-quality forecast now 5 days), and a night weather icon per day.
- Visual tiles: wind compass, pressure and air-quality gauges, humidity fill, UV level dots, sun arc; current conditions split into Current conditions and Sun & Air.
- Wind direction in forecast cards shown as a small compass with its label.
- Both units now shown everywhere, including feels-like, chart labels, the nowcast axis and the large widget's daily low/high.
- Updated svelte-check to 4.7.7 (web).

## [1.4.9] - 2026-10-16
- Holding a favorite's name renames it; the name replaces the place name in the header and the location menu, and Reset (or saving it empty) brings the place name back (Android and web).
- Fixed (web): with the browser's address bar at the bottom, the page could shift and crop the location name at the top or the footer at the bottom.
- Fixed (web): switching language while a favorite was shown replaced its name with the GPS location's name.

## [1.4.8] - 2026-10-14
- Holding either location-pager arrow lists the current location and all favorites to jump straight to one, including from the end of the list (Android and web).
- Holding the Plus Code grid icon, or the code in its tooltip, copies the code to the clipboard (Android and web).
- Plus Codes typed without the "+" are recognised (`5hh93g` -> `5HH9+3G`) when the word mixes letters and digits.

## [1.4.7] - 2026-10-14
- Plus Codes (Open Location Code): search with a full code, or a short one with or without a place (`PR6C+43 Nairobi`); also from shared text. A grid icon beside the coordinates shows the location's 10-digit code (Android and web).
- Place names in a non-Latin script get a discreet icon to switch name and region to Latin letters (القاهرة ↔ Cairo); it resets per location.
- Fixed: a language switched mid-session now applies to Settings, About and dialogs (Android).
- Fixed: coordinates no longer scramble in Arabic (Android and web).
- Fixed: favoriting the previewed location no longer reloads the screen (Android and web).
- Web: the coordinates shown are the requested point, not Open-Meteo's forecast grid point.

## [1.4.6] - 2026-10-14
- Location search lists a town before the region of the same name (e.g. Kidal town before Kidal region); regions and countries show a map icon instead of the pin (Android and web).

## [1.4.5] - 2026-10-14
- Refined Malagasy translations for location search.

## [1.4.4] - 2026-10-13
- Fixed (Android): the coordinates under the city name showed your current GPS position for every location; they now belong to the location shown. Present since 1.2.15.
- Header: the icons share the row with the city name and only the name shrinks; region, local time, coordinates and the updated line use the full width (Android and web).

## [1.4.3] - 2026-10-13
- Maintenance release; no changes to features. Supersedes 1.4.2, which was withdrawn.

## [1.4.1] - 2026-10-13
- The main screen keeps its scroll position when you come back from Settings or About.
- The font licences page loads its text in the background, so it opens without stutter.
- Web: Settings, About and dialogs stay inside the app frame on desktop; the air-quality dialog dims the whole screen.
- Web: the "Updated" line shows a short date ("Mon 12 Oct, 14:30") when data is not from today, matching Android.

## [1.4.0] - 2026-10-13
- New look: redesigned main screen with a cleaner header, a new current-weather card, glass sections and an even tile grid (Android and web).
- New About page listing every data source, font and library with its licence, including the bundled font licences (Settings → About, or tap the footer credits).
- Fixed: temperature wrapping onto two lines in Spanish and French.
- Updated F-Droid screenshots for all 8 languages.
- Updated Kotlin to 2.2.21; web: Svelte 5.57.2, Vite 7.3.7.

## [1.3.1] - 2026-10-05
- Updated libraries for stability and security: Jetpack Compose 1.11, Hilt 2.60.1, Glance 1.2.0, WorkManager 2.12.0, ExifInterface 1.4.2. No feature changes.
- Web: patched a build-tool advisory (esbuild).

## [1.3.0] - 2026-10-05
- Works offline: the last forecast is shown when there is no connection, and a "Couldn't refresh" line explains why (offline, invalid API key, rate limit, server error).
- Clearer error messages instead of a generic failure.
- Fixed: settings not updating in release builds, wrong weekday names on the large widget, Settings closing on rotation, duplicate search results.
- Settings screen translated into all 8 languages; Malagasy and Nepali dates on the web app.
- Location and Pirate Weather API key are excluded from cloud backups.
- Removed the WMO SWIC alert source (its service no longer exists); 7 alert sources remain.
- Web: self-hosted fonts (no Google Fonts), security headers, cached alert proxies, offline app shell.
- Stability: fixes for fetch races, cancellation handling, malformed weather data and alert desync.

## [1.2.20] - 2026-08-23
- Refined Malagasy nowcast headline phrasing for 2-hour clear forecast.

## [1.2.19] - 2026-08-23
- Updated Minute-by-Minute Nowcast sparkline to display 10-minute intervals over 2 hours.
- Interpolated precipitation sampling across Open-Meteo and Pirate Weather sources.

## [1.2.18] - 2026-08-23
- Enhanced Minute-by-Minute Nowcast for Web PWA: added Pirate Weather minutely forecast support and start index slice precision.
- Malagasy translation refinement for nowcast status.

## [1.2.17] - 2026-08-23
- Added Minute-by-Minute Nowcast card: 2-hour precipitation forecast sparkline with 15-minute resolution.
- Dynamic precipitation start/stop headline indicator.

## [1.2.16] - 2026-08-23
- Added Life & Activity Indices section: Running conditions, Outdoor Laundry Drying Time, and Sun Protection / UV Safety.
- High-latitude location degree distance precision fix for auto-refresh logic.

## [1.2.15] - 2026-08-09
- Location switching redesigned: search, favorite (♥), and current-location icons now live in the header.
- Searching a location previews it instantly; tap the heart icon to save it as a favorite.
- Swipe left/right — or tap the new arrow buttons — to move between Current Location and your favorites.
- "Expert Mode" is now called "Advanced Mode" in Settings.

## [1.2.14] - 2026-07-21
- Cards now expand one at a time — opening one collapses any other that was open.
- Switching to a different location now collapses all cards.

## [1.2.13] - 2026-07-20
- The 7-Day Forecast now scrolls horizontally like the Hourly Forecast, with a matching trend chart.
- Current Conditions now appears above the Hourly Forecast.
- The UV Forecast card now shows today's forecast high instead of the live reading.
- The live UV reading is back in Current Conditions.

## [1.2.12] - 2026-07-20
- Added an Air Quality forecast card with a 48-hour trend chart.
- Added a UV forecast card with a 7-day trend chart.
- The daily forecast now always shows exactly 7 days.
- Fixed forecast day labels sometimes showing the wrong date.

## [1.2.11] - 2026-07-20
- When alerts come from multiple sources, all their badges now show together on the alert banner.
- Each alert can now be expanded/collapsed individually to show or hide its full details.

## [1.2.10] - 2026-07-20
- Fixed NWS/ECCC/BOM alerts sometimes failing to show when location lookup was unreliable.
- NHC hurricane/storm alerts now show wind speed, pressure, movement, and distance instead of just the storm's name.

## [1.2.9] - 2026-07-17
- Shared weather images now include the capture date/time in their metadata (EXIF).

## [1.2.8] - 2026-07-17
- Location search now returns more results (up to 20) so places that share a common name aren't hidden.
- Fixed "City, State" searches (e.g. "Bloomington, Indiana") returning nothing on the default geocoder.

## [1.2.7] - 2026-07-17
- Added a colored tier badge (Low/Moderate/High/Very High/Extreme) to the UV Index card.
- Air Quality now sits next to UV Index in Current Conditions; Visibility moved to its own row.

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
