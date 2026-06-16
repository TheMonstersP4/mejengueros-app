---
name: nestjs-ddd-solid
description: Build and review NestJS TypeScript APIs using DDD, SOLID, Fastify, Pino, Prisma, Cognito, Google login, and Microsoft login. Use when Codex creates modules, controllers, use cases, domain entities, repositories, Prisma adapters, auth guards, or reviews NestJS architecture.
---

# NestJS DDD SOLID

Use this skill for NestJS API implementation and architecture review.

## Architecture

Prefer modular DDD by feature:

```text
src/modules/<bounded-context>/
  domain/
  application/
  infrastructure/
  interfaces/
```

Keep shared technical clients under:

```text
src/shared/infrastructure/
```

Examples:

```text
shared/infrastructure/database/prisma.service.ts
shared/infrastructure/logger/app-logger.service.ts
modules/users/infrastructure/persistence/prisma-user.repository.ts
modules/auth/infrastructure/cognito/cognito-token-verifier.adapter.ts
```

## Dependency Direction

Allowed:

```text
interfaces      -> application
infrastructure  -> application/domain
application     -> domain
domain          -> no framework dependencies
```

Do not import NestJS, Prisma, AWS SDK, Fastify, or HTTP types into domain code.

## Auth

For Google and Microsoft login, use Cognito as the identity broker:

```text
Google/Microsoft -> Cognito Hosted UI -> Cognito JWT -> NestJS Guard
```

The API validates Cognito-issued JWTs with `aws-jwt-verify`. It should not validate Google or Microsoft tokens directly.

## Prisma

Keep Prisma in infrastructure:

```text
shared/infrastructure/database/prisma.service.ts
modules/<feature>/infrastructure/persistence/prisma-*.repository.ts
modules/<feature>/infrastructure/mappers/*.mapper.ts
```

Do not leak Prisma models into domain or application code.
Use Prisma 7 style setup: datasource URL in `prisma.config.ts`, generated client in `src/generated/prisma`, and `@prisma/adapter-pg` in runtime infrastructure.

## Errors

Use layered errors with a single HTTP boundary:

```text
src/shared/domain/errors/base.error.ts
src/shared/domain/errors/domain.error.ts
src/shared/application/errors/application.error.ts
src/shared/infrastructure/errors/infrastructure.error.ts
src/shared/interfaces/http/filters/problem-details-exception.filter.ts
```

Feature-specific errors stay inside the feature that owns the rule:

```text
src/modules/auth/domain/errors/invalid-token.error.ts
src/modules/users/domain/errors/user-not-found.error.ts
```

Layer rules:

- Domain errors represent business invariants and must not import NestJS, HTTP, Prisma, AWS SDK, or Fastify.
- Application errors represent use-case orchestration failures.
- Infrastructure errors wrap provider, database, queue, storage, and SDK failures.
- HTTP filters live in `shared/interfaces/http/filters` and translate errors to Problem Details.
- Controllers and guards throw typed errors; they do not format error responses.

## Logging

Use Pino for all NestJS HTTP logging.

```text
LoggerModule.forRoot(loggerConfig())
app.useLogger(app.get(Logger))
```

Rules:

- Do not use `console.log` in application code.
- Use structured log context objects.
- Redact authorization headers, cookies, tokens, OAuth secrets, and passwords.
- The global Problem Details filter logs with `PinoLogger`.
- Local development may use `pino-pretty`; production should emit JSON logs.

## Implementation Rules

- Controllers stay thin.
- Use cases own application flow.
- Ports define external dependencies.
- Infrastructure implements ports.
- Mappers translate between domain and persistence/provider models.
- Add abstractions only at real boundaries: database, auth provider, storage, email, queues.
- TypeScript interfaces use the `I` prefix: `ITokenVerifierPort`, `IUserRepository`, `IUserProfileOutput`.
- Keep role suffixes after the `I` prefix for clarity: `IPaymentGatewayPort`, `IUserRepository`, `ICreateUserInput`.
- Expected errors extend `BaseError` through the layer-specific base class: `DomainError`, `ApplicationError`, or `InfrastructureError`.
- Controllers do not catch expected errors; the global Problem Details filter maps errors to HTTP responses.
- Error codes live in `APP_ERROR_CODES` and must be stable, uppercase snake case, and safe for clients.
- Expected errors are logged internally and returned externally as RFC 9457 Problem Details.
- Logging uses Pino; avoid direct `console.*` calls.

## Reference

Read these only when needed:

- `references/nestjs-ddd-structure.md`
- `references/ddd-solid-standards.md`
- `references/error-handling-standards.md`
- `references/prisma-standards.md`
