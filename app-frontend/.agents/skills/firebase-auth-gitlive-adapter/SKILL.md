---
name: firebase-auth-gitlive-adapter
description: "Trigger: Firebase auth, GitLive auth, anonymous session. Implement auth as an optional platform adapter."
license: Apache-2.0
metadata:
  author: ddgutierrezc
  version: "1.0"
---

## Activation Contract

Use when implementing Firebase/GitLive authentication behind the shared auth port.

## Hard Rules

- Keep shared feature code dependent on the auth port, not Firebase SDK types.
- Android/iOS actuals may use GitLive Firebase Auth.
- Desktop/JVM behavior must be explicit, such as in-memory anonymous session or unsupported.
- Authentication state drives UI gating through ViewModel/navigation, not direct screen SDK calls.

## Decision Gates

| Target             | Adapter behavior                                 |
| ------------------ | ------------------------------------------------ |
| Android/iOS        | GitLive Firebase Auth actual                     |
| Desktop prototype  | in-memory session acceptable                     |
| Production desktop | replace with real provider or unsupported policy |

## Execution Steps

1. Confirm the auth port API.
2. Implement platform actual/adapters.
3. Initialize Firebase before auth usage on mobile targets.
4. Expose session state/actions through ViewModel.

## Output Contract

Report auth port, target adapters, initialization requirement, and desktop policy.

## References

- `shared/src/commonMain/kotlin/io/github/kevinah95/spacex/auth/AuthClient.kt`
- `shared/src/androidMain/kotlin/io/github/kevinah95/spacex/auth/AuthClient.android.kt`
- `shared/src/iosMain/kotlin/io/github/kevinah95/spacex/auth/AuthClient.ios.kt`
- `shared/src/jvmMain/kotlin/io/github/kevinah95/spacex/auth/AuthClient.jvm.kt`
- `assets/firebase-auth-adapter-example.kt`
