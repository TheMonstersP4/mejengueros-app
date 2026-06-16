# DDD And SOLID Standards

Use these rules when adding NestJS modules or reviewing application code.

## DDD Rules

Model by business capability:

```text
modules/<bounded-context>/
  domain/
  application/
  infrastructure/
  interfaces/
```

Keep dependencies pointing inward:

```text
interfaces      -> application
infrastructure  -> application/domain
application     -> domain
domain          -> no framework dependencies
```

## Domain

Domain code contains business rules and should not import:

```text
@nestjs/*
@prisma/client
@aws-sdk/*
fastify
express
```

Use domain for:

- Entities.
- Value objects.
- Domain errors.
- Domain services.
- Repository interfaces.
- Domain events.

## Application

Application code coordinates use cases.

Use application for:

- Use cases.
- Commands and queries.
- Ports.
- Transaction boundaries.
- Application DTOs.

Avoid putting business flow in controllers or infrastructure adapters.

## Infrastructure

Infrastructure implements ports and integrates external systems.

Use infrastructure for:

- Prisma repositories.
- Cognito adapters.
- S3 adapters.
- Email providers.
- Mappers.

Keep reusable clients in `shared/infrastructure`, but keep feature-specific adapters in the feature module.

## Interfaces

Interfaces expose the application to the outside world.

Use interfaces for:

- Controllers.
- Guards.
- Request DTOs.
- Response DTOs.
- Presenters.
- HTTP filters and decorators.

Controllers should be thin. They validate input, call a use case, and return a response.

## SOLID Rules

Single Responsibility:

- One class should have one reason to change.
- A use case should perform one business action.
- A repository should handle persistence for one aggregate or model group.

Open/Closed:

- Depend on ports when behavior may have multiple implementations.
- Add new adapters instead of branching inside business code.

Liskov Substitution:

- Implement ports without surprising behavior.
- A fake repository in tests should behave like the real repository contract.

Interface Segregation:

- Keep ports small.
- Prefer `ITokenVerifierPort` and `IUserRepository` over large service interfaces.

Dependency Inversion:

- Application depends on abstractions.
- Infrastructure provides implementations.
- Domain does not depend on application or infrastructure.

## Practical Rules

- Start simple. Do not create abstractions before there is a reason.
- Add ports at application boundaries: database, auth provider, storage, email, queues.
- Keep mappers at infrastructure boundaries.
- Do not leak Prisma types into domain or application.
- Do not leak Cognito SDK responses into controllers or domain.
- Use tests to protect domain and application behavior first.

## TypeScript Naming

Use the `I` prefix for TypeScript interfaces.

Examples:

```text
IUserRepository
ITokenVerifierPort
IUserProfileOutput
ICreateUserInput
IWebSocketConnectionStore
```

Keep suffixes that explain the role. `IUserRepository` is clearer than `IUser`; `ITokenVerifierPort` is clearer than `ITokenVerifier`.
