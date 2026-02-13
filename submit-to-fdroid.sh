#!/bin/bash
# Convenience script: Run this to prepare submission

set -e

echo "F-Droid Submission Preparation"
echo "=============================="
echo ""

# Step 1: Build
echo "Step 1: Building F-Droid flavor..."
./gradlew clean assembleFdroidRelease

# Step 2: Tests
echo ""
echo "Step 2: Running verification tests..."
bash scripts/test_fdroid_build.sh

# Step 3: Summary
echo ""
echo "✅ Ready to submit!"
echo ""
echo "Next steps:"
echo "  1. Capture screenshots: bash scripts/capture_screenshots.sh"
echo "  2. Commit: git add fastlane/ && git commit -m 'Add F-Droid screenshots'"
echo "  3. Follow: FDROID_SUBMIT_NOW.md"
