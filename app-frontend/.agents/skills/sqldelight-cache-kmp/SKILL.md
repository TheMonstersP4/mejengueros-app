---
name: sqldelight-cache-kmp
description: "Trigger: SQLDelight, local cache, database schema. Keep persistence behind local datasource mapping."
license: Apache-2.0
metadata:
  author: ddgutierrezc
  version: "1.0"
---

## Activation Contract

Use when adding tables, queries, driver factories, cache reads/writes, or local datasource tests.

## Hard Rules

- Define SQL schema and named queries in `.sq` files.
- Keep SQLDelight generated APIs behind local datasources.
- Use platform `DriverFactory` actuals for Android, iOS, and JVM/Desktop.
- Perform domain/cache mapping in local datasource code.
- Wrap destructive refreshes in transactions.

## Decision Gates

| Need                     | Pattern                       |
| ------------------------ | ----------------------------- |
| Platform database driver | `DriverFactory` expect/actual |
| Query API                | SQLDelight `.sq` named query  |
| Domain reconstruction    | local datasource mapper       |
| Refresh all cache        | transaction clear + insert    |

## Execution Steps

1. Update `.sq` schema/query file.
2. Add local datasource mapping.
3. Register database/queries/datasource in DI.
4. Test mapping and empty/cache fallback through repository tests.

## Output Contract

Report schema changes, queries, mapping behavior, and driver targets.

## References

- `shared/src/commonMain/sqldelight`
- `shared/src/commonMain/kotlin/io/github/kevinah95/spacex/data/local/DriverFactory.kt`
- `shared/src/androidMain/kotlin/io/github/kevinah95/spacex/data/local/DriverFactory.android.kt`
- `shared/src/iosMain/kotlin/io/github/kevinah95/spacex/data/local/DriverFactory.ios.kt`
- `shared/src/jvmMain/kotlin/io/github/kevinah95/spacex/data/local/DriverFactory.jvm.kt`
- `assets/sqldelight-cache-example.sq`
- `assets/local-datasource-mapper-example.kt`
