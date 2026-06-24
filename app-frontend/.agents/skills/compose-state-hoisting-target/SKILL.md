---
name: compose-state-hoisting-target
description: "Trigger: Compose state hoisting, route content split, stateless screens. Apply modern hoisting without over-hoisting."
license: Apache-2.0
metadata:
  author: ddgutierrezc
  version: "1.0"
---

## Activation Contract

Use when creating new Compose screens or refactoring current screens toward target architecture.

## Hard Rules

- Do not hoist everything blindly; hoist state to the lowest stable owner that reads/writes it.
- Use `Route`/container composables for ViewModel, DI, state collection, and navigation callbacks.
- Use `Screen`/content composables for rendering immutable state and emitting callbacks.
- Keep private visual state local with `remember` when no parent/business logic needs it.
- Use ViewModel for screen/business state and plain state holders for complex UI-only logic.

## Decision Gates

| State kind                           | Owner                                  |
| ------------------------------------ | -------------------------------------- |
| Private visual toggle/scroll/dialog  | Local composable or plain state holder |
| Shared by sibling composables        | Lowest common ancestor                 |
| Depends on repository/business rules | ViewModel                              |
| Needs reusable preview/test content  | Stateless `Screen` params              |

## Execution Steps

1. Create `FeatureRoute` for wiring.
2. Collect immutable `UiState` in the route.
3. Pass state and callbacks to `FeatureScreen`.
4. Keep navigation as callback parameters, not direct controller access inside content.

## Output Contract

Report route/content split, state owner decisions, and any local state intentionally retained.

## References

- `composeApp/src/commonMain/kotlin/io/github/kevinah95/spacex/screens/AuthStartScreen.kt`
- `composeApp/src/commonMain/kotlin/io/github/kevinah95/spacex/screens/LaunchListScreen.kt`
- `composeApp/src/commonMain/kotlin/io/github/kevinah95/spacex/screens/LaunchDetailScreen.kt`
- `composeApp/src/commonMain/kotlin/io/github/kevinah95/spacex/screens/ProfileScreen.kt`
- `assets/route-screen-example.kt`
