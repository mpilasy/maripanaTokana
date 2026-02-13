# F-Droid Flavor Testing Guide

This document provides comprehensive testing procedures for the maripànaTokana F-Droid flavor.

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
# List running emulators
emulator -list-avds

# Start emulator
emulator @<emulator_name>

# Install APK
adb install app/build/outputs/apk/fdroid/debug/app-fdroid-debug.apk
```

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
# Expected: ~15-16MB
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

- [ ] **Weather Screen Layout**
  - [ ] Hero card displays prominently
  - [ ] Location name visible at top
  - [ ] Date and update time shown
  - [ ] All detail cards visible and readable
  - [ ] No text truncation or overflow

- [ ] **Hourly Forecast**
  - [ ] Hourly forecast section expands/collapses
  - [ ] Each hourly item shows: time, weather icon, temperature
  - [ ] Horizontal scroll works smoothly
  - [ ] Temperature shows dual units

- [ ] **10-Day Forecast**
  - [ ] 10-day section expands/collapses
  - [ ] Each day shows: day name, date, weather description, temp
  - [ ] Scrollable if exceeds screen height
  - [ ] Temperature shows dual units
  - [ ] Precipitation probability shown

- [ ] **Current Conditions (Collapsible)**
  - [ ] Section expands/collapses
  - [ ] All detail cards display properly
  - [ ] Cards are organized clearly
  - [ ] Values readable with good contrast

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
  - [ ] Native Chinese numerals (０１２３４５６７８９) or Arabic
  - [ ] No text overflow
  - [ ] Font renders clearly

**Language Cycling Test:**
- [ ] Flag icon visible in footer
- [ ] Tapping flag cycles through all 8 languages
- [ ] Cycles in correct order
- [ ] Native digits render for ar/hi/ne in each language

### Font Support

Test each of the 16 font pairings:

- [ ] **Font Icon Visible**
  - [ ] Font icon in footer is clickable
  - [ ] Font name displays next to icon

- [ ] **Font Cycling**
  - [ ] Tapping cycles through all 16 font pairings
  - [ ] All fonts render clearly
  - [ ] Display font (headlines) changes
  - [ ] Body font (text) changes
  - [ ] Font selection persists across restart

**Font Pairings to Test:**
1. Roboto + Lora
2. Inter + Merriweather
3. (and 14 more...)

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

### Location Updates

- [ ] **Location Caching**
  - [ ] Location used if available (instant load)
  - [ ] Background location update (5-10 sec)
  - [ ] Fresh location doesn't block UI

- [ ] **Location Permission Handling**
  - [ ] App requests permission on first launch
  - [ ] Permission denied → shows error message
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

- [ ] **Share Hero Card**
  - [ ] Share button visible on hero card
  - [ ] Tapping opens share menu
  - [ ] Shares as PNG image
  - [ ] Image contains card content
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
| Tablet (optional) | 24+ | Verify portrait-only design |

## Logging & Debugging

### Enable Logcat Monitoring

```bash
# Clear logs
adb logcat -c

# Follow app logs
adb logcat | grep "maripanaTokana\|LocationProvider\|WeatherScreen"

# Save to file
adb logcat > /tmp/app_logs.txt &
```

### Check for Errors

Watch logcat for:
- `PlayServicesLocationProvider` - Should NOT appear in F-Droid
- `FusedLocationProviderClient` - Should NOT appear in F-Droid
- `NativeLocationProvider` - Should appear
- `LocationManager` - Should appear
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

### Capture Sequence (4-5 screens per language)

**Screen 1: Main Weather Screen**
- Full current conditions visible
- Location name clear
- Date/time visible
- All weather metrics displayed

**Screen 2: Detail Cards (scroll down)**
- Pressure, humidity, dew point
- Wind, wind gust visible
- Sunrise/sunset times shown

**Screen 3: Hourly Forecast (expanded)**
- Hourly forecast section open
- Multiple hourly items visible
- Scrollable content shown

**Screen 4: 10-Day Forecast (expanded)**
- Weekly forecast section visible
- Several days of forecast shown
- Temperature min/max visible

**Screen 5 (optional): Language/Font Demo**
- Flag and font icons visible in footer
- Shows localization capabilities

### Technical Specs
- **Format:** PNG or JPEG
- **Dimensions:** 320px - 3840px per side
- **Aspect Ratio:** 4:3 to 16:9
- **Location:** `fastlane/metadata/android/{locale}/images/phoneScreenshots/`
- **Naming:** `1.png`, `2.png`, `3.png`, `4.png`, `5.png`

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
- Font Tests: [X/16] passed
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

GitHub Actions automatically builds F-Droid flavor on:
- Push to version tags (v*)
- Manual workflow dispatch

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

1. ✅ Verify all tests pass
2. ✅ Capture screenshots for all 8 languages
3. ✅ Document any found issues
4. ✅ Prepare F-Droid submission:
   - Fork F-Droid Data repository
   - Add metadata/orinasa.njarasoa.maripanatokana.yml
   - Create merge request
5. ✅ Respond to F-Droid team feedback

See [FDROID_SUBMISSION_GUIDE.md](FDROID_SUBMISSION_GUIDE.md) for submission instructions.
