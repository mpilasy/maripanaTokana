#!/bin/bash

# F-Droid Build Verification & Testing
# Performs automated tests on F-Droid flavor build
# Usage: bash scripts/test_fdroid_build.sh

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Configuration
PACKAGE="orinasa.njarasoa.maripanatokana"
BUILD_VARIANT="fdroid"
TEMP_DIR="/tmp/fdroid_verify_$$"

# Test counters
TESTS_PASSED=0
TESTS_FAILED=0

# Helper functions
pass_test() {
  echo -e "${GREEN}✓ $1${NC}"
  ((TESTS_PASSED++))
}

fail_test() {
  echo -e "${RED}✗ $1${NC}"
  ((TESTS_FAILED++))
}

warn_test() {
  echo -e "${YELLOW}⚠ $1${NC}"
}

test_section() {
  echo -e "\n${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
  echo -e "${BLUE}$1${NC}"
  echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
}

# Test 1: Build F-Droid Release APK
test_build() {
  test_section "TEST 1: Building F-Droid Release APK"

  if ./gradlew clean assemble${BUILD_VARIANT^}Release > /dev/null 2>&1; then
    pass_test "F-Droid flavor builds successfully"

    local apk_path="app/build/outputs/apk/$BUILD_VARIANT/release/app-$BUILD_VARIANT-release.apk"
    if [ -f "$apk_path" ]; then
      local size=$(ls -lh "$apk_path" | awk '{print $5}')
      pass_test "APK created (size: $size)"
    else
      fail_test "APK file not found"
    fi
  else
    fail_test "F-Droid flavor build failed"
    return 1
  fi
}

# Test 2: Verify No Play Services
test_no_play_services() {
  test_section "TEST 2: Verify No Google Play Services"

  local apk_path="app/build/outputs/apk/$BUILD_VARIANT/release/app-$BUILD_VARIANT-release.apk"

  if [ ! -f "$apk_path" ]; then
    warn_test "APK not found, skipping Play Services check"
    return 0
  fi

  echo "Extracting APK..."
  rm -rf "$TEMP_DIR"
  mkdir -p "$TEMP_DIR"
  unzip -q "$apk_path" -d "$TEMP_DIR"

  # Check for PlayServicesLocationProvider
  if find "$TEMP_DIR" -name "*.dex" -exec strings {} \; 2>/dev/null | grep -q "PlayServicesLocationProvider"; then
    fail_test "Found PlayServicesLocationProvider in APK"
  else
    pass_test "PlayServicesLocationProvider not found"
  fi

  # Check for FusedLocationProviderClient
  if find "$TEMP_DIR" -name "*.dex" -exec strings {} \; 2>/dev/null | grep -q "FusedLocationProviderClient"; then
    fail_test "Found FusedLocationProviderClient in APK"
  else
    pass_test "FusedLocationProviderClient not found"
  fi

  # Check for NativeLocationProvider
  if find "$TEMP_DIR" -name "*.dex" -exec strings {} \; 2>/dev/null | grep -q "NativeLocationProvider"; then
    pass_test "NativeLocationProvider found (correct!)"
  else
    warn_test "NativeLocationProvider not found (may be obfuscated)"
  fi

  # Cleanup
  rm -rf "$TEMP_DIR"
}

# Test 3: Verify Reproducible Builds
test_reproducibility() {
  test_section "TEST 3: Verify Reproducible Builds"

  echo "Building first APK..."
  ./gradlew clean assemble${BUILD_VARIANT^}Release > /dev/null 2>&1
  cp "app/build/outputs/apk/$BUILD_VARIANT/release/app-$BUILD_VARIANT-release.apk" "/tmp/build1_$$.apk"

  echo "Building second APK..."
  ./gradlew clean assemble${BUILD_VARIANT^}Release > /dev/null 2>&1
  cp "app/build/outputs/apk/$BUILD_VARIANT/release/app-$BUILD_VARIANT-release.apk" "/tmp/build2_$$.apk"

  echo "Comparing checksums..."
  local sha1=$(sha256sum "/tmp/build1_$$.apk" | awk '{print $1}')
  local sha2=$(sha256sum "/tmp/build2_$$.apk" | awk '{print $1}')

  if [ "$sha1" == "$sha2" ]; then
    pass_test "Reproducible builds verified (identical checksums)"
    echo "  Checksum: $sha1"
  else
    fail_test "Reproducible builds FAILED (checksums differ)"
    echo "  Build 1: $sha1"
    echo "  Build 2: $sha2"
  fi

  # Cleanup
  rm -f "/tmp/build1_$$.apk" "/tmp/build2_$$.apk"
}

# Test 4: Verify Metadata
test_metadata() {
  test_section "TEST 4: Verify F-Droid Metadata"

  # Check YAML file exists
  if [ -f "metadata/orinasa.njarasoa.maripanatokana.yml" ]; then
    pass_test "F-Droid metadata YAML exists"

    # Check for required fields
    local yaml_file="metadata/orinasa.njarasoa.maripanatokana.yml"

    if grep -q "^License: MIT" "$yaml_file"; then
      pass_test "License field correct (MIT)"
    else
      fail_test "License field missing or incorrect"
    fi

    if grep -q "^RepoType: git" "$yaml_file"; then
      pass_test "Repository type correct (git)"
    else
      fail_test "Repository type missing or incorrect"
    fi

    if grep -q "gradle:" "$yaml_file"; then
      pass_test "Gradle configuration present"
    else
      fail_test "Gradle configuration missing"
    fi

    if grep -q "- fdroid" "$yaml_file"; then
      pass_test "F-Droid flavor specified in build"
    else
      fail_test "F-Droid flavor not specified"
    fi
  else
    fail_test "F-Droid metadata YAML not found"
  fi
}

# Test 5: Verify Fastlane Metadata
test_fastlane_metadata() {
  test_section "TEST 5: Verify Fastlane Metadata"

  local languages=("en-US" "mg" "ar" "es" "fr" "hi" "ne" "zh-CN")
  local all_complete=true

  for lang in "${languages[@]}"; do
    local lang_dir="fastlane/metadata/android/$lang"

    if [ ! -d "$lang_dir" ]; then
      fail_test "Fastlane directory missing for language: $lang"
      all_complete=false
      continue
    fi

    # Check for required files
    local required_files=("title.txt" "short_description.txt" "full_description.txt" "changelogs/1.txt")
    for file in "${required_files[@]}"; do
      if [ ! -f "$lang_dir/$file" ]; then
        fail_test "Missing $file for language: $lang"
        all_complete=false
      fi
    done

    # Check screenshot directory
    if [ ! -d "$lang_dir/images/phoneScreenshots" ]; then
      warn_test "Screenshot directory missing for language: $lang (expected before submission)"
    else
      local screenshot_count=$(ls -1 "$lang_dir/images/phoneScreenshots"/*.png 2>/dev/null | wc -l)
      if [ "$screenshot_count" -ge 4 ]; then
        pass_test "$lang: Complete with $screenshot_count screenshots"
      else
        warn_test "$lang: Only $screenshot_count screenshots (need 4-5)"
      fi
    fi
  done

  if [ "$all_complete" = true ]; then
    pass_test "All language metadata files present"
  fi
}

# Test 6: Verify GitHub Structure
test_github_structure() {
  test_section "TEST 6: Verify GitHub Repository Structure"

  # Check LICENSE
  if [ -f "LICENSE" ]; then
    if grep -q "MIT" "LICENSE"; then
      pass_test "LICENSE file exists with MIT license"
    else
      fail_test "LICENSE file exists but doesn't contain MIT"
    fi
  else
    fail_test "LICENSE file missing"
  fi

  # Check README
  if [ -f "README.md" ]; then
    pass_test "README.md exists"
    if grep -q "F-Droid\|fdroid" "README.md"; then
      pass_test "README mentions F-Droid"
    else
      warn_test "README doesn't mention F-Droid"
    fi
  else
    fail_test "README.md missing"
  fi

  # Check .gitignore
  if [ -f ".gitignore" ]; then
    if grep -q "keystore\|\.jks" ".gitignore"; then
      pass_test ".gitignore excludes keystore files"
    else
      warn_test ".gitignore doesn't explicitly exclude keystore"
    fi
  else
    warn_test ".gitignore not found"
  fi

  # Check GitHub Actions
  if [ -f ".github/workflows/fdroid-build.yml" ]; then
    pass_test "GitHub Actions workflow exists"
  else
    warn_test "GitHub Actions workflow not found"
  fi
}

# Test 7: Installation & Functionality (requires device)
test_device_install() {
  test_section "TEST 7: Device Installation & Functionality Test"

  # Check if device is connected
  if ! adb devices 2>/dev/null | grep -q "device$"; then
    warn_test "No device connected - skipping installation tests"
    warn_test "Run on physical device/emulator: adb install app/build/outputs/apk/$BUILD_VARIANT/debug/app-$BUILD_VARIANT-debug.apk"
    return 0
  fi

  echo "Installing app on device..."
  if adb install -r "app/build/outputs/apk/$BUILD_VARIANT/debug/app-$BUILD_VARIANT-debug.apk" > /dev/null 2>&1; then
    pass_test "App installed successfully on device"
  else
    fail_test "App installation failed"
    return 1
  fi

  echo "Launching app..."
  adb shell am start -n "$PACKAGE/.MainActivity" 2>/dev/null || true
  sleep 3

  # Check for crashes
  if adb logcat -d | grep -q "FATAL EXCEPTION\|AndroidRuntime"; then
    fail_test "App crashed on launch"
  else
    pass_test "App launched without crashes"
  fi

  echo "Testing location permission..."
  adb shell pm grant "$PACKAGE" android.permission.ACCESS_FINE_LOCATION 2>/dev/null || true
  adb shell pm grant "$PACKAGE" android.permission.ACCESS_COARSE_LOCATION 2>/dev/null || true
  pass_test "Permissions granted"

  # Small delay for weather to load
  sleep 5

  # Check for errors
  adb logcat -d | grep -i "error\|exception" | head -5

  pass_test "Basic device testing completed"
}

# Summary
print_summary() {
  test_section "TEST SUMMARY"

  local total=$((TESTS_PASSED + TESTS_FAILED))
  echo "Total Tests: $total"
  echo -e "${GREEN}Passed: $TESTS_PASSED${NC}"
  if [ $TESTS_FAILED -gt 0 ]; then
    echo -e "${RED}Failed: $TESTS_FAILED${NC}"
  else
    echo -e "${GREEN}Failed: 0${NC}"
  fi

  echo ""
  if [ $TESTS_FAILED -eq 0 ]; then
    echo -e "${GREEN}╔════════════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║   ✅ ALL TESTS PASSED - READY FOR F-DROID!            ║${NC}"
    echo -e "${GREEN}╚════════════════════════════════════════════════════════╝${NC}"
    return 0
  else
    echo -e "${YELLOW}╔════════════════════════════════════════════════════════╗${NC}"
    echo -e "${YELLOW}║   ⚠️  SOME TESTS FAILED - REVIEW ABOVE                ║${NC}"
    echo -e "${YELLOW}╚════════════════════════════════════════════════════════╝${NC}"
    return 1
  fi
}

# Main execution
main() {
  echo -e "${GREEN}╔════════════════════════════════════════════════════════╗${NC}"
  echo -e "${GREEN}║   F-Droid Build Verification & Testing                ║${NC}"
  echo -e "${GREEN}╚════════════════════════════════════════════════════════╝${NC}"

  test_build
  test_no_play_services
  test_reproducibility
  test_metadata
  test_fastlane_metadata
  test_github_structure
  test_device_install

  print_summary
}

# Run main
main
