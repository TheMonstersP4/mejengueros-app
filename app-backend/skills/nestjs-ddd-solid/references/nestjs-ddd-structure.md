# NestJS DDD Structure Reference

Recommended base:

```text
api/src/
  config/
  shared/
    infrastructure/
      database/prisma.service.ts
      logger/app-logger.service.ts
  modules/
    auth/
      domain/
      application/
      infrastructure/
      interfaces/
    users/
      domain/
      application/
      infrastructure/
      interfaces/
    health/
  bootstrap/
```

Auth flow:

```text
Google/Microsoft -> Cognito Hosted UI -> Cognito JWT -> NestJS Guard -> Use Case -> Prisma/User
```

Feature modules own feature-specific repositories and adapters. Shared infrastructure owns reusable clients only.

TypeScript interfaces use the `I` prefix and keep role suffixes:

```text
IUserRepository
ITokenVerifierPort
IUserProfileOutput
```
