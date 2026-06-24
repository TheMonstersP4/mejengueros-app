---
name: gradle-kmp-build-targets
description: "Trigger: Gradle KMP, targets, version catalog, source sets. Maintain multiplatform build topology."
license: Apache-2.0
metadata:
  author: ddgutierrezc
  version: "1.0"
---

## Activation Contract

Use when changing Gradle modules, plugins, dependencies, source sets, SDK/toolchain, or KMP targets.

## Hard Rules

- Centralize versions in `gradle/libs.versions.toml`.
- Declare KMP targets in the module that owns the shared code.
- Add dependencies to the narrowest source set that needs them.
- Preserve Java/Kotlin toolchain compatibility across modules.
- Treat iOS build limitations on non-macOS hosts as expected when SwiftPM/cinterop is present.

## Decision Gates

| Change                               | File                          |
| ------------------------------------ | ----------------------------- |
| Plugin/library version               | `gradle/libs.versions.toml`   |
| Root formatting/common compiler args | root `build.gradle.kts`       |
| Shared data/business targets         | `shared/build.gradle.kts`     |
| Shared Compose UI targets            | `composeApp/build.gradle.kts` |
| App packaging/flavors                | platform app build file       |

## Execution Steps

1. Update catalog first.
2. Add dependency in the correct source set.
3. Verify target-specific warnings are understood.
4. Run focused Gradle task.

## Output Contract

Report catalog entries, source sets, targets, and validation command.

## References

- `gradle/libs.versions.toml`
- `shared/build.gradle.kts`
- `composeApp/build.gradle.kts`
- `assets/kmp-build-source-sets-example.kts`
