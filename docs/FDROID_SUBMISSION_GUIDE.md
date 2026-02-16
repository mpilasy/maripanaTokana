# F-Droid Submission Guide

Complete step-by-step guide to submit maripànaTokana to F-Droid.

## Table of Contents

1. [Pre-Submission Checklist](#pre-submission-checklist)
2. [F-Droid Data Repository Setup](#f-droid-data-repository-setup)
3. [Metadata Preparation](#metadata-preparation)
4. [Screenshots Preparation](#screenshots-preparation)
5. [Creating Merge Request](#creating-merge-request)
6. [Handling F-Droid Review](#handling-f-droid-review)
7. [After Approval](#after-approval)
8. [Future Updates](#future-updates)

## Pre-Submission Checklist

Before submitting, complete all these items:

### Code & Build
- [x] **Release Tagged**
  - [x] Git tag created: `git tag -a v1.0 -m "..."`
  - [x] Tag pushed: `git push origin v1.0`

- [x] **Both Flavors Build**
  - [x] Standard flavor: `./gradlew assembleStandardRelease`
  - [x] F-Droid flavor: `./gradlew assembleFdroidRelease`
  - [x] No warnings/errors in build output

- [x] **No Proprietary Dependencies**
  - [x] Verified: No PlayServicesLocationProvider in F-Droid APK
  - [x] Verified: No FusedLocationProviderClient in F-Droid APK
  - [x] Verified: Uses only native Android APIs

- [x] **Reproducible Builds**
  - [ ] Build twice and compare checksums (see TESTING_GUIDE.md)
  - [ ] Checksums should be identical

### Repository
- [x] **GitHub Repository Public**
  - [x] Repo is public: https://github.com/mpilasy/maripanaTokana
  - [x] No private branches or forks blocking access

- [x] **License File**
  - [x] LICENSE file in root directory
  - [x] MIT License text complete

- [x] **Documentation**
  - [x] README.md comprehensive
  - [x] FDROID.md included (in docs/)
  - [x] FDROID_IMPLEMENTATION_SUMMARY.md included (in docs/)
  - [x] TESTING_GUIDE.md included (in docs/)

- [x] **.gitignore Proper**
  - [x] Excludes keystore files
  - [x] Excludes build artifacts
  - [x] Excludes sensitive files (no credentials)

### Metadata
- [x] **Fastlane Metadata Complete**
  - [x] 8 language directories created
  - [x] All title.txt files created
  - [x] All short_description.txt files created
  - [x] All full_description.txt files created
  - [x] All changelogs/1.txt files created

- [ ] **Screenshots Captured**
  - [ ] 4-5 screenshots per language (8 locales)
  - [ ] Saved to: `fastlane/metadata/android/{locale}/images/phoneScreenshots/`
  - [ ] Named: `1.png`, `2.png`, `3.png`, `4.png`, `5.png`
  - [ ] Proper dimensions (320-3840px, 4:3 to 16:9 aspect ratio)
  - [ ] PNG or JPEG format

- [x] **F-Droid Metadata YAML**
  - [x] metadata/orinasa.njarasoa.maripanatokana.yml created
  - [x] YAML syntax valid
  - [x] All required fields present

### Testing
- [ ] **Functional Testing Complete** (see TESTING_GUIDE.md)
  - [ ] App launches without crash
  - [ ] Location permission works
  - [ ] Weather data loads
  - [ ] All 8 languages tested
  - [ ] Dual-unit toggle works
  - [ ] Pull-to-refresh works
  - [ ] Widgets function properly
  - [ ] No Google Play Services dependency

- [ ] **Device Testing**
  - [ ] Tested on emulator (API 24)
  - [ ] Tested on modern device (API 30+)
  - [ ] Works on portrait orientation
  - [ ] Performance acceptable (startup < 3s)

## F-Droid Data Repository Setup

### Step 1: Create GitLab Account

If you don't have one:
1. Go to https://gitlab.com/user/sign_up
2. Create account with email
3. Verify email

### Step 2: Fork F-Droid Data Repository

1. Go to https://gitlab.com/fdroid/fdroiddata
2. Click "Fork" button
3. Keep default fork settings
4. You'll have your own fork at: `https://gitlab.com/{your-username}/fdroiddata`

### Step 3: Clone Your Fork

```bash
# Clone your fork
git clone https://gitlab.com/{your-username}/fdroiddata.git
cd fdroiddata

# Add upstream remote for updates
git remote add upstream https://gitlab.com/fdroid/fdroiddata.git
```

Replace `{your-username}` with your actual GitLab username.

### Step 4: Create Feature Branch

```bash
# Create new branch for this app
git checkout -b add-maripanatokana

# Or if adding a new version to existing app:
git checkout -b update-maripanatokana-v1.1
```

## Metadata Preparation

### Step 1: Copy F-Droid Metadata

```bash
# From your maripanaTokana repo
cd /path/to/maripanaTokana

# Copy metadata to fdroiddata
cp metadata/orinasa.njarasoa.maripanatokana.yml \
   /path/to/fdroiddata/metadata/

# Verify it was copied
ls -la /path/to/fdroiddata/metadata/orinasa.njarasoa.maripanatokana.yml
```

### Step 2: Validate YAML Syntax

```bash
# Install yamllint (if not already installed)
pip install yamllint

# Validate the YAML file
yamllint /path/to/fdroiddata/metadata/orinasa.njarasoa.maripanatokana.yml

# Should show: OK (no errors)
```

### Step 3: Check Metadata File

Ensure the metadata file contains:

```yaml
Categories:
  - Science & Education          # ✓ Correct category
License: MIT                       # ✓ Correct license
AuthorName: Orinasa Njarasoa      # ✓ Your name
AuthorWebSite: https://github.com/mpilasy  # ✓ Your profile
SourceCode: https://github.com/mpilasy/maripanaTokana  # ✓ GitHub link
IssueTracker: https://github.com/mpilasy/maripanaTokana/issues  # ✓ Issues link

RepoType: git
Repo: https://github.com/mpilasy/maripanaTokana.git

Builds:
  - versionName: '1.0'           # ✓ Correct version
    versionCode: 1               # ✓ Correct code
    commit: v1.0                 # ✓ Tag exists in repo
    subdir: app                  # ✓ Subdir correct
    gradle:
      - fdroid                   # ✓ Flavor specified
    prebuild: ...                # ✓ Build instructions
```

## Screenshots Preparation

### Step 1: Capture Screenshots

See TESTING_GUIDE.md "Screenshot Capture Guidelines" section for detailed instructions.

Quick summary:
```bash
# For each of 8 languages:
# 1. Change device language to target language
# 2. Start app fresh
# 3. Capture 4-5 screenshots:
#    - Main weather screen
#    - Detail cards
#    - Hourly forecast
#    - 10-day forecast
#    - (Optional) Language/font demo
```

### Step 2: Place Screenshots in Fastlane

```bash
# Screenshots are already in repo:
# fastlane/metadata/android/en-US/images/phoneScreenshots/1.png
# fastlane/metadata/android/mg/images/phoneScreenshots/1.png
# ... (8 languages)
```

**Note:** Screenshots should already be in the GitHub repo in this structure. If not captured yet, capture and commit them:

```bash
# If you captured new screenshots locally:
git add fastlane/metadata/android/*/images/phoneScreenshots/
git commit -m "Add F-Droid screenshots for all languages"
git push origin add-app-name
```

### Step 3: Verify Screenshot Placement

```bash
# Check all screenshots present
find fastlane/metadata/android -name "*.png" -o -name "*.jpg" | sort

# Should show multiple images per language
```

## Creating Merge Request

### Step 1: Commit Changes to Your Fork

```bash
cd /path/to/fdroiddata

# Check what changed
git status

# Stage changes
git add metadata/orinasa.njarasoa.maripanatokana.yml

# Commit
git commit -m "New app: maripànaTokana

- Weather app with dual metric/imperial units
- 8 language support
- F-Droid compatible (no Google Play Services)
- Available on GitHub: https://github.com/mpilasy/maripanaTokana
- MIT License"

# Push to your fork
git push origin add-maripanatokana
```

### Step 2: Create Merge Request on GitLab

1. Go to your fork: `https://gitlab.com/{your-username}/fdroiddata`
2. Click "Merge requests" tab
3. Click "New merge request" button
4. Set:
   - **Source branch:** `add-maripanatokana` (your branch)
   - **Target branch:** `master` (main F-Droid branch)
   - **Title:** `New app: maripànaTokana`
5. In description, provide:

```markdown
## Description

Submission for maripànaTokana, a weather application.

## App Details

- **Package:** orinasa.njarasoa.maripanatokana
- **Version:** 1.0
- **License:** MIT
- **Repository:** https://github.com/mpilasy/maripanaTokana

## Features

- Real-time weather from Open-Meteo API (no API key needed)
- Dual-unit display (metric & imperial simultaneous)
- 8 languages with native digit support (ar, hi, ne)
- RTL support (Arabic)
- 16 font pairings for customization
- Home screen widgets
- GPS location with fallbacks
- Pull-to-refresh
- No ads, no tracking, fully open-source

## F-Droid Compliance

- ✅ No proprietary dependencies (Google Play Services removed)
- ✅ Uses native Android LocationManager
- ✅ MIT License
- ✅ Public GitHub repository
- ✅ Reproducible builds configured
- ✅ All metadata and screenshots included

## Verification

Build metadata has been tested and verified:
```bash
./gradlew clean assembleFdroidRelease
# Successfully builds F-Droid flavor
```

No proprietary APIs detected in APK.

## Links

- GitHub: https://github.com/mpilasy/maripanaTokana
- Issues: https://github.com/mpilasy/maripanaTokana/issues
```

6. Click "Create merge request"

### Step 3: Add Labels (Optional)

In your merge request, add labels:
- `app-submission`
- `new-application`

## Handling F-Droid Review

### What to Expect

- **Timeline:** 1-4 weeks typically
- **F-Droid team** will review your submission
- They may ask questions or request changes
- You'll be notified via GitLab comments

### Common Review Points

The F-Droid team typically checks:

1. **Metadata Quality**
   - Description accuracy
   - Screenshots clarity
   - Proper localization

2. **License Compliance**
   - Correct license file
   - No proprietary code
   - Dependencies properly licensed

3. **Build Configuration**
   - Build instructions work
   - APK builds reproducibly
   - No untrusted dependencies

4. **Code Quality**
   - No obvious security issues
   - Proper permissions usage
   - Good error handling

### Responding to Feedback

If F-Droid team requests changes:

1. Make requested changes in your GitHub repo
2. Push changes
3. Reply in the GitLab merge request comment
4. They'll re-check and approve if satisfied

**Example:**
```
F-Droid Team: "Screenshot quality issue on Screen 1 - too blurry"

Your Response:
"Fixed! Recaptured higher-quality screenshot. Have pushed updated
screenshot to fastlane/metadata/android/en-US/images/phoneScreenshots/1.png"
```

### Approval Process

Once satisfied, F-Droid team will:
1. Approve the merge request
2. Merge to master branch
3. Test build on their infrastructure
4. Add to F-Droid repository
5. Notify you of successful listing

## After Approval

### Step 1: Verify on F-Droid

Once listed, verify your app appears:

1. Go to https://f-droid.org/
2. Search for "maripànaTokana"
3. Should appear in results
4. Check all metadata displays correctly
5. Verify download works

### Step 2: Add F-Droid Badge to README

Update `README.md`:

```markdown
## Download

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
     alt="Get it on F-Droid"
     height="80">](https://f-droid.org/packages/orinasa.njarasoa.maripanatokana/)
```

### Step 3: Announce Release

- Post on social media (Twitter, Reddit r/androiddev, etc.)
- Notify GitHub followers
- Consider F-Droid Matrix channel discussion

### Step 4: Monitor Downloads

Track statistics:
- Visit https://f-droid.org/packages/orinasa.njarasoa.maripanatokana/
- Monitor download counts
- Check user reviews (when available)

## Future Updates

### Adding Version 1.1 (Example)

When you have updates ready:

1. **Update Version in Code**
   ```kotlin
   // app/build.gradle.kts
   versionCode = 2
   versionName = "1.1"
   ```

2. **Update Metadata**
   ```bash
   # Copy updated metadata to F-Droid data repo
   cp metadata/orinasa.njarasoa.maripanatokana.yml \
      /path/to/fdroiddata/metadata/
   ```

3. **Add Changelog**
   ```bash
   # Add version-specific changelog
   cat > fastlane/metadata/android/en-US/changelogs/2.txt << 'EOF'
   Version 1.1 - [Release Date]

   - Feature X added
   - Bug Y fixed
   - Performance improvements
   EOF
   ```

4. **Create Release**
   ```bash
   git tag -a v1.1 -m "Release version 1.1"
   git push origin v1.1
   ```

5. **Create New Merge Request**
   - Fork again (or update existing clone)
   - Create branch: `git checkout -b update-maripanatokana-v1.1`
   - Update metadata to new version
   - Create MR to F-Droid data repo

### UpdateCheckMode

Current configuration:
```yaml
AutoUpdateMode: Version v%v
UpdateCheckMode: Tags
```

This means:
- F-Droid automatically checks GitHub tags
- When new tag is found (e.g., `v1.1`)
- Automatically builds next version
- No manual MR needed (automatic!)

## Support & Resources

### F-Droid Documentation
- [Build Metadata Reference](https://f-droid.org/docs/Build_Metadata_Reference/)
- [Submitting Quick Start](https://f-droid.org/docs/Submitting_to_F-Droid_Quick_Start_Guide/)
- [Screenshots & Descriptions](https://f-droid.org/docs/All_About_Descriptions_Graphics_and_Screenshots/)
- [Reproducible Builds](https://f-droid.org/en/docs/Reproducible_Builds/)

### Communication
- **F-Droid Forum:** https://forum.f-droid.org/
- **Matrix Chat:** Matrix rooms for F-Droid discussion
- **GitLab Issues:** https://gitlab.com/fdroid/fdroiddata/-/issues

### Similar Apps in F-Droid
- Search for "weather" on F-Droid to see similar apps
- Study their metadata for examples

## Troubleshooting

### "Build fails on F-Droid servers"

Common causes:
1. **Missing prebuild steps** - i18n generation not run
2. **Dependency issues** - Check `build.gradle.kts`
3. **API version mismatch** - Verify SDK versions match

**Solution:**
- Review `metadata/orinasa.njarasoa.maripanatokana.yml`
- Verify prebuild step includes i18n generation
- Test build locally: `./gradlew clean assembleFdroidRelease`

### "Metadata validation fails"

Common causes:
1. **Invalid YAML syntax** - Check brackets, quotes
2. **Missing required fields** - License, category, etc.
3. **Broken links** - GitHub URL incorrect

**Solution:**
- Use yamllint to validate
- Review Build Metadata Reference
- Double-check all URLs

### "Screenshots not showing"

Common causes:
1. **Wrong directory** - Not in fastlane/metadata/
2. **Wrong naming** - Not numbered 1.png, 2.png, etc.
3. **Wrong format** - JPEG/PNG quality too low

**Solution:**
- Verify screenshot location in fastlane
- Ensure 4-5 per language minimum
- Recapture if low quality

## Checklist Before Pushing "Submit"

Before clicking "Create Merge Request":

- [ ] YAML metadata valid (yamllint passed)
- [ ] All 8 languages have title, short_description, full_description
- [ ] All 8 languages have changelog/1.txt
- [ ] At least 4 screenshots per language
- [ ] Screenshots are clear and representative
- [ ] GitHub repo is public
- [ ] LICENSE file exists and is readable
- [ ] Git tag exists: `git tag -l | grep v1.0`
- [ ] App builds locally: `./gradlew clean assembleFdroidRelease`
- [ ] README mentions F-Droid
- [ ] No proprietary code verified

## Final Checklist

After submission, check:

- [ ] Merge request created successfully
- [ ] F-Droid team has access to review
- [ ] Links in description are working
- [ ] Awaiting F-Droid team feedback
- [ ] Checking MR regularly for comments
- [ ] Ready to respond to any questions

**Good luck with your submission! 🎉**

Once approved, your app will reach F-Droid's privacy-conscious user base and join thousands of open-source apps in the F-Droid repository.
