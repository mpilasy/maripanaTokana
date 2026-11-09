#!/bin/bash
# SPDX-FileCopyrightText: Orinasa Njarasoa
# SPDX-License-Identifier: GPL-3.0-or-later
# Builds the standard release APK once and installs it on one or more network TVs.
# Usage: scripts/deploy_tv_ip.sh <ip>[:port] [<ip>[:port] ...]
set -euo pipefail

if [ $# -lt 1 ]; then
    echo "Usage: $0 <ip>[:port] [<ip>[:port] ...]" >&2
    exit 1
fi

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"
APK=app/build/outputs/apk/standard/release/app-standard-release.apk

TARGETS=()
for arg in "$@"; do
    [[ "$arg" == *:* ]] || arg="$arg:5555"
    TARGETS+=("$arg")
done

# A TV in standby is woken with Wake-on-LAN; its screen stays off. Its MAC address comes from
# scripts/wake-macs.local ("<ip> <mac>" per line, not in git) or from the ARP cache.
WAKE_MACS_FILE="${MARIPANA_WAKE_MACS:-$ROOT_DIR/scripts/wake-macs.local}"

mac_of() {
    local ip="${1%%:*}" mac=""
    if [ -f "$WAKE_MACS_FILE" ]; then
        mac="$(awk -v ip="$ip" '$1 == ip { print $2; exit }' "$WAKE_MACS_FILE")"
    fi
    if [ -z "$mac" ]; then
        mac="$(ip neigh show "$ip" 2>/dev/null | awk '/lladdr/ { for (i = 1; i <= NF; i++) if ($i == "lladdr") print $(i + 1); exit }')"
    fi
    echo "$mac"
}

# Wakes $1 with Wake-on-LAN and waits up to a minute for adb. A TV in deep standby wakes its
# network only for a moment, so the packets go on every 2 s, to this network's broadcast address
# too, until the script ends (the install included). Returns whether adb answers.
WAKE_PIDS=()
trap 'for pid in "${WAKE_PIDS[@]+"${WAKE_PIDS[@]}"}"; do kill "$pid" 2>/dev/null || true; done' EXIT
wake_target() {
    local target="$1" mac broadcast
    mac="$(mac_of "$target")"
    [ -n "$mac" ] || return 1
    broadcast="${target%%:*}"
    broadcast="${broadcast%.*}.255"
    echo "Device $target not reachable — sending Wake-on-LAN to $mac..."
    python3 - "$mac" "$broadcast" <<'PY' &
import socket, sys, time
mac = bytes.fromhex(sys.argv[1].replace(":", "").replace("-", ""))
packet = b"\xff" * 6 + mac * 16
s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
s.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)
end = time.time() + 900
while time.time() < end:
    for address in (sys.argv[2], "255.255.255.255"):
        for port in (9, 7):
            s.sendto(packet, (address, port))
    time.sleep(2)
PY
    WAKE_PIDS+=("$!")
    for _ in $(seq 1 30); do
        sleep 2
        adb disconnect "$target" >/dev/null 2>&1 || true
        adb connect "$target" >/dev/null 2>&1 || true
        [ "$(adb -s "$target" get-state 2>/dev/null)" = "device" ] && return 0
    done
    return 1
}


REACHABLE=()
for TARGET in "${TARGETS[@]}"; do
    adb connect "$TARGET" >/dev/null 2>&1 || true
    if [ "$(adb -s "$TARGET" get-state 2>/dev/null)" != "device" ] && ! wake_target "$TARGET"; then
        echo "Device $TARGET not reachable — skipping. Check it's powered on and on the network." >&2
        continue
    fi
    CHARACTERISTICS="$(adb -s "$TARGET" shell getprop ro.build.characteristics 2>/dev/null | tr -d '\r')"
    if [[ "$CHARACTERISTICS" != *tv* ]]; then
        echo "Device $TARGET does not report a TV characteristic (got '$CHARACTERISTICS') — skipping, this is the TV deploy script." >&2
        continue
    fi
    REACHABLE+=("$TARGET")
done

if [ ${#REACHABLE[@]} -eq 0 ]; then
    echo "No reachable devices." >&2
    exit 1
fi

./gradlew :app:assembleStandardRelease

PIDS=()
for TARGET in "${REACHABLE[@]}"; do
    ( adb -s "$TARGET" install -r --user 0 "$APK" && echo "Installed on $TARGET" ) &
    PIDS+=("$!")
done

FAILED=0
for PID in "${PIDS[@]}"; do
    wait "$PID" || FAILED=1
done
if [ "$FAILED" -eq 1 ]; then
    echo "One or more installs failed." >&2
    exit 1
fi
