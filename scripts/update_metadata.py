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

    # 1. Update/Add Build Entry
    # This regex matches the Build entry for the specific version
    pattern = rf"(  - versionName: {re.escape(v_name)}\n    versionCode: {v_code}.*?)(?=\n  -|\nAutoUpdateMode|\nCurrentVersion|\Z)"
    match = re.search(pattern, content, re.DOTALL)
    
    if match:
        entry_content = match.group(1)
        # Update commit hash
        entry_content = re.sub(r"commit: [^\n]+", f"commit: {commit_hash}", entry_content)
        # Ensure 'reproducible: Yes' is there
        if "reproducible: Yes" not in entry_content:
             entry_content = entry_content.rstrip() + "\n    reproducible: Yes"
        
        content = content[:match.start()] + entry_content + content[match.end():]
    else:
        print(f"Error: Could not find build entry for {v_name}")
        sys.exit(1)

    # 2. Ensure Top-level Binaries field is present (for F-Droid to download APK)
    if "Binaries:" not in content:
        repo_match = re.search(r"Repo: [^\n]+\n", content)
        bin_line = f"\nBinaries: https://github.com/mpilasy/maripanaTokana/releases/download/%v/maripanaTokana-%v-fdroid.apk\n"
        if repo_match:
            content = content[:repo_match.end()] + bin_line + content[repo_match.end():]
        else:
            content = bin_line + content

    # 3. Ensure AllowedAPKSigningKeys is present (Mandatory for Binaries)
    if "AllowedAPKSigningKeys:" not in content:
        fingerprint = "819fa7886022f2a5070dbef7d518f3e9469a302a577affc0311c092a6bf08c45"
        repo_match = re.search(r"Repo: [^\n]+\n", content)
        pin_line = f"\nAllowedAPKSigningKeys:\n  - {fingerprint}\n"
        if repo_match:
            content = content[:repo_match.end()] + pin_line + content[repo_match.end():]
        else:
            content = pin_line + content

    with open(yaml_path, 'w') as f:
        f.write(content)
    print("Successfully updated metadata.")

if __name__ == "__main__":
    if len(sys.argv) < 5:
        print("Usage: update_metadata.py <version_name> <version_code> <commit_hash> <yaml_path>")
        sys.exit(1)
    
    v_name, v_code, commit, yaml_f = sys.argv[1:5]
    # We don't need apk_url in the script anymore since we use a static template with %v
    update_metadata(v_name, v_code, "", commit, yaml_f)
