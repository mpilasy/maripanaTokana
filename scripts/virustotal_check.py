#!/usr/bin/env python3
# SPDX-FileCopyrightText: Orinasa Njarasoa
# SPDX-License-Identifier: GPL-3.0-or-later
"""Scan an APK on VirusTotal before a release is published.

Usage: VT_API_KEY=... scripts/virustotal_check.py FILE [FILE...]

Uploads each FILE (or uses the result VirusTotal already has for its SHA-256),
waits for the analyses, and prints the results. When VirusTotal already has the
file but no result yet (for example, an earlier run uploaded it), the script
waits for that result and does not upload the file again. The files are scanned at the
same time. Exits with 1 if one or more engines say "malicious" for a file.
"Suspicious" results are printed but do not fail. The public API allows 4
requests a minute, so the script waits 15 s between requests.
"""

import hashlib
import json
import os
import sys
import time
import urllib.error
import urllib.request
import uuid

API = "https://www.virustotal.com/api/v3"
WAIT = 15
TIMEOUT = 30 * 60


_last_request = 0.0


def request(method, path, key, body=None, content_type=None):
    global _last_request
    time.sleep(max(0.0, _last_request + WAIT - time.time()))
    _last_request = time.time()
    req = urllib.request.Request(API + path, data=body, method=method)
    req.add_header("x-apikey", key)
    if content_type:
        req.add_header("Content-Type", content_type)
    with urllib.request.urlopen(req, timeout=120) as resp:
        return json.load(resp)


def upload(path, key):
    boundary = uuid.uuid4().hex
    with open(path, "rb") as f:
        data = f.read()
    body = (
        f"--{boundary}\r\n"
        f'Content-Disposition: form-data; name="file"; filename="{os.path.basename(path)}"\r\n'
        "Content-Type: application/octet-stream\r\n\r\n"
    ).encode() + data + f"\r\n--{boundary}--\r\n".encode()
    result = request("POST", "/files", key, body, f"multipart/form-data; boundary={boundary}")
    return result["data"]["id"]


def report(stats, results, sha256):
    print(f"VirusTotal: {stats.get('malicious', 0)} malicious, "
          f"{stats.get('suspicious', 0)} suspicious, "
          f"{stats.get('undetected', 0)} undetected")
    for engine, r in sorted(results.items()):
        if r.get("category") in ("malicious", "suspicious"):
            print(f"  {engine}: {r['category']} ({r.get('result')})")
    print(f"https://www.virustotal.com/gui/file/{sha256}/detection")
    return 1 if stats.get("malicious", 0) > 0 else 0


def main():
    if len(sys.argv) < 2:
        print(__doc__, file=sys.stderr)
        return 2
    key = os.environ.get("VT_API_KEY")
    if not key:
        print("VT_API_KEY is not set", file=sys.stderr)
        return 2

    failed = 0
    pending = {}  # analysis ID -> (path, sha256)
    known = {}  # sha256 -> path, for files that VirusTotal has but has not analysed yet
    for path in sys.argv[1:]:
        with open(path, "rb") as f:
            sha256 = hashlib.sha256(f.read()).hexdigest()
        try:
            attrs = request("GET", f"/files/{sha256}", key)["data"]["attributes"]
            if attrs.get("last_analysis_results"):
                print(f"File: {path}\nSHA-256: {sha256}\nVirusTotal already has a result for this file.")
                failed |= report(attrs["last_analysis_stats"], attrs["last_analysis_results"], sha256)
            else:
                known[sha256] = path
                print(f"VirusTotal has {path} but no result yet. Waiting for the analysis...")
            continue
        except urllib.error.HTTPError as e:
            if e.code != 404:
                raise
        try:
            pending[upload(path, key)] = (path, sha256)
            print(f"Uploaded {path}. Waiting for the analysis...")
        except urllib.error.HTTPError as e:
            # 409: the file is already being uploaded or analysed
            if e.code != 409:
                raise
            known[sha256] = path
            print(f"VirusTotal is already analysing {path}. Waiting for the analysis...")

    deadline = time.time() + TIMEOUT
    while (pending or known) and time.time() < deadline:
        for analysis, (path, sha256) in list(pending.items()):
            attrs = request("GET", f"/analyses/{analysis}", key)["data"]["attributes"]
            if attrs.get("status") == "completed":
                print(f"File: {path}\nSHA-256: {sha256}")
                failed |= report(attrs["stats"], attrs.get("results", {}), sha256)
                del pending[analysis]
        for sha256, path in list(known.items()):
            try:
                attrs = request("GET", f"/files/{sha256}", key)["data"]["attributes"]
            except urllib.error.HTTPError as e:
                if e.code != 404:
                    raise
                continue
            if attrs.get("last_analysis_results"):
                print(f"File: {path}\nSHA-256: {sha256}")
                failed |= report(attrs["last_analysis_stats"], attrs["last_analysis_results"], sha256)
                del known[sha256]
    for path in [p for p, _ in pending.values()] + list(known.values()):
        print(f"The VirusTotal analysis of {path} did not complete in time", file=sys.stderr)
        failed = 1
    return failed


if __name__ == "__main__":
    sys.exit(main())
