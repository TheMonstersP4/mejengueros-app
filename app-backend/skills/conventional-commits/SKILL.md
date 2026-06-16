---
name: conventional-commits
description: Split repository changes into focused commits and write Conventional Commit messages. Use when Codex is asked to commit, prepare commits, review git diff/status, suggest commit messages, avoid mixing unrelated changes, or organize changes into feat/fix/docs/test/refactor/chore commits.
---

# Conventional Commits

Use this skill when preparing or reviewing commits.

## Workflow

1. Inspect `git status --short`.
2. Inspect the relevant diffs before deciding commit groups.
3. Group changes by purpose, not by file extension.
4. Keep unrelated infrastructure, API, documentation, formatting, and dependency changes in separate commits.
5. Propose or create Conventional Commit messages.
6. Never include ignored local files, secrets, Terraform state, `.env`, real `.tfvars`, build output, or dependency folders.

## Commit Format

```text
<type>(<scope>): <summary>
```

Use lowercase type and scope. Use imperative mood in the summary.

Good:

```text
feat(auth): add Cognito token verifier
docs(api): document DDD module layout
chore(terraform): add network module README
```

Avoid:

```text
feat: update stuff
feat: add auth, users, terraform, docs, and formatting
```

## Split Rules

Split commits when changes can be understood or reverted independently:

- Terraform module changes.
- API behavior changes.
- Prisma schema changes.
- Documentation updates.
- Formatting-only changes.
- Dependency upgrades.

Keep files together when they form one atomic change:

- A use case, its port, adapter, and tests.
- A Prisma schema change and the repository code that requires it.
- A Terraform module change and its README update.

## Reference

For the full local standard, read `references/commit-standards.md`.
