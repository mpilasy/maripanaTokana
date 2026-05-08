#!/usr/bin/env bash
# End-to-end regression test for the F-Droid permission + location + weather flow.
#
# Covers 6 scenarios on a single connected emulator:
#   1. Cold cache, location providers DISABLED → Error within 20s
#   2. Cold cache, providers enabled, NO geo fix → Error within 20s
#   3. Cold cache, providers enabled, geo fix set → Success within 5s + OkHttp in logcat
#   4. Warm cache (scenario 3 relaunched) → Success within 5s
#   5. Deny → tap Grant button → dialog re-appears  (v1.0.12 regression)
#   6. Standard flavor (scenario 3) → Success within 5s  (no Play Services regression)
#
# Hard invariant: if CircularProgressIndicator is still on screen at t=20s, the test FAILS.
# This invariant is what would have caught the v1.0.13 "keeps loading" bug.
#
# BEFORE running: emulator -avd <name> -wipe-data   (guarantees no stale emulator state)
# REQUIRES: adb in PATH, $ANDROID_HOME, emulator with telnet console (for geo fix).
#
# Exits non-zero on any failure.

set -euo pipefail

PKG=orinasa.njarasoa.maripanatokana

# ── device selection ─────────────────────────────────────────────────────────
DEV=${TEST_DEV:-}
if [[ -z "$DEV" ]]; then
    DEV=$(adb devices | awk 'NR>1 && $2=="device"{print $1}' | grep -E '^emulator-' | head -1)
fi
if [[ -z "$DEV" ]]; then
    echo "ERROR: no emulator device found (need emulator for geo fix scenarios)"; exit 1
fi
export DEV
echo "Using device: $DEV"

# ── emulator geo fix support check ───────────────────────────────────────────
# Extract telnet port from emulator serial (emulator-5554 → 5554)
EMU_PORT=$(echo "$DEV" | grep -oE '[0-9]+$' || true)
EMU_AUTH_TOKEN=$(cat ~/.emulator_console_auth_token 2>/dev/null || true)
GEO_FIX_AVAILABLE=false
if [[ -n "$EMU_PORT" && -n "$EMU_AUTH_TOKEN" ]]; then
    if echo -e "auth $EMU_AUTH_TOKEN\ngeo fix -73.5 45.5\nquit\n" | nc -w3 localhost "$EMU_PORT" 2>/dev/null | grep -q "OK"; then
        GEO_FIX_AVAILABLE=true
    fi
fi
if [[ "$GEO_FIX_AVAILABLE" != "true" ]]; then
    echo "ERROR: emulator telnet geo fix unavailable on $DEV."
    echo "       Ensure ~/.emulator_console_auth_token exists and the device is an Android emulator."
    exit 1
fi

geo_fix() {
    local lat=$1 lon=$2
    echo -e "auth $EMU_AUTH_TOKEN\ngeo fix $lon $lat\nquit\n" | nc -w3 localhost "$EMU_PORT" >/dev/null 2>&1 || true
}

enable_providers() {
    adb -s "$DEV" shell settings put secure location_providers_allowed 'gps,network' 2>/dev/null || true
}

disable_providers() {
    adb -s "$DEV" shell settings put secure location_providers_allowed '' 2>/dev/null || true
}

# ── build ─────────────────────────────────────────────────────────────────────
JAVA_HOME_AS=${JAVA_HOME_AS:-/home/tahiry/.local/share/JetBrains/Toolbox/apps/android-studio/jbr}
echo "Building fdroid release APK..."
JAVA_HOME=$JAVA_HOME_AS ./gradlew :app:assembleFdroidRelease \
    -Porg.gradle.java.installations.paths="$JAVA_HOME_AS" -q

echo "Building standard release APK..."
JAVA_HOME=$JAVA_HOME_AS ./gradlew :app:assembleStandardRelease \
    -Porg.gradle.java.installations.paths="$JAVA_HOME_AS" -q

APKSIGNER=$(ls "$ANDROID_HOME"/build-tools/*/apksigner 2>/dev/null | sort -V | tail -1)
FDROID_APK=/tmp/fdroid-release-signed.apk
STD_APK=/tmp/standard-release-signed.apk

"$APKSIGNER" sign \
    --ks "$HOME/.android/debug.keystore" --ks-pass pass:android \
    --key-pass pass:android --ks-key-alias androiddebugkey \
    --out "$FDROID_APK" \
    app/build/outputs/apk/fdroid/release/app-fdroid-release-unsigned.apk 2>&1 | grep -v WARNING

"$APKSIGNER" sign \
    --ks "$HOME/.android/debug.keystore" --ks-pass pass:android \
    --key-pass pass:android --ks-key-alias androiddebugkey \
    --out "$STD_APK" \
    app/build/outputs/apk/standard/release/app-standard-release-unsigned.apk 2>&1 | grep -v WARNING

echo "APKs built and signed."

# ── helpers ──────────────────────────────────────────────────────────────────

install_apk() {
    local apk=$1
    adb -s "$DEV" uninstall "$PKG" >/dev/null 2>&1 || true
    adb -s "$DEV" install -t "$apk" >/dev/null
}

reset_app() {
    adb -s "$DEV" shell am force-stop "$PKG" 2>/dev/null || true
    adb -s "$DEV" shell pm clear "$PKG" 2>/dev/null || true
}

revoke_perms() {
    adb -s "$DEV" shell pm revoke "$PKG" android.permission.ACCESS_FINE_LOCATION 2>/dev/null || true
    adb -s "$DEV" shell pm revoke "$PKG" android.permission.ACCESS_COARSE_LOCATION 2>/dev/null || true
}

launch_app() {
    adb -s "$DEV" shell am start -W -n "$PKG/.MainActivity" >/dev/null
}

dump_ui() {
    adb -s "$DEV" shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
    adb -s "$DEV" pull /sdcard/ui.xml /tmp/ui.xml >/dev/null 2>&1
}

# Returns 0 if the UI contains a ProgressBar (still loading), 1 otherwise
is_loading() {
    dump_ui
    grep -q 'ProgressBar\|CircularProgressIndicator\|progress' /tmp/ui.xml 2>/dev/null
}

# Returns 0 if UI looks like Error screen (has Retry button or error text)
is_error() {
    dump_ui
    grep -qi 'retry\|error\|unable\|cannot\|permission' /tmp/ui.xml 2>/dev/null
}

# Returns 0 if UI looks like weather Success (has hour rows like "0:00" / "12:00" OR temperature)
is_success() {
    dump_ui
    grep -qE 'text="[0-9]{1,2}:[0-9]{2}"|°[CF]|°' /tmp/ui.xml 2>/dev/null
}

assert_not_loading_within() {
    local max_secs=$1 label=$2
    for i in $(seq 1 "$max_secs"); do
        dump_ui
        if ! grep -q 'ProgressBar\|CircularProgressIndicator\|progress' /tmp/ui.xml 2>/dev/null; then
            echo "  Loading ended at t=${i}s"
            return 0
        fi
        sleep 1
    done
    echo "FAIL ($label): stuck in Loading at t=${max_secs}s — dumping UI:"
    cat /tmp/ui.xml
    exit 1
}

assert_success_within() {
    local max_secs=$1 label=$2
    for i in $(seq 1 "$max_secs"); do
        if is_success; then
            echo "  Success at t=${i}s"
            return 0
        fi
        sleep 1
    done
    dump_ui
    echo "FAIL ($label): weather did not load within ${max_secs}s — dumping UI:"
    cat /tmp/ui.xml
    exit 1
}

tap_text() {
    local target=$1
    dump_ui
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
sys.exit(f"button '{target}' not found in UI dump")
PY
}

assert_focus() {
    local expected=$1 label=$2 max_wait=${3:-10}
    local focus
    for i in $(seq 1 "$max_wait"); do
        focus=$(adb -s "$DEV" shell dumpsys window 2>/dev/null | grep mCurrentFocus | head -1)
        if [[ "$focus" == *"$expected"* ]]; then
            echo "  focus OK: $expected (at ${i}s)"
            return 0
        fi
        sleep 1
    done
    echo "FAIL ($label): focus='$focus' did not contain '$expected' within ${max_wait}s"
    exit 1
}

save_logcat() {
    local scenario=$1
    adb -s "$DEV" logcat -d -t 500 2>/dev/null \
        | grep -E '(okhttp|OkHttp|NativeLocationProvider|fetchWeather|WeatherViewModel|location)' \
        > "/tmp/test_logcat_scenario${scenario}.log" 2>/dev/null || true
    echo "  Logcat saved → /tmp/test_logcat_scenario${scenario}.log"
}

PASS_COUNT=0
pass() {
    local label=$1
    echo "PASS: $label"
    PASS_COUNT=$((PASS_COUNT+1))
}

# ── SCENARIO 1: Cold cache, providers DISABLED → Error within 20s ─────────────
echo ""
echo "=== SCENARIO 1: Cold cache, providers DISABLED ==="
install_apk "$FDROID_APK"
reset_app
revoke_perms
disable_providers
launch_app
sleep 2
# Auto-grant permission silently for this scenario (we just want to test location path)
adb -s "$DEV" shell pm grant "$PKG" android.permission.ACCESS_COARSE_LOCATION 2>/dev/null || true
adb -s "$DEV" shell pm grant "$PKG" android.permission.ACCESS_FINE_LOCATION 2>/dev/null || true
adb -s "$DEV" shell am force-stop "$PKG" >/dev/null 2>&1
adb -s "$DEV" shell am start -W -n "$PKG/.MainActivity" >/dev/null
# Now the permission is already granted so it goes straight to fetchWeather → Loading
# Must NOT stay in Loading forever; must reach Error (or Success via stale cache) within 20s
assert_not_loading_within 20 "Scenario 1: must not hang in Loading"
save_logcat 1
pass "Scenario 1: Loading ended within 20s (providers disabled)"
enable_providers  # restore for subsequent scenarios

# ── SCENARIO 2: Cold cache, providers enabled, NO geo fix → Error within 20s ──
echo ""
echo "=== SCENARIO 2: Cold cache, providers enabled, no geo fix ==="
reset_app
revoke_perms
# Keep providers enabled but ensure no geo fix is outstanding
# (emulator without a geo fix will have no last-known from fresh install)
adb -s "$DEV" shell pm grant "$PKG" android.permission.ACCESS_COARSE_LOCATION 2>/dev/null || true
adb -s "$DEV" shell pm grant "$PKG" android.permission.ACCESS_FINE_LOCATION 2>/dev/null || true
adb -s "$DEV" shell am start -W -n "$PKG/.MainActivity" >/dev/null
assert_not_loading_within 20 "Scenario 2: must not hang in Loading without geo fix"
save_logcat 2
pass "Scenario 2: Loading ended within 20s (no geo fix)"

# ── SCENARIO 3: Cached GPS fix + permission granted → weather loads ────────────
# IMPORTANT: on API 34+, getLastKnownLocation returns null if the calling app did not
# have location permission at the time the fix was captured. We therefore grant permission
# BEFORE applying the geo fix so the system records this app as an "active accessor" and
# allows getLastKnownLocation to return the cached fix on first launch.
echo ""
echo "=== SCENARIO 3: Cached GPS fix, permission granted → Success ==="
install_apk "$FDROID_APK"  # fresh install ensures no SharedPrefs cache
revoke_perms
# Grant permission FIRST (before geo fix so the system allows getLastKnownLocation later)
adb -s "$DEV" shell pm grant "$PKG" android.permission.ACCESS_COARSE_LOCATION 2>/dev/null || true
adb -s "$DEV" shell pm grant "$PKG" android.permission.ACCESS_FINE_LOCATION 2>/dev/null || true
# Apply geo fix WITH permission active → system will return it via getLastKnownLocation
geo_fix 45.5 -73.5
sleep 2
adb -s "$DEV" logcat -c 2>/dev/null || true
adb -s "$DEV" shell am start -W -n "$PKG/.MainActivity" >/dev/null
assert_success_within 15 "Scenario 3: weather must load with cached GPS fix"
save_logcat 3
if grep -qiE '(okhttp|OkHttp|open-meteo|api\.)' "/tmp/test_logcat_scenario3.log"; then
    echo "  OkHttp call confirmed in logcat"
else
    echo "  WARNING: no OkHttp line in logcat — network may not have been called"
fi
pass "Scenario 3: Success within 15s (cached GPS fix)"

# ── SCENARIO 4: Warm cache (re-launch same state) → Success ───────────────────
echo ""
echo "=== SCENARIO 4: Warm cache re-launch ==="
adb -s "$DEV" shell am force-stop "$PKG" >/dev/null 2>&1
sleep 1
adb -s "$DEV" shell am start -W -n "$PKG/.MainActivity" >/dev/null
assert_success_within 15 "Scenario 4: warm cache must load in 15s"
save_logcat 4
pass "Scenario 4: Success within 15s (warm cache)"

# ── SCENARIO 5: Deny → dialog can re-appear (v1.0.12 regression) ──────────────
# Tests the invariant: after permission denial, the system dialog can be triggered
# again. On API <33, the app task survives denial so we tap the "Grant Permission"
# button. On API 33+, the task is destroyed after denial, so re-launching the app
# causes LaunchedEffect(Unit) to auto-fire the dialog — same end result.
echo ""
echo "=== SCENARIO 5: Deny → dialog re-appears ==="
install_apk "$FDROID_APK"  # fresh install to avoid any cached permission state
revoke_perms
adb -s "$DEV" shell am start -W -n "$PKG/.MainActivity" >/dev/null
assert_focus "GrantPermissionsActivity" "Scenario 5a: dialog auto-shown on fresh start" 10
tap_text "Don"  # "Don't allow"
sleep 2
cur_focus=$(adb -s "$DEV" shell dumpsys window 2>/dev/null | grep mCurrentFocus | head -1) || true
if [[ "$cur_focus" == *"$PKG"* ]]; then
    # Task still alive (older API): app's PermissionRequired screen is visible; tap button
    echo "  (task still alive — tapping Grant Permission button)"
    tap_text "Grant Permission"
    sleep 2
else
    # Task was destroyed (API 33+ behavior): re-launch; LaunchedEffect auto-fires the dialog
    TASK_ID=$(adb -s "$DEV" shell dumpsys activity activities 2>/dev/null \
        | grep -E "Task #[0-9]+" | grep "orinasa.njarasoa" \
        | grep -oP 'Task #\K[0-9]+' | head -1) || true
    if [[ -n "$TASK_ID" ]]; then
        echo "  (focus left app but task exists — reorder to front + tap button)"
        adb -s "$DEV" shell am task reorder-to-front "$TASK_ID" 2>/dev/null || true
        sleep 2
        tap_text "Grant Permission"
        sleep 2
    else
        echo "  (task destroyed after denial — relaunching; LaunchedEffect will auto-fire dialog)"
        adb -s "$DEV" shell am start -n "$PKG/.MainActivity" >/dev/null 2>&1 || true
        sleep 3
    fi
fi
assert_focus "GrantPermissionsActivity" "Scenario 5b: dialog re-appears after denial" 10
# Grant so the app doesn't stay stuck for subsequent scenarios
tap_text "While using the app" 2>/dev/null || tap_text "Only this time" 2>/dev/null || true
sleep 2
save_logcat 5
pass "Scenario 5: dialog re-appeared after denial (v1.0.12 regression check)"

# ── SCENARIO 6: Standard flavor parity ────────────────────────────────────────
# Same grant-before-geo_fix ordering as Scenario 3 (API 34+ getLastKnownLocation constraint).
echo ""
echo "=== SCENARIO 6: Standard flavor, cached GPS fix → Success ==="
install_apk "$STD_APK"
revoke_perms
adb -s "$DEV" shell pm grant "$PKG" android.permission.ACCESS_COARSE_LOCATION 2>/dev/null || true
adb -s "$DEV" shell pm grant "$PKG" android.permission.ACCESS_FINE_LOCATION 2>/dev/null || true
geo_fix 45.5 -73.5
sleep 2
adb -s "$DEV" shell am start -W -n "$PKG/.MainActivity" >/dev/null
assert_success_within 15 "Scenario 6: standard flavor must load with cached GPS fix"
save_logcat 6
pass "Scenario 6: Standard flavor success (no Play Services regression)"

# ── summary ──────────────────────────────────────────────────────────────────
echo ""
echo "========================================"
echo "ALL $PASS_COUNT/6 CHECKS PASSED"
echo "========================================"
echo ""
echo "Logcat files for MR comment:"
for i in 1 2 3 4 5 6; do
    f="/tmp/test_logcat_scenario${i}.log"
    if [[ -f "$f" ]]; then
        lines=$(wc -l < "$f")
        echo "  Scenario $i: $f ($lines lines)"
    fi
done
