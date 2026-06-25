---
name: kmp-module-boundaries
description: "Trigger: KMP modules, shared UI, shared logic, app targets. Preserve SpaceX-style module boundaries."
license: Apache-2.0
metadata:
  author: ddgutierrezc
  version: "1.0"
---

## Activation Contract

Use when adding modules, moving code, or creating features in a Kotlin Multiplatform app modeled after this repo.

## Hard Rules

- Keep platform app entry modules thin.
- Put shared business/data/presentation code in `shared`.
- Put shared Compose UI in `composeApp`.
- Keep Android-only app wiring in `androidApp`, Desktop app wiring in `desktopApp`, and Swift/iOS wrapper code in `iosApp`.
- Do not put networking, repositories, database, or domain logic into platform entry modules.

## Decision Gates

| Need                                      | Location                    |
| ----------------------------------------- | --------------------------- |
| Shared UI screen                          | `composeApp/src/commonMain` |
| ViewModel, repository, datasource, entity | `shared/src/commonMain`     |
| Android activity/application/resources    | `androidApp`                |
| Desktop main/package settings             | `desktopApp`                |
| SwiftUI wrapper/Xcode metadata            | `iosApp`                    |

## Execution Steps

1. Identify whether the change is UI, business/data, or platform bootstrap.
2. Place code in the narrowest module that supports all required targets.
3. Add target-specific implementations only in the matching source set.
4. Update Gradle dependencies only in the module that consumes them.

## Output Contract

Report modules touched and why each change belongs there.

## References

- `settings.gradle.kts`
- `README.md`
