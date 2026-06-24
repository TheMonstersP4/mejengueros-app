---
name: platform-service-ports-kmp
description: "Trigger: platform service ports, auth, analytics, crash, messaging. Keep vendor SDKs behind KMP facades."
license: Apache-2.0
metadata:
  author: ddgutierrezc
  version: "1.0"
---

## Activation Contract

Use when adding auth, monitoring, messaging, permissions, storage, sensors, or other platform services.

## Hard Rules

- Model the shared app dependency as a port/facade first.
- Keep Firebase, Apple, Android, or desktop SDK details out of common feature code.
- Define whether each target has real, no-op, in-memory, or unsupported behavior.
- Keep auth/session ports separate from monitoring and messaging ports.

## Decision Gates

| Service               | Suggested port              |
| --------------------- | --------------------------- |
| Identity/session      | Auth client/session service |
| Analytics             | Analytics reporter          |
| Crash/error reporting | Crash reporter              |
| Push/token/topic      | Messaging reporter          |

## Execution Steps

1. Define a minimal common API around app behavior.
2. Implement platform adapters in target source sets.
3. Wire through DI or explicit facade access consistently.
4. Ensure desktop/non-mobile behavior is deliberate.

## Output Contract

State the port, adapters, target behavior, and vendor coupling avoided.

## References

- `shared/src/commonMain/kotlin/io/github/kevinah95/spacex/monitoring/AnalyticsReporter.kt`
- `shared/src/commonMain/kotlin/io/github/kevinah95/spacex/monitoring/CrashReporter.kt`
- `shared/src/commonMain/kotlin/io/github/kevinah95/spacex/notifications/PushMessagingReporter.kt`
- `assets/platform-service-port-example.kt`
