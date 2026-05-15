#!/usr/bin/env python3
import sys
import os
import hashlib
import yaml
import re

def get_sha256(file_path):
    sha256_hash = hashlib.sha256()
    with open(file_path, "rb") as f:
        for byte_block in iter(lambda: f.read(4096), b""):
            sha256_hash.update(byte_block)
    return sha256_hash.hexdigest()

def update_metadata(version_name, version_code, apk_url, sha256, yaml_path):
    if not os.path.exists(yaml_path):
        print(f"Error: {yaml_path} not found")
        sys.exit(1)

    with open(yaml_path, 'r') as f:
        content = f.read()

    # Find the specific build entry
    pattern = rf"(  - versionName: {re.escape(version_name)}\n    versionCode: {version_code}\n    commit: [^\n]+\n    subdir: [^\n]+\n    gradle:\n      - [^\n]+)\n"
    
    replacement = r"\1\n    binaries:\n      - url: " + apk_url + "\n        sha256: " + sha256 + "\n"
    
    if "binaries:" in content and apk_url in content:
        print(f"Version {version_name} already has this binary. Skipping.")
        return

    new_content = re.sub(pattern, replacement, content)
    
    if new_content == content:
        print(f"Warning: Could not find build entry for {version_name} ({version_code}) to update.")
        # Fallback for slightly different structures
        pattern_short = rf"(  - versionName: {re.escape(version_name)}\n    versionCode: {version_code}.*?\n)\s*(  -|AutoUpdateMode)"
        replacement_short = r"\1    binaries:\n      - url: " + apk_url + "\n        sha256: " + sha256 + "\n\n\2"
        new_content = re.sub(pattern_short, replacement_short, content, flags=re.DOTALL)

    with open(yaml_path, 'w') as f:
        f.write(new_content)

if __name__ == "__main__":
    if len(sys.argv) < 5:
        print("Usage: update_metadata.py <version_name> <version_code> <apk_url> <apk_path> <yaml_path>")
        sys.exit(1)

    v_name = sys.argv[1]
    v_code = sys.argv[2]
    url = sys.argv[3]
    apk = sys.argv[4]
    yaml_f = sys.argv[5]

    hash_val = get_sha256(apk)
    print(f"Updating metadata for {v_name} ({v_code})")
    print(f"APK: {apk}")
    print(f"SHA256: {hash_val}")
    print(f"URL: {url}")

    update_metadata(v_name, v_code, url, hash_val, yaml_f)
