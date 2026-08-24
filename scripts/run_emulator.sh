#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

# 1. Build and deploy
"$SCRIPT_DIR/deploy_emulator.sh"

APP_ID="orinasa.njarasoa.maripanatokana"
MAIN_ACTIVITY="${APP_ID}/.MainActivity"

# 2. Detect target emulator used
TARGET_DEVICE="${DEVICE_ID:-${DEVICE:-}}"
if [ -z "$TARGET_DEVICE" ]; then
    TARGET_DEVICE=$(adb devices | awk 'NR>1 && $1~/^emulator-/ && $2=="device" {print $1; exit}')
fi

if [ -n "$TARGET_DEVICE" ]; then
    echo "Starting $APP_ID on $TARGET_DEVICE..."
    adb -s "$TARGET_DEVICE" shell am start -n "$MAIN_ACTIVITY" >/dev/null
    echo "App started!"
fi
