#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

# 1. Build and deploy
"$SCRIPT_DIR/deploy_phone.sh"

APP_ID="orinasa.njarasoa.maripanatokana"
MAIN_ACTIVITY="${APP_ID}/.MainActivity"

# 2. Detect target device used
TARGET_DEVICE="${DEVICE_ID:-${DEVICE:-}}"
if [ -z "$TARGET_DEVICE" ]; then
    DEVICES=$(adb devices | awk 'NR>1 && $2=="device" {print $1}')
    for dev in $DEVICES; do
        CHAR=$(adb -s "$dev" shell getprop ro.build.characteristics 2>/dev/null | tr -d '\r')
        if [ "$CHAR" != "tv" ]; then
            TARGET_DEVICE="$dev"
            break
        fi
    done
fi

if [ -n "$TARGET_DEVICE" ]; then
    echo "Starting $APP_ID on $TARGET_DEVICE..."
    adb -s "$TARGET_DEVICE" shell am start -n "$MAIN_ACTIVITY" >/dev/null
    echo "App started!"
fi
