# Contributing

Thank you for your help. Bug reports, translations and code are welcome.

## License of contributions

When you send a contribution (a pull request, a patch or a translation), you agree to these terms:

1. You wrote the contribution, or you have the right to send it under these terms.
2. You license the contribution under the GNU General Public License, version 3 or later (GPL-3.0-or-later), the same license as the project. See [LICENSE](LICENSE).
3. You also give the additional permission for Google Play services that is in the [README](README.md#additional-permission-for-google-play-services). Without it, the Standard APK cannot include your contribution.
4. You agree that the project owner (Orinasa Njarasoa) can add more additional permissions under GPL version 3 section 7 to the project, and that these permissions also apply to your contribution. This lets the project keep up with changes in its dependencies without a new agreement from each contributor.

Add a `Signed-off-by:` line to each commit (`git commit -s`) to show that you agree. The line follows the [Developer Certificate of Origin](https://developercertificate.org/).

## New files

Each new source file starts with two SPDX lines, in the comment style of its language:

```kotlin
// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later
```

Files that cannot hold a comment (images, fonts, JSON) get their data from [REUSE.toml](REUSE.toml). If you add third-party files, add an entry for them there, and put their license text in `LICENSES/`.

To check all files, run [REUSE](https://reuse.software/):

```bash
pipx run reuse lint
```

## Before you send a pull request

- Android: `./gradlew testFdroidDebugUnitTest testStandardDebugUnitTest lintFdroidDebug`
- Web: in `web/`, run `npm run check`, `npm test` and `npm run build`
- Translations: `node scripts/check_i18n_parity.js`
- Add a line to the `[Unreleased]` part of [CHANGELOG.md](CHANGELOG.md) for each change that users can see.
