#!/bin/bash
# SPDX-FileCopyrightText: Orinasa Njarasoa
# SPDX-License-Identifier: GPL-3.0-or-later
# Builds the standard release APK once and installs it on every device in a targets file.
# Usage: scripts/deploy_all.sh [targets file, default scripts/deploy-targets.local]
#
# The targets file (not in git: it holds the home network's addresses) has one device per line:
#   <kind> <target>
#   kind    tv | phone
#   target  <ip>[:port] (a network TV) or a USB serial
# "#" starts a comment. Only list a device that may get the app.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"
TARGETS_FILE="${1:-$ROOT_DIR/scripts/deploy-targets.local}"
WAKE_MACS_FILE="${MARIPANA_WAKE_MACS:-$ROOT_DIR/scripts/wake-macs.local}"
APK=app/build/outputs/apk/standard/release/app-standard-release.apk
LOG_DIR="$(mktemp -d)"

if [ ! -f "$TARGETS_FILE" ]; then
    echo "No targets file at $TARGETS_FILE." >&2
    exit 1
fi

# --- Wake-on-LAN ---

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

WAKE_PIDS=()
trap 'for pid in "${WAKE_PIDS[@]+"${WAKE_PIDS[@]}"}"; do kill "$pid" 2>/dev/null || true; done; rm -rf "$LOG_DIR"' EXIT

# Wakes network TV $1 and waits up to a minute for adb; the packets go on until the script ends.
wake_target() {
    local target="$1" mac broadcast
    mac="$(mac_of "$target")"
    [ -n "$mac" ] || return 1
    broadcast="${target%%:*}"
    broadcast="${broadcast%.*}.255"
    echo "$target not reachable — sending Wake-on-LAN to $mac..."
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

SERIALS=()
while read -r kind target _; do
    [[ -z "${kind:-}" || "$kind" == \#* ]] && continue
    case "$kind" in tv | phone) ;; *) echo "Skipping line: unknown kind '$kind'." >&2 && continue ;; esac
    serial="$target"
    if [[ "$target" == *.*.*.* ]]; then
        [[ "$target" == *:* ]] || serial="$target:5555"
        adb connect "$serial" >/dev/null 2>&1 || true
        if [ "$(adb -s "$serial" get-state 2>/dev/null)" != "device" ] && ! wake_target "$serial"; then
            echo "$serial not reachable — skipping." >&2
            continue
        fi
    elif [ "$(adb -s "$serial" get-state 2>/dev/null)" != "device" ]; then
        echo "$serial not connected — skipping." >&2
        continue
    fi
    is_tv=false
    adb -s "$serial" shell pm has-feature android.software.leanback </dev/null 2>/dev/null | grep -q true && is_tv=true
    if { [ "$kind" = tv ] && ! $is_tv; } || { [ "$kind" = phone ] && $is_tv; }; then
        echo "$serial is not a $kind device — skipping." >&2
        continue
    fi
    SERIALS+=("$serial")
done <"$TARGETS_FILE"

if [ ${#SERIALS[@]} -eq 0 ]; then
    echo "No reachable devices." >&2
    exit 1
fi

./gradlew :app:assembleStandardRelease

PIDS=()
for i in "${!SERIALS[@]}"; do
    # --user 0: a plain install reaches every profile of a multi-user device
    adb -s "${SERIALS[$i]}" install -r --user 0 "$APK" </dev/null >"$LOG_DIR/$i.log" 2>&1 &
    PIDS+=("$!")
done

FAILED=0
for i in "${!PIDS[@]}"; do
    if wait "${PIDS[$i]}"; then
        echo "OK ${SERIALS[$i]}"
    else
        FAILED=1
        echo "FAILED ${SERIALS[$i]}:" >&2
        sed 's/^/    /' "$LOG_DIR/$i.log" >&2
    fi
done
exit "$FAILED"
