# Mejengueros Frontend

Mejengueros Kotlin Multiplatform application with current targets for Android, iOS, and Desktop (JVM).

## Purpose

This subproject provides the MVP frontend technical foundation: shared UI with Compose Multiplatform, typed navigation, Cognito authentication, and early functional flows that serve as references for new features.

## Main structure

```text
androidApp/   Android entry point
desktopApp/   Desktop/JVM entry point
iosApp/       SwiftUI/Xcode wrapper for iOS
shared/       Shared UI, navigation, and common logic
Taskfile.yml  Recommended command surface
```

The shared logic lives in `shared/`, with a main organization like this:

```text
shared/src/commonMain/kotlin/io/github/themonstersp4/mejengueros/
  app/
  data/
  di/
  domain/
  navigation/
  presentation/
  screens/
  theme/
```

## Core technical decisions

- Shared UI and app logic live in `shared/`; there is no `composeApp` module.
- The app uses Navigation 3, Koin, Ktor, SQLDelight, and Material 3.
- Authentication uses Amazon Cognito; Firebase is not part of this foundation.
- Screens render immutable state and delegate actions; `ViewModel`s depend on repositories, not directly on Ktor or SQLDelight.

## Expected architectural flow

```text
Screen
  -> Route/Entry
    -> ViewModel
      -> Repository interface
        -> Repository implementation
          -> RemoteDataSource -> Ktor
          -> LocalDataSource  -> SQLDelight
```

## Current reference features

- Authenticated shell with main navigation.
- Cognito authentication flow (email/password and Hosted UI).
- `Pokedex` feature as an example of architecture, navigation, local cache, and remote consumption.

## Prerequisites and basic setup

- JDK compatible with the project's Gradle/Kotlin Multiplatform setup.
- Android SDK for Android builds.
- Xcode if you are going to run `iosApp/`.
- [Task](https://taskfile.dev/) is optional, but it is the recommended interface for repeatable commands.

If you need integration values, review `.env.example` as a development configuration reference. That file is not automatically loaded at runtime.

## Main commands

Run these commands from `app-frontend/`.

### Via Taskfile (recommended)

```bash
task spotless:apply
task spotless:check
task format
task check
task test
task test:auth
task android:debug
task android:host-test
task android:host-test:auth
task desktop:compile
task verify
```

### Direct Gradle (fallback)

Use `./gradlew` on Unix/macOS and `./gradlew.bat` on PowerShell/Windows.

```bash
./gradlew spotlessCheck :shared:jvmTest :shared:testAndroidHostTest :androidApp:assembleDebug :desktopApp:compileKotlin --no-configuration-cache --console=plain
./gradlew :androidApp:assembleDebug
./gradlew :shared:jvmTest
./gradlew :shared:testAndroidHostTest
./gradlew :desktopApp:compileKotlin
```

```powershell
./gradlew.bat spotlessCheck :shared:jvmTest :shared:testAndroidHostTest :androidApp:assembleDebug :desktopApp:compileKotlin --no-configuration-cache --console=plain
./gradlew.bat :androidApp:assembleDebug
./gradlew.bat :shared:jvmTest
./gradlew.bat :shared:testAndroidHostTest
./gradlew.bat :desktopApp:compileKotlin
```

## Current verification scope

Active validation covers Android, Desktop/JVM, and shared KMP logic. The iOS wrapper remains in the repository, but it is not currently part of the main CI scope.

## Where to go deeper

- [`shared/`](shared/): shared architecture, screens, navigation, and data.
- [`Taskfile.yml`](Taskfile.yml): repeatable commands for formatting, builds, and tests.
- [`../docs/design/README.md`](../docs/design/README.md): product visual and functional context.
- [`../README.md`](../README.md): overall repository context and MVP scope.
