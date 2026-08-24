#!/usr/bin/env bash
set -euo pipefail

# Automated F-Droid Release Script for maripànaTokana
# Usage: ./scripts/release_fdroid.sh [VERSION_NAME] [VERSION_CODE]
# Example: ./scripts/release_fdroid.sh 1.2.17 60

if [ $# -lt 2 ]; then
    CURRENT_VERSION=$(grep -E 'versionName\s*=\s*"[^"]+"' app/build.gradle.kts | sed -E 's/.*"([^"]+)".*/\1/')
    CURRENT_CODE=$(grep -E 'versionCode\s*=\s*[0-9]+' app/build.gradle.kts | sed -E 's/[^0-9]*([0-9]+).*/\1/')
    
    NEW_CODE=$((CURRENT_CODE + 1))
    
    MAJOR=$(echo "$CURRENT_VERSION" | cut -d. -f1)
    MINOR=$(echo "$CURRENT_VERSION" | cut -d. -f2)
    PATCH=$(echo "$CURRENT_VERSION" | cut -d. -f3)
    NEW_PATCH=$((PATCH + 1))
    NEW_VERSION="${MAJOR}.${MINOR}.${NEW_PATCH}"
else
    NEW_VERSION="$1"
    NEW_CODE="$2"
fi

echo "=========================================="
echo " Preparing F-Droid Release v${NEW_VERSION} (code ${NEW_CODE})"
echo "=========================================="

# 1. Update version strings
echo "1. Updating version in build configs & web..."
sed -i -E "s/versionCode = [0-9]+/versionCode = ${NEW_CODE}/" app/build.gradle.kts
sed -i -E "s/versionName = \"[^\"]+\"/versionName = \"${NEW_VERSION}\"/" app/build.gradle.kts
sed -i -E "s/\"version\": \"[^\"]+\"/\"version\": \"${NEW_VERSION}\"/" web/package.json
sed -i -E "s/<span class=\"version\">v[^<]+<\/span>/<span class=\"version\">v${NEW_VERSION}<\/span>/" web/src/lib/components/Footer.svelte

(cd web && npm install --package-lock-only)

# 2. Verify version sync
python3 scripts/check_version_sync.py

# 3. Ensure changelog files exist
LOCALES=("ar" "en-US" "es" "fr" "hi" "mg" "ne" "zh-CN")
for loc in "${LOCALES[@]}"; do
    CL_FILE="fastlane/metadata/android/${loc}/changelogs/${NEW_CODE}.txt"
    if [ ! -f "$CL_FILE" ]; then
        echo "Error: Missing changelog $CL_FILE" >&2
        echo "Please create $CL_FILE before running release." >&2
        exit 1
    fi
done

# 4. Commit version bump & feature changes
echo "2. Committing version bump & pending changes..."
git add -A
git commit -m "Bump to v${NEW_VERSION}"

BUMP_COMMIT=$(git rev-parse HEAD)
echo "Bump commit: ${BUMP_COMMIT}"

# 5. Tag release on the bump commit (CRITICAL: tag must point to bump commit WITHOUT [skip ci])
echo "3. Tagging v${NEW_VERSION}..."
git tag -a "v${NEW_VERSION}" "${BUMP_COMMIT}" -m "Release v${NEW_VERSION}"

# 6. Update F-Droid metadata YAML
echo "4. Updating F-Droid metadata YAML..."
python3 scripts/update_metadata.py "${NEW_VERSION}" "${NEW_CODE}" "${BUMP_COMMIT}" metadata/orinasa.njarasoa.maripanatokana.yml

# Verify Binaries trailing space
grep -P '^Binaries: $' metadata/orinasa.njarasoa.maripanatokana.yml >/dev/null || {
    echo "Error: Binaries line in metadata YAML is missing trailing space!" >&2
    exit 1
}

# 7. Commit metadata update with [skip ci]
echo "5. Committing metadata update..."
git add metadata/orinasa.njarasoa.maripanatokana.yml
git commit -m "Update F-Droid metadata for v${NEW_VERSION} [skip ci]"

# 8. Push main and tag
echo "6. Pushing main and tag v${NEW_VERSION} to origin..."
git push origin main
git push origin "v${NEW_VERSION}"

echo "=========================================="
echo " Release v${NEW_VERSION} successfully pushed!"
echo " CI workflow 'F-Droid Build' will run on tag v${NEW_VERSION}."
echo " Build takes ~5 minutes. Do NOT poll repeatedly."
echo "=========================================="
