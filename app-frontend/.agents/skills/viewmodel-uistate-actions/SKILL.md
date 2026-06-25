---
name: viewmodel-uistate-actions
description: "Trigger: ViewModel, UiState, screen actions. Model screen state with immutable flows and explicit actions."
license: Apache-2.0
metadata:
  author: ddgutierrezc
  version: "1.0"
---

## Activation Contract

Use when creating or changing presentation logic in shared KMP code.

## Hard Rules

- Expose immutable `StateFlow<UiState>` from ViewModels.
- Mutate state internally through private mutable state.
- Provide explicit action methods such as `load`, `refresh`, `signIn`, or `signOut`.
- Do not invent one-shot event/effect channels unless the feature needs transient effects.
- Keep repository calls inside ViewModel methods or init blocks, not UI content.

## Decision Gates

| Need                                 | Pattern                                  |
| ------------------------------------ | ---------------------------------------- |
| Persistent screen state              | `UiState` data class                     |
| User/business action                 | ViewModel method                         |
| Transient snackbar/navigation effect | Separate effect channel only if required |
| UI-only local behavior               | Composable state holder                  |

## Execution Steps

1. Define a small `UiState` data class.
2. Expose `StateFlow` read-only.
3. Implement actions with coroutine scope and repository calls.
4. Update loading/error/empty states deliberately.

## Output Contract

Report UiState fields, action methods, and whether transient effects were needed.

## References

- `shared/src/commonMain/kotlin/io/github/kevinah95/spacex/presentation/auth/AuthViewModel.kt`
- `shared/src/commonMain/kotlin/io/github/kevinah95/spacex/presentation/auth/AuthUiState.kt`
- `shared/src/commonMain/kotlin/io/github/kevinah95/spacex/presentation/rocketLaunch/RocketLaunchViewModel.kt`
- `shared/src/commonMain/kotlin/io/github/kevinah95/spacex/presentation/rocketLaunch/RocketLaunchUiState.kt`
- `assets/viewmodel-uistate-example.kt`
