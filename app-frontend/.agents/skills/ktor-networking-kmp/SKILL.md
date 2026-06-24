---
name: ktor-networking-kmp
description: "Trigger: Ktor networking, API client, HTTP datasource. Use platform clients behind common datasources."
license: Apache-2.0
metadata:
  author: ddgutierrezc
  version: "1.0"
---

## Activation Contract

Use when adding API calls, HTTP client config, serializers, or remote datasource tests.

## Hard Rules

- Keep endpoint calls inside remote datasources.
- Create the Ktor `HttpClient` through a platform seam when engines differ.
- Install common content negotiation/JSON behavior consistently.
- Keep URLs, decoding, and response mapping testable with `MockEngine`.
- Do not expose Ktor types to UI or domain layers unless explicitly designing a low-level API.

## Decision Gates

| Concern       | Owner                        |
| ------------- | ---------------------------- |
| Engine choice | platform actual              |
| JSON config   | common network module        |
| Endpoint call | remote datasource            |
| HTTP test     | commonTest with `MockEngine` |

## Execution Steps

1. Add/extend remote datasource method.
2. Use injected `HttpClient` and dispatcher.
3. Decode into chosen DTO/domain model.
4. Add MockEngine tests for success, empty, error, and nullable payloads.

## Output Contract

Report endpoint, client config, model decoding, and tests.

## References

- `shared/src/commonMain/kotlin/io/github/kevinah95/spacex/data/remote/RemoteRocketLaunchesDataSource.kt`
- `shared/src/commonMain/kotlin/io/github/kevinah95/spacex/data/remote/IRemoteRocketLaunchesDataSource.kt`
- `shared/src/commonMain/kotlin/io/github/kevinah95/spacex/di/modules/NetworkModule.kt`
- `assets/ktor-datasource-example.kt`
