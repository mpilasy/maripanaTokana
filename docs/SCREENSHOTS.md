# Store Screenshots

## Overview

The store has 6 phone screenshots per language and 3 TV screenshots.

- Phone: `fastlane/metadata/android/{locale}/images/phoneScreenshots/1.png` to `6.png`, for the 8 locales ar, en-US, es, fr, hi, mg, ne, zh-CN (48 files). PNG, 1080x2424, taken on the Pixel emulator.
- TV: `fastlane/metadata/android/en-US/images/tvScreenshots/1.png` to `3.png`. PNG, 1920x1080, taken on the TV emulator. The other locales have no TV screenshots, so F-Droid shows the en-US ones.
- Compress each file with a 256-colour quantize: `magick in.png -colors 256 -define png:compression-level=9 PNG8:out.png`. A phone file is then about 350-550 KB.

The screenshots reach F-Droid with the next release tag. A push of screenshots does not start a release.

The only other store image is `fastlane/metadata/android/en-US/images/icon.png` (512x512). See [FDROID.md](./FDROID.md) for the release process.

## The Screens

The main page has always-open cards. Its header folds 3 seconds after the last scroll, and the footer hides. Take each phone screenshot with the header folded, so the footer (with the version) is not in it.

Phone:

1. Main card, with the alert above it
2. Hourly forecast
3. Chart view (hourly), opened from the chart button of the hourly card
4. Current conditions
5. Sky and air
6. Sea and activities (the end of the page)

On screens 2, 4 and 5, the card title is just below the folded header.

TV:

1. Home screen
2. Chart at 12:00: OK on the first hour card, then Right until 12:00 shows
3. Current conditions

## Method

Take all screenshots at Ambila Lemaitso, Madagascar, with the F-Droid debug APK. Before you use an emulator, make sure that no other session uses it.

1. Build and install the app:
   ```bash
   ./gradlew assembleFdroidDebug
   adb install -r app/build/outputs/apk/fdroid/debug/app-fdroid-debug.apk
   ```

2. Set a mock GPS location (-18.8430, 49.1535). First allow the shell to mock locations:
   ```bash
   adb shell appops set com.android.shell android:mock_location allow
   ```
   `scripts/set_default_mock_location.sh` is not for this step: it puts the emulator at Antananarivo. Then, for each of `gps`, `network` and `fused`, run `adb shell cmd location providers add-test-provider <provider>` and `adb shell cmd location providers set-test-provider-enabled <provider> true`. Then run `adb shell cmd location providers set-test-provider-location <provider> --location -18.8430,49.1535`.

3. Set the emulator time zone to `Indian/Antananarivo` and the 24-hour clock. Then the app shows no second time, and the times are as in Madagascar. First write down the current values (`adb shell getprop persist.sys.timezone`, `adb shell settings get system time_12_24`):
   ```bash
   adb shell settings put global auto_time_zone 0
   adb shell cmd alarm set-timezone Indian/Antananarivo
   adb shell settings put system time_12_24 24
   ```
   Start the app again after you change the clock.

4. On the phone, turn on System UI demo mode for a clean status bar:
   ```bash
   adb shell settings put global sysui_demo_allowed 1
   adb shell am broadcast -a com.android.systemui.demo -e command enter
   adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 1031
   adb shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false
   adb shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4 -e mobile hide
   adb shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false
   ```

5. Start the app and grant the location permission. Wait until the weather and any real alert have loaded.

6. Phone: take the 6 screenshots in all 8 languages in one run, so the weather is the same in all of them. The script changes the app language with the footer flag, scrolls each card title to its place, and waits for the header to fold:
   ```bash
   python3 scripts/capture_store_screenshots.py OUT_DIR en es fr hi ne zh mg ar
   ```
   Check the files on a contact sheet (`montage`), then compress each file and copy it to its locale folder (`en` goes to `en-US`, `zh` to `zh-CN`).

7. TV: if the TV app has no current location entry, add Ambila Lemaitso with the place search and choose it. Take the 3 screenshots with `adb exec-out screencap -p > N.png`, compress them and copy them to `en-US/images/tvScreenshots/`. Afterwards, choose the place that was active before and remove Ambila Lemaitso.

8. When you finish, restore the real GPS, the time zone, the clock and the status bar:
   ```bash
   adb shell am broadcast -a com.android.systemui.demo -e command exit
   adb shell settings put global sysui_demo_allowed 0
   for p in gps network fused; do adb shell cmd location providers remove-test-provider $p; done
   adb shell appops set com.android.shell android:mock_location default
   adb shell cmd alarm set-timezone <the time zone from step 3>
   adb shell settings put global auto_time_zone 1
   adb shell settings put system time_12_24 <the value from step 3>
   ```

## Checks

- Each locale folder has exactly `1.png` to `6.png`; en-US also has `tvScreenshots/1.png` to `3.png`.
- All files show the same place and alerts.
- Arabic shows the RTL layout. Arabic, Hindi and Nepali show native digits.
- No screenshot shows the footer or the version.

## Old Script

`scripts/capture_screenshots.sh` is out of date. It takes 5 screenshots, changes the system language, and writes to `en_US` and `zh_CN` folders instead of `en-US` and `zh-CN`. Use `scripts/capture_store_screenshots.py`.
