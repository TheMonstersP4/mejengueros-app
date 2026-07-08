---
name: gradle-windows-opencode-safe-runner
description: "Trigger: Gradle, gradlew, KMP tests, frontend validation, Windows hangs, OpenCode subagents. Run Gradle safely under OpenCode on Windows."
license: Apache-2.0
metadata:
  author: "ddgutierrezc"
  version: "1.0"
---

# Gradle Windows OpenCode Safe Runner

## Activation Contract

Use when an agent or subagent must run Gradle/KMP validation from OpenCode on Windows, especially `gradlew.bat`, frontend checks, `jvmTest`, Android host tests, or when a previous command left `java.exe` running.

## Hard Rules

- Do not run long Gradle commands from subagents without this protocol.
- Prefer orchestrator-run validation for long/full gates; subagents may run focused Gradle only with bounded timeout and cleanup.
- Always stop Gradle before and after validation.
- Always disable file watching for OpenCode Windows runs.
- If a Gradle command is cancelled or times out, inspect and terminate leftover `java`/`gradle` processes before continuing.
- Do not read or expose `.env` or secrets while diagnosing Gradle.

## Decision Gates

| Situation | Action |
| --- | --- |
| Focused quick test | Use safe flags and timeout. |
| Full frontend gate | Prefer orchestrator-run command, not background subagent. |
| Command hangs/cancels | Stop Gradle, kill leftover Java, then report partial evidence. |
| Repeated hangs | Split tasks and run one Gradle target per invocation. |

## Execution Steps

1. From `app-frontend`, run `./gradlew.bat --stop`.
2. Run Gradle with safe flags:
   `./gradlew.bat <tasks> --no-daemon --no-watch-fs --no-configuration-cache --max-workers=2 --console=plain`.
3. Use the tool timeout deliberately; avoid unbounded waits.
4. After the command completes, run `./gradlew.bat --stop` again.
5. If interrupted, run `Get-Process -Name java,gradle -ErrorAction SilentlyContinue` and terminate stale build processes with `Stop-Process` before retrying.
6. Report exact commands, duration/result, and whether cleanup found leftover processes.

## Output Contract

Return the Gradle tasks run, safe flags used, pass/fail status, cleanup result, and any leftover process IDs killed.

## References

- `../gradle-kmp-build-targets/SKILL.md`
- `../quality-formatting-hooks/SKILL.md`
