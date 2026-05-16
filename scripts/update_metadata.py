#!/usr/bin/env python3
import sys
import os
import re

def update_metadata(version_name, version_code, apk_url, commit_hash, yaml_path):
    if not os.path.exists(yaml_path):
        print(f"Error: {yaml_path} not found")
        sys.exit(1)

    with open(yaml_path, 'r') as f:
        content = f.read()

    v_name = str(version_name).strip()
    v_code = str(version_code).strip()
    
    print(f"Updating metadata for {v_name} ({v_code})")

    # 1. Ensure Top-level Binaries and AllowedAPKSigningKeys are present
    # (Values are static based on our verified setup)
    if "Binaries:" not in content:
        repo_match = re.search(r"Repo: [^\n]+\n", content)
        bin_line = "\nBinaries: https://github.com/mpilasy/maripanaTokana/releases/download/v%v/maripanaTokana-v%v-fdroid.apk\n"
        if repo_match:
            content = content[:repo_match.end()] + bin_line + content[repo_match.end():]
        else:
            content = bin_line + content
            
    if "AllowedAPKSigningKeys:" not in content:
        fingerprint = "819fa7886022f2a5070dbef7d518f3e9469a302a577affc0311c092a6bf08c45"
        pin_line = f"\nAllowedAPKSigningKeys:\n  - {fingerprint}\n"
        # Insert after Binaries:
        bin_match = re.search(r"Binaries: [^\n]+\n", content)
        if bin_match:
            content = content[:bin_match.end()] + pin_line + content[bin_match.end():]
        else:
            content = pin_line + content

    # 2. Update/Add Build Entry
    # Find the specific build block
    pattern = rf"(  - versionName: {re.escape(v_name)}\n    versionCode: {v_code}.*?)(?=\n  -|\nAutoUpdateMode|\nCurrentVersion|\Z)"
    match = re.search(pattern, content, re.DOTALL)
    
    if match:
        entry_content = match.group(1)
        # Update commit hash
        entry_content = re.sub(r"commit: [^\n]+", f"commit: {commit_hash}", entry_content)
        # Add 'binary:' (singular, lowercase) trigger for verification
        if "binary:" in entry_content:
            entry_content = re.sub(r"binary: [^\n]+", f"binary: {apk_url}", entry_content)
        else:
            entry_content = entry_content.rstrip() + f"\n    binary: {apk_url}"
        
        content = content[:match.start()] + entry_content + content[match.end():]
    else:
        # Fallback: Create entry if not found
        # (Usually it should be there because we bump version first)
        pass

    with open(yaml_path, 'w') as f:
        f.write(content)
    print("Successfully updated metadata.")

if __name__ == "__main__":
    if len(sys.argv) < 5:
        print("Usage: update_metadata.py <version_name> <version_code> <apk_url> <commit_hash> <yaml_path>")
        sys.exit(1)
    
    v_name, v_code, url, commit, yaml_f = sys.argv[1:6]
    update_metadata(v_name, v_code, url, commit, yaml_f)
