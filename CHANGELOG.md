# Changelog

All notable changes to maripànaTokana are documented here, based on the curated
release notes in `fastlane/metadata/android/en-US/changelogs/`. The Android app
and Web PWA share one version number as of 1.2.1; before that, only Android was
versioned. Not every point release has recorded notes (some were build/CI-only
retriggers with no user-facing change) — those are omitted rather than guessed at.

## [1.5.17] - 2026-10-21
- Location details: a tap on a line opens the point in a map app again (Android: the geo: app chooser, without this app; web: OpenStreetMap), as a tap on the coordinates did before 1.5.16. The share sheet with a Google Maps link from 1.5.16 is removed. Holding a line still copies it.

## [1.5.16] - 2026-10-21
- Location details: a grid icon in front of the second header line opens a small light card with the place, the coordinates (degrees, minutes and seconds) and the full Plus Code. A tap on a line opens the share sheet with the value and a Google Maps link; holding a line copies the value (coordinates in decimal degrees). A tap on the city name no longer shows the coordinates (Android and web).
- The second header line always shows: the region and country, or the decimal coordinates when they are not known (the Plus Code when the name itself is coordinates).
- Android: the current GPS location shows its region and country again, as on web.
- Holding a card title that is cut short shows the full title (Android and web).
- Release builds shrink unused resources (standard APK about 5% smaller).
- Translations: derived alert texts in Arabic, Hindi, Nepali and Chinese; Malagasy review (the user's own words restored, wrong words fixed, "toetrandro" and "Maripàna" used everywhere, dew point "Maripàna manando").

## [1.5.15] - 2026-10-20
- Each GitHub release has two APKs: `maripanaTokana-vX.apk` (F-Droid version, the APK that F-Droid publishes) and `maripanaTokana-standard-vX.apk` (standard version, Google Play Services location). Both have the same app ID and signing key.
- README: Obtainium badge, and a description of the two APKs.

## [1.5.14] - 2026-10-20
- Place search reads US state and Canadian province codes after the comma ("Sudbury, ON") and matches the qualifier only at the start of a word, so "on" no longer matches "Washington". When no Open-Meteo result fits the region, the whole query goes to Nominatim, which finds names Open-Meteo only knows in longer form (Greater Sudbury) (Android and web).
- Web: a cached place name saved in another language is looked up again instead of shown as is.
- New F-Droid screenshots: current design, four per language.

## [1.5.13] - 2026-10-20
- "Choose a place" button on the error screen (Android and web) and the location-permission screen (Android). A place picked while no weather is shown is saved as a favorite rather than a 12-hour preview, so it persists (8 languages).
- Android: with location permission denied, a selected favorite or preview loads at launch, and resuming no longer drops back to the permission screen.
- Forecast tabs reordered: Precipitation before Wind (Android and web).

## [1.5.12] - 2026-10-19
- Hourly and 10-day cards show the chance of precipitation behind a raindrop icon, only on the Conditions and Precipitation tabs; elsewhere the line stays blank so card heights don't jump (Android and web).

## [1.5.11] - 2026-10-19
- Hourly and 10-day charts use the same chart type per tab: daily wind, UV and air quality switched from bars to zero-based lines; precipitation stays bars on both (Android and web).
- 10-day temperature chart labels the warmest high and coldest low in both units, and is as tall as the hourly chart (Android and web).
- Precipitation charts show "No precip" when every value is zero (Android and web).

## [1.5.10] - 2026-10-19
- The Minutely Nowcast block only appears when the next two hours hold precipitation; a dry nowcast repeated the main card's rain line (Android and web).
- Shared card images end with two faint lines: "maripànaTokana v{version}" and https://maripana.njarasoa.org (Android and web).
- About page: "On the web" link to maripana.njarasoa.org (Android and web, 8 languages).
- Test that a language change renames the current-location place.

## [1.5.9] - 2026-10-19
- Widgets send their images (background, weather icons, refresh) to the launcher as bitmaps instead of resource IDs. Some launchers keep the previous version's resources after an update, and a shifted ID drew the wrong image: the background as a stretched sun, refresh as the share icon, clear day as a moon. Widgets already showing this need removing and adding once.
- The 4x1 widget has less vertical padding and a smaller icon so both rows fit in one cell.

## [1.5.8] - 2026-10-19
- A favorite's region and country line is looked up again in the current language on every load and right after a language change, instead of keeping the language it was saved in (a Malagasy "Nepaly" in a Nepali screen). The favorite's own name is kept. On Android the GPS place's name now also follows a language change (Android and web).

## [1.5.7] - 2026-10-19
- The UV index tile shows a five-level dial (Low to Extreme) like the air-quality tile, the dot at the reading's place within its level, replacing the badge and dots (Android and web).

## [1.5.6] - 2026-10-19
- About page: a new "This device" section lists what the system reports: model, system version, screen, pixel density, text size, languages (the device's and the app's), time zone, processor, memory, touch, where the app was installed from and the app build (Android and web). No hardware identifiers.

## [1.5.5] - 2026-10-19
- The wind compass and the pressure and air-quality dials sit larger in the tile's bottom corner, so titles show in full (Android and web).
- For places in another time zone, the phone's time under each hour is centred and marked with a visible flat phone icon (the dimmed emoji was all but invisible), on the main card and in the hourly cards; the "Updated" time gets the same icon.
- The clock emoji is removed from the "Updated" line in the languages that had it.

## [1.5.4] - 2026-10-19
- The main card's rain line now agrees with the rest of the screen: an hour counts as rainy when the nowcast covering it shows rain or, beyond the nowcast, when its weather icon is rain or snow; the chance of rain alone no longer counts (Android and web).
- For places in another time zone, the main card's next three hours also show the phone's time, as the hourly cards do.

## [1.5.3] - 2026-10-19
- Fixed: on launchers that size widgets by their minimum size (Nova, Lineage's Trebuchet and older Android), the small widget took 2 rows instead of 1 and the large one 3 instead of 2.

## [1.5.2] - 2026-10-19
- Home-screen widgets use the Meteocons icons (day or night for the current weather); the large widget's second-unit low/high is dimmed like the app's.
- Chart labels no longer sit on the line: the hourly temperature chart puts the high above its peak and the low below its trough; the nowcast marks its peak with a dot and shows the amount in the far top corner (Android and web).
- Removed 11 strings no longer used on either platform.

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
