# F-Droid Publishing & Update Guide

This guide describes how to publish new versions of maripànaTokana to F-Droid.

## Automatic Updates

The app is configured for **Auto-Update** via GitHub tags. F-Droid's build server periodically checks for new git tags that match the version pattern.

Current `metadata/orinasa.njarasoa.maripanatokana.yml` configuration:
```yaml
AutoUpdateMode: Version v%v
UpdateCheckMode: Tags
```

This means when you push a tag like `v1.1`, F-Droid will:
1. Detect the new tag.
2. Update the `versionName` and `versionCode` in its internal metadata.
3. Automatically start a build of the new version.

## Manual Update Process

If you need to manually trigger an update or if the auto-update fails, follow these steps:

### 1. Prepare the Code
1. Update `versionCode` and `versionName` in `app/build.gradle.kts`.
2. Ensure `CHANGELOG.md` or localized changelogs are updated.

### 2. Update Localized Changelogs
F-Droid uses Fastlane metadata. For a new version (e.g., versionCode 2):
1. Create a new file for each of the 8 languages: `fastlane/metadata/android/{locale}/changelogs/2.txt`.
2. Add a brief summary of what's new in that language.

### 3. Create and Push Tag
```bash
git tag -a v1.1 -m "Release version 1.1"
git push origin v1.1
```

### 4. Verify Build Locally
Before assuming F-Droid will build it successfully, test it yourself:
```bash
# Generate i18n strings
node shared/i18n/generate-android-strings.js

# Build F-Droid flavor
./gradlew clean assembleFdroidRelease
```

### 5. (Optional) Manual Metadata Update
If you need to change build instructions or other metadata:
1. Fork [fdroiddata](https://gitlab.com/fdroid/fdroiddata).
2. Edit `metadata/orinasa.njarasoa.maripanatokana.yml` in your fork.
3. Create a Merge Request to the main fdroiddata repository.

## Adding/Updating Screenshots

If the UI changes significantly, you should update the screenshots:

1. Capture 4-5 screenshots per language (8 locales = 32-40 total).
2. Save to: `fastlane/metadata/android/{locale}/images/phoneScreenshots/`.
3. Name them: `1.png`, `2.png`, `3.png`, `4.png`, `5.png`.
4. Commit and push to GitHub. F-Droid will pick them up automatically for the next release.

## Troubleshooting Builds

If a build fails on the F-Droid server:
1. Check the build logs on [f-droid.org](https://f-droid.org/packages/orinasa.njarasoa.maripanatokana/).
2. Common issue: Missing prebuild steps. Ensure `prebuild` in the YAML includes `node shared/i18n/generate-android-strings.js`.
3. Common issue: Dependency changes. If you added a new library, ensure it's open-source and compatible with F-Droid (no proprietary binaries).

## Links
- [F-Droid Package Page](https://f-droid.org/packages/orinasa.njarasoa.maripanatokana/)
- [F-Droid Build Metadata Reference](https://f-droid.org/docs/Build_Metadata_Reference/)
- [F-Droid Data Repository](https://gitlab.com/fdroid/fdroiddata)
