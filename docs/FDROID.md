# F-Droid Deployment Guide for maripànaTokana

## Overview

This document describes how maripànaTokana is configured for F-Droid deployment, including the build process, flavors, and submission requirements.

## Build Flavors

The app has two build flavors configured in `app/build.gradle.kts`:

### Standard Flavor
- Uses Google Play Services for location (`libs.play.services.location`)
- Includes Accompanist Permissions library for permissions UI
- Uses `PlayServicesLocationProvider` for enhanced location accuracy
- Optimized for Google Play distribution
- Built with: `./gradlew assembleStandardRelease`

### F-Droid Flavor
- Uses only native Android `LocationManager` (no Google Play Services)
- Uses simplified permissions handling without Accompanist
- Uses `NativeLocationProvider` for location services
- Fully compatible with F-Droid's requirements
- Built with: `./gradlew assembleFdroidRelease`

## Location Provider Architecture

### LocationProvider Interface
Located at: `app/src/main/java/orinasa/njarasoa/maripanatokana/data/location/LocationProvider.kt`

Provides two methods:
- `getLastLocation()`: Returns cached location from device
- `getFreshLocation()`: Requests fresh location update with 30-second timeout

### Implementations

#### PlayServicesLocationProvider (Standard)
Location: `app/src/standard/java/.../PlayServicesLocationProvider.kt`
- Uses FusedLocationProviderClient from Google Play Services
- Higher accuracy, better battery efficiency
- Uses PRIORITY_BALANCED_POWER_ACCURACY

#### NativeLocationProvider (F-Droid)
Location: `app/src/fdroid/java/.../NativeLocationProvider.kt`
- Uses Android's built-in LocationManager
- Supports both GPS_PROVIDER and NETWORK_PROVIDER
- Selects most accurate available location
- 30-second timeout for fresh location requests

## Dependency Injection

Flavor-specific DI modules provide the appropriate LocationProvider:

### Standard Flavor Module
Location: `app/src/standard/java/.../di/LocationModule.kt`
- Provides `FusedLocationProviderClient`
- Provides `PlayServicesLocationProvider` as `LocationProvider`

### F-Droid Flavor Module
Location: `app/src/fdroid/java/.../di/LocationModule.kt`
- Provides `NativeLocationProvider` as `LocationProvider`
- No Play Services dependencies required

## UI Adaptations

### WeatherScreen.kt
Flavor-specific versions exist to handle permissions differently:

**Standard Flavor** (`app/src/standard/.../WeatherScreen.kt`)
- Uses Accompanist Permissions library
- Shows permission request UI with `rememberMultiplePermissionsState`
- Smooth permission handling integration

**F-Droid Flavor** (`app/src/fdroid/.../WeatherScreen.kt`)
- Simplified permissions handling
- Automatically attempts location fetch
- Falls back to cached location from widgets if permissions denied

### WidgetWeatherFetcher
Flavor-specific widget location fetcher implementations:

**Standard Flavor** (`app/src/standard/.../widget/WidgetWeatherFetcher.kt`)
- Uses FusedLocationProviderClient directly

**F-Droid Flavor** (`app/src/fdroid/.../widget/WidgetWeatherFetcher.kt`)
- Uses native LocationManager
- Same functionality, F-Droid compatible

## Build Configuration

### Reproducible Builds

The F-Droid flavor is configured for reproducible builds:

1. **BUILD_TIME buildConfigField**
   - Standard flavor: Current timestamp (for Google Play version tracking)
   - F-Droid flavor: "reproducible" string (for byte-identical builds)

2. **PNG Crunching**
   - Disabled in release builds for reproducibility

3. **Consistent Dependencies**
   - Version-locked in `libs.versions.toml`
   - F-Droid flavor excludes Play Services

## Metadata

### Fastlane Structure
Located at: `fastlane/metadata/android/`

Directory structure for 8 languages:
```
fastlane/metadata/android/
├── en-US/
│   ├── title.txt
│   ├── short_description.txt
│   ├── full_description.txt
│   ├── changelogs/
│   │   └── 1.txt
│   └── images/
│       └── phoneScreenshots/
├── mg/  (Malagasy)
├── ar/  (Arabic)
├── es/  (Spanish)
├── fr/  (French)
├── hi/  (Hindi)
├── ne/  (Nepali)
└── zh-CN/ (Chinese Simplified)
```

Each language directory contains:
- `title.txt`: App name (max 50 chars)
- `short_description.txt`: Short description (max 80 chars)
- `full_description.txt`: Detailed description with features
- `changelogs/1.txt`: Version 1.0 changelog
- `images/phoneScreenshots/`: 4-5 screenshots per language

### F-Droid YAML Metadata
Located at: `metadata/orinasa.njarasoa.maripanatokana.yml`

Contains:
- App metadata (license, author, URLs)
- Build instructions (gradle flavor, prebuild steps)
- Version tracking configuration
- Maintenance notes about i18n generation

## Building for F-Droid

### Local Build

```bash
# Clean build
./gradlew clean

# Generate i18n strings
node shared/i18n/generate-android-strings.js

# Build F-Droid flavor
./gradlew assembleFdroidRelease

# Output location
ls -lh app/build/outputs/apk/fdroid/release/
```

### Verify No Google Dependencies

```bash
# Extract and check for Play Services references
unzip -q app/build/outputs/apk/fdroid/release/app-fdroid-release.apk -d /tmp/fdroid-apk
find /tmp/fdroid-apk -name "*.dex" -exec strings {} \; | grep -i "google.android.gms"
# Should return nothing
```

### Test Reproducibility

```bash
# First build
./gradlew clean assembleFdroidRelease
cp app/build/outputs/apk/fdroid/release/*.apk /tmp/build1.apk

# Second build (without any changes)
./gradlew clean assembleFdroidRelease
cp app/build/outputs/apk/fdroid/release/*.apk /tmp/build2.apk

# Compare checksums
sha256sum /tmp/build1.apk /tmp/build2.apk
# Hashes should be identical
```

## Submission Process

1. **Prepare Release**
   - Tag version: `git tag -a v1.0 -m "Release 1.0 for F-Droid"`
   - Push tag: `git push origin v1.0`

2. **Fork F-Droid Data Repository**
   - Fork: https://gitlab.com/fdroid/fdroiddata
   - Clone your fork

3. **Add App Metadata**
   - Copy `metadata/orinasa.njarasoa.maripanatokana.yml` to fdroiddata repo
   - Validate YAML syntax

4. **Add Fastlane Metadata**
   - Ensure `fastlane/metadata/android/` structure is in project
   - F-Droid server will pick this up automatically

5. **Create Merge Request**
   ```bash
   git checkout -b add-maripanatokana
   git add metadata/orinasa.njarasoa.maripanatokana.yml
   git commit -m "New app: maripànaTokana"
   git push origin add-maripanatokana
   ```
   - Create MR on https://gitlab.com/fdroid/fdroiddata

6. **F-Droid Review**
   - F-Droid team reviews the merge request
   - May request changes to metadata or build configuration
   - Timeline: 1-4 weeks typically

## Testing Locally

### Device Installation

```bash
# Install F-Droid flavor
adb install app/build/outputs/apk/fdroid/release/*.apk

# Test functionality
# - Grant location permission when prompted
# - Wait for location acquisition (5-10 seconds)
# - Verify weather data loads
# - Test language cycling
# - Test font cycling
# - Test unit toggling
# - Test widgets on home screen
```

### Critical Tests

- [ ] App launches without crash
- [ ] Location permission request appears
- [ ] Location is acquired successfully
- [ ] Weather data loads and displays correctly
- [ ] Current conditions show dual units
- [ ] Hourly forecast scrolls smoothly
- [ ] 10-day forecast displays all days
- [ ] Unit toggle works (primary/secondary swap)
- [ ] Language cycling works (8 languages)
- [ ] Font cycling works (16 pairings)
- [ ] Pull-to-refresh updates weather
- [ ] Widgets appear on home screen
- [ ] Widgets update automatically
- [ ] Background widget refresh works
- [ ] RTL layout works for Arabic
- [ ] Native digits render for Arabic, Hindi, Nepali

### No Google Play Services Verification

```bash
adb shell pm list packages | grep gms
# Should return empty
```

## Maintenance and Updates

### For Future Versions

1. Update `versionCode` and `versionName` in `app/build.gradle.kts`
2. Create corresponding changelog file: `fastlane/metadata/android/*/changelogs/{versionCode}.txt`
3. Update `metadata/orinasa.njarasoa.maripanatokana.yml` with new version
4. Create git tag: `git tag -a v{version} -m "Release {version}"`
5. Push tag and create new F-Droid merge request

### Screenshots

Screenshots should be captured for each supported language:
- Minimum 2-4 per language
- Dimensions: 320px - 3840px on any side
- Aspect ratio: 4:3 to 16:9
- Format: PNG or JPEG
- Save to: `fastlane/metadata/android/{locale}/images/phoneScreenshots/`

### Localization Updates

If i18n translations are updated:
1. Update JSON files in `shared/i18n/locales/`
2. Run: `node shared/i18n/generate-android-strings.js`
3. The build process will automatically regenerate Android strings

## References

- [F-Droid Build Metadata Reference](https://f-droid.org/docs/Build_Metadata_Reference/)
- [F-Droid Submitting Quick Start](https://f-droid.org/docs/Submitting_to_F-Droid_Quick_Start_Guide/)
- [F-Droid Reproducible Builds](https://f-droid.org/en/docs/Reproducible_Builds/)
- [F-Droid Screenshots & Descriptions](https://f-droid.org/docs/All_About_Descriptions_Graphics_and_Screenshots/)
- [Migrate from Play Services Location](https://fobo66.dev/post/play-services-location-migration/)

## Support

For issues or questions about F-Droid deployment:
1. Check F-Droid documentation
2. Review similar apps in F-Droid Data repository
3. Ask questions on F-Droid's communication channels
