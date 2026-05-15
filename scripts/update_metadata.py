#!/usr/bin/env python3
import sys
import os
import re

def update_metadata(version_name, version_code, apk_url, sha256, commit_hash, yaml_path):
    if not os.path.exists(yaml_path):
        print(f"Error: {yaml_path} not found")
        sys.exit(1)

    with open(yaml_path, 'r') as f:
        content = f.read()

    v_name = str(version_name).strip()
    v_code = str(version_code).strip()
    
    print(f"Targeting version {v_name} with code {v_code}")

    # Regex to find the build block for this version
    pattern = rf"(  - versionName: {re.escape(v_name)}\n    versionCode: {v_code}.*?)(?=\n  -|\nAutoUpdateMode|\nCurrentVersion|\Z)"
    
    match = re.search(pattern, content, re.DOTALL)
    if not match:
        print(f"Error: Could not find build entry for {v_name} ({v_code})")
        sys.exit(1)
        
    entry_content = match.group(1)
    
    # Update commit hash if provided
    if commit_hash:
        entry_content = re.sub(r"commit: [^\n]+", f"commit: {commit_hash}", entry_content)
        print(f"Updated commit to {commit_hash}")

    # Remove existing binaries block if any
    entry_content = re.sub(r"\n    binaries:\n      - url: [^\n]+\n        sha256: [^\n]+", "", entry_content)
    
    # Append the binaries block (no extra blank lines)
    new_bin_block = f"\n    binaries:\n      - url: {apk_url}\n        sha256: {sha256}"
    new_entry_content = entry_content.rstrip() + new_bin_block
        
    new_content = content[:match.start()] + new_entry_content + content[match.end():]

    with open(yaml_path, 'w') as f:
        f.write(new_content)
    print(f"Successfully updated metadata for {v_name}")

if __name__ == "__main__":
    if len(sys.argv) < 7:
        print("Usage: update_metadata.py <version_name> <version_code> <apk_url> <sha256> <commit_hash> <yaml_path>")
        sys.exit(1)
    
    v_name, v_code, url, sha256, commit, yaml_f = sys.argv[1:7]
    update_metadata(v_name, v_code, url, sha256, commit, yaml_f)
