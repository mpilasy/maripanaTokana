# Ready to Submit to F-Droid? 🚀

Everything is prepared. Follow these steps:

## 1. Capture Real Screenshots (2 hours)
```bash
bash scripts/capture_screenshots.sh
```
- Requires Android device or emulator connected via ADB
- Captures 5 screenshots for each of 8 languages
- Saves to: `fastlane/metadata/android/{lang}/images/phoneScreenshots/`

## 2. Run Verification Tests (30 minutes)
```bash
bash scripts/test_fdroid_build.sh
```
- Verifies F-Droid build is complete
- Confirms no Play Services dependencies
- Tests reproducible builds
- Checks metadata completeness
- Installs and tests on device (if connected)

## 3. Commit Screenshots
```bash
git add fastlane/metadata/
git commit -m "Add F-Droid screenshots for all languages"
git push origin main
```

## 4. Submit to F-Droid
```bash
# Step 1: Fork F-Droid Data
# https://gitlab.com/fdroid/fdroiddata
# Click "Fork" button

# Step 2: Clone your fork
git clone https://gitlab.com/{your-username}/fdroiddata.git
cd fdroiddata
git checkout -b add-maripanatokana

# Step 3: Copy metadata
cp /path/to/maripanaTokana/metadata/orinasa.njarasoa.maripanatokana.yml metadata/

# Step 4: Commit and push
git add metadata/orinasa.njarasoa.maripanatokana.yml
git commit -m "New app: maripànaTokana

- Weather app with dual metric/imperial units
- 8 language support
- F-Droid compatible (no Google Play Services)
- Available on GitHub: https://github.com/mpilasy/maripanaTokana
- MIT License"
git push origin add-maripanatokana
```

# Step 5: Create Merge Request
- Go to: https://gitlab.com/fdroid/fdroiddata
- Click "Merge Requests"
- Click "New merge request"
- Set source: your branch, target: master
- Create MR
- F-Droid team will review (1-4 weeks)

## Files Ready
✓ Location abstraction (Play Services + native)
✓ Build flavors configured (standard + fdroid)
✓ All 8 languages metadata
✓ F-Droid submission YAML
✓ GitHub Actions CI
✓ Comprehensive documentation
✓ Automated scripts

## Current Status
- Code: ✅ 100%
- Build: ✅ 100%
- Metadata: ✅ 100%
- Screenshots: ⏳ Ready (run capture script)
- Testing: ⏳ Ready (run test script)
- Submission: ⏳ Ready (follow step 4 above)

## Troubleshooting
- Screenshot capture fails? Check device connection: `adb devices`
- Build fails? Run: `./gradlew clean assembleFdroidRelease`
- Tests fail? See: `TESTING_GUIDE.md`
- Submission help? See: `FDROID_SUBMISSION_GUIDE.md`

## Next: Run These Commands
```bash
bash scripts/capture_screenshots.sh
bash scripts/test_fdroid_build.sh
```

Then follow step 3-5 above to submit! 🎉
