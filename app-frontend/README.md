# Mejengueros

Kotlin Multiplatform app targeting Android, iOS, and Desktop (JVM).

## Architecture decisions

This project keeps the modern Kotlin Multiplatform wizard structure:

```text
androidApp/   Android application entry point
desktopApp/   Desktop JVM application entry point
iosApp/       SwiftUI/Xcode wrapper for iOS
shared/       Shared Compose UI and shared Kotlin logic
```

The project intentionally does **not** create a `composeApp` module. Shared UI and shared app logic live in `shared`, because all current targets use Compose Multiplatform UI.

The project also intentionally avoids Firebase and Navigation 2. It uses local/manual auth, Navigation 3, Koin, Ktor, SQLDelight, Material 3, and standard repositories/Maven Central.

## Module responsibilities

### `shared`

Owns shared code:

- Compose app root
- Navigation 3 route state
- Shared screens
- Theme tokens
- Presentation state for multiplatform screens
- Domain repository contracts
- Repository and datasource implementations
- Ktor remote networking
- SQLDelight local persistence
- Koin dependency injection

Current common structure:

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

### `androidApp`

Thin Android entry point. It should contain Android app bootstrap/configuration only and call shared `App()`.

### `desktopApp`

Thin Desktop entry point. It should configure the desktop window and call shared `App()`.

### `iosApp`

SwiftUI/Xcode wrapper that consumes the `Shared` framework produced by the `shared` module.

## Architecture flow

Feature code should follow this direction:

```text
Screen
  -> Route/Entry
    -> ViewModel
      -> Repository interface
        -> Repository implementation
          -> RemoteDataSource -> Ktor
          -> LocalDataSource  -> SQLDelight
```

Rules:

- Screens render immutable state and emit callbacks.
- Route/Entry composables wire Koin ViewModels, collect state, and connect navigation callbacks.
- ViewModels call repositories, not datasources, Ktor, or SQLDelight.
- Repositories coordinate remote/local behavior and fallback policy.
- Datasources own Ktor and SQLDelight details.

## Navigation

The app uses Navigation 3, not Navigation 2.

Rules:

- routes are `@Serializable` `NavKey` types;
- authenticated navigation owns separate Home and Pokédex back stacks;
- destinations are rendered with `NavDisplay` and `entryProvider`;
- screens stay controller-free and receive callbacks.

After login or session restore, users enter an authenticated shell. The shell owns the shared top app bar and bottom navigation:

```text
Top app bar: Mejengueros                         Sign out
Bottom bar:  Home | Pokédex
```

`Home` is the authenticated landing tab. `Pokédex` preserves its own stack, so switching to Home and back can return to the same Pokémon detail.

Navigation callbacks are split by context:

```text
LoginNavigationActions
AuthenticatedShellActions
PokedexNavigationActions
```

This keeps `AppNavHost` responsible for back stack ownership without turning navigation callbacks into a global god object.

The app uses Material 3 `NavigationBar` and `NavigationBarItem` for the bottom bar. Those components are imported from `androidx.compose.material3` and are the Compose Multiplatform/Material 3 bottom navigation API.

## Pokédex feature

The Pokédex tab is the first real feature used to exercise the SpaceX-style architecture while keeping the modern project decisions.

```text
PokemonListScreen / PokemonDetailScreen
  -> PokemonListViewModel / PokemonDetailViewModel
    -> IPokemonRepository
      -> IPokemonRemoteDataSource -> Ktor -> PokéAPI
      -> IPokemonLocalDataSource -> SQLDelight
```

Current behavior:

- list endpoint: `https://pokeapi.co/api/v2/pokemon?limit=20&offset=0`;
- detail endpoint: `https://pokeapi.co/api/v2/pokemon/{id}`;
- manual infinite scroll with `limit = 20`;
- pull-to-refresh with Material 3 `PullToRefreshBox`;
- endpoint-backed search against PokéAPI with paginated results;
- stable search header that remains available during loading, empty results, and refresh;
- local-first favorites stored in SQLDelight;
- SQLDelight cache for list summaries and details;
- repository fallback to local cache when non-search remote loading fails.

PokéAPI does not expose a documented partial-search query parameter for `/api/v2/pokemon`. Partial search is therefore implemented by scanning paginated PokéAPI list results and filtering those remote endpoint results in the remote datasource. This keeps search endpoint-backed, but it can require more network calls for sparse queries than a real server-side search endpoint.

## Authentication

Firebase is intentionally not part of this project. Authentication at this stage is a local/manual example only.

The current example follows the same architectural seams intended for real features:

```text
LoginScreen -> AuthViewModel -> IAuthRepository -> IAuthLocalDataSource -> SQLDelight
```

The session is stored locally with SQLDelight only to demonstrate repository, datasource, and cache boundaries. It is not secure authentication and should be replaced when a real backend or product requirement exists.

## How to add a feature

1. Add typed Navigation 3 route keys in `navigation/`.
2. Add route entries in `AppNavEntries.kt` or a feature-specific `EntryProviderScope` extension.
3. Create state-hoisted screens under `screens/<feature>/`.
4. Create `UiState` and `ViewModel` under `presentation/<feature>/`.
5. Add domain models and repository interface under `domain/`.
6. Add remote/local datasource interfaces and implementations under `data/`.
7. Wire dependencies in Koin modules under `di/modules/`.
8. Add tests with the behavior: `commonTest` for repository/ViewModel seams and `jvmTest` for SQLDelight in-memory tests.
9. Update README when the feature changes architecture or user-visible behavior.

## Quality

Formatting is handled by Spotless/ktfmt for Kotlin and Gradle Kotlin files.

CI runs the current non-iOS readiness scope on GitHub Actions for:

```bash
./gradlew spotlessCheck :shared:jvmTest :shared:testAndroidHostTest :androidApp:assembleDebug :desktopApp:compileKotlin --no-configuration-cache --console=plain
```

Current readiness intentionally covers Android, Desktop/JVM, and shared KMP logic. The iOS wrapper remains in the repository, but iOS validation is outside the readiness scope for this app and is not part of CI.

## Taskfile

If you use [Task](https://taskfile.dev/), common commands are available:

```bash
task format
task check
task test
task android:debug
task android:host-test
task desktop:compile
task verify
```

On Windows or without Task installed, use the Gradle commands directly with `gradlew.bat`.

## Running the apps

- Android app: `./gradlew :androidApp:assembleDebug`
- Desktop app:
  - Hot reload: `./gradlew :desktopApp:hotRun --auto`
  - Standard run: `./gradlew :desktopApp:run`
- iOS app: open the `iosApp` directory in Xcode and run it from there.

## Running tests

- Android host tests: `./gradlew :shared:testAndroidHostTest`
- Desktop/shared JVM tests: `./gradlew :shared:jvmTest`

The project still contains an iOS wrapper, but iOS tests are intentionally outside the current readiness/CI scope.
