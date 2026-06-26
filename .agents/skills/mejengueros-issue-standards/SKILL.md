---
name: mejengueros-issue-standards
description: "Trigger: Mejengueros issues, bug reports, feature requests, GitHub issue creation, Project tracking, Sprint assignment. Enforces repo-local issue standards without replacing global issue/PR skills."
license: Apache-2.0
metadata:
  author: mejengueros-team
  version: "1.0"
---

## Activation Contract

Use this skill when creating, updating, or triaging Mejengueros GitHub issues, bug reports, feature requests, Project metadata, sprint assignment, or parent issue links.

## Hard Rules

- This skill COMPLEMENTS the global `issue-creation` skill and the repo-local `mejengueros-pr-standards` skill; do not replace or overwrite either workflow.
- GitHub issue titles and bodies MUST be written in Spanish.
- PR title/body language is governed by `mejengueros-pr-standards` and remains English; never apply PR language rules to issues.
- Search for duplicates before creating a new issue.
- For bug reports, validate the reported behavior before creating the issue when feasible; if not feasible, state that validation was not completed.
- Every non-epic issue must include Project `Mejengueros`, sprint, estimate, assignee, milestone, and parent issue triage.
- If the requested assignee is not assignable, create or update the issue, report the blocker, and leave assignment unresolved; never silently assign someone else.

## Decision Gates

| Case | Rule |
| --- | --- |
| Existing matching issue | Update or reuse it instead of creating a duplicate |
| Bug report | Validate behavior first when repo access, reproduction steps, or evidence make that feasible |
| No feasible validation | Create or update the bug with explicit unverified status |
| Non-epic issue | Require Project `Mejengueros`, sprint, estimate, assignee, milestone, and parent triage |
| Epic/container issue | Allow execution metadata gaps only if child issues will carry execution ownership |
| Requested assignee cannot be assigned | Leave assignee unresolved and report the blocker |
| PR workflow | Defer PR title/body language and PR governance to `mejengueros-pr-standards` |

## Execution Steps

1. Run the global `issue-creation` workflow first, then apply this repo-local issue overlay.
2. Search GitHub issues and the Mejengueros Project for duplicates or an existing parent before creating anything new.
3. If the request is a bug and validation is feasible, verify the reported behavior from available evidence before creation.
4. Write the GitHub issue title and body in Spanish.
5. Set or confirm Project `Mejengueros`, sprint, estimate, milestone, labels, and parent issue triage.
6. Set the requested assignee only if GitHub allows it; otherwise leave assignment unresolved and record the blocker.
7. Keep PR governance separate: if the workflow continues into PR work, hand off PR title/body and review rules to `mejengueros-pr-standards`.

## Output Contract

Report: title language, body language, project, sprint, estimate, assignee result, milestone, parent, and labels. If a bug issue was created, also report whether the behavior was validated or left unverified. If assignee resolution failed, state the blocker explicitly.

## References

- `opencode.json`
- `.agents/skills/mejengueros-pr-standards/SKILL.md`
