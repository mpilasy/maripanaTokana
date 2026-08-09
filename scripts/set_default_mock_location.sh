#!/usr/bin/env bash
# Set mock location to Roseville, MN on all connected emulators.
# Run after launching an emulator — cold boot forced (fastboot.forceColdBoot=yes
# on both AVDs), so there's no persisted GPS default; re-apply each boot.
set -euo pipefail

LAT=45.0061
LON=-93.1566

for dev in $(adb devices | awk '/^emulator-/{print $1}'); do
    echo "Setting $dev -> Roseville, MN ($LAT, $LON)"
    adb -s "$dev" emu geo fix "$LON" "$LAT"
done
