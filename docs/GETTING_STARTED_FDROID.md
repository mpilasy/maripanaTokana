# Getting Started with F-Droid Deployment

Welcome! This guide will get you from here to F-Droid listing.

## What Has Been Done ✅

- [x] Location provider abstraction (Play Services + Native)
- [x] Build flavors configured (standard + F-Droid)
- [x] All 8 language metadata created
- [x] F-Droid submission YAML prepared
- [x] GitHub Actions CI configured
- [x] Comprehensive documentation written
- [x] Release tagged (v1.0)

## What You Need to Do 📋

### 1. Capture Screenshots (1-2 hours)
**File:** `SCREENSHOTS_README.md`

```bash
# Install the app on device/emulator
./gradlew clean assembleFdroidDebug
adb install app/build/outputs/apk/fdroid/debug/*.apk

# Capture 4-5 screenshots for each of 8 languages
# Save to: fastlane/metadata/android/{language}/images/phoneScreenshots/
```

**Estimated effort:** 1-2 hours to capture all 40 screenshots

### 2. Test the F-Droid Flavor (1-2 hours)
**File:** `docs/TESTING_GUIDE.md`

Test on device/emulator:
- Location permission works
- Weather loads correctly
- All 8 languages render
- Dual-unit toggle works
- Widgets function

**Estimated effort:** 1-2 hours for thorough testing

### 3. Submit to F-Droid (30 minutes)
**File:** `docs/FDROID_SUBMISSION_GUIDE.md`

Step-by-step instructions:
1. Fork F-Droid Data repository
2. Add metadata to your fork
3. Create merge request
4. Wait for F-Droid team review (1-4 weeks)

**Estimated effort:** 30 minutes to submit; 1-4 weeks for review

## Quick Links

| Document | Purpose |
|----------|---------|
| `README.md` | Main project README (updated) |
| `docs/FDROID.md` | Detailed F-Droid architecture guide |
| `docs/TESTING_GUIDE.md` | Complete testing procedures |
| `docs/FDROID_SUBMISSION_GUIDE.md` | Step-by-step submission instructions |
| `FDROID_IMPLEMENTATION_SUMMARY.md` | What was implemented |
| `SCREENSHOTS_README.md` | How to capture screenshots |
| `docs/GETTING_STARTED_FDROID.md` | This file |

## Directory Structure

```
maripanaTokana/
├── app/
│   ├── build.gradle.kts (updated with flavors)
│   ├── src/
│   │   ├── main/                          # Shared code
│   │   ├── standard/                      # Standard flavor (Play Services)
│   │   └── fdroid/                        # F-Droid flavor (native APIs)
│   └── ...
├── .github/
│   └── workflows/
│       └── fdroid-build.yml               # Automated builds
├── metadata/
│   └── orinasa.njarasoa.maripanatokana.yml  # F-Droid YAML
├── fastlane/metadata/android/             # Localized metadata
│   ├── en-US/
│   ├── mg/, ar/, es/, fr/, hi/, ne/, zh-CN/
│   └── ... (each with title, description, changelog, screenshots/)
├── docs/
│   ├── FDROID.md                          # Architecture guide
│   ├── TESTING_GUIDE.md                   # Test procedures
│   ├── FDROID_SUBMISSION_GUIDE.md         # Submission steps
│   └── GETTING_STARTED_FDROID.md          # This file
├── SCREENSHOTS_README.md                  # Screenshot guide
├── FDROID_IMPLEMENTATION_SUMMARY.md       # Implementation details
└── README.md                              # Main README (updated)
```

## Build Commands

### Build F-Droid Flavor
```bash
# Debug (for testing)
./gradlew clean assembleFdroidDebug
# Output: app/build/outputs/apk/fdroid/debug/app-fdroid-debug.apk

# Release (for submission)
./gradlew clean assembleFdroidRelease
# Output: app/build/outputs/apk/fdroid/release/app-fdroid-release.apk
```

### Build Standard Flavor (for reference)
```bash
# Standard flavor with Google Play Services
./gradlew clean assembleStandardRelease
# Output: app/build/outputs/apk/standard/release/app-standard-release.apk
```

## Verification

### Verify F-Droid APK
```bash
# Check no Play Services in F-Droid APK
unzip -q app/build/outputs/apk/fdroid/release/*.apk -d /tmp/verify
find /tmp/verify -name "*.dex" -exec strings {} \; | grep -i "PlayServices"
# Should return nothing ✓

# Check file size
ls -lh app/build/outputs/apk/fdroid/release/*.apk
# Should be ~15-16MB
```

### Test Reproducibility
```bash
# Build twice and compare checksums
./gradlew clean assembleFdroidRelease
sha256sum app/build/outputs/apk/fdroid/release/*.apk > /tmp/build1.sha

./gradlew clean assembleFdroidRelease
sha256sum app/build/outputs/apk/fdroid/release/*.apk > /tmp/build2.sha

diff /tmp/build1.sha /tmp/build2.sha
# Should show identical (both builds produce same APK)
```

## Implementation Summary

### Dual-Flavor Architecture

**Standard Flavor:**
- Uses Google Play Services (`PlayServicesLocationProvider`)
- Accompanist Permissions UI
- Can be distributed on Google Play

**F-Droid Flavor:**
- Uses native Android LocationManager (`NativeLocationProvider`)
- Simplified permissions handling
- No proprietary dependencies
- Ready for F-Droid

### Key Files Created

**Location Services:**
- `app/src/main/java/.../LocationProvider.kt` - Interface
- `app/src/standard/java/.../PlayServicesLocationProvider.kt` - Play Services impl
- `app/src/fdroid/java/.../NativeLocationProvider.kt` - Native impl

**Build Configuration:**
- `app/build.gradle.kts` - Flavor configuration
- `app/src/standard/java/.../di/LocationModule.kt` - Standard DI
- `app/src/fdroid/java/.../di/LocationModule.kt` - F-Droid DI

**UI (Flavor-Specific):**
- `app/src/standard/java/.../WeatherScreen.kt` - With Accompanist
- `app/src/fdroid/java/.../WeatherScreen.kt` - Simplified
- `app/src/standard/java/.../WidgetWeatherFetcher.kt` - Play Services
- `app/src/fdroid/java/.../WidgetWeatherFetcher.kt` - Native

**Metadata:**
- `metadata/orinasa.njarasoa.maripanatokana.yml` - F-Droid config
- `fastlane/metadata/android/{locale}/*` - All 8 languages

## Timeline

| Phase | Time | Status |
|-------|------|--------|
| **Architecture** | Done | ✅ |
| **Build Setup** | Done | ✅ |
| **Metadata** | Done | ✅ |
| **Documentation** | Done | ✅ |
| **Screenshots** | ~2 hrs | ⏳ You are here |
| **Testing** | ~2 hrs | ⏳ |
| **Submission** | ~30 min | ⏳ |
| **F-Droid Review** | 1-4 weeks | ⏳ |

**Total effort remaining:** ~4-5 hours + F-Droid review wait

## Next Steps

### Immediate (This Session)
1. Review `docs/TESTING_GUIDE.md`
2. Install F-Droid APK on device
3. Perform quick functionality test
4. Verify app works without crashes

### Short Term (Today/Tomorrow)
1. Capture 4-5 screenshots per language
2. Save to `fastlane/metadata/android/{lang}/images/phoneScreenshots/`
3. Commit to GitHub
4. Complete comprehensive testing

### Medium Term (This Week)
1. Review `docs/FDROID_SUBMISSION_GUIDE.md`
2. Fork F-Droid Data repository
3. Create merge request
4. Respond to any F-Droid team feedback

## Common Issues & Solutions

### "App won't start"
- Ensure Android 7.0+ (API 24)
- Check location permission
- See TESTING_GUIDE.md troubleshooting

### "No location acquired"
- Grant location permission in Settings
- Enable device location services
- Use network/GPS location
- See TESTING_GUIDE.md location section

### "Screenshots placement wrong"
- Must be in: `fastlane/metadata/android/{locale}/images/phoneScreenshots/`
- Must be named: `1.png`, `2.png`, `3.png`, `4.png`, `5.png`
- See SCREENSHOTS_README.md for details

### "YAML syntax error"
- Use yamllint to validate
- Check quotes, brackets, indentation
- See FDROID_SUBMISSION_GUIDE.md metadata section

## Resources

### Documentation
- **Architecture:** `docs/FDROID.md`
- **Testing:** `docs/TESTING_GUIDE.md`
- **Submission:** `docs/FDROID_SUBMISSION_GUIDE.md`
- **Screenshots:** `SCREENSHOTS_README.md`

### External References
- [F-Droid Submitting Guide](https://f-droid.org/docs/Submitting_to_F-Droid_Quick_Start_Guide/)
- [F-Droid Build Metadata](https://f-droid.org/docs/Build_Metadata_Reference/)
- [F-Droid Screenshots Guide](https://f-droid.org/docs/All_About_Descriptions_Graphics_and_Screenshots/)

### GitHub
- **Repo:** https://github.com/mpilasy/maripanaTokana
- **Issues:** https://github.com/mpilasy/maripanaTokana/issues
- **Actions:** https://github.com/mpilasy/maripanaTokana/actions

## Success Criteria

When complete, you'll have:
- ✅ F-Droid flavor that builds successfully
- ✅ No Google Play Services in F-Droid APK
- ✅ Working location services (native Android)
- ✅ All 8 language metadata complete
- ✅ Screenshots captured for all 8 languages
- ✅ Submission YAML valid and ready
- ✅ App listed on F-Droid repository
- ✅ Accessible to 100k+ F-Droid users

## Questions?

1. **About F-Droid?** See `docs/FDROID_SUBMISSION_GUIDE.md`
2. **About testing?** See `docs/TESTING_GUIDE.md`
3. **About architecture?** See `docs/FDROID.md`
4. **About screenshots?** See `SCREENSHOTS_README.md`
5. **About implementation?** See `FDROID_IMPLEMENTATION_SUMMARY.md`

## You're Ready! 🚀

Everything is set up. Now you just need to:
1. Capture screenshots
2. Test on device
3. Submit to F-Droid

Good luck! Your open-source weather app is about to reach a whole new audience.
