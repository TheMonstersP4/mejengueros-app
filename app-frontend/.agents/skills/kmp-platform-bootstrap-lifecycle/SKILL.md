---
name: kmp-platform-bootstrap-lifecycle
description: "Trigger: app startup, KMP bootstrap, platform lifecycle. Wire Android, iOS, and Desktop entrypoints safely."
license: Apache-2.0
metadata:
  author: ddgutierrezc
  version: "1.0"
---

## Activation Contract

Use when changing app startup, Koin/Firebase/service initialization, permissions, window setup, or platform entrypoints.

## Hard Rules

- Initialize shared dependency graph before rendering shared Compose UI.
- Keep lifecycle and permission work in platform entrypoints, not shared screens.
- Android app-wide initialization belongs in `Application`; UI setup belongs in `Activity`.
- iOS should configure shared Kotlin services inside the Compose UIViewController bridge.
- Desktop should initialize shared services before `application { Window { App() } }`.

## Decision Gates

| Platform                            | Owner                                |
| ----------------------------------- | ------------------------------------ |
| Android app services/token sync     | `Application`                        |
| Android Compose content/permissions | `Activity`                           |
| iOS KMP bridge                      | `MainViewController` / Swift wrapper |
| Desktop window lifecycle            | `desktopApp/main.kt`                 |

## Execution Steps

1. Locate the target platform entrypoint.
2. Keep shared `App()` free of platform permission/bootstrap code.
3. Preserve initialization order: platform services, DI, then shared UI.
4. Report platform-specific lifecycle assumptions.

## Output Contract

List startup files touched, initialization order, and platform constraints.

## References

- `androidApp/src/main/kotlin/io/github/kevinah95/spacex/MainAplication.kt`
- `composeApp/src/iosMain/kotlin/io/github/kevinah95/spacex/MainViewController.kt`
- `desktopApp/src/main/kotlin/io/github/kevinah95/spacex/main.kt`
- `assets/platform-bootstrap-entrypoints-example.kt`
