#!/usr/bin/env bash
# SPDX-FileCopyrightText: Orinasa Njarasoa
# SPDX-License-Identifier: GPL-3.0-or-later
# Set mock location to Antananarivo on all connected emulators.
# Run after launching an emulator — cold boot forced (fastboot.forceColdBoot=yes
# on both AVDs), so there's no persisted GPS default; re-apply each boot.
set -euo pipefail

LAT=-18.8792
LON=47.5079

for dev in $(adb devices | awk '/^emulator-/{print $1}'); do
    echo "Setting $dev -> Antananarivo ($LAT, $LON)"
    adb -s "$dev" emu geo fix "$LON" "$LAT"
done
