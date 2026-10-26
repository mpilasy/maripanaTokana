# F-Droid Flavor Testing Guide

This document provides comprehensive testing procedures for the maripànaTokana F-Droid flavor. Most steps also apply to the standard flavor.

## Automated Tests

```bash
./gradlew testFdroidDebugUnitTest testStandardDebugUnitTest   # JVM unit tests
./gradlew lintFdroidDebug                                     # Android lint
```

The unit tests are in `app/src/test/java/orinasa/njarasoa/maripanatokana/` (JUnit 4 and MockK). They cover the domain models (temperature, wind, UV, air quality, precipitation outlook, activities), the location parsers (coordinates, Plus Codes, shared links, place names, scripts), the geocoders, the Open-Meteo mapper, the weather cache, the alert-source failures, the country resolver, the widget formats and `WeatherViewModel`. There are no instrumented tests (`app/src/androidTest` is empty).

CI (`.github/workflows/ci.yml`) runs the i18n parity check, these unit tests, lint (allowed to fail) and the web checks on each push to `main` and on each pull request.

Two scripts test on a device or emulator:
- `scripts/test_fdroid_build.sh`: builds the F-Droid release, checks that it has no Google classes, checks the metadata and fastlane files, checks that two builds are identical, and tests on a device.
- `scripts/test_permission_flow.sh`: 11 permission, location and weather scenarios. Start the emulator with `emulator -avd <name> -wipe-data` first. Set `TEST_DEV` to choose the device. It uses the emulator console to set the location.

## Prerequisites

- Android device or emulator (API 24+)
- F-Droid APK build: `./gradlew assembleFdroidDebug` or `./gradlew assembleFdroidRelease`
- USB debugging enabled (for physical device)

## Installation

### Physical Device
```bash
adb install app/build/outputs/apk/fdroid/debug/app-fdroid-debug.apk
```

### Emulator
```bash
# List the emulators you have (adb devices lists the running ones)
emulator -list-avds

# Start emulator
emulator @<emulator_name>

# Install APK
adb install app/build/outputs/apk/fdroid/debug/app-fdroid-debug.apk
```

On a phone with more than one user, install for the current user only: `adb install --user 0 ...`.

### Mock Location

To test a fixed place on an emulator:
- `scripts/set_default_mock_location.sh` puts all running emulators at Antananarivo (`emu geo fix`).
- For another place, or for the `network` and `fused` providers too, use the test-provider commands in [SCREENSHOTS_README.md](SCREENSHOTS_README.md#method) (step 2), and remove the test providers when you finish (step 9).

## Pre-Installation Verification

### Verify No Google Play Services

Before testing, confirm F-Droid APK has no proprietary dependencies:

```bash
# Extract APK
unzip -q app/build/outputs/apk/fdroid/debug/app-fdroid-debug.apk -d /tmp/fdroid-test

# Check for Play Services Location
find /tmp/fdroid-test -name "*.dex" -exec strings {} \; | grep -i "FusedLocationProviderClient"
# Should return nothing

# Check for Play Services classes
find /tmp/fdroid-test -name "*.dex" -exec strings {} \; | grep -i "PlayServices"
# Should return nothing
```

### Check APK Size
```bash
ls -lh app/build/outputs/apk/fdroid/debug/app-fdroid-debug.apk
# Expected: about 22 MB (debug). The release APK is about 9 MB.
```

## Functional Testing Checklist

### Core Functionality

- [ ] **App Launch**
  - [ ] Opens without crash
  - [ ] Shows loading indicator
  - [ ] Resolves to weather screen or permission request

- [ ] **Location Permission**
  - [ ] Permission dialog appears on first launch
  - [ ] "Grant Permission" button grants location access
  - [ ] App proceeds to fetch weather after permission granted
  - [ ] Does not force-close on permission denial

- [ ] **Location Acquisition**
  - [ ] GPS location obtained within 10 seconds on first run
  - [ ] Falls back to network location if GPS unavailable
  - [ ] Location cached for subsequent app launches
  - [ ] No crashes if location unavailable

- [ ] **Weather Display**
  - [ ] Current weather loads successfully
  - [ ] Temperature, feels-like, min/max visible
  - [ ] Humidity, pressure, dew point displayed
  - [ ] Wind speed and direction shown
  - [ ] UV index displayed
  - [ ] Precipitation (rain/snow) shown when applicable
  - [ ] Visibility displayed
  - [ ] Sunrise/sunset times shown

### Dual-Unit Functionality

- [ ] **Metric Primary**
  - [ ] Temperature shows °C as primary, °F secondary
  - [ ] Wind speed shows m/s as primary, mph secondary
  - [ ] Pressure shows hPa as primary, inHg secondary
  - [ ] Precipitation shows mm as primary, inches secondary
  - [ ] Visibility shows km as primary, mi secondary

- [ ] **Imperial Primary (after toggle)**
  - [ ] Tap temperature to toggle units
  - [ ] All values properly swap primary/secondary
  - [ ] Toggle persists across screen rotations
  - [ ] Toggle persists across app restart

- [ ] **Unit Toggle Interaction**
  - [ ] Tap on temperature swaps units
  - [ ] Tap on feels-like swaps units
  - [ ] Tap on min/max swaps units
  - [ ] Tap on wind swaps units
  - [ ] Tap on pressure swaps units
  - [ ] Tap on dew point swaps units
  - [ ] Tap on visibility swaps units
  - [ ] All values update consistently

### UI & Navigation

- [ ] **Location & Saved Places**
  - [ ] Location header shows city name and subtext (region and country)
  - [ ] Search icon opens the search. You can search by city, postal code, coordinates (decimal or DMS), Plus Code or map link
  - [ ] A picked result shows as a preview for 12 hours. It is not saved
  - [ ] The empty heart shows only for a preview. Tapping it saves the place
  - [ ] Tapping the full heart removes a saved place
  - [ ] Holding the place name opens the rename dialog (Save, Cancel, Reset)
  - [ ] The pin icon (shown for a preview or a saved place) goes back to the current location
  - [ ] Swipe left/right or tap arrows to cycle between Current Location and saved places
  - [ ] Holding an arrow opens the list of places
  - [ ] The grid icon opens the location details: the place, DMS coordinates and Plus Code. Tapping a line opens a map; holding a line copies it
  - [ ] The Latin-letters icon shows for place names in another script, and switches the name
  - [ ] For a place in another time zone, a chip shows its local time

- [ ] **Shared Places**
  - [ ] Sharing a `geo:` link, a map link (Google, OpenStreetMap, Apple) or coordinates as text to the app opens that place as a preview

- [ ] **Alert Banner**
  - [ ] The banner shows when there are alerts, and expands on tap
  - [ ] In Advanced Mode, the app lists the alert sources that failed

- [ ] **Free-Scroll Layout**
  - [ ] All cards sit in one column that scrolls up and down
  - [ ] All cards are always open (no accordion, no collapse or expand control)
  - [ ] Card order: alert banner (when alerts exist), hero card, hourly forecast, 10-day forecast, Current conditions, Sun & Air, Activities
  - [ ] Header stays fixed at the top; footer stays fixed at the bottom
  - [ ] Changing location scrolls the content back to the top

- [ ] **Hero Card**
  - [ ] Meteocons weather icon (day or night) and condition text
  - [ ] Temperature with 1 decimal, with the second unit below it
  - [ ] Low and high temperatures
  - [ ] Side column shows the rain or snow outlook when rain or snow is expected
  - [ ] US AQI and EU AQI show in the side column only when air quality is not good
  - [ ] 4 hour tiles show the hours +0, +4, +8 and +12
  - [ ] Share button shares the card as a PNG image

- [ ] **Nowcast**
  - [ ] "Minutely Nowcast" shows in the Hourly Forecast card only when precipitation is expected in the next 2 hours (Open-Meteo source)
  - [ ] Nowcast is not shown when no precipitation is expected

- [ ] **Hourly Forecast**
  - [ ] Card shows 6 tabs: Conditions, Precipitation, Wind, Pressure, UV, Air quality
  - [ ] Each tab shows a horizontal row of hourly cards and an overview chart below it
  - [ ] Air quality tab shows only when air quality data exists
  - [ ] Horizontal scroll works smoothly

- [ ] **10-Day Forecast**
  - [ ] Card shows the same 6 tabs
  - [ ] Each tab shows a row of daily cards (10 days) and an overview chart below it
  - [ ] Daily cards show day, date, condition, and low and high temperatures

- [ ] **Current Conditions and Sun & Air (Cards)**
  - [ ] Both cards show tiles for their data (temperature, precipitation, wind, gust, pressure, humidity, sun, air quality, UV index, visibility)
  - [ ] Air quality tile opens the detailed pollutant and pollen breakdown
  - [ ] UV tile shows the severity tier

- [ ] **Activities**
  - [ ] Activities card shows activity indices

- [ ] **Share (Cards)**
  - [ ] Each card has a share button that shares the card as a PNG image

- [ ] **Help Page**
  - [ ] "Help" link in the footer opens the Help page
  - [ ] Help page shows sections with questions and answers
  - [ ] Back button returns to the weather screen

- [ ] **About Page**
  - [ ] "About" link in the footer opens the About page
  - [ ] About page shows the app name, version, license, source code link, device information, and data source credits
  - [ ] "Help" link on the About page opens the Help page
  - [ ] Back button returns to the weather screen

### Language Support

Test each of the 8 languages:

- [ ] **English (en-US)**
  - [ ] All text in English
  - [ ] Proper English word order
  - [ ] Correct translations of weather conditions

- [ ] **Malagasy (mg)**
  - [ ] All text in Malagasy
  - [ ] Font renders correctly
  - [ ] Weather terms translated

- [ ] **Arabic (ar)**
  - [ ] Text flows right-to-left
  - [ ] Native Arabic numerals (٠١٢٣٤٥٦٧٨٩)
  - [ ] RTL layout applied to all screens
  - [ ] Icons properly positioned for RTL

- [ ] **Spanish (es)**
  - [ ] All text in Spanish
  - [ ] Accent marks (á, é, í, ó, ú) display correctly
  - [ ] Weather terms translated

- [ ] **French (fr)**
  - [ ] All text in French
  - [ ] Accent marks (é, è, ê, ë, etc.) display correctly
  - [ ] Proper French capitalization

- [ ] **Hindi (hi)**
  - [ ] All text in Devanagari script
  - [ ] Native Hindi numerals (०१२३४५६७८९)
  - [ ] Font renders clearly

- [ ] **Nepali (ne)**
  - [ ] All text in Devanagari script
  - [ ] Native Nepali numerals
  - [ ] Font renders clearly

- [ ] **Chinese Simplified (zh-CN)**
  - [ ] All text in simplified Chinese characters
  - [ ] Standard digits (0-9)
  - [ ] No text overflow
  - [ ] Font renders clearly

**Language Cycling Test:**
- [ ] Flag icon visible at the right end of the footer
- [ ] Tapping the flag cycles through all 8 languages
- [ ] Cycles in this order: Malagasy, Arabic, English, Spanish, French, Hindi, Nepali, Chinese
- [ ] Native digits render for ar/hi/ne; standard digits render for the other languages

### Font Support

Test each of the 22 font pairings:

- [ ] **Font Icon Visible**
  - [ ] Font icon at the left end of the footer is tappable
  - [ ] The first font name of the pair displays next to the icon

- [ ] **Font Cycling**
  - [ ] Tapping cycles through all 22 font pairings
  - [ ] All fonts render clearly
  - [ ] Display font (headlines) changes
  - [ ] Body font (text) changes
  - [ ] Font selection persists across restart

See `app/src/main/java/orinasa/njarasoa/maripanatokana/ui/theme/AppFonts.kt` for the full list of 22 pairings (index 0 = system default).

### Refresh & Pull-to-Refresh

- [ ] **Pull-to-Refresh**
  - [ ] Dragging down shows refresh indicator
  - [ ] Releasing fetches fresh weather
  - [ ] Data updates without full reload
  - [ ] Loading spinner appears during fetch
  - [ ] No crashes on refresh

- [ ] **Auto-Refresh on Resume**
  - [ ] Data older than 30 minutes auto-refreshes on app resume
  - [ ] Fresh data refreshes without bothering user
  - [ ] Loading indicator shown during refresh
  - [ ] No UI freezing during refresh

- [ ] **Tap to Refresh**
  - [ ] Tapping the update time under the place name refreshes the weather

- [ ] **Offline**
  - [ ] Without a connection, the app shows the last forecast for the place and a "refresh failed" line

### Location Updates

- [ ] **Location Caching**
  - [ ] Location used if available (instant load)
  - [ ] Background location update (5-10 sec)
  - [ ] Fresh location doesn't block UI

- [ ] **Location Permission Handling**
  - [ ] App requests permission on first launch
  - [ ] Permission denied → shows an error screen in the app language and, when different, the system language, with "Choose a place" and "Open Settings" buttons
  - [ ] Without location access, a picked place is saved directly
  - [ ] Permission granted → fetches location
  - [ ] Changing permission in Settings → app adapts

### Widget Functionality

Test both widgets (if configured):

- [ ] **4x1 Compact Widget**
  - [ ] Widget appears on home screen
  - [ ] Current temperature visible
  - [ ] Location name shown
  - [ ] Updates automatically
  - [ ] Tapping opens app

- [ ] **4x2 Detailed Widget**
  - [ ] Widget appears on home screen
  - [ ] Current conditions shown
  - [ ] 3-day forecast visible
  - [ ] Updates automatically
  - [ ] Tapping opens app

### Settings Screen (Android)

- [ ] **Access**
  - [ ] Gear icon visible top-right, always (not gated behind Advanced Mode)
  - [ ] Tapping gear opens Settings screen full-screen

- [ ] **Advanced Mode Off**
  - [ ] Settings shows the Advanced mode switch, its description and the About row only

Turn on Advanced Mode for the next sections.

- [ ] **Weather Source**
  - [ ] Open-Meteo selected by default; weather loads without API key
  - [ ] Switching to Pirate Weather shows API key input field
  - [ ] "Test" button with a valid key shows success; weather loads from Pirate Weather
  - [ ] "Test" button with an invalid key shows error; the key is not saved
  - [ ] Source selection persists after closing and reopening Settings

- [ ] **Alert Toggles**
  - [ ] Master alerts toggle disables all alert fetching
  - [ ] Each of the 7 per-source checkboxes (NWS, GDACS, MeteoAlarm, JMA, ECCC, BOM, NHC) can be toggled independently
  - [ ] Disabled sources show no banner even in regions they cover
  - [ ] Toggles persist across app restarts

- [ ] **Geocoding Source**
  - [ ] Nominatim selected by default on the F-Droid build; System Geocoder on the standard build
  - [ ] Both options work (search returns results)
  - [ ] Geocoding source persists after closing Settings

### Advanced Mode

- [ ] **Activation**
  - [ ] Toggling "Advanced mode" on in Settings shows the Weather source, Alerts and Location sections
  - [ ] It stays on after an app restart, until you turn it off

- [ ] **Deactivation**
  - [ ] Toggling "Advanced mode" off hides the sections, clears a place preview and goes back to the current location
  - [ ] Clearing app data resets it

Search and place previews do not need Advanced Mode. A preview expires after 12 hours.

### Performance & Stability

- [ ] **No Crashes**
  - [ ] No ANR (Application Not Responding) errors
  - [ ] No force-closes during any operation
  - [ ] Graceful error handling if network unavailable

- [ ] **Memory Usage**
  - [ ] No excessive memory consumption
  - [ ] No memory leaks on repeated refresh
  - [ ] Smooth scrolling in lists

- [ ] **Network Handling**
  - [ ] Handles no internet connection gracefully
  - [ ] Retries on connection timeout
  - [ ] Shows error message when appropriate
  - [ ] Recovers when connection restored

- [ ] **Screen Rotation**
  - [ ] Landscape mode works (if supported)
  - [ ] Data persists on rotation
  - [ ] Layout adapts properly
  - [ ] No UI elements cut off

### Share Functionality

- [ ] **Share a Card**
  - [ ] Share button visible on each card
  - [ ] Tapping opens share menu
  - [ ] Shares as PNG image
  - [ ] Image contains the place, the card, and a footer with the app name, version and web address
  - [ ] Image has the date and time in its EXIF data, and no location
  - [ ] Can share via WhatsApp, email, etc.

### Accessibility

- [ ] **Text Readability**
  - [ ] All text readable at normal zoom
  - [ ] Sufficient color contrast
  - [ ] No tiny fonts

- [ ] **Touch Targets**
  - [ ] Buttons easily tappable
  - [ ] Minimum 48dp touch target size
  - [ ] No accidental taps from proximity

## Reproducibility Testing

Verify reproducible builds (critical for F-Droid):

```bash
# First build
./gradlew clean assembleFdroidRelease
cp app/build/outputs/apk/fdroid/release/*.apk /tmp/build1.apk
sha256sum /tmp/build1.apk > /tmp/build1.sha

# Second build (no changes)
./gradlew clean assembleFdroidRelease
cp app/build/outputs/apk/fdroid/release/*.apk /tmp/build2.apk
sha256sum /tmp/build2.apk > /tmp/build2.sha

# Compare
diff /tmp/build1.sha /tmp/build2.sha
# Should show no differences (identical checksums)
```

## Device Testing Matrix

Recommend testing on:

| Device Type | API Level | Notes |
|------------|-----------|-------|
| Emulator | 24 (min SDK) | Test minimum support |
| Emulator | 30+ | Test modern Android |
| Physical Phone | 24+ | Real-world performance |
| Tablet (optional) | 24+ | Verify the layout on a wide screen |

## Logging & Debugging

### Enable Logcat Monitoring

```bash
# Clear logs
adb logcat -c

# Follow app logs
adb logcat | grep "NativeLocation\|PlayLocation\|WeatherRepo\|OpenMeteo\|WeatherCache\|CountryResolver"

# Save to file
adb logcat > /tmp/app_logs.txt &
```

### Check for Errors

Watch logcat for these tags (the app logs only in debug builds):
- `PlayLocation` - Should NOT appear in F-Droid
- `NativeLocation` - Should appear in F-Droid
- Exception stack traces - Should be none during normal use

## Performance Metrics

### Startup Time
- [ ] Cold start: < 3 seconds
- [ ] Warm start: < 1 second
- [ ] Location acquisition: < 10 seconds

### Data Fetch Time
- [ ] Weather API call: < 2 seconds
- [ ] Total refresh: < 5 seconds

### Memory
- [ ] Initial load: < 100MB RAM
- [ ] After refresh: No increase in memory

## Screenshot Capture Guidelines

When capturing screenshots for F-Droid:

### Settings
- Use **default** system settings for each language
- **Do NOT** modify:
  - System font size
  - Text scaling
  - Developer options

### Capture Sequence

The store screenshots are 4 per language (`1.png` to `4.png`). See [`docs/SCREENSHOTS_README.md`](SCREENSHOTS_README.md) for the capture steps and the content of each screenshot.

### Technical Specs
- **Format:** PNG or JPEG
- **Dimensions:** 320px - 3840px per side
- **Aspect Ratio:** 4:3 to 16:9
- **Location:** `fastlane/metadata/android/{locale}/images/phoneScreenshots/`
- **Naming:** `1.png`, `2.png`, `3.png`, `4.png`

## Test Report Template

After testing, fill out this report:

```markdown
# F-Droid Testing Report

## Device Information
- Device: [Device Model]
- Android Version: [API Level]
- App Version: [Version Code]

## Test Date
[Date]

## Overall Status
[ ] PASS - All tests passed
[ ] PASS WITH NOTES - Passed with minor issues
[ ] FAIL - Critical issues found

## Test Results Summary
- Functional Tests: [X/Y] passed
- Language Tests: [X/8] passed
- Font Tests: [X/22] passed
- UI Tests: [X/Y] passed

## Issues Found
[List any issues discovered]

## Performance Notes
- Startup time: [X] seconds
- Location acquisition: [X] seconds
- Memory usage: [X] MB

## Screenshots Captured
- [ ] English
- [ ] Malagasy
- [ ] Arabic
- [ ] Spanish
- [ ] French
- [ ] Hindi
- [ ] Nepali
- [ ] Chinese

## Tester Notes
[Any additional observations]
```

## Continuous Integration

GitHub Actions has three workflows:
- `fdroid-build.yml`: on a version tag (`v*`), builds the F-Droid and standard release APKs and makes the GitHub release
- `ci.yml`: on each push to `main` and each pull request, runs the i18n parity check, the unit tests, lint and the web checks
- `version-sync-check.yml`: checks that the Android and web versions match

Check workflow status: https://github.com/mpilasy/maripanaTokana/actions

## Support & Troubleshooting

### App Won't Start
- [ ] Check device has minimum Android 7.0 (API 24)
- [ ] Check permissions are granted
- [ ] Clear app data and reinstall

### Location Not Working
- [ ] Check location permission in Settings
- [ ] Ensure location services enabled on device
- [ ] Check logcat for errors
- [ ] Verify device has GPS/Network location available

### Crashes
- [ ] Check logcat for stack traces
- [ ] Report crash with logcat output
- [ ] Note device, Android version, and steps to reproduce

### Translations Wrong
- [ ] Verify correct language selected
- [ ] Check device system language doesn't override
- [ ] Clear app data and restart

## Next Steps After Testing

1. Verify all tests pass
2. Capture screenshots for all 8 languages
3. Document any found issues
4. Release as described in [FDROID.md](FDROID.md#for-future-versions). F-Droid's bot picks up new tags automatically

See [FDROID.md](FDROID.md) for the release/publishing process.
