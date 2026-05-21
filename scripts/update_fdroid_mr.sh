#!/bin/bash
# scripts/update_fdroid_mr.sh
# Automates updating the F-Droid Merge Request on GitLab

set -e

APP_ID="orinasa.njarasoa.maripanatokana"
GITLAB_REPO="git@gitlab.com:mpilasy/fdroiddata.git"
BRANCH="add-maripanatokana"
LOCAL_METADATA="metadata/$APP_ID.yml"
TEMP_DIR="/tmp/fdroiddata_sync"

echo "Checking local metadata..."
if [ ! -f "$LOCAL_METADATA" ]; then
    echo "Error: $LOCAL_METADATA not found."
    exit 1
fi

# Check if the metadata has been updated with binaries (SHA256)
if ! grep -q "sha256:" "$LOCAL_METADATA"; then
    echo "Warning: No sha256 hash found in $LOCAL_METADATA."
    echo "Ensure the GitHub Action has finished and you have pulled the latest changes."
    read -p "Proceed anyway? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

echo "Cloning F-Droid data fork..."
rm -rf "$TEMP_DIR"
git clone --depth 1 --branch "$BRANCH" "$GITLAB_REPO" "$TEMP_DIR"

echo "Updating metadata in fork..."
cp "$LOCAL_METADATA" "$TEMP_DIR/metadata/$APP_ID.yml"

cd "$TEMP_DIR"

if git diff --quiet; then
    echo "No changes detected in metadata. MR is already up to date."
else
    echo "Committing and pushing to GitLab..."
    git add "metadata/$APP_ID.yml"
    git commit -m "Update $APP_ID metadata (Reproducible Builds)"
    git push origin "$BRANCH"
    echo "Successfully updated F-Droid MR!"
fi

rm -rf "$TEMP_DIR"
