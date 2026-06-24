---
name: compose-screen-contracts-current
description: "Trigger: current Compose screens, SpaceX UI DNA. Understand the repo's mixed screen contracts accurately."
license: Apache-2.0
metadata:
  author: ddgutierrezc
  version: "1.0"
---

## Activation Contract

Use when analyzing or changing current Compose screens in this repo.

## Hard Rules

- Do not claim the current repo is fully stateless or fully state-hoisted.
- Recognize the mixed pattern: some screens receive `UiState` and callbacks, while launch screens receive a ViewModel directly.
- Preserve current behavior unless the task explicitly requests refactoring toward route/content separation.
- Keep navigation ownership in the app root, not deep reusable content.

## Decision Gates

| Current pattern                     | Meaning                           |
| ----------------------------------- | --------------------------------- |
| `AuthStartScreen(state, callbacks)` | Hoisted/stateless-ish content     |
| `ProfileScreen(state, callbacks)`   | Hoisted/stateless-ish content     |
| `LaunchListScreen(viewModel)`       | Screen-bound ViewModel collection |
| `LaunchDetailScreen(viewModel)`     | Screen-bound ViewModel lookup     |

## Execution Steps

1. Identify whether the screen consumes a ViewModel or state params.
2. Keep changes compatible with its current contract.
3. If refactoring, introduce a Route composable before changing content signatures.

## Output Contract

Report whether the touched screen is state-hoisted, ViewModel-bound, or refactored.

## References

- `composeApp/src/commonMain/kotlin/io/github/kevinah95/spacex/screens/AuthStartScreen.kt`
- `composeApp/src/commonMain/kotlin/io/github/kevinah95/spacex/screens/LaunchListScreen.kt`
- `composeApp/src/commonMain/kotlin/io/github/kevinah95/spacex/screens/LaunchDetailScreen.kt`
- `composeApp/src/commonMain/kotlin/io/github/kevinah95/spacex/screens/ProfileScreen.kt`
