# NestJS DDD Structure

Proposed structure for a NestJS API using Fastify and Pino.

The goal is to keep business rules isolated from frameworks, databases, HTTP, and cloud services. NestJS is the delivery framework, not the center of the domain.

## Recommended Layout

```text
api/
  src/
    main.ts
    app.module.ts

    config/
      app.config.ts
      database.config.ts
      auth.config.ts
      logger.config.ts
      configuration.ts
      env.validation.ts

    shared/
      domain/
        errors/
        events/
        value-objects/
      application/
        ports/
        result.ts
      infrastructure/
        logger/
        persistence/
        crypto/
      interfaces/
        http/
          filters/
          interceptors/
          pipes/
          decorators/

    modules/
      auth/
        auth.module.ts
        domain/
          entities/
          value-objects/
          events/
          errors/
          repositories/
          services/
        application/
          use-cases/
          commands/
          queries/
          dto/
          ports/
        infrastructure/
          cognito/
          persistence/
          mappers/
        interfaces/
          http/
            controllers/
            dto/

      users/
        users.module.ts
        domain/
        application/
        infrastructure/
        interfaces/

      health/
        health.module.ts
        interfaces/
          http/
            health.controller.ts

    bootstrap/
      fastify.ts
      swagger.ts
      shutdown.ts
      validation.ts

  test/
    unit/
    integration/
    e2e/
```

## Layer Responsibilities

### Domain

Pure business code. It should not import NestJS, Prisma, TypeORM, Fastify, AWS SDK, or HTTP types.

Common contents:

- Entities
- Value objects
- Domain events
- Domain services
- Repository interfaces
- Domain errors

Example:

```text
modules/users/domain/
  entities/user.entity.ts
  value-objects/email.value-object.ts
  repositories/user.repository.ts
  errors/user-not-found.error.ts
```

### Application

Orchestrates use cases. It coordinates domain objects and calls ports, but does not know concrete infrastructure details.

Common contents:

- Use cases
- Commands and queries
- Application DTOs
- Ports for external dependencies
- Transaction boundaries

Example:

```text
modules/users/application/
  use-cases/create-user.use-case.ts
  use-cases/get-user-profile.use-case.ts
  ports/password-hasher.port.ts
  ports/user-session.port.ts
```

### Infrastructure

Adapters for external systems. This is where database clients, Cognito, S3, email, queues, and third-party SDKs live.

Common contents:

- Repository implementations
- ORM models
- AWS SDK clients
- Mappers between persistence and domain
- External service adapters

Example:

```text
modules/auth/infrastructure/
  cognito/cognito-auth-provider.ts
  cognito/cognito-token-verifier.ts
  mappers/cognito-user.mapper.ts
```

### Interfaces

Delivery layer. For this API, that mainly means HTTP controllers, request DTOs, response presenters, filters, and guards.

Common contents:

- Controllers
- Request DTOs
- Response DTOs
- Guards
- Route-level pipes
- Presenters

Example:

```text
modules/users/interfaces/http/
  controllers/users.controller.ts
  dto/create-user.request.ts
  dto/user.response.ts
```

## Shared vs Common

Use `shared/` only for code that is stable and genuinely reusable across modules.

Good candidates:

- `Email`
- `Result`
- `DomainEvent`
- `AppLogger`
- HTTP exception filter
- Validation pipe setup

Avoid turning `shared/` into a dumping ground. If code belongs to one business capability, keep it inside that module.

## Config

Keep config global and explicit.

Recommended files:

```text
config/
  configuration.ts       Loads all config sections.
  env.validation.ts      Validates process.env at startup.
  app.config.ts          Port, environment, app name.
  database.config.ts     PostgreSQL connection settings.
  auth.config.ts         Cognito, Google, Microsoft settings.
  logger.config.ts       Pino options.
```

Use `@nestjs/config` with schema validation. Fail fast when required env vars are missing.

## Logger

Use Pino through `nestjs-pino`.

Recommended pattern:

```text
shared/infrastructure/logger/
  app-logger.module.ts
  app-logger.service.ts
```

Use structured logs. Avoid passing raw request bodies, tokens, passwords, cookies, or OAuth secrets to logs.

Typical fields:

```text
requestId
userId
route
method
statusCode
durationMs
errorCode
```

The HTTP exception filter should log through `PinoLogger` so error logs use the same structured pipeline as request logs.

Avoid `console.*` in application code. Use framework/Pino logging from infrastructure or boundary layers.

## Fastify Bootstrap

Keep Fastify setup outside business modules.

```text
bootstrap/
  fastify.ts
  validation.ts
  swagger.ts
  shutdown.ts
```

`main.ts` should stay small:

```ts
async function bootstrap() {
  const app = await createFastifyApp();
  configureValidation(app);
  configureShutdown(app);
  await app.listen({ host: '0.0.0.0', port: config.port });
}
```

## Naming Rules

Use suffixes that explain the role:

```text
*.entity.ts
*.value-object.ts
*.repository.ts
*.use-case.ts
*.controller.ts
*.request.ts
*.response.ts
*.mapper.ts
*.port.ts
*.adapter.ts
*.module.ts
```

TypeScript interfaces use the `I` prefix and keep their role suffix:

```text
IUserRepository
ITokenVerifierPort
IUserProfileOutput
ICreateUserInput
```

Use cases should be named by intent:

```text
create-user.use-case.ts
authenticate-user.use-case.ts
refresh-session.use-case.ts
upload-profile-image.use-case.ts
```

## Dependency Direction

Allowed dependencies:

```text
interfaces      -> application
infrastructure  -> application/domain
application     -> domain
domain          -> nothing project-specific
```

Avoid:

```text
domain -> infrastructure
domain -> NestJS
application -> controllers
application -> ORM models
```

## Module Example

```text
modules/users/
  users.module.ts

  domain/
    entities/user.entity.ts
    value-objects/user-id.value-object.ts
    value-objects/email.value-object.ts
    repositories/user.repository.ts

  application/
    use-cases/create-user.use-case.ts
    use-cases/find-user-by-id.use-case.ts
    dto/create-user.input.ts
    dto/user.output.ts

  infrastructure/
    persistence/postgres-user.repository.ts
    persistence/user.orm-entity.ts
    mappers/user.mapper.ts

  interfaces/
    http/controllers/users.controller.ts
    http/dto/create-user.request.ts
    http/dto/user.response.ts
```

## Testing Layout

Recommended split:

```text
test/
  unit/
    modules/users/domain/
    modules/users/application/
  integration/
    modules/users/infrastructure/
  e2e/
    users.e2e-spec.ts
```

What to test:

- Domain: entities, value objects, domain services.
- Application: use cases with mocked ports.
- Infrastructure: repository implementations and external adapters.
- E2E: HTTP behavior through Fastify.

## Practical Starting Point

Start with these modules:

```text
modules/
  auth/
  users/
  health/
```

Add more modules only when the business concept is clear. Do not create folders for future features before they exist.

## Recommended Dependencies

Core:

```text
@nestjs/core
@nestjs/common
@nestjs/platform-fastify
@nestjs/config
nestjs-pino
pino
pino-pretty
class-validator
class-transformer
zod
```

AWS/Auth:

```text
aws-jwt-verify
@aws-sdk/client-cognito-identity-provider
@aws-sdk/client-s3
```

Database:

```text
prisma
@prisma/client
@prisma/adapter-pg
pg
```

Prisma is a practical default for PostgreSQL. Keep Prisma models and generated client usage in `infrastructure`, not in `domain`.
Use Prisma 7 style configuration: `schema.prisma` keeps the provider only, `prisma.config.ts` owns the CLI datasource URL, and runtime clients use `@prisma/adapter-pg`.

## Final Recommendation

Use modular DDD by feature:

```text
modules/<bounded-context>/
  domain/
  application/
  infrastructure/
  interfaces/
```

Keep global concerns in:

```text
config/
shared/
bootstrap/
```

This structure stays readable for a small API and still holds up when the system grows.

## Proposed Base For This Project

For this backend, start with:

```text
api/
  prisma/
    schema.prisma
    migrations/

  src/
    main.ts
    app.module.ts

    bootstrap/
      fastify.ts
      logger.ts
      validation.ts
      shutdown.ts

    config/
      configuration.ts
      env.validation.ts
      app.config.ts
      auth.config.ts
      database.config.ts
      logger.config.ts

    shared/
      domain/
        errors/domain.error.ts
        value-objects/email.value-object.ts
      application/
        result.ts
        ports/transaction-manager.port.ts
      infrastructure/
        database/
          prisma.module.ts
          prisma.service.ts
        logger/
          app-logger.module.ts
          app-logger.service.ts
      interfaces/
        http/
          filters/http-exception.filter.ts
          interceptors/request-context.interceptor.ts
          decorators/current-user.decorator.ts

    modules/
      health/
        health.module.ts
        interfaces/http/health.controller.ts

      auth/
        auth.module.ts
        domain/
          value-objects/auth-provider.value-object.ts
          errors/invalid-token.error.ts
        application/
          use-cases/get-current-user.use-case.ts
          ports/token-verifier.port.ts
          dto/authenticated-user.output.ts
        infrastructure/
          cognito/cognito-token-verifier.adapter.ts
          cognito/cognito-jwks.provider.ts
        interfaces/
          http/
            guards/cognito-auth.guard.ts
            controllers/auth.controller.ts

      users/
        users.module.ts
        domain/
          entities/user.entity.ts
          value-objects/user-id.value-object.ts
          repositories/user.repository.ts
        application/
          use-cases/sync-authenticated-user.use-case.ts
          use-cases/find-user-profile.use-case.ts
          dto/user-profile.output.ts
        infrastructure/
          persistence/prisma-user.repository.ts
          mappers/user.mapper.ts
        interfaces/
          http/
            controllers/users.controller.ts
            dto/user-profile.response.ts
```

## Login With Google And Microsoft

Use Cognito as the identity broker:

```text
Google login      -> Cognito hosted UI -> API receives Cognito JWT
Microsoft login   -> Cognito hosted UI -> API receives Cognito JWT
Email/password    -> Cognito hosted UI -> API receives Cognito JWT
```

The API should not validate Google or Microsoft tokens directly. It should validate Cognito-issued JWTs. Cognito handles the external providers and gives the backend one consistent token format.

Recommended flow:

```text
1. Frontend redirects user to Cognito hosted UI.
2. User chooses Google or Microsoft.
3. Cognito returns tokens to the frontend callback URL.
4. Frontend sends the Cognito access token or ID token to the API.
5. API guard verifies the token with `aws-jwt-verify`.
6. `sync-authenticated-user.use-case.ts` creates or updates the local user row.
```

Use `auth` for token verification and request identity. Use `users` for local application user data.

## Prisma Placement

Prisma should be shared infrastructure, not domain code.

```text
shared/infrastructure/database/
  prisma.module.ts
  prisma.service.ts
```

Generated client imports should come from:

```text
src/generated/prisma/client
```

Do not import Prisma Client or model types from `@prisma/client` in application code.

Feature-specific repositories stay inside each module:

```text
modules/users/infrastructure/persistence/prisma-user.repository.ts
```

The repository maps Prisma records to domain entities:

```text
Prisma model -> mapper -> User entity
User entity  -> mapper -> Prisma write input
```

Do not pass Prisma models into `domain` or `application` code.

## First Prisma Models

Start small:

```prisma
model User {
  id              String   @id @default(uuid())
  cognitoSub      String   @unique
  email           String   @unique
  name            String?
  pictureUrl      String?
  provider        String?
  createdAt       DateTime @default(now())
  updatedAt       DateTime @updatedAt
}
```

`cognitoSub` is the stable user identifier from Cognito. `provider` can store values such as `Google`, `Microsoft`, or `COGNITO`.

## Why Not Global `src/domain`, `src/application`, `src/infrastructure`

Both styles are valid. For this project, use feature modules because NestJS already organizes applications around modules.

Global layers:

```text
src/domain/users
src/application/users
src/infrastructure/users
```

Feature modules:

```text
src/modules/users/domain
src/modules/users/application
src/modules/users/infrastructure
```

The feature-module style keeps a business capability in one place. Shared infrastructure still lives globally under `shared/infrastructure`.

Use this rule:

```text
If it belongs to one feature, keep it in that feature module.
If several features use it, move only the reusable adapter/client to shared.
```

Examples:

```text
shared/infrastructure/database/prisma.service.ts
modules/users/infrastructure/persistence/prisma-user.repository.ts

shared/infrastructure/logger/app-logger.service.ts
modules/auth/infrastructure/cognito/cognito-token-verifier.adapter.ts

shared/infrastructure/storage/s3-client.provider.ts
modules/files/infrastructure/storage/s3-file-storage.adapter.ts
```
