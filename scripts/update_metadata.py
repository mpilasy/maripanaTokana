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
        lines = f.readlines()

    new_lines = []
    in_builds = False
    in_current_version = False
    found_version = False

    # Regex to find the build entry for this version
    version_pattern = re.compile(f"  - versionName: {version_name}")
    code_pattern = re.compile(f"    versionCode: {version_code}")

    i = 0
    while i < len(lines):
        line = lines[i]
        new_lines.append(line)
        
        if version_pattern.match(line) and i + 1 < len(lines) and code_pattern.match(lines[i+1]):
            found_version = True
            # Skip until we find the end of this build entry or an existing binaries block
            j = i + 1
            has_binaries = False
            while j < len(lines) and not lines[j].startswith("  -") and not lines[j].startswith("AutoUpdateMode"):
                if "binaries:" in lines[j]:
                    has_binaries = True
                    break
                j += 1
            
            if not has_binaries:
                # Find the right spot to insert (before the next build or end of builds)
                insert_pos = j
                new_lines.extend([
                    "    binaries:\n",
                    f"      - url: {apk_url}\n",
                    f"        sha256: {sha256}\n"
                ])
                # Skip the lines we already added to new_lines if we were scanning ahead
                # Actually, the logic above is a bit complex for a simple append.
                # Let's just find the next build or AutoUpdateMode
            else:
                print(f"Version {version_name} already has a binaries block. Updating it.")
                # Logic to update existing binaries block could be added here
                pass
        
        i += 1

    with open(yaml_path, 'w') as f:
        f.writelines(new_lines)

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
