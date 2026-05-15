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
    with open(yaml_path, 'r') as f:
        lines = f.readlines()

    new_lines = []
    found_entry = False
    updated = False
    
    # Clean version strings
    v_name = str(version_name).strip()
    v_code = str(version_code).strip()

    print(f"Target: versionName: {v_name}, versionCode: {v_code}")

    for i, line in enumerate(lines):
        new_lines.append(line)
        # Match versionName (allowing for quotes and spaces)
        if re.search(rf"versionName:\s*[\"']?{re.escape(v_name)}[\"']?", line):
            # Check if next line is the correct versionCode
            if i + 1 < len(lines) and re.search(rf"versionCode:\s*{v_code}", lines[i+1]):
                found_entry = True
                print(f"Found build entry for {v_name} ({v_code}) at line {i+1}")
        
        if found_entry and not updated:
            # Check if we hit the end of the builds block or the next build
            is_end = False
            if i + 1 < len(lines):
                next_line = lines[i+1]
                if next_line.startswith("  -") or next_line.startswith("AutoUpdateMode") or next_line.startswith("CurrentVersion"):
                    is_end = True
            else:
                is_end = True
                
            if is_end:
                # Check if binaries block already exists
                has_binaries = False
                for j in range(max(0, i-5), i+1):
                    if "binaries:" in lines[j]:
                        has_binaries = True
                        break
                
                if has_binaries:
                    print("Binaries block already exists. Skipping.")
                else:
                    print(f"Appending binaries block after line {i+1}")
                    new_lines.append("    binaries:\n")
                    new_lines.append(f"      - url: {apk_url}\n")
                    new_lines.append(f"        sha256: {sha256}\n")
                updated = True

    if not updated:
        print(f"Error: Could not update metadata for {v_name} ({v_code})")
        sys.exit(1)

    with open(yaml_path, 'w') as f:
        f.writelines(new_lines)

if __name__ == "__main__":
    if len(sys.argv) < 5:
        print("Usage: update_metadata.py <version_name> <version_code> <apk_url> <apk_path> <yaml_path>")
        sys.exit(1)
    
    v_name, v_code, url, apk, yaml_f = sys.argv[1:6]
    hash_val = get_sha256(apk)
    update_metadata(v_name, v_code, url, hash_val, yaml_f)
