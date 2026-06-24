---
name: mejengueros-pr-standards
description: "Trigger: Mejengueros PRs, branch naming, reviewers, GitHub PR creation. Extends the global branch-pr workflow with repository-local rules."
license: Apache-2.0
metadata:
  author: mejengueros-team
  version: "1.0"
---

## Activation Contract

Use this skill when work includes Mejengueros pull requests, branch naming, reviewer selection, or GitHub PR creation for this repository.

## Hard Rules

- This skill COMPLEMENTS the global `branch-pr` skill; it does not replace it.
- First follow the complete `branch-pr` flow for approved issues, `type:*` labels, PR body, Conventional Commit titles, and base validations.
- Do not create or overwrite a local skill named `branch-pr`.
- All branches must use the `type/<github-username>/<description>` format.
- Valid types are `feat|fix|chore|docs|style|refactor|perf|test|build|ci|revert`.
- Every PR title must keep the Conventional Commit format, and the summary after `:` must be written in English.
- Every PR body must be written in clear English.
- Every PR must request `@kevinah95` as reviewer.
- `CODEOWNERS` covers all files and will enforce this review when branch protection requires code owner reviews.

## Decision Gates

| Case | Rule |
| --- | --- |
| Base PR flow | Follow `branch-pr` first |
| New branch | Use `type/<github-username>/<description>` |
| PR title | Use the `type(scope): summary in English` format |
| PR body | Write it in clear English |
| Required reviewer | Request `@kevinah95` |
| Ownership automation | Rely on `CODEOWNERS` once branch protection requires code owner reviews |

## Execution Steps

1. Run the global `branch-pr` skill workflow first.
2. Create or validate the branch with the `type/<github-username>/<description>` pattern.
3. Verify that `type` belongs to the allowed set.
4. Write the PR title as `type(scope): English summary`, for example `chore(repo): standardize PR governance`.
5. Write the PR body in clear English.
6. When opening or editing the PR, request `@kevinah95` as reviewer.
7. When documenting examples, use real repository conventions such as `feat/ddgutierrezc/issue-48-complex-frontend`, `fix/ddgutierrezc/owner-onboarding`, and `chore/ddgutierrezc/readme-contribucion`.

## Output Contract

Report whether the PR follows the global `branch-pr` flow, whether the branch matches the local pattern, whether the title and body are in English, and whether `@kevinah95` was requested as reviewer.

## References

- `README.md`
- `CODEOWNERS`
- `.github/workflows/pr-validation.yml`
