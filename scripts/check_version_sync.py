#!/usr/bin/env python3
# SPDX-FileCopyrightText: Orinasa Njarasoa
# SPDX-License-Identifier: GPL-3.0-or-later
"""Fail if the Android and web version strings have drifted apart,
or if the current version has no release notes."""
import json
import os
import re
import sys

from release_notes import CHANGELOG_PATH, notes_for

GRADLE_PATH = "app/build.gradle.kts"
PACKAGE_JSON_PATH = "web/package.json"
PACKAGE_LOCK_PATH = "web/package-lock.json"
FOOTER_PATH = "web/src/lib/components/Footer.svelte"
FASTLANE_DIR = "fastlane/metadata/android"


def read(path):
    with open(path) as f:
        return f.read()


def get_android_version():
    m = re.search(r'versionName\s*=\s*"([^"]+)"', read(GRADLE_PATH))
    if not m:
        sys.exit(f"Could not find versionName in {GRADLE_PATH}")
    return m.group(1)


def get_package_json_version():
    return json.loads(read(PACKAGE_JSON_PATH))["version"]


def get_package_lock_versions():
    data = json.loads(read(PACKAGE_LOCK_PATH))
    return data["version"], data["packages"][""]["version"]


def get_footer_version():
    m = re.search(r'class="version">v([^<]+)</span>', read(FOOTER_PATH))
    if not m:
        sys.exit(f"Could not find version span in {FOOTER_PATH}")
    return m.group(1)


def get_android_version_code():
    m = re.search(r'versionCode\s*=\s*(\d+)', read(GRADLE_PATH))
    if not m:
        sys.exit(f"Could not find versionCode in {GRADLE_PATH}")
    return m.group(1)


def missing_release_notes(version, code):
    """The release notes that are missing or empty for this version."""
    missing = []
    if notes_for(version, read(CHANGELOG_PATH)) is None:
        missing.append(f"{CHANGELOG_PATH} (## [{version}] section)")
    for locale in sorted(os.listdir(FASTLANE_DIR)):
        path = f"{FASTLANE_DIR}/{locale}/changelogs/{code}.txt"
        if not os.path.isfile(path) or not read(path).strip():
            missing.append(path)
    return missing


def main():
    android_version = get_android_version()
    lock_top, lock_nested = get_package_lock_versions()

    sources = {
        f"{GRADLE_PATH} (versionName)": android_version,
        f"{PACKAGE_JSON_PATH} (version)": get_package_json_version(),
        f"{PACKAGE_LOCK_PATH} (top-level version)": lock_top,
        f"{PACKAGE_LOCK_PATH} (packages.\"\".version)": lock_nested,
        f"{FOOTER_PATH} (displayed version)": get_footer_version(),
    }

    mismatched = {k: v for k, v in sources.items() if v != android_version}
    if mismatched:
        print(f"Version mismatch: Android is {android_version!r} but:")
        for path, version in mismatched.items():
            print(f"  {path} = {version!r}")
        sys.exit(1)

    missing = missing_release_notes(android_version, get_android_version_code())
    if missing:
        print(f"Release notes missing for {android_version}:")
        for path in missing:
            print(f"  {path}")
        sys.exit(1)

    print(f"All version strings match: {android_version}, and its release notes exist")


if __name__ == "__main__":
    main()
