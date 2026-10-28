# Changelog

All notable changes to maripànaTokana are documented here, based on the curated
release notes in `fastlane/metadata/android/en-US/changelogs/` and the git history.
The Android app and the web app share one version number as of 1.2.1; before that,
only Android was versioned. Releases with no change that users can see (build and
CI retriggers, such as 1.0.1, 1.0.8, 1.0.9, 1.0.19 to 1.0.35 and the
`v1.0.18-*` tags) are not listed. There is no 1.4.2 tag: that version was withdrawn.

## [Unreleased]
- New Moon card next to the Sun card, with the same layout: the moon's arc across the sky, the part lit, moonrise and moonset, the phase (shown as south of the equator when the place is), and the azimuth. The app calculates these on the device (SunCalc formulas).
- Sun card: the length of the day (hours:minutes) under the arc.
- Pressure, air quality and UV cards: a large dial in the middle of the card with both units in it; the air quality and UV levels are badges in the dial.
- New Cloud cover card with a dial, next to Visibility. The precipitation card no longer shows the cloud cover.
- Humidity card: the dew point label and its values are on separate lines.
- Malagasy: "Tako-drahona" for cloud cover.

## [1.6.20261028182225] - 2026-10-28
- Hourly and daily forecasts: a new Humidity tab. Each card shows the relative humidity and the dew point in both units, and the chart under the cards shows the humidity (blue) and the dew point (amber), each on its own scale. The daily values are the means for the day.

## [1.6.20261028141910] - 2026-10-28
- No change in the app. The release workflow now does the F-Droid build check (`scripts/fdroid_check.sh`) before it publishes: it builds the tag with fdroidserver in F-Droid's build image and compares the result with the signed APK. If they differ, nothing is published.

## [1.6.20261028114142] - 2026-10-28
- F-Droid: the build hash in the app is now always the first 7 characters of the commit. Before, `git rev-parse --short` gave more characters on a clone with more objects, so the F-Droid build of 1.6.20261026220516 did not match the GitHub APK and F-Droid did not publish it.
- Malagasy: "Antonony" for the Moderate rating and "Faritra azo kitihina" for the touch screen row.

## [1.6.20261026220516] - 2026-10-26
- Malagasy: better device info labels on the About page (Hatevin'ny teboka, RAM, cpu, Fitaratra mandray kitika).
- Android About page: the device info no longer has the App build row. The flavor and the version code are now in the small line under the version, with the hash and the build time.
- Web About page: the device info no longer repeats the app version, which is already at the top of the page.
- Web About page: Firefox and Safari show a short browser name (for example "Firefox 158"), not the whole user agent, and the pixel density has at most 2 decimals.
- Web and README: the Obtainium badge adds the app with an APK filter, so Obtainium installs the F-Droid APK and does not ask which of the two APKs to use.
- Web: with the app in Malagasy, the Add to Home screen steps (the dialog and the iPhone Help section) use the phone language, or English when the app does not have it. The steps name browser menu items, and browsers do not show them in Malagasy.
- Web: an "Add to Home screen" badge on the About page, for all browsers. Chrome, Edge and Samsung Internet open their own install dialog; Safari and Firefox show the steps. The footer no longer has the Install link (it was only on iPhone and iPad). The badge hides when the app already runs from the Home screen.
- Web: the About page no longer links to the web version, because it is the web version. The Android About page keeps the link.
- Web: on Android, the About page links to the Android app on F-Droid, Obtainium and the GitHub releases, with badges.

## [1.6.20261026193250] - 2026-10-26
- Version numbers are now MAJOR.MINOR.YYYYMMDDHHMMSS, with the release time in UTC.
- Android: the footer shows only the version. The build hash and build time are now on the About page, under the version.

## [1.6.0] - 2026-10-26
- Android widgets: the refresh button no longer makes the app stop responding (ANR) when the phone has no recent location. A background job does the refresh.
- Android widgets: the place name agrees with the weather. Before, after the phone moved, the widget could show the weather for the new place with the name of the old place.
- Android build: compileSdk 37, Kotlin 2.4.21, AGP 9.4.1 and Gradle 9.8.2. Also new versions of Compose (BOM 2026.09.00), core-ktx, lifecycle, activity-compose, hilt-navigation-compose, kotlinx-serialization and kotlinx-coroutines. The target SDK stays 36. The app does not change for users.
- Web: the app uses SvelteKit 3.1, Vite 8.3, Vitest 5.0, `@sveltejs/adapter-node` 6.0 and TypeScript 6.0. `npm audit` shows no vulnerabilities.

## [1.5.32] - 2026-10-26
- French and Spanish: the help text, the store descriptions and the changelogs name the language Malagasy, not "malgache".

## [1.5.31] - 2026-10-26
- License: a GPL v3 section 7 additional permission lets anyone convey the Standard APK with Google Play services (README).
- About page (Android): tap Meteocons to read its MIT License text in the app.
- Each source file starts with SPDX lines for the copyright holder and the license (GPL-3.0-or-later).
- About page: tap the license line to read the full GPL v3 text in the app (Android) or on GitHub (web).
- The project follows the REUSE specification: `REUSE.toml` and `LICENSES/` give the copyright and license of each file, also fonts, icons and images.
- About page (Android): the Material Design icons are listed (Apache-2.0); tap the entry to read the license text in the app.
- New `CONTRIBUTING.md`: the license terms for contributions, and the checks to run before a pull request.
- Android widgets: the alert badge shows the number of active weather alerts. The widgets did not load alerts before, so the badge never showed.
- New app icon: the same Blue Marble Earth, sharper, with the thermometer at the lower left and Madagascar highlighted. The themed icon (Android 13 and later) shows the globe, Madagascar and the thermometer; before, it was a solid square. Also the web app icons and the store icon.
- The Blue Marble background is sharper (1440 px, from the full photo), and Madagascar is faintly raised. The Android image is a WebP now, half the size of the old PNG.
- Each GitHub release shows its notes from this file. The release build and the version check fail when a version has no notes in this file or no store changelog in each language.

## [1.5.30] - 2026-10-26
- Android: the forecast and the place name load at the same time, so a refresh is one network round trip shorter.
- Android: Nominatim gets one request at a time, at most one per second, as its usage policy asks. The same place is asked for once, not up to 3 times for each refresh.
- Android: the HTTP client has a 10 MB disk cache and a 40-second call timeout. The world GDACS feed is downloaded once in 10 minutes, not for each alerts fetch.
- Android: a refresh loads the weather once, not twice, when the new GPS fix answers before the weather for the last fix. When the new fix is more than about 5 km from the last fix, the app now loads the weather for the new fix (before, it compared the fix with itself and kept the old place).
- Shared coordinates show the place name (for example "Antananarivo") in place of the coordinates (Android and web).

## [1.5.29] - 2026-10-26
- Hourly temperature chart: the high's label goes under the line when it would cover the 00:00 or 12:00 label (Android and web).
- Web: on iPhone and iPad, an Install link in the footer shows how to add the app to the Home Screen, and Help has the same steps. The Home Screen name and a 180x180 icon are set.
- Docs: README, web README, design, testing, F-Droid and screenshot guides checked against the code and corrected.
- Android widgets: a failed refresh is reported as failed, so the worker retries up to 3 times; the fetcher keeps the last forecast. Each refresh fetches once, not once for each widget. The forecast cache moves from SharedPreferences to `files/widget_cache/forecast.json`. Standard flavor: the background location request uses balanced power and stops after 20 seconds (#88).
- Android: a shared place that is still being resolved no longer replaces a place the user picked after it. The background GPS refresh during a preview runs one at a time and fills the GPS cache (#88).
- Android: MeteoAlarm parsing builds its date parser and regexes once (#88).
- Store full descriptions (all languages except Malagasy): °C to one decimal place, and 4 tiles for the next 12 hours.

## [1.5.28] - 2026-10-23
- Malagasy: the widget low and high use "iva" and "avo"; the Help text puts the language flag at the bottom right ("ankavanan'ny"); store description corrected by the author (Android and web).
- Store descriptions rewritten for the current app in all 8 languages; docs (design, testing, F-Droid, screenshots, web README) updated to the current app.
- Malagasy short and full descriptions: "mpanjanaka" and "toetrandro" used.

## [1.5.27] - 2026-10-23
- MeteoAlarm alerts: area names match only as whole words, so a short name no longer matches inside a longer one ("Ain" inside "Maine-et-Loire") (Android and web). Taken from the old `jules/regional-alerts` branch, with a word match that also works for Greek and Cyrillic names.
- README: screenshots in 4 languages and an updated feature list.

## [1.5.26] - 2026-10-23
- New F-Droid screenshots in all 8 languages: version 1.5.25 at Ambila Lemaitso, four per language.
- The changelog repeats the license change from 1.5.25, because F-Droid skips that version.

## [1.5.25] - 2026-10-23
- License: GPL-3.0-or-later in place of MIT. `LICENSE` has the full GPL v3 text; the About page, README, F-Droid descriptions and F-Droid metadata say so in all 8 languages. Versions up to 1.5.24 stay under MIT.

## [1.5.24] - 2026-10-22
- Hero card: rain or snow and air quality fill the space beside the °F and high/low lines. Air quality shows only when its level is not good, with the US and EU indices and the level badge (Android and web).
- Hero card: four hour tiles, at the first forecast hour and every 4 hours after it, up to 12 hours later (Android and web).
- Android: in RTL languages, the phone icon shows before the phone time and no longer covers it.

## [1.5.23] - 2026-10-22
- Malagasy: refined texts across the app, help and settings (Android and web).

## [1.5.22] - 2026-10-21
- Hero card: the three hour tiles show the first forecast hour, 6 hours later and 12 hours later, in place of the next three hours (Android and web).
- Hero card: the rain or snow line shows only when rain or snow is coming; "No rain in the next 24 hours" is removed from all 8 languages (Android and web).

## [1.5.21] - 2026-10-21
- Android: after a language change, the GPS place takes its region and country line in the new language as well as its name. Since 1.5.16 the GPS place shows that line, and it stayed in the old language.
- Arabic and Hindi: numbers written inside texts (24 hours, 10 days, 2 hours, 12 hours) use local digits.

## [1.5.20] - 2026-10-21
- About and Help pages: the language flag at the right end of the title row; a tap shows the next language (Android and web).
- Settings, About and Help: the title row stays at the top while the page scrolls (Android and web).
- Malagasy: the Help title is "Anatranatra".

## [1.5.19] - 2026-10-21
- Help page: 11 questions and answers in 3 groups (places and favorites; language, font and units; weather data), in all 8 languages. The texts are in `shared/i18n/locales/*.json` (`help_*` keys); the Malagasy text is not reviewed yet (Android and web).
- Footer: separate "Help" and "About" links. The About page links to Help.

## [1.5.18] - 2026-10-21
- Footer: "About" (underlined, opens the About page, which credits the data sources) in place of the weather data credit; `credits_weather_data` removed from all 8 languages (Android and web).
- Android footer: the middle text wraps between the font name and the flag, so a long text (Malagasy) no longer pushes the flag off the screen.

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
- F-Droid metadata: the WebSite field and the list of every server the app contacts.

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

## [1.2.24] - 2026-08-24
- Places shared to the app as map links open directly.
- Smoother switching between places.
- Peak and trough labels on the temperature, air-quality and UV charts.

## [1.2.23] - 2026-08-24
- Nowcast chart: the maximum label sits below the curve and the minimum label above the baseline.

## [1.2.22] - 2026-08-24
- Web: the nowcast chart has the same smooth curve and 30-minute axis as Android.

## [1.2.21] - 2026-08-24
- Android: the nowcast chart is a smooth curve with a 30-minute time axis.

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
- Version number change only. The features below came in 1.0.40.

## [1.0.40] - 2026-06-26
- Pluggable weather sources (Open-Meteo default, Pirate Weather optional).
- Eight weather alert sources (NWS, GDACS, MeteoAlarm, JMA, ECCC, BOM, NHC, WMO SWIC), each individually toggleable in Settings.
- Settings screen now includes geocoding and source configuration.
- Web: Node.js server (adapter-node), with server routes for the alert sources that the browser cannot call.

## [1.0.39] - 2026-05-28
- Loading screen with text that cycles through the languages, and a seconds counter.
- Fixes to the developer location override.

## [1.0.38] - 2026-05-28
- Location works on phones without Google services (passive provider).
- The place name follows the app language.
- Date format fix.
- F-Droid: the app is marked as needing network services (TetheredNet).

## [1.0.37] - 2026-05-20
- The hourly chart fits the screen width, with a band for the part in view and 00:00 and 12:00 labels.

## [1.0.36] - 2026-05-20
- Faster loading and drawing on Android and web.

## [1.0.18] - 2026-05-15
- Technical release to enable F-Droid Reproducible Builds.

## [1.0.17] - 2026-05-08
- Fixed missing data in release builds (R8 rules for the API data classes).
- Location callbacks moved off the main thread.

## [1.0.16] - 2026-05-08
- Errors show in the app language and the phone language.
- The app detects when location is turned off.
- Fixed a refresh race. The version shows in the footer.

## [1.0.15] - 2026-05-08
- Better handling of a denied permission, of approximate-only location and of a removed permission.

## [1.0.14] - 2026-05-08
- Fixed a hang when the phone has no cached location.

## [1.0.13] - 2026-05-07
- F-Droid flavor: more fixes to the permission request.

## [1.0.12] - 2026-05-06
- F-Droid flavor: fixes to the permission request.

## [1.0.11] - 2026-04-22
- Fixed startup crash caused by R8 stripping WorkManager database classes.

## [1.0.10] - 2026-03-25
- 10-day temperature chart, with dots and grid lines on the charts.
- Build fixes for F-Droid (JDK 21 toolchain, no foojay, no JDK downloads).

## [1.0.7] - 2026-03-16
- Developer Mode: set the location by hand.
- Tap the GPS coordinates to open a map.
- The last update time moved to the main card, with a manual refresh.
- Better alerts and widgets.

## [1.0.6] - 2026-03-07
- Fixed crash in forecast lists caused by duplicate keys.
- Gradle upgrade.

## [1.0.5] - 2026-03-04
- Fixed a race and a leak in the F-Droid location provider.
- Shared images are made off the main thread.
- Better accessibility and touch targets.

## [1.0.4] - 2026-03-02
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
