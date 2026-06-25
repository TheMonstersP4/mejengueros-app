---
name: quality-formatting-hooks
description: "Trigger: Spotless, formatting, ktfmt, Prettier, pre-commit. Preserve repository formatting gates."
license: Apache-2.0
metadata:
  author: ddgutierrezc
  version: "1.0"
---

## Activation Contract

Use when touching formatting, hooks, generated local files, or quality checks.

## Hard Rules

- Use Spotless as the formatting authority for Kotlin, Gradle Kotlin, YAML/JSON, and misc files.
- Keep license headers consistent for Kotlin/KTS files.
- Do not commit local Pi/runtime files unless intentionally part of the repo.
- On Windows, watch CRLF issues for JSON/YAML files included in Spotless.
- Prefer `spotlessCheck`; use `spotlessApply` only when formatting changes are intended.

## Decision Gates

| Need               | Command/config                          |
| ------------------ | --------------------------------------- |
| Check formatting   | `./gradlew spotlessCheck`               |
| Apply formatting   | `./gradlew spotlessApply`               |
| Install hook       | `task setup:spotless`                   |
| Local runtime file | ignore/exclude, do not format into repo |

## Execution Steps

1. Check whether touched file types are under Spotless.
2. Run focused formatting check when practical.
3. Fix includes/excludes before formatting local runtime artifacts.
4. Report command result and residual local-only issues.

## Output Contract

Report formatting command, changed files, and hook/config impact.

## References

- `build.gradle.kts`
- `.githooks/pre-commit`
- `.prettierrc.yml`
