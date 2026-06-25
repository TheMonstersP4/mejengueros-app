---
name: firebase-messaging-gitlive-adapter
description: "Trigger: Firebase messaging, FCM token, push topics. Isolate push messaging as a platform adapter."
license: Apache-2.0
metadata:
  author: ddgutierrezc
  version: "1.0"
---

## Activation Contract

Use when adding push token retrieval, topic subscription, or notification permission flows.

## Hard Rules

- Keep push messaging behind a shared messaging port.
- Request notification permissions in platform UI/lifecycle code, not common screens.
- Token sync belongs in platform bootstrap or a dedicated service, not reusable UI content.
- Desktop/JVM unsupported behavior must be explicit and safe.

## Decision Gates

| Concern              | Owner                                   |
| -------------------- | --------------------------------------- |
| Runtime permission   | platform Activity/app layer             |
| FCM token/topic      | messaging adapter                       |
| Backend registration | app/service layer after token retrieval |
| Unsupported target   | no-op or thrown unsupported by policy   |

## Execution Steps

1. Confirm permission and token lifecycle.
2. Implement messaging adapter per supported target.
3. Keep token logging/sync out of shared UI.
4. Document unsupported target behavior.

## Output Contract

Report permission flow, token flow, adapters, and unsupported targets.

## References

- `shared/src/commonMain/kotlin/io/github/kevinah95/spacex/notifications/PushMessagingReporter.kt`
- `shared/src/androidMain/kotlin/io/github/kevinah95/spacex/notifications/PushMessagingReporter.android.kt`
- `shared/src/iosMain/kotlin/io/github/kevinah95/spacex/notifications/PushMessagingReporter.ios.kt`
- `shared/src/jvmMain/kotlin/io/github/kevinah95/spacex/notifications/PushMessagingReporter.jvm.kt`
- `androidApp/src/main/kotlin/io/github/kevinah95/spacex/MainActivity.kt`
- `assets/firebase-messaging-adapter-example.kt`
