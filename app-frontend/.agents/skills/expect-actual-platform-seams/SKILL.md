---
name: expect-actual-platform-seams
description: "Trigger: expect actual, platform seam, KMP abstraction. Create explicit multiplatform boundaries."
license: Apache-2.0
metadata:
  author: ddgutierrezc
  version: "1.0"
---

## Activation Contract

Use when common code needs platform-specific behavior.

## Hard Rules

- Declare the common contract in `commonMain` with `expect` only when DI alone is insufficient or platform APIs differ.
- Put every `actual` in the narrowest platform source set: `androidMain`, `iosMain`, or `jvmMain`.
- Keep the common contract small and domain-oriented; do not leak vendor SDK types into `commonMain`.
- Desktop/JVM fallbacks must be explicit: no-op, in-memory, unsupported, or real implementation.

## Decision Gates

| Situation                            | Pattern                                  |
| ------------------------------------ | ---------------------------------------- |
| Platform API object/factory          | `expect`/`actual`                        |
| Runtime-swappable implementation     | interface + DI                           |
| Vendor SDK unavailable on one target | common facade + platform actual fallback |

## Execution Steps

1. Define the common behavior without vendor types.
2. Add actual implementations per supported target.
3. Register factories/adapters in platform DI modules when needed.
4. Document fallback behavior for unsupported targets.

## Output Contract

Report common contract, actual files, and unsupported-target behavior.

## References

- `shared/src/commonMain/kotlin/io/github/kevinah95/spacex/auth/AuthClient.kt`
- `shared/src/commonMain/kotlin/io/github/kevinah95/spacex/di/modules/NetworkModule.kt`
- `assets/expect-actual-seam-example.kt`
