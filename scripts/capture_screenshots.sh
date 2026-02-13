#!/bin/bash

# F-Droid Screenshot Capture Automation
# Captures 5 screenshots for each of 8 languages
# Usage: bash scripts/capture_screenshots.sh

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
PACKAGE="orinasa.njarasoa.maripanatokana"
ACTIVITY="$PACKAGE.MainActivity"
APP_DIR="."
BUILD_VARIANT="fdroid"
SCREENSHOT_COUNT=5
LANGUAGES=(
  "en_US"
  "mg"
  "ar"
  "es"
  "fr"
  "hi"
  "ne"
  "zh_CN"
)

LANGUAGE_NAMES=(
  "English (US)"
  "Malagasy"
  "Arabic"
  "Spanish"
  "French"
  "Hindi"
  "Nepali"
  "Chinese Simplified"
)

# Check prerequisites
check_requirements() {
  echo -e "${YELLOW}Checking requirements...${NC}"

  if ! command -v adb &> /dev/null; then
    echo -e "${RED}❌ adb not found. Please install Android SDK Platform Tools.${NC}"
    exit 1
  fi

  if ! command -v ./gradlew &> /dev/null; then
    echo -e "${RED}❌ gradlew not found. Run this script from repository root.${NC}"
    exit 1
  fi

  # Check if device is connected
  if ! adb devices | grep -q "device$"; then
    echo -e "${RED}❌ No Android device connected. Please connect a device or emulator.${NC}"
    exit 1
  fi

  echo -e "${GREEN}✓ All requirements met${NC}\n"
}

# Build F-Droid flavor if not already built
build_apk() {
  echo -e "${YELLOW}Building F-Droid flavor...${NC}"

  if [ ! -f "app/build/outputs/apk/$BUILD_VARIANT/debug/app-$BUILD_VARIANT-debug.apk" ]; then
    echo "APK not found. Building..."
    ./gradlew clean assemble${BUILD_VARIANT^}Debug
  else
    echo "APK already built."
  fi

  echo -e "${GREEN}✓ APK ready${NC}\n"
}

# Install APK on device
install_apk() {
  echo -e "${YELLOW}Installing APK on device...${NC}"

  adb install -r "app/build/outputs/apk/$BUILD_VARIANT/debug/app-$BUILD_VARIANT-debug.apk"

  echo -e "${GREEN}✓ APK installed${NC}\n"
}

# Set device language
set_device_language() {
  local lang_code=$1

  echo -e "${YELLOW}Setting device language to: $lang_code${NC}"

  # Convert language code format (en_US -> en-US)
  local lang_setting=$(echo "$lang_code" | tr '_' '-')

  # Try to set language (requires device to support it)
  # Note: This may not work on all devices/Android versions
  adb shell settings put system system_locale "$lang_code" 2>/dev/null || true

  sleep 2
}

# Clear app data
clear_app_data() {
  echo -e "${YELLOW}Clearing app data...${NC}"
  adb shell pm clear "$PACKAGE" 2>/dev/null || true
  sleep 1
}

# Launch app
launch_app() {
  echo -e "${YELLOW}Launching app...${NC}"
  adb shell am start -n "$ACTIVITY" 2>/dev/null || true
  sleep 5  # Wait for app to load
}

# Capture screenshots
capture_screenshots() {
  local lang_code=$1
  local lang_name=$2
  local output_dir="fastlane/metadata/android/$lang_code/images/phoneScreenshots"

  echo -e "${YELLOW}Capturing $SCREENSHOT_COUNT screenshots for $lang_name...${NC}"

  mkdir -p "$output_dir"

  for i in $(seq 1 $SCREENSHOT_COUNT); do
    echo "  Capturing screenshot $i/$SCREENSHOT_COUNT..."

    # Capture on device
    adb shell screencap -p "/sdcard/screenshot_$i.png"

    # Pull to computer
    adb pull "/sdcard/screenshot_$i.png" "$output_dir/$i.png" > /dev/null 2>&1

    # Remove from device
    adb shell rm "/sdcard/screenshot_$i.png" 2>/dev/null || true

    # Small delay between captures
    sleep 1
  done

  echo -e "${GREEN}✓ Captured $SCREENSHOT_COUNT screenshots for $lang_name${NC}"
}

# Main execution
main() {
  echo -e "${GREEN}╔════════════════════════════════════════════════════════╗${NC}"
  echo -e "${GREEN}║   F-Droid Screenshot Capture Automation               ║${NC}"
  echo -e "${GREEN}╚════════════════════════════════════════════════════════╝${NC}\n"

  check_requirements
  build_apk
  install_apk

  # Capture screenshots for each language
  for i in "${!LANGUAGES[@]}"; do
    lang_code="${LANGUAGES[$i]}"
    lang_name="${LANGUAGE_NAMES[$i]}"

    echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${YELLOW}Language $((i+1))/8: $lang_name${NC}"
    echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}\n"

    set_device_language "$lang_code"
    clear_app_data
    launch_app
    capture_screenshots "$lang_code" "$lang_name"

    echo ""
  done

  echo -e "${GREEN}╔════════════════════════════════════════════════════════╗${NC}"
  echo -e "${GREEN}║   ✅ Screenshot capture complete!                     ║${NC}"
  echo -e "${GREEN}╚════════════════════════════════════════════════════════╝${NC}\n"

  echo -e "${YELLOW}Next steps:${NC}"
  echo "  1. Review screenshots in: fastlane/metadata/android/"
  echo "  2. Verify quality and content"
  echo "  3. Commit to GitHub: git add fastlane && git commit -m 'Add F-Droid screenshots'"
  echo "  4. Continue with testing and submission"
}

# Run main
main
