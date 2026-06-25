---
name: navigation-3-state-first
description: "Trigger: Navigation 3, NavDisplay, NavKey, back stack. Use state-first navigation for new KMP projects."
license: Apache-2.0
metadata:
  author: ddgutierrezc
  version: "1.0"
---

## Activation Contract

Use for new-project navigation or explicit migration from Navigation 2 to Navigation 3. This is target architecture, not current repo DNA.

## Hard Rules

- Model routes as `@Serializable` `NavKey` types.
- Use `NavBackStack`/`rememberNavBackStack` or a custom `NavigationState` as the source of truth.
- Render destinations with `NavDisplay` and `entryProvider { entry<Route> { key -> ... } }`.
- Use a `Navigator` wrapper for top-level switching, guarded routes, and back behavior.
- Use entry decorators when ViewModel/saveable state must be scoped to entries.

## Decision Gates

| Need            | Navigation 3 pattern                        |
| --------------- | ------------------------------------------- |
| Simple stack    | `rememberNavBackStack(Start)`               |
| Tabs            | multiple back stacks + `topLevelRoute`      |
| Route args      | fields on typed `NavKey`                    |
| Auth gate       | navigator interception or stack replacement |
| ViewModel scope | lifecycle Navigation3 decorator             |

## Execution Steps

1. Define route keys implementing `NavKey`.
2. Create back stack/navigation state.
3. Build `entryProvider` entries.
4. Wire UI callbacks to `Navigator`/backStack changes.
5. Keep content screens controller-free.

## Output Contract

Report route keys, state owner, entries, decorators, and top-level/back behavior.

## References

- `assets/navigation3-app-example.kt`
