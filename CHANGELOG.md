# Changelog

All notable changes to maripànaTokana are documented here, based on the curated
release notes in `fastlane/metadata/android/en-US/changelogs/` and the git history.
The Android app and the web app share one version number as of 1.2.1; before that,
only Android was versioned. Releases with no change that users can see (build and
CI retriggers, such as 1.0.1, 1.0.8, 1.0.9, 1.0.19 to 1.0.35 and the
`v1.0.18-*` tags) are not listed. There is no 1.4.2 tag: that version was withdrawn.

## [Unreleased]

- Moon tile (phone and web): the moon no longer shows twice. While it is up, the moon on the arc shows the phase and the middle shows only the phase name; while it is down, the middle shows the phase icon and name. The name has more width, so a long name (Malagasy "Volan-tandrony") no longer breaks inside the word.

## [1.7.20261112190552] - 2026-11-12

- About (Android, Android TV and web): the version, the hash and the commit time (UTC) on every platform, in a tighter block; the web About had only the version, and Android showed the build time ("reproducible" in the F-Droid build). The credits moved to a new Licenses page, opened from About; a tap on a credit shows its licence text in the app, on the web too. On the TV, a Licenses card at the end of the link row opens the credits.
- About (Android and web): credits for the sea data and the 10-year normals (Open-Meteo Marine and Historical Weather, with the Copernicus lines), the SunCalc moon formulas (BSD-2-Clause), the F-Droid, GitHub and Obtainium badges, and on the web the Material Design and Feather icons and the PWA logo. The web About links to the licence texts of all code and icons in the web bundle (`/licenses/third-party.txt`). Corrected licence names: ECCC Data Services End-use Licence, JMA Public Data License (Version 1.0), GDACS under the European Commission reuse policy.

## [1.7.20261112154705] - 2026-11-12

- Folded header: at a saved place or a preview, the current-location pin takes the place of the search button, so one tap goes back to where you are. At the current location, the search button stays.
- Web: the header now folds 3 seconds after the page opens. Before, it often stayed open until the first scroll.

## [1.7.20261112032642] - 2026-11-12

- Times follow the 24-hour or 12-hour clock of the device (phone, TV, screensaver, widgets) or of the browser region (web): "9:05 PM" for a time with minutes and "9 PM" for a full hour on a 12-hour clock; before, most times were always 24-hour.
- Android TV: times as on the phone. For a place in another time zone, the home panel shows the place's time with a clock, the update time is the TV's own with a TV icon, and the hourly cards and the screensaver hours also show the TV's time.
- Android TV: after a failed first load (no network when the TV starts, for example), the app tries again every 5 minutes; before, it waited for Retry. The screensaver clock changes at the start of each minute, and the moon phase on the details screen updates with each weather refresh.
- Web: sharing a card frees the memory of its images at once, gives an error instead of a crash when the browser cannot make the image, and no longer stops the download in some browsers.

## [1.7.20261112013642] - 2026-11-12

- Android: the place-name lookup of the system geocoder no longer runs on the main thread, so a slow network cannot freeze the app.
- Web: expired place names are removed from the browser storage, so the storage does not fill up over time.
- Web: in the saved places search, the results of an older, slower query no longer replace the results of the newer query.
- Web: the offline copy of the weather and place-name responses keeps at most 50 entries.
- MeteoAlarm alerts (Android and web): an alert for another area that only shares one word with the place, such as "Haute-Savoie" for "Haute-Garonne", no longer shows.
- Android: a failed short-link lookup no longer leaves its network connection open, and the F-Droid location code always stops its thread.
- Android: the 30-minute background weather update runs only while a widget is on the home screen. Without a widget, it no longer uses the battery and the network.
- Android widgets: the background image is 400 × 200, so a widget sends half as much image data to the home screen (0.9 MB instead of 1.9 MB).
- MeteoAlarm alerts (Android and web): in France, a department no longer gets the alerts of a department whose name contains its name ("Loire" and "Haute-Loire", "Savoie" and "Haute-Savoie"). Italy now gets its alerts: they are for regions, and the app now compares them with the region of the place, also when its name is in English ("Tuscany" is "Toscana").
- Web: MeteoAlarm alerts use the local area names of the place ("Haute-Savoie", not "Upper Savoy"), so places in departments with an English name get their alerts in an English browser. The place lookups wait at least 1.1 s between requests, as the Nominatim usage policy asks.

## [1.7.20261111184249] - 2026-11-11

- When the shown position is more than 30 minutes old, or the app uses the last known position because a new one failed, a line under the place name says "Using last known location" with its time, on Android, Android TV and the web.
- About: the F-Droid, Obtainium and GitHub badges show on Android too, and again on iPhone and iPad on the web, so a user can pass the app to someone else.
- Web: sharing a card works again for cards with coloured badges (it failed since the badges used mixed colours). The Content Security Policy is now enforced.
- Web: on the first visit, the app uses the browser language when it has it, else English.
- Screen readers read a summary of the nowcast chart. Android frees the share images after use.

## [1.7.20261111165956] - 2026-11-11

- Chart view (Android and web): a daily mode with one point per day, opened from the 10-day card; an Hourly | Daily switch at the top. The temperature panel shows the high and the low, the precipitation panel the daily totals, and the wind and UV panels the highest gust and air quality of each day.
- On the first start, the app uses the language of the device or browser when it has it, else English (it was Malagasy). An existing install keeps its language.
- Store descriptions and docs: Android TV, the chart view and the forecast tabs.

## [1.7.20261111160128] - 2026-11-11

- Charts on the TV, the phone and the web use one colour set: the dew point green and dashed, the gust amber, the heat index pink and the wind chill purple.
- Chart view (Android and web): the temperature panel shows the heat index and the wind chill when they apply. The air quality panel is now a UV index panel, with the air quality as a dashed line on its own scale.
- Android TV: the daily chart has the air quality (the highest of each day); the UV plot shows the air quality as a dashed line on its own scale.
- The app shows the wind chill only at 0 °C and below, and the heat index only when it is 5 °C or more above the temperature. Otherwise the source's "feels like" shows.

## [1.7.20261111152148] - 2026-11-11

- Android TV charts: the scale stays the same while the selected hour moves. When OK shows the air quality and the selected hour is after its last value, the selection moves to that last value.

## [1.7.20261111150921] - 2026-11-11

- Android TV charts: the temperature plot also shows the dew point and, when they apply, the heat index and the wind chill. The precipitation plot shows the chance of rain, and the wind plot the gust. Each line has its own colour, and the cards above the plot use the same colours. The air quality plot stops at its last value.

## [1.7.20261111135439] - 2026-11-11

- Android TV: a day card opens a daily chart. One plot shows the high, the low and the dew point, each in its own colour; another shows the precipitation as bars with the chance of rain as a line. The other plots show the wind, the UV index and the humidity of each day.

## [1.7.20261111014915] - 2026-11-10

- Android TV: the banner shows a larger globe in the center and no app name.
- Android TV: the labels on the chart screen show in full on two lines, for example "Maripàna manando" (dew point), which showed only as "Maripàna".

## [1.7.20261110082440] - 2026-11-10

- Android: a tile that is alone in its row, such as the visibility tile, is as tall as the other tiles.

## [1.7.20261110053846] - 2026-11-09

- Sky: the sun card shows how far from straight overhead the noon sun is today and its range in a year at the place. A new declination card shows where on Earth the sun is straight overhead, on a dial from 23° S to 23° N, with the days until the next solstice or equinox, on Android, Android TV and the web.
- Web: iOS launch images for iPhones in landscape.
- Android TV: the current conditions screen has rows of five, five and four tiles.

## [1.7.20261110042513] - 2026-11-09

- Web: on iPhone and iPad, the About page does not show the F-Droid and Obtainium badges, because they install Android apps only.
- Web: the app that is open from the iPhone or iPad Home Screen shows the globe on the app background while it loads, in place of a blank screen.
- Web: the main screen and the Settings page do not show a scrollbar, as in the Android app.

## [1.7.20261110020422] - 2026-11-09

- About: the source code, web app, F-Droid and Obtainium links show their QR code in a popup, one at a time, instead of four QR codes on the page, on Android, Android TV and the web.

## [1.7.20261109231555] - 2026-11-09

- Android TV: on the current conditions screen, the card titles and the line under the value are smaller and can take two lines, so long Malagasy texts are not cut.

## [1.7.20261109230325] - 2026-11-09

- Moon tile: the middle of the arc shows the phase icon and name instead of the part lit, on Android, Android TV and the web. The phase no longer shows a second time at the bottom of the tile.

## [1.7.20261109225355] - 2026-11-09

- Web About: the four QR codes are in one even grid, four across on a computer and two on a phone. F-Droid and Obtainium have their badge under their code. The GitHub and Add to Home screen badges are in a row below.

## [1.7.20261109223736] - 2026-11-09

- About: on Android and the web, each QR code is the link to its address, and the text links that repeated them are gone. On Android TV, About opens from the home screen, beside Settings.

## [1.7.20261109222628] - 2026-11-09

- About: QR codes for the source code, the web app, F-Droid and Obtainium, on Android, Android TV and the web. On the web, the F-Droid and Obtainium codes are under their badges, which now also show on a computer.

## [1.7.20261109215736] - 2026-11-09

- Android TV: About shows QR codes for the source code and the web app. The place name shows only on screens with weather.

## [1.7.20261109214759] - 2026-11-09

- Android TV: each chart shows its highest and lowest value. The home screen shows the time and when the weather loaded, and the weather loads again when it is 30 minutes old. The app is also a screensaver with the time, the current weather, the worst alert and the next hours.

## [1.7.20261109212705] - 2026-11-09

- Android TV: the app always opens the TV screens on a TV, and Back leaves the app. The places list hides "Current location" when the device has location off, and shows a message when no position arrives.

## [1.7.20261109205306] - 2026-11-09

- Android TV: the same APK now also runs on Android TV, with screens made for the remote. They show the current weather, hourly and 10-day forecasts, a chart of 9 values, alerts, air quality, details, places and search. Settings has units, language, Help and About.

## [1.6.20261109165738] - 2026-11-09

- Hourly and 10-day forecasts: the temperatures use the colours of the temperature scale.
- Air quality: the chart view and the hourly forecast show air quality for places at a half-hour UTC offset, such as New Delhi. Before, each hour was exactly 30 minutes from the air quality data, and no hour matched.
- Web: vite 8.3.7.

## [1.6.20261109040327] - 2026-11-09

- Moon tile: shows only the next new or full moon, whichever comes first, on one line with its date. In 1.6.20261109035136, the two dates did not fit on a phone and the Malagasy names ran together.

## [1.6.20261109035136] - 2026-11-09

- Moon tile: shows the dates of the next new moon and full moon, the first one first.
- Chart view: each day label shows the moon of the day. On the day of a new or full moon, the label also shows its name. The day labels are smaller (10 sp).
- Malagasy: the moon phases use the new names "Volana tsinana" (new moon), "Volan-tandrony" (waxing crescent) and "Fenomanana" (full moon).

## [1.6.20261109032458] - 2026-11-08

- Chart view: each hour is half as wide, so the screen shows twice as many hours. The weather icons, UV badges, wind arrows and hour labels show every 4 hours (was every 3 hours).
- Chart view: the wind, pressure, humidity and air quality charts show the high and the low of each day on their main line, as the temperature chart does. The labels use the colour of the line; the air quality labels use the colour of their level.
- Chart view: a high or low label stays inside the chart at the edges. On the web, the labels show on top of the dots of the selected hour.

## [1.6.20261108202449] - 2026-11-08

- Chart view: the card of the selected hour is compact, so the charts get about half of a phone screen. The tiles fill 2 rows (5 and 4), and the air quality tile shows only the value of the primary standard.
- Chart view: the card keeps the same tiles and the same size for every hour, so the charts do not move when you scroll. An hour without a value shows a dash.
- Chart view: the tile labels fit in every language. They all get the largest size at which each label takes at most 2 lines and no word breaks.

## [1.6.20261108174717] - 2026-11-08

- Chart view: each chart is easier to see. The titles are bigger, on their own row, and each chart has a faint band.
- Chart view: a new air quality chart shows the hourly AQI in the colours of its level, for about 5 days. Where the data stops, it shows "No data". The selected hour shows its air quality in a tile, in both standards (US and EU).
- Chart view: the highest rain bar of each day shows its value in both units.
- Chart view: the value tiles fill their rows evenly (9 tiles make 3 rows of 3).
- The hourly air quality starts at the current hour, as the forecast hours do.
- The header folds and the footer hides after 3 seconds without a scroll (was 5 seconds).

## [1.6.20261108041252] - 2026-11-08

- Chart view: the same look as the main screen. The place name is the big line at the top, with "Chart view" under it. The selected hour shows in a glass card, with its values in tiles as in the main card: the temperature on its colour pill, each value in both units, the values lined up when a label wraps. The charts are in a second glass card. A soft column one hour wide marks the selected hour in place of the thin line. The temperature chart shows the high and the low of each day in orange and blue, as the chart on the main page.

## [1.6.20261107213135] - 2026-11-07

- Chart view: the selection line starts on the current hour at the start of the chart, moves to the middle as the hours scroll, and moves to the end for the last hour, so the chart has no empty half. Zoom (pinch, double tap, Ctrl + wheel, + and −) is removed; a tap now selects at once.

## [1.6.20261107194118] - 2026-11-07
- Chart view: one finger scrolls the hours with fling; the selection line stays in the middle of the chart, and the hour under it is selected, with a tick at each new hour. A tap brings that hour to the middle; pinch and double tap zoom around the line. This replaces the slide-to-select rule of the previous version.
- Chart view: the hint under the title is removed.

## [1.6.20261107190528] - 2026-11-07
- Chart view: a one-finger slide selects the hours, with a tick at each hour, as on the main-page charts; near the start or end edge the chart scrolls by itself while the selection follows. Two fingers scroll and zoom; a double tap resets the zoom; a tap selects with a tick. The long-press rule is removed. Web: a mouse drag selects, and Shift + wheel or a horizontal wheel scrolls.
- Chart view: the days and hours also show at the top, above the icons.
- Chart view: each chart's scale comes from all the hours, so it does not change while the hours scroll or zoom.
- The hint in the chart view describes the new gestures (Malagasy "Asosay ny rantsantananao hahitana ny sanda. …").

## [1.6.20261107183042] - 2026-11-07
- Header and footer: while the page scrolls, the big header and the footer slide in; after 5 seconds without scrolling, the header slides to its small state and the footer slides out, so the content gets more room. Open dialogs and menus stop the fold. On Android, the header and footer stay as before while TalkBack is on. The footer floats over the content with a fade under it, and the content does not jump. Web: moving the mouse to the bottom of the app or moving keyboard focus into the footer shows it. With reduced motion, the change has no slide. The header no longer folds after a short scroll.
- Chart view: the button is only on the Hourly card (the 10-Day button opened the same view). The selection line and the midnight lines stop at the edge of each chart, so the charts no longer look joined.

## [1.6.20261107173942] - 2026-11-07
- Chart view: a chart button in the header of the Hourly and 10-Day cards opens a full-screen view with up to 240 hours from now in panels that scroll sideways together: icons and UV, temperature and dew point, rain and chance of rain, wind and gusts with arrows, pressure, humidity and the days. A header shows all values of the selected hour in both units. Tap or drag to select an hour, pinch to zoom, double tap to reset; night hours are shaded. Malagasy "Sarivina".
- Data: Open-Meteo is asked for 11 days and always for hourly gusts; the other views still show 24 hours and 10 days. Pirate Weather gives 168 hours (extend=hourly).
- Main-page charts: the same style as the chart view, scales with steps of 1, 2 or 5, a selection line with dots, night shading and midnight lines on the hourly charts, the chance of rain over the rain bars, gusts in the hourly wind tab and the dew point in the hourly temperature chart.
- Malagasy: the hour word comes before the number ("ora 6"); "Sarivina" replaces "Sarin'ny vinavina"; the line about the normal starts with the number. Web: short day names no longer cut to three letters ("Ala" for both Sunday and Monday).

## [1.6.20261107163242] - 2026-11-07
- Normal temperatures: 10 years of daily highs and lows from Open-Meteo's archive (ERA5), one request per place, saved for 30 days; the normal of a day is the mean over ±7 days. The hero shows "High 4 °C (7 °F) above normal" (Malagasy "Ambony indrindra: … mihoatra ny mahazatra") when today's high is 2 °C or more from the normal. The 10-day chart shows the normal high and low as faint dashed lines with a "Normal" label.
- Forecast changes: the device saves a summary of each forecast (high, low, rain, strongest wind; at most every 3 hours, 5 days, 10 places, per source and model). At the top of the 10-day card, "Changed since yesterday" lists up to 4 changes from the forecast of 18–36 hours ago: high ±3 °C, rain now expected or now dry, rain ±5 mm, wind ±15 km/h.
- Sea (coastal places only, from Open-Meteo's marine data): waves and swell (height, period, direction), sea temperature, and the tide level with the next high and low tide (Malagasy "Famonta", "Samonta", "Aka"), with "Estimated. Not for navigation."
- Forecast rows show no wind arrow below 5 km/h (3 mph); the current wind tile keeps it.
- The hero's next hours show a drop and the chance of rain when it is 30 % or more.

## [1.6.20261107151745] - 2026-11-07
- Settings: with Open-Meteo, Advanced mode shows a "Weather model" list (Malagasy "Modely toetrandro"): Automatic ("Tsotra", the default) and 8 global models: ECMWF IFS, ECMWF AIFS, GFS, ICON, Météo-France, UK Met Office, GEM and JMA, each with its centre and number of days. The app asks Open-Meteo for the chosen model and best_match in one request (`models=<chosen>,best_match`) and fills each missing value from best_match: the UV, the visibility, the chance of rain and the days after the model stops. About shows the model ("Open-Meteo · ECMWF IFS"). The widgets use the same setting.
- Android: a model change clears the saved forecasts and loads the shown place again.

## [1.6.20261107144224] - 2026-11-07
- Hero card: a chip next to the temperature shows the largest change in the next 6 hours when it is 2 °C (4 °F) or more: ↑ or ↓, the difference in both units (from the exact difference) and the hour. Warm colours for a rise, cool for a fall. On a narrow screen it goes to the end of the second-unit line. Screen readers read "Rising 6 °C (11 °F) by 18:00" (Malagasy "Hiakatra … hatramin'ny …").
- Hero card: the temperatures in the next-hours row are in pills tinted by the temperature (violet at −20 °C, blue, cyan, green, yellow, orange, red at 40 °C), with dark text and both units.
- Hero card: the UV index (one decimal) and its level show under the air quality when the UV is moderate or higher.

## [1.6.20261101103743] - 2026-11-01
- Android: a share or a map link opens the app in its own task. Back now returns to the app that shared (before: to the launcher), and the recent apps show the two apps apart (before: our screen showed under the name of the calling app). A share to an app that is already open goes to that app.

## [1.6.20261031154812] - 2026-10-31
- Shared places: a Google Maps link from Google Photos ("Open in Maps") opens the place in the link. The maps.app.goo.gl link leads to a place URL that has the place only as `!3d<lat>!4d<lon>`; the app did not read it, searched for the place by name and found a different place. When a link has both, the app now uses this place pin, not the centre of the map view (`/@`). Links with the comma encoded as `%2C` also work. Same change on the web.
- Release workflow: the VirusTotal check asks again when a new analysis is not visible yet (HTTP 404), or on 429 and server errors.
- Store notes: they now cover the versions since 1.6.20261029031929, the version on the F-Droid page.

## [1.6.20261030204811] - 2026-10-30
- Tiles: the dials in one row line up when one title wraps to two lines (for example Malagasy "Tahan'ny tara-masoandro" next to "Hadion'ny rivotra"). Each title in a row takes as many lines as the longest one.
- Android: the two lines of a tile title sit close together, as on the web.
- Release workflow: the VirusTotal check waits for a file that VirusTotal already has instead of uploading it again. A rerun had failed with "409 Conflict".

## [1.6.20261030194342] - 2026-10-30
- Folded header: the place name always shows (on a narrow phone such as the Xperia XZ2 Compact it disappeared when the place was not the current location). The heart and pin hide while the header is folded, and the local-time chip shows only if it fits in full.
- Temperature tile: today's low and high at the dial ends, in both units; the footer shows only the heat index or the wind chill (the hero already shows "Feels like").
- Alerts: titles have no "GDACS: " or "NHC: " prefix; the badge names the source. Web: in Arabic the alert stripe is on the start side.
- Hero: the precipitation start ("Rain from 05:00") is on its own line under the condition; "© Orinasa Njarasoa" shows only in share images.
- The "Sun & Air" section is now "Sky & Air" (Malagasy "Lanitra sy rivotra"). The "Default" font name is translated (Malagasy "Mahazatra").
- Charts: the Android high and low labels use the same placement as the web, so they no longer cover the line at the edges. In Arabic, all chart labels keep the unit after the number.
- Android: alert times use the app language and its digits.
- Accessibility: share buttons say what they share; screen readers skip the hero icon and the clock emoji; on the web the temperature dial works from the keyboard. The UV, air quality and pollen badges have text at 4.5:1 or more.
- Place list: distances show both units ("12 km · 7 mi").
- Dials: the end labels stay clear of the knob. Tile titles wrap to 2 lines (Malagasy "Tahan'ny tara-masoandro" was cut).
- Folded header: after a failed refresh, the refresh icon is red and its label says that the refresh failed.

## [1.6.20261030191159] - 2026-10-30
- Alerts: each alert shows its level in words (Watch, Warning, Emergency; Malagasy Mailo, Tandremo, Loza) next to the title. Emergency has its own colour (purple) and icon. The number dots and source badges have dark text on light colours, or white on dark.
- Web: the alert banner and its rows work from the keyboard (Enter or Space) and tell screen readers if they are open. Values that change units are read as their value, not as "Toggle units". The refresh button keeps the data age in its name. The page follows prefers-reduced-motion, and font sizes are in rem, so the browser text size applies.
- Android: TalkBack says "Show details" or "Hide details" on alerts; section titles, the place name and the nowcast title are headings; long presses have labels. The status bar and navigation bar icons are always light. Titles can wrap to 2 lines at large font scale.
- Arabic: dial values, feels like and dew point keep the unit after the number ("١٦°C", not "C°١٦").
- Contrast: the heat-index orange, activity badges, phone-time lines, day dates, font button, Help and About links and the version are at least 4.5:1.
- Header: after a short scroll it shrinks to one row (place name, local time, refresh, search, settings). At the top of the page it opens again.
- Dials: wind gust, pressure, visibility and cloud cover show their minimum and maximum at the arc ends, in both units.
- Night: the sun and the moon show as a faint dot under the horizon, with "Rises in …" (Malagasy "Hiposaka afaka …"). Sun protection shows "Not needed" ("Tsy ilaina") at night.
- Precipitation tile: the chance reads "60% chance" ("Mety 60%").
- Footer: a ▾ after the font name and after the flag opens the full list ("Asehoy daholo"). The place list has a rename button on each favourite. The loading screen shows "Choose a place" after 5 seconds.
- Touch targets: at least 48 dp on Android and 44 px on web, with no change to the look.
- Desktop web: the header shows at the top of the window (the page was 62 px taller than a 900 px window), and the scrollbar is dark.
- Android build: KSP 2.3.13.

## [1.6.20261030023347] - 2026-10-30
- Android, Pirate Weather: the minutely nowcast card now works (the app reads the minutely block of the response, as the web does).
- Pirate Weather: the Android app and the web now give the same values for the same response. The web shows the next 24 hours from now (before: the first 48 hours of the response, which could include past hours), uses the current temperature for today's low and high when there is no daily data, and shows no rain or snow at 0 mm. Android rounds percentages and visibility to the nearest whole number (before: cut down) and shows no value, not 0, when the response has no humidity, cloud cover, visibility, wind direction or UV index. Both use the same weather codes for the Pirate Weather icons: drizzle 53, rain 63, sleet 66 (freezing rain, with the sleet icon), snow 73, thunderstorm 95 and wind 3.
- Alerts: MeteoAlarm alerts stay when the region name is empty or blank (before, Android dropped them all). The web now uses the Android rules: it skips a MeteoAlarm entry with no event name, reads the severity in any case, gives no time for an onset it cannot read, and gives an unknown NHC storm type the title "NHC: {name}".
- Android: the daily humidity from Open-Meteo and the moon's lit percentage round .5 up, as on the web.
- Code: long functions split into smaller ones with the same structure on both platforms (alerts, Pirate Weather, activity indices, weather fetch, forecast cards, detail tiles, sun tile); lookup tables are maps. The screens do not change. Average cyclomatic complexity 2.4; 2 functions above lizard's limits (19 before).
- Web dependencies: `@sveltejs/kit` 3.1.1 and `@sveltejs/adapter-node` 6.0.1.

## [1.6.20261029195135] - 2026-10-29
- Sun tile in a polar night or midnight sun (Open-Meteo sends sunrise and sunset at 00:00 for them): no arc and no times, but "Polar night" or "Midnight sun" in the middle of the tile with the first and last day of the period ("Since" / "Until"), calculated on the device from the sun's declination. In a midnight sun, the sun's azimuth shows under the dates. Malagasy: "Tsy miposaka ny masoandro", "Tsy mody ny masoandro", "Nanomboka", "Vita".
- Day and night icons: a polar night is night all day and a midnight sun is day all day (the main card, the hourly cards, the tiles and the widgets). Before, the 00:00 card showed a sun in a polar night.
- Charts: a line of zeros is at the bottom of the chart, not in the middle (for example the UV at the North Pole).
- Temperature charts (hourly, 10-day, heat index and wind chill tabs): a faint dashed line at freezing (0 °C, 32 °F) when it is inside the chart.

## [1.6.20261029185249] - 2026-10-29
- Place list: the distances are now from the place on screen, not from the device, and the favorites are sorted nearest first from that place. "Current location" stays first and shows its distance when another place is on screen. The place on screen shows no distance.
- Malagasy: "Ny misy antsika" for the current location in the list.
- Store notes: they now cover every version that is not yet on the F-Droid page (since 1.5.28), as a summary, because F-Droid builds only the newest tag. `docs/FDROID.md` describes the rule.

## [1.6.20261029174922] - 2026-10-29
- Places: a tap on the place name, or on the ▾ before it, opens the list of places (current location first, then the favorites). Each favorite shows its distance from the device's last location in parentheses, in km (metric) or miles (imperial). Holding the name still renames a favorite. The ▾ is in the start margin, outside the share image, and shows only when there is a favorite.
- Removed: the swipe between places and the row of dots and arrows under the header (holding an arrow opened the list). The help answer "How do I move between my favorites?" is rewritten in all 8 languages.

## [1.6.20261029165440] - 2026-10-29
- Current conditions: when the wind chill is −28 °C (−18 °F) or colder, the time for bare skin to freeze shows under it, in Environment Canada's bands: 10–30 min (−28 to −39), 5–10 min (−40 to −47), 2–5 min (−48 to −54), under 2 min (−55 or colder). The line wraps to the width of the values above it. Malagasy: "Mitsovaka afaka 5–10 minitra".

## [1.6.20261029135939] - 2026-10-29
- Spanish: missing accents added in 13 strings ("Sensación térmica" in the app and the widget, "ubicación", "aplicación", "última", "más", "precipitación", "Presión", "Índice UV", "Pronóstico por hora", "mín./máx.").

## [1.6.20261029134909] - 2026-10-29
- Heat index: shown only when it is above the air temperature. In very dry heat (for example Kidal, Mali: 37.9 °C at 12 % humidity gives 35 °C), the NWS formula falls below the temperature; then "Feels like" from the weather source shows, and the forecast tab shows a dash for that hour or day. The wind chill is always below the temperature, so it needs no such rule.

## [1.6.20261029133216] - 2026-10-29
- Footer: holding the language flag opens a list of the languages (flag and the language's own name); holding the font button opens a list of the 22 font pairings, each name in its own display font. The list opens next to the button and starts at the current item. A tap still shows the next language or font. The Help and About pages have the same language list.
- Android: the language list no longer opens far above the flag (the standard menu keeps 48 dp from the screen edges).
- Store notes: the newest lines of the earlier notes follow, as far as the 500-character limit allows, because F-Droid builds only the newest tag.

## [1.6.20261029125014] - 2026-10-29
- Sun and moon tiles: the arcs are taller. The day length is in the middle of the sun's arc, in words on two lines ("12 hours" / "45 minutes"; Malagasy "Adiny 12" / "sy" / "45 minitra"), with a singular form for 1. The moon's lit part is in the middle of its arc. The store notes also repeat those of 1.6.20261029031501, because F-Droid builds only the newest tag.

## [1.6.20261029121958] - 2026-10-29
- Day length under the sun's arc: hours and minutes with units ("10h 38m"; Malagasy "10o sy 38m"), in place of "10:38". The store notes also repeat those of 1.6.20261029031501, because F-Droid builds only the newest tag.

## [1.6.20261029031929] - 2026-10-29
- No change in the app. The Malagasy store notes are corrected ("sarina maripàna", "rehefa ilaina"). The store notes repeat those of 1.6.20261029031501, because F-Droid builds only the newest tag.

## [1.6.20261029031501] - 2026-10-29
- Heat index and wind chill, calculated on the device with the NWS formulas. The app shows one felt value next to the temperature (hero card, under the low/high, and the Temperature card): the heat index from 27 °C (80 °F), the wind chill at 10 °C (50 °F) or lower with wind over 4.8 km/h (3 mph), else "Feels like" from the weather source. A thermometer icon with a plus (heat) or a minus (cold) goes with them.
- Hourly and 10-day forecasts: a Heat index or Wind chill tab after the Conditions tab, only when one hour or day has the value. The daily heat index uses the day's high with the humidity from the day's mean dew point; the daily wind chill uses the day's low with the day's wind.
- Values in two units: the secondary unit is under the primary and smaller in the card bottom rows and the hero air quality; chart labels draw it smaller and dimmer.
- Detail cards: the bottom row is centred, and under a dial it sits in the middle of the space that is left.
- Charts: a missing value leaves a gap in the line. Sliding along a chart scrolls the row of cards smoothly.
- Malagasy: "Hafanana tsapa" (heat index) and "Hatsiaka tsapa" (wind chill). In "until" messages, the time and "eo ho eo" wrap together.

## [1.6.20261029003459] - 2026-10-28
- Share images of the hourly and daily forecasts: the items wrap in rows (at most 6 hours or 5 days a line), with only the selected tab. Chat apps shrink large images (Telegram to 1280 px), so the one-line image of 1.6.20261028234248 became unreadable.
- Favorites: when you add a place, the list is sorted by distance from the device's last location, nearest first.

## [1.6.20261028234248] - 2026-10-28
- Hourly and daily forecast charts: slide a finger along a chart to see the hour or the day under it (a line and a label). The row of cards scrolls to that hour or day. Only a horizontal slide starts it, so the page still scrolls up and down.
- The share image of the hourly and daily forecasts shows all the hours or days on one line, with the selected tab and the full chart.
- Web: the weather icons now show in share images.
- Release workflow: the F-Droid build, our build and the VirusTotal scan run at the same time, so a release takes about 8 minutes instead of 17.

## [1.6.20261028191734] - 2026-10-28
- New Moon card next to the Sun card, with the same layout: the moon's arc across the sky, the part lit, moonrise and moonset, the phase (shown as south of the equator when the place is), and the azimuth. The app calculates these on the device (SunCalc formulas).
- Sun card: the length of the day (hours:minutes) under the arc.
- Details cards: each card has its title at the top, one graphic in the middle with the values in it (both units), and labels in a bottom row. The dials of two cards side by side line up.
  - Temperature: a dial from today's low to high; "Feels like" in the bottom row.
  - Precipitation: the weather icon with the amount; the chance of precipitation in the bottom row.
  - Wind: a compass ring with the speed in it; the direction in the bottom row. Wind gust, pressure, humidity, visibility (log scale), air quality and UV: dials. The air quality and UV levels are badges in the dial.
  - Humidity: the dew point as a labelled value in the bottom row.
- New Cloud cover card with a dial, next to Visibility. The precipitation card no longer shows the cloud cover.
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
