#!/usr/bin/env bash
set -euo pipefail

APP_ID="orinasa.njarasoa.maripanatokana"
MAIN_ACTIVITY="${APP_ID}/.MainActivity"

# 1. Detect attached phone (non-TV device)
TARGET_DEVICE="${DEVICE_ID:-}"

if [ -z "$TARGET_DEVICE" ]; then
    echo "Detecting attached phone..."
    DEVICES=$(adb devices | awk 'NR>1 && $2=="device" {print $1}')
    for dev in $DEVICES; do
        CHAR=$(adb -s "$dev" shell getprop ro.build.characteristics 2>/dev/null | tr -d '\r')
        if [ "$CHAR" != "tv" ]; then
            TARGET_DEVICE="$dev"
            MODEL=$(adb -s "$dev" shell getprop ro.product.model 2>/dev/null | tr -d '\r')
            echo "Selected phone device: $TARGET_DEVICE ($MODEL)"
            break
        fi
    done
fi

if [ -z "$TARGET_DEVICE" ]; then
    echo "Error: No connected phone device found." >&2
    exit 1
fi

# 2. Build release APK
echo "Building F-Droid Release APK..."
./gradlew assembleFdroidRelease

APK=$(find app/build/outputs/apk/fdroid/release -name "*.apk" ! -name "*unsigned*" | head -1)
if [ -z "$APK" ]; then
    echo "Error: No signed APK found in app/build/outputs/apk/fdroid/release/" >&2
    exit 1
fi

echo "Built APK: $APK"

# 3. Deploy to device
echo "Deploying to device $TARGET_DEVICE..."
INSTALL_OUTPUT=$(adb -s "$TARGET_DEVICE" install -r "$APK" 2>&1 || true)

if echo "$INSTALL_OUTPUT" | grep -q "Success"; then
    echo "Installed successfully."
elif echo "$INSTALL_OUTPUT" | grep -q "INSTALL_FAILED_UPDATE_INCOMPATIBLE"; then
    echo "Signature mismatch detected — reinstalling to update..."
    adb -s "$TARGET_DEVICE" uninstall "$APP_ID"
    adb -s "$TARGET_DEVICE" install "$APK"
    echo "Installed successfully."
else
    echo "Installation failed:" >&2
    echo "$INSTALL_OUTPUT" >&2
    exit 1
fi

# 4. Launch app
echo "Launching $APP_ID..."
adb -s "$TARGET_DEVICE" shell am start -n "$MAIN_ACTIVITY" >/dev/null

echo "Done!"
