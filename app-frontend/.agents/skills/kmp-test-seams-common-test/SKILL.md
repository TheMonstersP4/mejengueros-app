---
name: kmp-test-seams-common-test
description: "Trigger: KMP tests, commonTest, MockEngine, Mokkery. Test architecture through shared seams."
license: Apache-2.0
metadata:
  author: ddgutierrezc
  version: "1.0"
---

## Activation Contract

Use when adding tests or making architecture more testable.

## Hard Rules

- Prefer `commonTest` for shared repository, datasource, mapper, and ViewModel behavior when dependencies support it.
- Test remote datasources with Ktor `MockEngine`.
- Test repository orchestration through datasource interfaces and mocks.
- Inject dispatchers so coroutine/Flow tests are deterministic.
- Avoid placeholder tests that do not validate architecture behavior.

## Decision Gates

| Subject                         | Test seam                                  |
| ------------------------------- | ------------------------------------------ |
| HTTP decoding/error             | Ktor `MockEngine`                          |
| Repository fallback/cache write | mocked datasource interfaces               |
| Coroutine Flow                  | `runTest` + test dispatcher                |
| DI wiring                       | Koin test only when graph behavior matters |

## Execution Steps

1. Identify the seam/interface to test.
2. Add focused common tests for success, error, empty, and nullable cases.
3. Use deterministic dispatchers.
4. Run relevant module tests and report skipped platform targets.

## Output Contract

Report test target, seam used, commands run, and platform skips.

## References

- `shared/src/commonTest/kotlin`
- `composeApp/src/commonTest/kotlin`
- `assets/common-test-examples.kt`
