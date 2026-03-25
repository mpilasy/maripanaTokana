# GitHub Copilot Instructions

Read [AGENTS.md](../AGENTS.md) for the full project context — architecture, tech stack, build commands, critical rules, and mandates.

For F-Droid build issues, read [docs/FDROID.md](../docs/FDROID.md) before touching any Gradle config.

## Key Rules

- Do NOT add `Co-Authored-By` or any AI attribution to commit messages.
- Do NOT add `id("org.jetbrains.kotlin.android")` to `app/build.gradle.kts` — causes extension conflict.
- Do NOT add foojay-resolver to `settings.gradle.kts` — F-Droid blocks it.
- `kotlin { jvmToolchain(21) }` must stay in `app/build.gradle.kts`.
- `auto-provisioning=disabled` must stay in `gradle.properties`.
- Feature parity: keep Android and Web in sync.
- Owner prefers concise, technically accurate code. Skip redundant explanations.
