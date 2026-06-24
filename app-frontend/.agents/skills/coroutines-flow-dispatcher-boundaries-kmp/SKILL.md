---
name: coroutines-flow-dispatcher-boundaries-kmp
description: "Trigger: Flow, coroutines, dispatchers, KMP threading. Own execution context at data boundaries."
license: Apache-2.0
metadata:
  author: ddgutierrezc
  version: "1.0"
---

## Activation Contract

Use when adding flows, repository operations, network calls, cache writes, or coroutine tests.

## Hard Rules

- Inject dispatchers into data-layer classes when behavior must be testable.
- Apply `flowOn` at the boundary that owns upstream work.
- Keep UI collectors simple; ViewModels should not fix data-layer thread mistakes.
- Handle errors where fallback decisions are made.
- Use test dispatchers or unconfined test dispatchers in common tests.

## Decision Gates

| Work                          | Dispatcher owner            |
| ----------------------------- | --------------------------- |
| Network I/O                   | Remote datasource           |
| Cache write/read coordination | Repository/local datasource |
| CPU mapping/sync              | Repository or mapper owner  |
| UI collection                 | ViewModel/UI lifecycle      |

## Execution Steps

1. Identify upstream work for each flow.
2. Place `flowOn` close to that upstream work.
3. Put `catch` where recovery policy belongs.
4. Test success, error, and cancellation/fallback behavior.

## Output Contract

Report dispatcher injection, `flowOn` placement, and error/fallback behavior.

## References

- `shared/src/commonMain/kotlin/io/github/kevinah95/spacex/data/remote/RemoteRocketLaunchesDataSource.kt`
- `shared/src/commonMain/kotlin/io/github/kevinah95/spacex/data/repository/RocketLaunchesRepository.kt`
- `assets/flow-dispatcher-boundary-example.kt`
