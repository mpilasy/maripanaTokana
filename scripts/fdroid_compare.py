#!/usr/bin/env python3
# SPDX-FileCopyrightText: Orinasa Njarasoa
# SPDX-License-Identifier: GPL-3.0-or-later
"""Compare our signed APK with F-Droid's unsigned build of the same version.

Usage: scripts/fdroid_compare.py SIGNED_APK UNSIGNED_APK

Uses the same fdroidserver function that "fdroid build" uses: it copies the
signature of SIGNED_APK into UNSIGNED_APK and checks that the result is the
same APK. Needs fdroidserver and an Android SDK with apksigner (ANDROID_HOME).
Exits with 1 if the APKs are not the same.
"""

import os
import sys
import tempfile

from fdroidserver import common


def main():
    if len(sys.argv) != 3:
        print(__doc__, file=sys.stderr)
        return 2
    signed, unsigned = (os.path.realpath(p) for p in sys.argv[1:])
    with tempfile.TemporaryDirectory() as d:
        # read_config reads config.yml in the current directory: use an empty one.
        os.chdir(d)
        common.config = common.read_config() if hasattr(common, "read_config") else {}
        err = common.verify_apks(signed, unsigned, d)
    print(err or "compared built binary to supplied reference binary successfully")
    return 1 if err else 0


if __name__ == "__main__":
    sys.exit(main())
