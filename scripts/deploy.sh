#!/usr/bin/env bash
set -euo pipefail

APP_ID="orinasa.njarasoa.maripanatokana"

echo "Building..."
./gradlew assembleFdroidRelease

APK=$(find app/build/outputs/apk/fdroid/release -name "*.apk" ! -name "*unsigned*" | head -1)
if [ -z "$APK" ]; then
    echo "Error: no signed APK found in app/build/outputs/apk/fdroid/release/" >&2
    exit 1
fi
echo "Built: $APK"

DEVICE="${DEVICE:-b048cf47}"
DEVICES=("$DEVICE")

for device in "${DEVICES[@]}"; do
    echo ""
    echo "→ $device"
    output=$(adb -s "$device" install -r "$APK" 2>&1 || true)
    if echo "$output" | grep -q "Success"; then
        echo "  Installed."
    elif echo "$output" | grep -q "INSTALL_FAILED_UPDATE_INCOMPATIBLE"; then
        echo "  Signature mismatch — uninstalling previous version..."
        adb -s "$device" uninstall "$APP_ID"
        adb -s "$device" install "$APK"
        echo "  Installed."
    else
        echo "  Failed: $output" >&2
        exit 1
    fi
done

echo ""
echo "Done."
