---
name: feature-blueprint
description: "Trigger: new feature, feature blueprint, vertical slice. Orchestrate focused SpaceX-style KMP feature work."
license: Apache-2.0
metadata:
  author: ddgutierrezc
  version: "1.0"
---

## Activation Contract

Use when adding a new user-facing feature or vertical slice.

## Hard Rules

- This is an orchestration checklist; load focused skills for actual implementation rules.
- Start with route/screen contract, then ViewModel/UiState, then repository/datasource/cache, then DI, then tests.
- Keep platform services optional and behind ports.
- Choose Navigation 2 only for current repo maintenance; choose Navigation 3 for the new target project when approved.
- Do not skip tests for data and ViewModel seams.

## Decision Gates

| Feature need     | Load skill                                  |
| ---------------- | ------------------------------------------- |
| UI/screen        | compose hoisting/current contracts          |
| Navigation       | navigation-2 or navigation-3                |
| Data/API/cache   | repository, Ktor, SQLDelight, model mapping |
| Platform service | platform-service ports + adapter            |
| Tests            | kmp-test-seams-common-test                  |

## Execution Steps

1. Define feature route and screen contract.
2. Define UiState and ViewModel actions.
3. Add repository/datasource/cache seams only as needed.
4. Register dependencies in Koin.
5. Add focused common tests and run validation.

## Output Contract

Report loaded skills, vertical slice files, tests, and non-goals.

## References

- `skills/`
- `assets/feature-slice-checklist.md`
