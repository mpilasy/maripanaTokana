#!/usr/bin/env bash
# Scripted end-to-end test of the F-Droid location-permission flow.
# Run on a single connected device/emulator. Builds the release APK,
# signs it with the local debug key (so it can be installed without the
# release keystore), and exercises:
#   1. Fresh launch shows the system permission dialog.
#   2. Tapping "Don't allow" shows the in-app PermissionRequired UI.
#   3. Tapping the "Grant Permission" button re-launches the system dialog.
#   4. Tapping "While using the app" loads the weather screen.
#
# Exits non-zero if any step fails. Re-run after any change touching
# permission, location, or weather flow before claiming a fix.
#
# Requires: adb, $ANDROID_HOME, python3, single device attached.

set -euo pipefail

PKG=orinasa.njarasoa.maripanatokana

# Prefer an emulator (avoids messing with the dev's phone if both are connected).
# Override with TEST_DEV=<serial> to force a specific device.
DEV=${TEST_DEV:-}
if [[ -z "$DEV" ]]; then
    DEV=$(adb devices | awk 'NR>1 && $2=="device"{print $1}' | grep -E '^emulator-' | head -1)
fi
if [[ -z "$DEV" ]]; then
    DEV=$(adb devices | awk 'NR>1 && $2=="device"{print $1}' | head -1)
fi
if [[ -z "$DEV" ]]; then
    echo "ERROR: no adb device found"; exit 1
fi
echo "Using device: $DEV"

JAVA_HOME_AS=${JAVA_HOME_AS:-/home/tahiry/.local/share/JetBrains/Toolbox/apps/android-studio/jbr}
JAVA_HOME=$JAVA_HOME_AS ./gradlew :app:assembleFdroidRelease \
    -Porg.gradle.java.installations.paths=$JAVA_HOME_AS -q

APKSIGNER=$(ls "$ANDROID_HOME"/build-tools/*/apksigner 2>/dev/null | sort -V | tail -1)
SIGNED=/tmp/fdroid-release-debugsigned.apk
"$APKSIGNER" sign \
    --ks "$HOME/.android/debug.keystore" --ks-pass pass:android \
    --key-pass pass:android --ks-key-alias androiddebugkey \
    --out "$SIGNED" \
    app/build/outputs/apk/fdroid/release/app-fdroid-release-unsigned.apk 2>&1 | grep -v WARNING

adb -s "$DEV" uninstall "$PKG" >/dev/null 2>&1 || true
adb -s "$DEV" install "$SIGNED" >/dev/null
adb -s "$DEV" shell pm revoke "$PKG" android.permission.ACCESS_FINE_LOCATION || true
adb -s "$DEV" shell pm revoke "$PKG" android.permission.ACCESS_COARSE_LOCATION || true
adb -s "$DEV" shell am force-stop "$PKG"

assert_focus() {
    local expected_substring=$1 step=$2
    local focus
    focus=$(adb -s "$DEV" shell dumpsys window | grep mCurrentFocus | head -1)
    if [[ "$focus" != *"$expected_substring"* ]]; then
        echo "FAIL ($step): focus='$focus' did not contain '$expected_substring'"
        exit 1
    fi
    echo "PASS ($step): $expected_substring"
}

tap_text() {
    local target=$1
    adb -s "$DEV" shell uiautomator dump /sdcard/ui.xml >/dev/null
    adb -s "$DEV" pull /sdcard/ui.xml /tmp/ui.xml >/dev/null
    python3 - "$target" <<'PY'
import re, subprocess, sys, os
target = sys.argv[1]
xml = open('/tmp/ui.xml').read()
for m in re.finditer(r'text="([^"]+)"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml):
    t = m.group(1)
    if t == target or target in t:
        x1,y1,x2,y2 = map(int, m.groups()[1:])
        cx, cy = (x1+x2)//2, (y1+y2)//2
        subprocess.run(['adb','-s',os.environ['DEV'],'shell','input','tap',str(cx),str(cy)], check=True)
        print(f"  tapped '{t}' at ({cx},{cy})")
        sys.exit(0)
sys.exit(f"button '{target}' not found")
PY
}

export DEV
adb -s "$DEV" shell am start -W -n "$PKG/.MainActivity" >/dev/null
sleep 3
assert_focus "GrantPermissionsActivity" "Step 1: dialog auto-shown on launch"

tap_text "Don" # "Don't allow"
sleep 2
assert_focus "$PKG/$PKG.MainActivity" "Step 2: returned to app after deny"

tap_text "Grant Permission"
sleep 2
assert_focus "GrantPermissionsActivity" "Step 3: button re-launches dialog"

tap_text "While using the app"
sleep 4
assert_focus "$PKG/$PKG.MainActivity" "Step 4: returned to app after grant"

# Allow up to 15s for weather to load (one of the hour rows must appear)
for i in {1..15}; do
    adb -s "$DEV" shell uiautomator dump /sdcard/ui.xml >/dev/null
    adb -s "$DEV" pull /sdcard/ui.xml /tmp/ui.xml >/dev/null
    if grep -qE 'text="[0-9]{1,2}:00"' /tmp/ui.xml; then
        echo "PASS (Step 5: weather loaded after $i s)"
        echo "ALL CHECKS PASSED"
        exit 0
    fi
    sleep 1
done
echo "FAIL (Step 5): weather did not load within 15s"
exit 1
