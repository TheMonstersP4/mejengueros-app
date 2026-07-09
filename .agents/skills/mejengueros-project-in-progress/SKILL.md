---
name: mejengueros-project-in-progress
description: "Trigger: Project status, In progress, issue work, PR start. Keep Mejengueros issue work aligned with In progress status and PR-start automation."
license: Apache-2.0
metadata:
  author: mejengueros-team
  version: "1.0"
---

## Activation Contract

Use this skill when starting Mejengueros issue-related work, linking a PR to an issue, or updating Project status expectations.

## Hard Rules

- Ensure the active issue is in Project `Mejengueros` and treated as `In progress` when work starts.
- Do not create a duplicate local `branch-pr` skill or override PR governance owned by `mejengueros-pr-standards`.
- Rely on GitHub automation to move referenced issues to `In progress` when a PR is opened, edited, reopened, synchronized, or marked ready for review with `Closes #N`, `Fixes #N`, or `Resolves #N`.
- Do not add custom PR automation for `Done` or `Todo` unless the user explicitly changes the workflow.

## Decision Gates

| Case | Rule |
| --- | --- |
| Starting issue work before a PR exists | Confirm the issue should already be treated as `In progress` |
| PR references the issue with closing keywords | Trust the repository workflow to sync Project status |
| No linked issue | Ask for or add the correct issue link before assuming Project automation will run |
| Workflow fails because of permissions, API retries, or Project constant drift | Stop assuming automation succeeded, inspect the failed workflow run, and coordinate a manual `In progress` update if work has already started |

## Execution Steps

1. Identify the issue that owns the work.
2. Confirm the issue belongs to Project `Mejengueros`.
3. When opening or editing the PR, include `Closes #N`, `Fixes #N`, or `Resolves #N` in the PR body.
4. Let `.github/workflows/project-in-progress.yml` handle the Project `In progress` transition from PR metadata.
5. If that workflow fails, verify trusted repository access for the PR author, verify Project token/config constants, and manually move the issue to `In progress` before continuing if the team is already working the issue.

## Output Contract

Report the linked issue, whether the PR body contains a valid closing reference, and whether Project `In progress` should be handled manually now or by GitHub automation.

## Failure Handling

- Permission failure: confirm the PR author is a trusted collaborator with `write`, `maintain`, or `admin` access before expecting automation to mutate Project state.
- API/transient failure: rerun the workflow after the retry window and inspect the failed job logs before making more changes.
- Config drift: compare the workflow's Project constants and guarded action version against the live repository setup before re-enabling automation.
- If automation is still blocked, move the issue to `In progress` manually and note the failure in the task or PR so the workflow can be repaired separately.

## References

- `README.md`
- `.github/workflows/project-in-progress.yml`
- `.agents/skills/mejengueros-pr-standards/SKILL.md`
