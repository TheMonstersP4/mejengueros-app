---
name: firebase-monitoring-gitlive-adapter
description: "Trigger: Firebase analytics, Crashlytics, monitoring. Keep reporting behind shared reporter ports."
license: Apache-2.0
metadata:
  author: ddgutierrezc
  version: "1.0"
---

## Activation Contract

Use when wiring analytics or crash reporting through Firebase/GitLive.

## Hard Rules

- Common code calls reporter ports only.
- Android/iOS adapters may forward events/exceptions to GitLive Firebase Analytics/Crashlytics.
- Desktop/JVM may no-op, log locally, or use another provider, but must be deliberate.
- Do not let reporting failures break business flows.

## Decision Gates

| Concern                         | Adapter                    |
| ------------------------------- | -------------------------- |
| User behavior metrics           | Analytics reporter         |
| Exceptions and recovery context | Crash reporter             |
| Unsupported target              | no-op or alternate adapter |

## Execution Steps

1. Define event/error reporting at the behavior boundary.
2. Forward platform-supported calls in actual adapters.
3. Keep event names/params simple and serializable.
4. Report no-op targets explicitly.

## Output Contract

Report reporter ports, adapter targets, and failure/no-op policy.

## References

- `shared/src/commonMain/kotlin/io/github/kevinah95/spacex/monitoring/AnalyticsReporter.kt`
- `shared/src/commonMain/kotlin/io/github/kevinah95/spacex/monitoring/CrashReporter.kt`
- `shared/src/androidMain/kotlin/io/github/kevinah95/spacex/monitoring/AnalyticsReporter.android.kt`
- `shared/src/androidMain/kotlin/io/github/kevinah95/spacex/monitoring/CrashReporter.android.kt`
- `shared/src/iosMain/kotlin/io/github/kevinah95/spacex/monitoring/AnalyticsReporter.ios.kt`
- `shared/src/iosMain/kotlin/io/github/kevinah95/spacex/monitoring/CrashReporter.ios.kt`
- `shared/src/jvmMain/kotlin/io/github/kevinah95/spacex/monitoring/AnalyticsReporter.jvm.kt`
- `shared/src/jvmMain/kotlin/io/github/kevinah95/spacex/monitoring/CrashReporter.jvm.kt`
- `assets/firebase-monitoring-adapter-example.kt`
