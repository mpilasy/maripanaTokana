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
    if not os.path.exists(yaml_path):
        print(f"Error: {yaml_path} not found")
        sys.exit(1)

    with open(yaml_path, 'r') as f:
        lines = f.readlines()

    new_lines = []
    found_start = False
    updated = False
    
    v_name_line = f"- versionName: {version_name}"
    v_code_line = f"versionCode: {version_code}"

    for i, line in enumerate(lines):
        new_lines.append(line)
        if v_name_line in line and i + 1 < len(lines) and v_code_line in lines[i+1]:
            found_start = True
            print(f"Found version {version_name} at line {i+1}")
            
        # If we found the start, look for the right place to insert
        if found_start and not updated:
            # Check if we are at the end of the build entry
            # The next build entry starts with '  -' or we hit AutoUpdateMode
            is_end_of_entry = False
            if i + 1 < len(lines):
                next_line = lines[i+1]
                if next_line.startswith("  -") or next_line.startswith("AutoUpdateMode") or next_line.startswith("CurrentVersion"):
                    is_end_of_entry = True
            else:
                is_end_of_entry = True
            
            # Also check if binaries already exists
            if "binaries:" in line:
                print(f"Warning: binaries block already exists for {version_name}. Skipping.")
                updated = True
                continue

            if is_end_of_entry:
                print(f"Inserting binaries block after line {i+1}")
                new_lines.append("    binaries:\n")
                new_lines.append(f"      - url: {apk_url}\n")
                new_lines.append(f"        sha256: {sha256}\n")
                updated = True

    if not updated:
        print(f"Error: Could not find build entry for {version_name} ({version_code}) to update.")
        sys.exit(1)

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
