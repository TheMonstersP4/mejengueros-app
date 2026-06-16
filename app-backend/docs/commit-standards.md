# Commit Standards

Use Conventional Commits and keep each commit focused on one purpose.

## Format

```text
<type>(<scope>): <summary>
```

Examples:

```text
feat(auth): add Cognito token verifier
fix(users): handle missing Cognito email claim
docs(api): document DDD module layout
chore(terraform): add network module README
test(auth): cover invalid token flow
refactor(users): move Prisma mapping into repository
```

## Types

Use these types:

```text
feat
fix
docs
test
refactor
chore
build
ci
perf
style
revert
```

## Scopes

Use scopes that match the changed area:

```text
auth
users
health
api
infra
terraform
docs
deps
ci
prisma
logger
```

## Commit Size

Prefer one reason per commit.

Good split:

```text
feat(auth): add Cognito token verifier
feat(users): sync Cognito identity to local user
docs(api): document authentication flow
```

Avoid:

```text
feat: add auth, users, terraform, docs, formatting
```

## When To Split

Split commits when changes can be understood or reverted separately:

- Terraform module changes.
- API behavior changes.
- Prisma schema changes.
- Documentation updates.
- Formatting-only changes.
- Dependency upgrades.

Keep together when the files form one atomic change:

- A use case, its port, its adapter, and its tests.
- A Prisma schema change and the repository code that requires it.
- A Terraform module change and its README update.

## Summary Style

- Use imperative mood: `add`, `fix`, `move`, `remove`.
- Keep the summary under 72 characters when practical.
- Do not end the summary with a period.
- Be specific enough to understand the change from `git log`.

## Body

Add a body when the change needs context:

```text
feat(auth): add Cognito token verifier

The API validates Cognito-issued JWTs instead of Google or Microsoft tokens
directly. Cognito remains the identity broker for external providers.
```

## Before Committing

Run the checks that match the change:

```text
terraform fmt / validate for infra
lint / test for API code
prisma validate for schema changes
```

Never commit:

```text
.terraform/
*.tfstate
*.tfvars
.env
node_modules/
dist/
coverage/
.agents/
.skills/
```
