# Screenshots for F-Droid

This directory is for storing screenshots used in F-Droid's fastlane metadata.

## Quick Start

### Capturing Screenshots

1. **Install F-Droid flavor APK**
   ```bash
   ./gradlew clean assembleFdroidDebug
   adb install app/build/outputs/apk/fdroid/debug/*.apk
   ```

2. **For each language** (8 total):
   - Change device language in Settings
   - Clear app data
   - Launch app fresh
   - Grant location permission
   - Wait for weather to load

3. **Capture 4-5 screenshots**
   ```bash
   adb shell screencap -p /sdcard/1.png
   adb shell screencap -p /sdcard/2.png
   adb shell screencap -p /sdcard/3.png
   adb shell screencap -p /sdcard/4.png
   adb shell screencap -p /sdcard/5.png
   ```

4. **Pull screenshots to correct directory**
   ```bash
   # For each language
   adb pull /sdcard/1.png fastlane/metadata/android/en-US/images/phoneScreenshots/1.png
   adb pull /sdcard/2.png fastlane/metadata/android/en-US/images/phoneScreenshots/2.png
   # ... etc
   ```

### Screenshot Requirements

- **Minimum:** 4 per language (8 languages = 32 screenshots minimum)
- **Recommended:** 5 per language (40 screenshots total)
- **Format:** PNG or JPEG
- **Dimensions:** 320px - 3840px on any side; aspect ratio 4:3 to 16:9
- **Quality:** Clear, readable, representative of app features

### Suggested Screens to Capture

**Screen 1: Main Weather (Hero Card)**
- Full current conditions visible
- Location name, date, update time
- Temperature prominently displayed
- All detail cards showing

**Screen 2: Details (Temperature/Pressure/Humidity)**
- Scroll down to show detail cards
- Temperature section expanded
- Pressure, humidity, dew point
- Cloud cover visible

**Screen 3: Hourly Forecast**
- Hourly section expanded
- Multiple hourly items visible
- Shows different times with temps
- Horizontal scrollable list

**Screen 4: 10-Day Forecast**
- Weekly forecast expanded
- Shows multiple days
- Each with day, date, weather icon, temp
- Scrollable if needed

**Screen 5 (Optional): Language Feature Demo**
- Show footer with flag icon
- Show font icon in footer
- Demonstrates language/font switching capability

### Directory Structure

```
fastlane/metadata/android/
├── en-US/images/phoneScreenshots/
│   ├── 1.png  (Main weather)
│   ├── 2.png  (Details)
│   ├── 3.png  (Hourly)
│   ├── 4.png  (10-day)
│   └── 5.png  (Optional)
├── mg/images/phoneScreenshots/
│   ├── 1.png
│   ├── 2.png
│   ├── 3.png
│   ├── 4.png
│   └── 5.png
├── ar/images/phoneScreenshots/
│   ├── 1.png  (RTL layout)
│   ├── 2.png
│   ├── 3.png
│   ├── 4.png
│   └── 5.png
├── es/images/phoneScreenshots/
│   ├── 1.png
│   └── ... (4-5 total)
├── fr/images/phoneScreenshots/
│   └── ... (4-5 per language)
├── hi/images/phoneScreenshots/
│   └── ... (native Hindi numerals visible)
├── ne/images/phoneScreenshots/
│   └── ... (native Nepali numerals visible)
└── zh-CN/images/phoneScreenshots/
    └── ... (Chinese characters visible)
```

### Device Settings for Each Language

When capturing, use these system settings:

| Language | Device Language | Notes |
|----------|-----------------|-------|
| English | English (US) | Standard |
| Malagasy | Malagasy (if available) | Or use Settings → Language |
| Arabic | العربية | Check RTL layout |
| Spanish | Español | Or Español (España) |
| French | Français | Or Français (France) |
| Hindi | हिन्दी | Verify Hindi numerals |
| Nepali | नेपाली | Verify Nepali numerals |
| Chinese | 中文 | Simplified Chinese |

### Verification Checklist

Before committing screenshots:

- [ ] 4-5 screenshots captured for each language
- [ ] All 8 languages represented
- [ ] Screenshots are clear and readable
- [ ] Text is legible (not too small)
- [ ] Weather data visible and current
- [ ] No UI glitches or overlays
- [ ] File sizes reasonable (not huge PNG files)
- [ ] Correct directory structure maintained
- [ ] Named sequentially: 1.png, 2.png, etc.

### Automatic GitHub Actions

When you push screenshots to the repository:
1. GitHub Actions workflow triggers on version tags (v*)
2. Builds F-Droid flavor
3. Uploads APK as artifact
4. Creates release with notes

See `.github/workflows/fdroid-build.yml` for details.

### Tools Recommendations

**Screenshot Capture:**
- **Built-in:** ADB screencap (fastest)
- **GUI:** Android Studio Device Explorer
- **Automation:** adb_screenshot_capture.sh (see below)

### Automation Script

Create `capture_screenshots.sh` to automate:

```bash
#!/bin/bash
# Captures screenshots for all languages

LANGUAGES=(
  "en_US:English US"
  "mg:Malagasy"
  "ar:Arabic"
  "es:Spanish"
  "fr:French"
  "hi:Hindi"
  "ne:Nepali"
  "zh_CN:Chinese"
)

for lang_info in "${LANGUAGES[@]}"; do
  IFS=':' read -r lang_code lang_name <<< "$lang_info"

  echo "Capturing screenshots for $lang_name ($lang_code)..."

  # Change device language (requires developer mode)
  # adb shell settings put system system_locale $lang_code

  # Clear app data
  adb shell pm clear orinasa.njarasoa.maripanatokana

  # Launch app
  adb shell am start -n orinasa.njarasoa.maripanatokana/.MainActivity

  sleep 5  # Wait for app to load

  # Capture 5 screenshots
  for i in {1..5}; do
    adb shell screencap -p /sdcard/screen_$i.png
    adb pull /sdcard/screen_$i.png \
      "fastlane/metadata/android/${lang_code}/images/phoneScreenshots/$i.png"
    echo "  Captured screen $i"
  done

  echo "✓ Completed $lang_name"
done

echo "All screenshots captured!"
```

### Checking Screenshot Status

```bash
# Count total screenshots
find fastlane/metadata/android -name "*.png" | wc -l

# Check per language
for lang in en-US mg ar es fr hi ne zh-CN; do
  count=$(find "fastlane/metadata/android/$lang" -name "*.png" 2>/dev/null | wc -l)
  echo "$lang: $count screenshots"
done

# Expected output:
# en-US: 5 screenshots
# mg: 5 screenshots
# ar: 5 screenshots
# ... etc (40 total)
```

## Status

- [ ] Screenshots for en-US (English)
- [ ] Screenshots for mg (Malagasy)
- [ ] Screenshots for ar (Arabic)
- [ ] Screenshots for es (Spanish)
- [ ] Screenshots for fr (French)
- [ ] Screenshots for hi (Hindi)
- [ ] Screenshots for ne (Nepali)
- [ ] Screenshots for zh-CN (Chinese)

## F-Droid Notes

- Screenshots are displayed on the app's F-Droid page
- Used to showcase app features to potential users
- Quality screenshots improve download rates
- Multiple languages help international users

See `docs/FDROID_SUBMISSION_GUIDE.md` for complete submission instructions.
