# Agent Standards

This file sets the rules for automated repository work.

## Workflow

- Read the existing structure before changing files.
- Keep Terraform modules small and focused.
- Do not commit local agent folders, temporary notes, or generated state.
- Keep examples useful, but never include real secrets.
- Keep changes grouped by purpose. Do not mix infrastructure, API behavior, formatting, and docs in one commit unless they are part of the same change.

## Defaults

- Public access stays off unless a feature needs it.
- Databases stay private by default.
- S3 buckets use encryption and block public access.
- Outputs that expose credentials or tokens must be marked `sensitive`.

## Infrastructure Changes

Use this order for Terraform changes:

```text
1. Add or update the module.
2. Wire it from infra/root.
3. Add outputs only when another system needs them.
4. Update the example tfvars.
5. Run fmt and validate.
```

## TypeScript Documentation

Follow `docs/tsdoc-standards.md` for NestJS and TypeScript code.

- Document exported contracts, use cases, ports, entities, and value objects.
- Do not add file headers everywhere.
- Do not add comments that repeat the code.
- Use `@remarks`, `@param`, `@returns`, `@throws`, and `@example` only when they add context.

## Application Design

Follow `docs/nestjs-ddd-structure.md` for NestJS, DDD, Prisma, Cognito, Fastify, and Pino structure.

- Keep domain code independent from NestJS, Prisma, AWS SDK, and HTTP.
- Put shared clients in `shared/infrastructure`.
- Put feature-specific repositories and adapters inside the feature module.
- Prefer use cases over placing business flow in controllers.
- Follow `docs/error-handling-standards.md` for error codes, base errors, logs, and Problem Details responses.
- Follow `docs/prisma-standards.md` for Prisma 7 config, generated client output, and adapter usage.

## Commits

Follow `docs/commit-standards.md`.

- Use Conventional Commits.
- Split unrelated changes into separate commits.
- Prefer small commits that can be reverted independently.
- Do not commit ignored local folders, real `.tfvars`, state files, secrets, or generated build output.

## Local Folders

The folders `.agents/` and `.skills/` are ignored by git. Keep private local instructions and experiments there.

The `skills/` folder is tracked. Use it for reusable project skills that should travel with the repository.
