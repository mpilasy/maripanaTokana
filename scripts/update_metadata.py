#!/usr/bin/env python3
import sys
import os
import hashlib
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

    if "binaries:" in content and sha256 in content:
        print(f"Version {version_name} already has this binary. Skipping.")
        return

    # More flexible regex that matches the structure precisely
    pattern = rf"(  - versionName: {re.escape(version_name)}\n    versionCode: {version_code}\n    commit: [^\n]+\n    subdir: [^\n]+\n    gradle:\n      - [^\n]+)"
    
    binaries_block = f"\n    binaries:\n      - url: {apk_url}\n        sha256: {sha256}"
    
    if re.search(pattern, content):
        print("Found primary match")
        new_content = re.sub(pattern, rf"\1{binaries_block}", content)
    else:
        print("Falling back to secondary match")
        pattern_fallback = rf"(  - versionName: {re.escape(version_name)}\n    versionCode: {version_code})"
        new_content = re.sub(pattern_fallback, rf"\1{binaries_block}", content)

    if new_content == content:
        print(f"Error: Could not update metadata for {version_name}. Pattern not found.")
        sys.exit(1)

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
