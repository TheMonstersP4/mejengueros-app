---
name: koin-di-kmp
description: "Trigger: Koin, dependency injection, KMP modules. Wire shared and platform dependencies consistently."
license: Apache-2.0
metadata:
  author: ddgutierrezc
  version: "1.0"
---

## Activation Contract

Use when adding services, repositories, ViewModels, datasources, drivers, clients, or platform modules.

## Hard Rules

- Keep shared bindings in common Koin modules.
- Use platform modules for platform-specific factories such as drivers or context-backed services.
- Register interfaces to implementations for data seams.
- Do not instantiate repositories, clients, or databases directly in UI.
- Keep `initKoin` as the common entry for startup wiring.

## Decision Gates

| Dependency                  | Binding location           |
| --------------------------- | -------------------------- |
| Repository/datasource       | shared data module         |
| ViewModel                   | shared presentation module |
| SQL driver/platform context | platform module            |
| HTTP client factory         | platform seam/module       |

## Execution Steps

1. Add the implementation class.
2. Bind it in the nearest Koin module.
3. Include platform modules from the shared root module.
4. Verify consumers use DI rather than constructors at the UI edge.

## Output Contract

Report modules updated and dependency graph path.

## References

- `shared/src/commonMain/kotlin/io/github/kevinah95/spacex/di/KoinHelper.kt`
- `shared/src/commonMain/kotlin/io/github/kevinah95/spacex/di/modules/SharedModule.kt`
- `shared/src/commonMain/kotlin/io/github/kevinah95/spacex/di/modules/DataModule.kt`
- `shared/src/commonMain/kotlin/io/github/kevinah95/spacex/di/modules/PresentationModule.kt`
- `assets/koin-modules-example.kt`
