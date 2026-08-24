#!/usr/bin/env bash
set -euo pipefail

APP_ID="orinasa.njarasoa.maripanatokana"
MAIN_ACTIVITY="${APP_ID}/.MainActivity"

# 1. Detect attached emulator
TARGET_DEVICE="${DEVICE_ID:-${DEVICE:-}}"

if [ -z "$TARGET_DEVICE" ]; then
    echo "Detecting attached emulator..."
    TARGET_DEVICE=$(adb devices | awk 'NR>1 && $1~/^emulator-/ && $2=="device" {print $1; exit}')
fi

if [ -z "$TARGET_DEVICE" ]; then
    echo "Error: No connected emulator found." >&2
    exit 1
fi

echo "Selected emulator device: $TARGET_DEVICE"

# 2. Build release APK
echo "Building F-Droid Release APK..."
./gradlew assembleFdroidRelease

APK=$(find app/build/outputs/apk/fdroid/release -name "*.apk" ! -name "*unsigned*" | head -1)
if [ -z "$APK" ]; then
    echo "Error: No signed APK found in app/build/outputs/apk/fdroid/release/" >&2
    exit 1
fi

echo "Built APK: $APK"

# 3. Deploy to emulator
echo "Deploying to emulator $TARGET_DEVICE..."
INSTALL_OUTPUT=$(adb -s "$TARGET_DEVICE" install -r "$APK" 2>&1 || true)

if echo "$INSTALL_OUTPUT" | grep -q "Success"; then
    echo "Installed successfully."
elif echo "$INSTALL_OUTPUT" | grep -q "INSTALL_FAILED_UPDATE_INCOMPATIBLE"; then
    echo "Signature mismatch detected — attempting downgrade install with -r -d to preserve user data..."
    adb -s "$TARGET_DEVICE" install -r -d "$APK"
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
