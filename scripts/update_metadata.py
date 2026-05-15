#!/usr/bin/env python3
import sys
import os
import hashlib

def get_sha256(file_path):
    sha256_hash = hashlib.sha256()
    with open(file_path, "rb") as f:
        for byte_block in iter(lambda: f.read(4096), b""):
            sha256_hash.update(byte_block)
    return sha256_hash.hexdigest()

def update_metadata(version_name, version_code, apk_url, sha256, yaml_path):
    with open(yaml_path, 'r') as f:
        content = f.read()

    v_pattern = f"  - versionName: {version_name}\n    versionCode: {version_code}"
    if v_pattern not in content:
        print(f"Error: Could not find version {version_name} ({version_code})")
        sys.exit(1)

    if "binaries:" in content and sha256 in content:
        print("Metadata already up to date.")
        return

    binaries_block = f"\n    binaries:\n      - url: {apk_url}\n        sha256: {sha256}"
    
    # Simple replacement: find the build entry and append the binaries block
    # We look for the end of the build entry (either next build or end of builds)
    entry_start = content.find(v_pattern)
    next_entry = content.find("  -", entry_start + len(v_pattern))
    if next_entry == -1:
        next_entry = content.find("AutoUpdateMode", entry_start)
    
    if next_entry == -1:
        new_content = content + binaries_block + "\n"
    else:
        new_content = content[:next_entry].rstrip() + binaries_block + "\n\n" + content[next_entry:]

    with open(yaml_path, 'w') as f:
        f.write(new_content)
    print("Successfully updated metadata.")

if __name__ == "__main__":
    if len(sys.argv) < 5:
        print("Usage: update_metadata.py <version_name> <version_code> <apk_url> <apk_path> <yaml_path>")
        sys.exit(1)
    
    v_name, v_code, url, apk, yaml_f = sys.argv[1:6]
    hash_val = get_sha256(apk)
    update_metadata(v_name, v_code, url, hash_val, yaml_f)
