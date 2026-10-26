# Store Screenshots

## Overview

The store has exactly 4 screenshots per language. There are 8 languages, so 32 files in total.

- Location: `fastlane/metadata/android/{locale}/images/phoneScreenshots/1.png` to `4.png`
- Locales: ar, en-US, es, fr, hi, mg, ne, zh-CN
- Format: PNG, 1080x2424, taken on the Pixel emulator
- Size: about 470-620 KB each. Compress each file with a 256-colour quantize.

The screenshots reach F-Droid with the next release tag. A push of screenshots does not start a release.

The only other store image is `fastlane/metadata/android/en-US/images/icon.png` (512x512). See [FDROID.md](./FDROID.md) for the release process.

## The 4 Screens

The layout has always-open cards. There is no accordion.

1. Main card
2. Hourly and 10-day forecasts
3. Current conditions
4. Sun & Air and Activities

## Method

Take all screenshots at Ambila Lemaitso, Madagascar, with the F-Droid debug APK.

1. Build and install the app:
   ```bash
   ./gradlew assembleFdroidDebug
   adb install app/build/outputs/apk/fdroid/debug/*.apk
   ```
   On a phone with more than one user, install for the current user only (`adb install --user 0`).

2. Set a mock GPS location (-18.8430, 49.1535). First allow the shell to mock locations:
   ```bash
   adb shell appops set com.android.shell android:mock_location allow
   ```
   `scripts/set_default_mock_location.sh` is not for this step: it puts the emulator at Antananarivo. Then, for each of `gps`, `network` and `fused`, run `adb shell cmd location providers add-test-provider <provider>` and `adb shell cmd location providers set-test-provider-enabled <provider> true`. Then run `adb shell cmd location providers set-test-provider-location <provider> --location -18.8430,49.1535`.

3. Set the emulator time zone to `Indian/Antananarivo`. Then the app shows no second time. First write down the current time zone (`adb shell getprop persist.sys.timezone`):
   ```bash
   adb shell settings put global auto_time_zone 0
   adb shell cmd alarm set-timezone Indian/Antananarivo
   ```

4. Turn on System UI demo mode for a clean status bar:
   ```bash
   adb shell settings put global sysui_demo_allowed 1
   adb shell am broadcast -a com.android.systemui.demo -e command enter
   adb shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false
   adb shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4 -e mobile hide
   adb shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false
   ```

5. Start the app and grant the location permission. Wait until the weather and any real alert have loaded. Wait for the alert before you take the first language, so all languages match.

6. Change the app language with the footer flag. Do not change the system language.

7. For each language, scroll so that each card title sits just below the pinned header. Then take the 4 screenshots:
   ```bash
   adb exec-out screencap -p > N.png
   ```

8. Compress each file (256-colour quantize) and copy it to the locale folder.

9. When you finish, restore the real GPS, the time zone and the status bar:
   ```bash
   adb shell am broadcast -a com.android.systemui.demo -e command exit
   adb shell settings put global sysui_demo_allowed 0
   for p in gps network fused; do adb shell cmd location providers remove-test-provider $p; done
   adb shell appops set com.android.shell android:mock_location default
   adb shell cmd alarm set-timezone <the time zone from step 3>
   adb shell settings put global auto_time_zone 1
   ```

## Checks

- Each locale folder has exactly `1.png` to `4.png`.
- All 32 files show the same location, time and alerts.
- Arabic shows the RTL layout. Arabic, Hindi and Nepali show native digits.

## Old Script

`scripts/capture_screenshots.sh` is out of date. It takes 5 screenshots, changes the system language, and writes to `en_US` and `zh_CN` folders instead of `en-US` and `zh-CN`. Do not use it as is.
