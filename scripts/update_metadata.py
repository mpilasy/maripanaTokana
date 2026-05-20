#!/usr/bin/env python3
import sys
import os
import re

BINARIES_URL = "https://github.com/mpilasy/maripanaTokana/releases/download/v%v/maripanaTokana-v%v.apk"
FINGERPRINT = "819fa7886022f2a5070dbef7d518f3e9469a302a577affc0311c092a6bf08c45"

def update_metadata(version_name, version_code, commit_hash, yaml_path):
    if not os.path.exists(yaml_path):
        print(f"Error: {yaml_path} not found")
        sys.exit(1)

    with open(yaml_path, 'r') as f:
        content = f.read()

    v_name = str(version_name).strip()
    v_code = str(version_code).strip()

    print(f"Updating metadata for {v_name} ({v_code}) at {commit_hash}")

    # Ensure Binaries: is present in F-Droid canonical block-scalar format.
    # The URL must be on the indented next line — fdroid rewritemeta enforces this.
    # No blank line between Repo: and Binaries: (rewritemeta removes it).
    if "Binaries:" not in content:
        repo_match = re.search(r"Repo: [^\n]+\n", content)
        bin_block = f"Binaries:\n  {BINARIES_URL}\n"
        if repo_match:
            content = content[:repo_match.end()] + bin_block + content[repo_match.end():]
        else:
            content = bin_block + content

    # Ensure AllowedAPKSigningKeys: is present as an inline single value
    # placed AFTER the Builds: block — fdroid rewritemeta enforces both.
    if "AllowedAPKSigningKeys:" not in content:
        pin_line = f"\nAllowedAPKSigningKeys: {FINGERPRINT}\n"
        insert_after = re.search(r"\nAutoUpdateMode:", content)
        if insert_after:
            content = content[:insert_after.start()] + pin_line + content[insert_after.start():]
        else:
            content = content.rstrip() + pin_line

    # Build entry for this version.
    new_entry = (
        f"  - versionName: {v_name}\n"
        f"    versionCode: {v_code}\n"
        f"    commit: {commit_hash}\n"
        f"    subdir: app\n"
        f"    gradle:\n"
        f"      - fdroid"
    )

    # Update existing entry for this version, or insert before AllowedAPKSigningKeys.
    pattern = rf"(  - versionName: {re.escape(v_name)}\n    versionCode: {v_code}.*?)(?=\n  -|\nAllowedAPKSigningKeys|\nAutoUpdateMode|\nCurrentVersion|\Z)"
    match = re.search(pattern, content, re.DOTALL)
    if match:
        content = content[:match.start()] + new_entry + "\n" + content[match.end():]
    else:
        insert_before = re.search(r"\nAllowedAPKSigningKeys:|\nAutoUpdateMode:|\nCurrentVersion:", content)
        if insert_before:
            content = content[:insert_before.start()] + "\n" + new_entry + "\n" + content[insert_before.start():]
        else:
            content = content.rstrip() + "\n" + new_entry + "\n"

    # Update CurrentVersion / CurrentVersionCode.
    content = re.sub(r"CurrentVersion: [^\n]+", f"CurrentVersion: {v_name}", content)
    content = re.sub(r"CurrentVersionCode: [^\n]+", f"CurrentVersionCode: {v_code}", content)

    with open(yaml_path, 'w') as f:
        f.write(content)
    print("Successfully updated metadata.")

if __name__ == "__main__":
    if len(sys.argv) < 5:
        print("Usage: update_metadata.py <version_name> <version_code> <commit_hash> <yaml_path>")
        sys.exit(1)

    v_name, v_code, commit, yaml_f = sys.argv[1:5]
    update_metadata(v_name, v_code, commit, yaml_f)
