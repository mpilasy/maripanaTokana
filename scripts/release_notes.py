#!/usr/bin/env python3
# SPDX-FileCopyrightText: Orinasa Njarasoa
# SPDX-License-Identifier: GPL-3.0-or-later
"""Print the CHANGELOG.md notes for a version. Fail if the version has no notes.

Usage: release_notes.py VERSION   (for example 1.5.31 or v1.5.31)
"""
import re
import sys

CHANGELOG_PATH = "CHANGELOG.md"


def notes_for(version, changelog):
    """The text of the '## [VERSION]' section, without its heading, or None."""
    m = re.search(
        rf"^## \[{re.escape(version)}\][^\n]*\n(.*?)(?=^## |\Z)",
        changelog,
        re.MULTILINE | re.DOTALL,
    )
    if not m:
        return None
    text = m.group(1).strip()
    return text or None


def main():
    if len(sys.argv) != 2:
        sys.exit(__doc__.strip())
    version = sys.argv[1].removeprefix("v")
    with open(CHANGELOG_PATH) as f:
        notes = notes_for(version, f.read())
    if notes is None:
        sys.exit(f"{CHANGELOG_PATH} has no notes for {version}: add a '## [{version}]' section")
    print(notes)


if __name__ == "__main__":
    main()
