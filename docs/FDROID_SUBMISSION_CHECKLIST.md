# F-Droid Submission Checklist

## Pre-Submission (Local)
- [x] Code builds successfully (both flavors)
- [x] No Google Play Services in F-Droid APK
- [x] All 8 languages have metadata
- [x] F-Droid YAML metadata created
- [x] GitHub repository is public
- [x] LICENSE file exists (MIT)
- [x] README.md complete
- [x] Release tagged (v1.0)

## Screenshots
- [ ] Captured 4-5 screenshots per language (8 languages = 32-40 total)
- [ ] Screenshots are clear and readable
- [ ] All 8 languages represented
- [ ] Images saved to: fastlane/metadata/android/{lang}/images/phoneScreenshots/
- [ ] Named sequentially: 1.png, 2.png, 3.png, 4.png, 5.png

## Testing
- [ ] App launches without crash
- [ ] Location permission works
- [ ] Weather loads from Open-Meteo
- [ ] All 8 languages render correctly
- [ ] Dual-unit toggle works
- [ ] Pull-to-refresh works
- [ ] Widgets function
- [ ] No Play Services detected
- [ ] Build is reproducible (identical checksums)

## GitHub
- [ ] Changes committed: git add fastlane/metadata/
- [ ] Commit message: "Add F-Droid screenshots for all languages"
- [ ] Changes pushed to main branch

## F-Droid Submission
- [ ] Fork fdroiddata: https://gitlab.com/fdroid/fdroiddata
- [ ] Clone your fork locally
- [ ] Create branch: git checkout -b add-maripanatokana
- [ ] Copy metadata YAML to your fork
- [ ] Validate YAML syntax: yamllint metadata/*.yml
- [ ] Commit: git commit -m "New app: maripànaTokana"
- [ ] Push: git push origin add-maripanatokana
- [ ] Create merge request on GitLab
- [ ] Wait for F-Droid team review (1-4 weeks)

## During Review
- [ ] Monitor merge request for comments
- [ ] Respond to F-Droid team feedback
- [ ] Make requested changes (if any)
- [ ] Push updates to your fork
- [ ] Await approval

## Success
- [ ] Merge request accepted
- [ ] App appears on F-Droid
- [ ] Download available to users
- [ ] Monitor download statistics

## Links
- F-Droid Submit: https://f-droid.org/docs/Submitting_to_F-Droid_Quick_Start_Guide/
- Metadata Ref: https://f-droid.org/docs/Build_Metadata_Reference/
- Your App URL: https://f-droid.org/packages/orinasa.njarasoa.maripanatokana/
