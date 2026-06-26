---
name: mejengueros-pr-standards
description: "Trigger: Mejengueros issues, PRs, branch naming, reviewers, GitHub Project tracking, and GitHub PR creation. Extends global issue/PR workflows with repository-local rules."
license: Apache-2.0
metadata:
  author: mejengueros-team
  version: "1.0"
---

## Activation Contract

Use this skill when work includes Mejengueros issues, GitHub Project tracking, pull requests, branch naming, reviewer selection, or GitHub PR creation for this repository.

## Hard Rules

- This skill COMPLEMENTS the global `branch-pr` and `comment-writer` skills; it does not replace them.
- First follow the complete `branch-pr` flow for approved issues, `type:*` labels, PR body, Conventional Commit titles, and base validations.
- Do not create or overwrite a local skill named `branch-pr`.
- All branches must use the `type/<github-username>/<description>` format.
- Valid types are `feat|fix|chore|docs|style|refactor|perf|test|build|ci|revert`.
- Every PR title must keep the Conventional Commit format, and the summary after `:` must be written in English.
- Every PR body must be written in clear English.
- Every PR comment, review comment, inline review, and maintainer reply must be written in clear English by default.
- If the user explicitly requests another language for a specific PR comment or review, follow that request for that comment only.
- Do not apply PR language rules to GitHub issues; issue titles and bodies remain governed by `mejengueros-issue-standards`.
- Every PR must use exactly one primary `type:*` label chosen by evaluating the dominant impact of the full change; do not copy a template option mechanically.
- Every PR must request `@kevinah95` as reviewer.
- `CODEOWNERS` covers all files and will enforce this review when branch protection requires code owner reviews.
- Every non-epic issue must be assigned to the `Mejengueros` GitHub Project, have a sprint, estimate, assignee, and milestone.
- If a new issue is created because no existing approved issue matches the planned PR work, parent issue triage is mandatory before opening the PR.
- When creating an issue, assign it to the current authenticated GitHub user unless the user explicitly names another assignee.
- Assign new issues to the current sprint by default; if there is no current sprint or the intended sprint is ambiguous, ask before creating or updating the issue.
- Use the repository estimation scale: `1` means up to 1.5 days, `3` means about half a week, and `5` means about one full week.
- Issues labeled `type:epic` are container issues and may omit sprint, estimate, assignee, or milestone only when their child issues carry execution ownership and planning fields.

## Decision Gates

| Case | Rule |
| --- | --- |
| Base PR flow | Follow `branch-pr` first |
| New branch | Use `type/<github-username>/<description>` |
| PR title | Use the `type(scope): summary in English` format |
| PR body | Write it in clear English |
| PR comments and reviews | Write human review/conversation feedback in clear English by default |
| Explicit comment language request | Use the requested language for that comment only |
| PR type | Choose exactly one primary `type:*` based on the dominant impact of the full change |
| Required reviewer | Request `@kevinah95` |
| Ownership automation | Rely on `CODEOWNERS` once branch protection requires code owner reviews |
| New non-epic issue | Add it to the `Mejengueros` Project with sprint, estimate, assignee, and milestone |
| New issue for PR work | Search existing parents; assign a clear parent before opening the PR, or ask before leaving it top-level |
| Issue assignee unspecified | Use the authenticated GitHub user |
| Sprint unspecified | Use the current sprint; ask if no current sprint can be identified |
| Estimate needed | Choose `1`, `3`, or `5` from expected workload |
| Container issue | Add `type:epic` and keep execution fields on child issues |

## Execution Steps

1. Run the global `branch-pr` skill workflow first.
2. Create or validate the branch with the `type/<github-username>/<description>` pattern.
3. Verify that `type` belongs to the allowed set.
4. Evaluate the full PR diff and choose the single primary `type:*` label that best represents the dominant impact.
5. Do not use `type:epic` for PRs; it is only for container issues.
6. Write the PR title as `type(scope): English summary`, for example `chore(repo): standardize PR governance`.
7. Write the PR body in clear English.
8. Write PR comments, review comments, inline reviews, and maintainer replies in clear English by default.
9. When opening or editing the PR, request `@kevinah95` as reviewer.
10. When documenting examples, use real repository conventions such as `feat/ddgutierrezc/issue-48-complex-frontend`, `fix/ddgutierrezc/owner-onboarding`, and `chore/ddgutierrezc/readme-contribucion`.

## Issue Tracking Steps

1. Before creating an issue for PR work, search the repository and Project for an existing approved issue that already matches the planned change.
2. If no existing issue matches, create a new issue and identify whether it is execution work or a container/epic.
3. For execution work, ensure the issue is assigned to the `Mejengueros` GitHub Project.
4. Set the assignee to the authenticated GitHub user unless the user explicitly requested another assignee.
5. Set the sprint to the current Project iteration unless the user explicitly requested a different sprint.
6. If there is no current sprint, or more than one sprint could apply, ask the user before assigning it.
7. Set the estimate using the `1`, `3`, `5` scale: `1` for up to 1.5 days, `3` for about half a week, and `5` for about one full week.
8. Set the milestone that matches the target delivery window; ask if the milestone is not clear from the user request or issue context.
9. Search existing `type:epic` and parent issues for a clear parent based on the new issue scope.
10. If a clear parent exists, assign the new issue as a child before opening the PR that closes it.
11. If no clear parent exists, ask the user whether to leave the issue top-level with a reason or create a new `type:epic` parent.
12. For container work, add `type:epic` and verify the child issues carry sprint, estimate, assignee, and milestone.

## Output Contract

Report whether the PR follows the global `branch-pr` flow, whether the branch matches the local pattern, whether the title, body, and PR comments/reviews are in English, and whether `@kevinah95` was requested as reviewer. For issue work, report Project assignment, sprint, estimate, assignee, milestone, parent issue assignment with reason and confidence, and whether `type:epic` was used as an explicit container exception.

## References

- `README.md`
- `CODEOWNERS`
- `.github/workflows/pr-validation.yml`
