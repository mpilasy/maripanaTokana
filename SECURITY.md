# Security Policy

## Supported versions

Only the latest release gets security fixes. This applies to the Android app (F-Droid and Standard APK) and to the web app at [maripana.njarasoa.org](https://maripana.njarasoa.org). Update to the latest version before you report a problem.

## Report a vulnerability

Do not report a security problem in a public issue, a pull request or a discussion.

Use GitHub private vulnerability reporting:

1. Go to the [Security tab](https://github.com/mpilasy/maripanaTokana/security) of the repository.
2. Click **Report a vulnerability**, or open the [new advisory form](https://github.com/mpilasy/maripanaTokana/security/advisories/new) directly.
3. Give this data:
   - The app and version (Android F-Droid, Android Standard, Android TV or web), and the Android version or browser.
   - The steps to reproduce the problem.
   - What an attacker can do with it.
   - A fix or a workaround, if you know one.

Only the maintainers can see the report.

## What happens next

- We try to reply in 7 days.
- We tell you if we accept the report, and we keep you informed about the fix.
- We publish the fix in a new release, and then we publish a security advisory. We give you credit in the advisory if you agree.

Please give us a reasonable time to release a fix before you disclose the problem in public.

## Scope

In scope:

- The Android app and the Android TV app in this repository.
- The web app (SvelteKit PWA) in `web/`.
- The build and release scripts and workflows in this repository.

Out of scope:

- Problems in the weather data services (for example Open-Meteo or MeteoAlarm). Report these to the service.
- Problems in third-party libraries that are already known and fixed upstream. Tell us if our version is old.
