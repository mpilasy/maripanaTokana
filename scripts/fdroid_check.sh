#!/usr/bin/env bash
# SPDX-FileCopyrightText: Orinasa Njarasoa
# SPDX-License-Identifier: GPL-3.0-or-later
set -euo pipefail

# Do the F-Droid reproducible build check before a release is published.
# F-Droid builds the app from source and compares it with our signed APK.
# If they are not the same, F-Droid does not publish that version.
#
# This script does the same check in F-Droid's own build image, with
# fdroidserver's own "fdroid build" code. The repository copy gets 20000
# extra objects, so "git rev-parse --short" gives more characters, like on
# the large clone on F-Droid's server.
#
# Usage: scripts/fdroid_check.sh VERSION_NAME VERSION_CODE COMMIT SIGNED_APK
#        scripts/fdroid_check.sh --build-only VERSION_NAME VERSION_CODE COMMIT UNSIGNED_OUT
# Example: scripts/fdroid_check.sh 1.6.20261028114142 116 ccf3f64... app.apk
# The commit must be on GitHub, because fdroid build clones from there.
# With --build-only, the script does not compare. It writes F-Droid's unsigned
# APK to UNSIGNED_OUT. Compare it later with scripts/fdroid_compare.py. The
# release workflow does this, so that the F-Droid build and our build can run
# at the same time.

BUILD_ONLY=
if [ "${1:-}" = "--build-only" ]; then
    BUILD_ONLY=1
    shift
fi
if [ $# -ne 4 ]; then
    echo "Usage: $0 [--build-only] VERSION_NAME VERSION_CODE COMMIT SIGNED_APK|UNSIGNED_OUT" >&2
    exit 2
fi
V_NAME=$1
V_CODE=$2
COMMIT=$3
APK=$(realpath "$4")
APP_ID=orinasa.njarasoa.maripanatokana
IMAGE=registry.gitlab.com/fdroid/fdroidserver:buildserver-trixie
ROOT=$(cd "$(dirname "$0")/.." && pwd)

WORK=$(mktemp -d); echo "Work dir: $WORK" >&2
[ -n "${KEEP:-}" ] || trap 'docker run --rm -v "$WORK:/work" "$IMAGE" rm -rf /work/build /work/tmp /work/unsigned /work/logs >/dev/null 2>&1 || true; rm -rf "$WORK"' EXIT
mkdir -p "$WORK/metadata" "$WORK/build"
cp "$ROOT/scripts/fdroid_compare.py" "$WORK/"
if [ -z "$BUILD_ONLY" ]; then
    mkdir -p "$WORK/ref"
    cp "$APK" "$WORK/ref/$APP_ID.apk"
fi

# Our metadata, with this build. Binaries is removed: the signed APK is not
# on GitHub yet, so the comparison is done after the build (see inner.sh),
# with the same fdroidserver function that "fdroid build" uses.
cp "$ROOT/metadata/$APP_ID.yml" "$WORK/metadata/$APP_ID.yml"
python3 "$ROOT/scripts/update_metadata.py" "$V_NAME" "$V_CODE" "$COMMIT" "$WORK/metadata/$APP_ID.yml" >/dev/null
sed -i -E '/^Binaries:/,/^$/d' "$WORK/metadata/$APP_ID.yml"
! grep -q "^Binaries" "$WORK/metadata/$APP_ID.yml" || { echo "Could not remove Binaries" >&2; exit 1; }

# Same URL as Repo in the metadata, else fdroid deletes this clone and makes a new one.
REPO=$(sed -n 's/^Repo: //p' "$WORK/metadata/$APP_ID.yml")
git clone -q "$REPO" "$WORK/build/$APP_ID"
printf 'git %s' "$REPO" > "$WORK/build/.fdroidvcs-$APP_ID"
python3 -c 'import sys; sys.stdout.write("".join(f"blob\ndata {len(b)}\n{b}\n" for b in (f"pad {i}" for i in range(20000))))' \
    | git -C "$WORK/build/$APP_ID" fast-import --quiet

cat > "$WORK/inner.sh" <<'EOF'
#!/bin/bash
# Follows the "fdroid build" job of fdroiddata's .gitlab-ci.yml.
set -e
# Give the files back to the caller, whose user ID can differ from vagrant's.
trap 'chown -R "$HOST_UID" /work' EXIT
BUILD=$1
APP_ID=${BUILD%:*}
export ANDROID_HOME=/opt/android-sdk
source /etc/profile.d/bsenv.sh
apt-get update -q >/dev/null
apt-get install -qy sudo openjdk-21-jdk-headless >/dev/null
update-alternatives --set java /usr/lib/jvm/java-21-openjdk-amd64/bin/java
sdkmanager "platform-tools" "build-tools;31.0.0" >/dev/null
rm -rf $fdroidserver
git clone -q https://gitlab.com/fdroid/fdroidserver.git $fdroidserver
git -C $home_vagrant/gradlew-fdroid pull -q || true
cd /work
for d in logs tmp unsigned $home_vagrant/.android $home_vagrant/.gradle $home_vagrant/metadata; do
    mkdir -p $d; chown -R vagrant $d
done
cp metadata/$APP_ID.yml $home_vagrant/metadata/
mkdir -p $home_vagrant/build
cp -a build/. $home_vagrant/build/
ln -sfn /work/tmp $home_vagrant/tmp
chown -R vagrant $home_vagrant /work
cd $home_vagrant
sudo --preserve-env --user vagrant env PATH=$fdroidserver:$PATH \
    PYTHONPATH=$fdroidserver:$fdroidserver/examples PYTHONUNBUFFERED=true \
    HOME=$home_vagrant GRADLE_USER_HOME=$home_vagrant/.gradle \
    fdroid build --verbose --test --on-server --no-tarball $BUILD
UNSIGNED=$(ls /work/tmp/${APP_ID}_*.apk)
cp "$UNSIGNED" /work/unsigned.apk
echo "F-Droid build done: $UNSIGNED"
if [ -f /work/ref/$APP_ID.apk ]; then
    PYTHONPATH=$fdroidserver python3 /work/fdroid_compare.py /work/ref/$APP_ID.apk "$UNSIGNED" || true
fi
EOF

LOG="$WORK/fdroid_build.log"
set +e
docker run --rm -e HOST_UID="$(id -u):$(id -g)" -v "$WORK:/work" "$IMAGE" bash /work/inner.sh "$APP_ID:$V_CODE" > "$LOG" 2>&1
set -e
if [ -n "$BUILD_ONLY" ]; then
    if grep -q "^F-Droid build done:" "$LOG"; then
        cp "$WORK/unsigned.apk" "$APK"
        echo "F-Droid build of $V_NAME ($V_CODE) done: $APK"
        exit 0
    fi
    tail -60 "$LOG" >&2
    echo "F-Droid build FAILED for $V_NAME ($V_CODE). See the log above." >&2
    exit 1
fi
if grep -q "compared built binary to supplied reference binary successfully" "$LOG"; then
    echo "F-Droid check passed: the F-Droid build of $V_NAME ($V_CODE) is the same as the signed APK."
    exit 0
fi
tail -60 "$LOG" >&2
echo "F-Droid check FAILED: F-Droid would not publish $V_NAME ($V_CODE). See the log above." >&2
exit 1
