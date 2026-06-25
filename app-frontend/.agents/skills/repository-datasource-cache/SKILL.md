---
name: repository-datasource-cache
description: "Trigger: repository, datasource, cache fallback. Keep data access behind repository and source interfaces."
license: Apache-2.0
metadata:
  author: ddgutierrezc
  version: "1.0"
---

## Activation Contract

Use when adding or changing data retrieval, caching, or synchronization behavior.

## Hard Rules

- Expose feature data through repository interfaces.
- Keep remote access in remote datasource classes.
- Keep persistence access in local datasource classes.
- Repositories coordinate remote/local behavior and fallback policy.
- Do not let UI or ViewModels call Ktor or SQLDelight directly.

## Decision Gates

| Responsibility         | Owner             |
| ---------------------- | ----------------- |
| API call               | Remote datasource |
| SQL query/mapping      | Local datasource  |
| Cache write/fallback   | Repository        |
| UI loading/error state | ViewModel         |

## Execution Steps

1. Define repository and datasource interfaces when the seam needs tests or swapping.
2. Implement remote/local sources independently.
3. Put sync/fallback decisions in the repository.
4. Add tests around success, failure, and empty-cache behavior.

## Output Contract

Report source boundaries, fallback policy, and tests added/updated.

## References

- `shared/src/commonMain/kotlin/io/github/kevinah95/spacex/data/repository/IRocketLaunchesRepository.kt`
- `shared/src/commonMain/kotlin/io/github/kevinah95/spacex/data/repository/RocketLaunchesRepository.kt`
- `shared/src/commonMain/kotlin/io/github/kevinah95/spacex/data/remote/RemoteRocketLaunchesDataSource.kt`
- `shared/src/commonMain/kotlin/io/github/kevinah95/spacex/data/remote/IRemoteRocketLaunchesDataSource.kt`
- `shared/src/commonMain/kotlin/io/github/kevinah95/spacex/data/local/ILocalRocketLaunchesDataSource.kt`
- `shared/src/commonMain/kotlin/io/github/kevinah95/spacex/data/local/LocalRocketLaunchesDataSource.kt`
- `assets/repository-datasource-example.kt`
