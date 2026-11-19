# Contributing

Thank you for your help. Bug reports, translations and code are welcome.

## License of contributions

When you send a contribution (a pull request, a patch or a translation), you agree to these terms:

1. You wrote the contribution, or you have the right to send it under these terms.
2. You license the contribution under the GNU Affero General Public License, version 3 or later (AGPL-3.0-or-later), the same license as the project. See [LICENSE](LICENSE).
3. You also give the additional permission for Google Play services that is in the [README](README.md#additional-permission-for-google-play-services). Without it, the Standard APK cannot include your contribution.
4. You agree that the project owner (Orinasa Njarasoa) can add more additional permissions under AGPL version 3 section 7 to the project, and that these permissions also apply to your contribution. This lets the project keep up with changes in its dependencies without a new agreement from each contributor.

Add a `Signed-off-by:` line to each commit (`git commit -s`) to show that you agree. The line follows the [Developer Certificate of Origin](https://developercertificate.org/).

## Commit messages

Write commit messages as [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/):

```
type(scope): description

An optional body that tells what changed and why.

Signed-off-by: Your Name <you@example.com>
```

- **type** is one of these:
  - `feat`: a new feature that users can see.
  - `fix`: a bug fix.
  - `docs`: a change to documentation only.
  - `refactor`: a code change that does not change what the app does.
  - `perf`: a change that makes the app faster or smaller.
  - `test`: a change to tests only.
  - `build`: a change to Gradle, npm or dependencies.
  - `ci`: a change to the GitHub Actions workflows.
  - `chore`: other work, for example releases and F-Droid metadata.
  - `revert`: a commit that reverts an earlier commit.
- **scope** is optional. It is the area of the change: `android`, `web`, `tv`, `widget`, `alerts`, `i18n`, `fdroid`, `release` or `deps`.
- **description** starts with a lower-case verb in the imperative ("add", "fix", "remove"), and has no period at the end. Keep the first line at 72 characters or less.
- If a change breaks something that users or contributors depend on, put `!` before the colon, and explain the change in a `BREAKING CHANGE:` line at the end of the body.

Examples:

```
feat(tv): show the moon phase on the main screen
fix(alerts): show MeteoAlarm "Moderate" alerts as a watch
build(deps): update the Compose BOM to 2026.09.00
chore(release): v1.7.20261118024115
```

## New files

Each new source file starts with two SPDX lines, in the comment style of its language:

```kotlin
// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: AGPL-3.0-or-later
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
